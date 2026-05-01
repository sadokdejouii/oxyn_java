package org.example.windowshello;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Bridge PowerShell vers Windows Hello (sans JNA) :
 * Java exécute un script PS qui appelle UserConsentVerifier.
 */
public final class WindowsHelloBridge {

    private static final Gson GSON = new Gson();
    private static volatile Path extractedScriptPath;

    private WindowsHelloBridge() {
    }

    public static WindowsHelloResult verify(String message) {
        try {
            Path script = ensureScriptExtracted();
            List<String> cmd = new ArrayList<>();
            cmd.add("powershell");
            cmd.add("-NoProfile");
            cmd.add("-STA");
            cmd.add("-ExecutionPolicy");
            cmd.add("Bypass");
            cmd.add("-File");
            cmd.add(script.toAbsolutePath().toString());
            cmd.add("-Action");
            cmd.add("verify");
            if (message != null && !message.isBlank()) {
                cmd.add("-Message");
                cmd.add(message);
            }

            Process p = new ProcessBuilder(cmd)
                    .redirectErrorStream(true)
                    .start();

            String out = readAll(p.getInputStream()).trim();
            p.waitFor();

            if (out.isEmpty()) {
                return new WindowsHelloResult(false, null, "Unknown", "Error", "Aucune sortie PowerShell.");
            }
            WindowsHelloResult r = GSON.fromJson(out, WindowsHelloResult.class);
            return r != null ? r : new WindowsHelloResult(false, null, "Unknown", "Error", "Réponse PowerShell invalide.");
        } catch (Exception e) {
            return new WindowsHelloResult(false, null, "Unknown", "Error", e.getMessage() != null ? e.getMessage() : e.toString());
        }
    }

    private static Path ensureScriptExtracted() throws IOException {
        Path cached = extractedScriptPath;
        if (cached != null && Files.exists(cached)) {
            return cached;
        }
        synchronized (WindowsHelloBridge.class) {
            cached = extractedScriptPath;
            if (cached != null && Files.exists(cached)) {
                return cached;
            }
            try (InputStream in = WindowsHelloBridge.class.getResourceAsStream("/powershell/windows-hello-bridge.ps1")) {
                if (in == null) {
                    throw new IOException("Script PowerShell introuvable dans les ressources: /powershell/windows-hello-bridge.ps1");
                }
                Path tmp = Files.createTempFile("oxyn-windows-hello-", ".ps1");
                tmp.toFile().deleteOnExit();
                Files.writeString(tmp, readAll(in), StandardCharsets.UTF_8);
                extractedScriptPath = tmp;
                return tmp;
            }
        }
    }

    private static String readAll(InputStream in) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append('\n');
            }
        }
        return sb.toString();
    }
}

