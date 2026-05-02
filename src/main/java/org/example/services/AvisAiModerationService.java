package org.example.services;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;

public final class AvisAiModerationService {

    private static final AvisAiModerationService INSTANCE = new AvisAiModerationService();
    private static final Gson GSON = new Gson();
    private static final double BLOCK_THRESHOLD = 0.65;
    private static final Pattern MULTI_SPACE = Pattern.compile("\\s+");
    private static final Pattern NON_ALNUM = Pattern.compile("[^a-z0-9\\s]");
    private static final Pattern REPEATED_CHAR = Pattern.compile("(.)\\1{3,}");
    private static final Object PYTHON_WORKER_LOCK = new Object();
    private static final ExecutorService PYTHON_IO_EXECUTOR = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "avis-python-worker-io");
        thread.setDaemon(true);
        return thread;
    });

    private static Process pythonWorkerProcess;
    private static BufferedReader pythonWorkerReader;
    private static BufferedWriter pythonWorkerWriter;

    private static final Set<String> BLOCKED_TERMS = Set.of(
            "connard", "connasse", "encule", "enculer", "fdp", "pute", "salope",
            "batard", "tapette", "pd", "nigger", "negro", "bougnoule", "kys"
    );

    private static final List<String> BLOCKED_PHRASES = List.of(
            "nique ta mere",
            "sale arabe",
            "sale noir",
            "sale juif",
            "sale race",
            "retourne dans ton pays",
            "retourne chez toi",
            "va mourir",
            "va crever",
            "je vais te tuer",
            "kill yourself"
    );

    private AvisAiModerationService() {
    }

    public static ModerationResult analyze(String comment) {
        return INSTANCE.analyzeInternal(comment);
    }

    public static void validateCommentOrThrow(String comment) {
        ModerationResult result = analyze(comment);
        if (result.blocked()) {
            throw new AvisModerationException(result);
        }
    }

    public static String normalizeAcceptedComment(String comment) {
        if (comment == null) {
            return "";
        }
        return MULTI_SPACE.matcher(comment.trim()).replaceAll(" ");
    }

    private ModerationResult analyzeInternal(String comment) {
        String cleaned = normalizeAcceptedComment(comment);
        if (cleaned.isEmpty()) {
            return new ModerationResult(cleaned, 0.0, false, "", "moderation-ai", "toxicity", List.of(), "");
        }

        ModerationResult pythonResult = tryAnalyzeWithPythonModel(cleaned);
        if (pythonResult != null) {
            return pythonResult;
        }

        return analyzeWithFallbackHeuristic(cleaned);
    }

    private ModerationResult tryAnalyzeWithPythonModel(String cleanedComment) {
        Path scriptPath = resolvePythonScript();
        if (scriptPath == null) {
            return null;
        }

        List<String> command = resolvePythonCommand(scriptPath, true);
        if (command == null || command.isEmpty()) {
            return null;
        }

        synchronized (PYTHON_WORKER_LOCK) {
            try {
                if (!ensurePythonWorker(command)) {
                    return null;
                }

                pythonWorkerWriter.write(GSON.toJson(new PythonModerationRequest(cleanedComment)));
                pythonWorkerWriter.newLine();
                pythonWorkerWriter.flush();

                String output = readLineWithTimeout(pythonWorkerReader, 30);
                if (output == null || output.isBlank()) {
                    stopPythonWorker();
                    return null;
                }

                PythonModerationPayload payload = GSON.fromJson(output, PythonModerationPayload.class);
                if (payload == null || payload.error != null) {
                    stopPythonWorker();
                    return null;
                }

                String category = payload.primaryCategory == null || payload.primaryCategory.isBlank()
                    ? "toxicity"
                    : payload.primaryCategory;
                String userMessage = payload.userMessage == null || payload.userMessage.isBlank()
                    ? blockedMessage(category)
                        : payload.userMessage;

                return new ModerationResult(
                        payload.cleanedComment == null || payload.cleanedComment.isBlank() ? cleanedComment : payload.cleanedComment,
                        payload.toxicityScore,
                        payload.blocked,
                    payload.blocked ? userMessage : "",
                    payload.provider,
                    category,
                    payload.reasons,
                    payload.recommendation
                );
            } catch (IOException | InterruptedException | JsonSyntaxException exception) {
                if (exception instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                stopPythonWorker();
                return null;
            }
        }
    }

    private ModerationResult analyzeWithFallbackHeuristic(String cleanedComment) {
        String normalized = normalizeForModel(cleanedComment);
        int blockedTermMatches = countTermMatches(normalized);
        int blockedPhraseMatches = countPhraseMatches(normalized);
        double uppercaseRatio = computeUppercaseRatio(cleanedComment);
        List<String> reasons = new ArrayList<>();
        String primaryCategory = "toxicity";

        double score = 0.0;

        if (blockedTermMatches > 0) {
            score += Math.min(0.72, blockedTermMatches * 0.36);
            primaryCategory = "insult";
            reasons.add("insultes ou langage humiliant");
        }

        if (blockedPhraseMatches > 0) {
            score += Math.min(0.95, blockedPhraseMatches * 0.58);
            primaryCategory = "hate";
            reasons.clear();
            reasons.add("attaque visant une identite, une origine ou une religion");
        }

        if ((blockedTermMatches > 0 || blockedPhraseMatches > 0) && uppercaseRatio >= 0.55) {
            score += 0.08;
        }

        if ((blockedTermMatches > 0 || blockedPhraseMatches > 0) && hasAggressivePunctuation(cleanedComment)) {
            score += 0.06;
        }

        if ((blockedTermMatches > 0 || blockedPhraseMatches > 0) && REPEATED_CHAR.matcher(normalized).find()) {
            score += 0.05;
        }

        score = Math.min(1.0, score);
        boolean blocked = score >= BLOCK_THRESHOLD;

        return new ModerationResult(
                cleanedComment,
                score,
                blocked,
            blocked ? blockedMessage(primaryCategory) : "",
            "java-fallback",
            primaryCategory,
            reasons,
            blocked ? recommendationForCategory(primaryCategory) : ""
        );
    }

    private boolean ensurePythonWorker(List<String> command) throws IOException, InterruptedException {
        if (pythonWorkerProcess != null && pythonWorkerProcess.isAlive()
                && pythonWorkerReader != null && pythonWorkerWriter != null) {
            return true;
        }

        stopPythonWorker();

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectError(ProcessBuilder.Redirect.DISCARD);
        pythonWorkerProcess = processBuilder.start();
        pythonWorkerReader = new BufferedReader(new InputStreamReader(pythonWorkerProcess.getInputStream(), StandardCharsets.UTF_8));
        pythonWorkerWriter = new BufferedWriter(new OutputStreamWriter(pythonWorkerProcess.getOutputStream(), StandardCharsets.UTF_8));

        String readyLine = readLineWithTimeout(pythonWorkerReader, 240);
        if (readyLine == null || readyLine.isBlank()) {
            stopPythonWorker();
            return false;
        }

        PythonWorkerReadyPayload readyPayload = GSON.fromJson(readyLine, PythonWorkerReadyPayload.class);
        if (readyPayload == null || readyPayload.error != null || !"ready".equalsIgnoreCase(readyPayload.status)) {
            stopPythonWorker();
            return false;
        }

        return true;
    }

    private String readLineWithTimeout(BufferedReader reader, int timeoutSeconds) throws IOException, InterruptedException {
        Future<String> future = PYTHON_IO_EXECUTOR.submit(reader::readLine);
        try {
            return future.get(timeoutSeconds, TimeUnit.SECONDS);
        } catch (TimeoutException exception) {
            future.cancel(true);
            return null;
        } catch (ExecutionException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof IOException ioException) {
                throw ioException;
            }
            throw new IOException("Impossible de lire la reponse du worker Python.", cause);
        }
    }

    private void stopPythonWorker() {
        closeQuietly(pythonWorkerWriter);
        closeQuietly(pythonWorkerReader);

        if (pythonWorkerProcess != null) {
            pythonWorkerProcess.destroy();
            pythonWorkerProcess = null;
        }

        pythonWorkerWriter = null;
        pythonWorkerReader = null;
    }

    private void closeQuietly(AutoCloseable closeable) {
        if (closeable == null) {
            return;
        }

        try {
            closeable.close();
        } catch (Exception ignored) {
            // Ignore cleanup failures.
        }
    }

    private List<String> resolvePythonCommand(Path scriptPath, boolean workerMode) {
        String envPython = System.getenv("OXYN_PYTHON_EXE");
        List<String> command = new ArrayList<>();
        if (envPython != null && !envPython.isBlank()) {
            command.add(envPython.trim());
            command.add(scriptPath.toString());
            if (workerMode) {
                command.add("--serve");
            }
            return command;
        }

        Path currentDir = Paths.get("").toAbsolutePath().normalize();
        List<Path> candidates = List.of(
                currentDir.resolve(".venv").resolve(windowsPythonRelativePath()),
                currentDir.resolve("..").normalize().resolve(".venv").resolve(windowsPythonRelativePath()),
                currentDir.resolve(".venv").resolve(unixPythonRelativePath()),
                currentDir.resolve("..").normalize().resolve(".venv").resolve(unixPythonRelativePath())
        );

        for (Path candidate : candidates) {
            if (Files.isRegularFile(candidate)) {
                command.add(candidate.toString());
                command.add(scriptPath.toString());
                if (workerMode) {
                    command.add("--serve");
                }
                return command;
            }
        }

        command.add("python");
        command.add(scriptPath.toString());
        if (workerMode) {
            command.add("--serve");
        }
        return command;
    }

    private String windowsPythonRelativePath() {
        return Paths.get("Scripts", "python.exe").toString();
    }

    private String unixPythonRelativePath() {
        return Paths.get("bin", "python").toString();
    }

    private Path resolvePythonScript() {
        Path currentDir = Paths.get("").toAbsolutePath().normalize();
        List<Path> candidates = List.of(
                currentDir.resolve(Paths.get("src", "main", "python", "avis_moderation_model.py")),
                currentDir.resolve(Paths.get("oxyn_java", "src", "main", "python", "avis_moderation_model.py")),
                currentDir.resolve(Paths.get("target", "classes", "python", "avis_moderation_model.py"))
        );

        for (Path candidate : candidates) {
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
        }

        return null;
    }

    private int countTermMatches(String normalized) {
        if (normalized.isBlank()) {
            return 0;
        }

        int matches = 0;
        for (String token : normalized.split(" ")) {
            if (BLOCKED_TERMS.contains(token)) {
                matches++;
            }
        }
        return matches;
    }

    private int countPhraseMatches(String normalized) {
        int matches = 0;
        for (String phrase : BLOCKED_PHRASES) {
            if (normalized.contains(phrase)) {
                matches++;
            }
        }
        return matches;
    }

    private String normalizeForModel(String comment) {
        String lowered = comment.toLowerCase(Locale.ROOT)
                .replace('0', 'o')
                .replace('1', 'i')
                .replace('3', 'e')
                .replace('4', 'a')
                .replace('5', 's')
                .replace('7', 't')
                .replace('@', 'a')
                .replace('$', 's');

        String ascii = Normalizer.normalize(lowered, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");

        String noSymbols = NON_ALNUM.matcher(ascii).replaceAll(" ");
        return MULTI_SPACE.matcher(noSymbols.trim()).replaceAll(" ");
    }

    private double computeUppercaseRatio(String text) {
        int letters = 0;
        int uppercase = 0;

        for (char current : text.toCharArray()) {
            if (Character.isLetter(current)) {
                letters++;
                if (Character.isUpperCase(current)) {
                    uppercase++;
                }
            }
        }

        return letters == 0 ? 0.0 : (double) uppercase / letters;
    }

    private boolean hasAggressivePunctuation(String text) {
        return text.contains("!!") || text.contains("??") || text.contains("?!") || text.contains("!?");
    }

    private String blockedMessage(String category) {
        return switch (category) {
            case "hate" -> "Votre commentaire a ete refuse car il contient un contenu haineux ou discriminatoire.";
            case "threat" -> "Votre commentaire a ete refuse car il contient une menace ou un appel a la violence.";
            case "insult" -> "Votre commentaire a ete refuse car il contient des insultes ou un langage humiliant.";
            default -> "Votre commentaire a ete refuse car il contient un contenu offensant, haineux ou inapproprie.";
        };
    }

    private String recommendationForCategory(String category) {
        return switch (category) {
            case "hate" -> "Reformulez votre avis sans attaque contre une origine, une religion ou une identite.";
            case "threat" -> "Supprimez toute menace ou appel a la violence et gardez une critique factuelle.";
            case "insult" -> "Expliquez le probleme de maniere precise et polie, sans insulte.";
            default -> "Decrivez les points a ameliorer avec des faits concrets et un ton respectueux.";
        };
    }

    public record ModerationResult(String cleanedComment,
                                   double toxicityScore,
                                   boolean blocked,
                                   String userMessage,
                                   String provider,
                                   String primaryCategory,
                                   List<String> reasons,
                                   String recommendation) {

        public ModerationResult {
            cleanedComment = cleanedComment == null ? "" : cleanedComment;
            userMessage = userMessage == null ? "" : userMessage;
            provider = provider == null || provider.isBlank() ? "moderation-ai" : provider;
            primaryCategory = primaryCategory == null || primaryCategory.isBlank() ? "toxicity" : primaryCategory;
            reasons = reasons == null ? List.of() : List.copyOf(reasons);
            recommendation = recommendation == null ? "" : recommendation;
        }

        public String displayCategory() {
            return switch (primaryCategory) {
                case "hate" -> "Haine / discrimination";
                case "threat" -> "Menace / violence";
                case "insult" -> "Insulte";
                default -> "Toxicite";
            };
        }

        public String displayProvider() {
            return provider.replace("citizenlab/distilbert-base-multilingual-cased-toxicity", "DistilBERT multilingue");
        }
    }

    private static final class PythonModerationPayload {
        private String cleanedComment;
        private double toxicityScore;
        private boolean blocked;
        private String userMessage;
        private String provider;
        private String primaryCategory;
        private List<String> reasons;
        private String recommendation;
        private String error;
    }

    private static final class PythonWorkerReadyPayload {
        private String status;
        private String provider;
        private String error;
    }

    private static final class PythonModerationRequest {
        private final String comment;

        private PythonModerationRequest(String comment) {
            this.comment = comment;
        }
    }
}