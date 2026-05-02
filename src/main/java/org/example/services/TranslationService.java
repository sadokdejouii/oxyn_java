package org.example.services;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

/**
 * Service de traduction du texte d'un message vers l'arabe.
 *
 * <p>Stratégie :</p>
 * <ol>
 *   <li>Tente l'API <em>MyMemory</em> (gratuite, sans clé) avec un timeout
 *       court — c'est la traduction de référence quand la machine a un accès
 *       Internet.</li>
 *   <li>En cas d'échec (offline, timeout, quota), retombe sur un lexique
 *       local FR↔AR de phrases / mots usuels — mieux qu'un message d'erreur
 *       et suffisant pour démontrer la fonctionnalité.</li>
 * </ol>
 *
 * <p>Mémoïse les traductions pour éviter les appels répétés.</p>
 */
public final class TranslationService {

    private static final TranslationService INSTANCE = new TranslationService();

    public static TranslationService getInstance() {
        return INSTANCE;
    }

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .executor(Executors.newCachedThreadPool(r -> {
                Thread t = new Thread(r, "translation-svc");
                t.setDaemon(true);
                return t;
            }))
            .build();

    private final Map<String, String> cache = new ConcurrentHashMap<>();
    private final Map<String, String> offlineDict = buildOfflineDict();

    private TranslationService() {
    }

    /**
     * Traduit (asynchrone) {@code text} de FR → AR.
     */
    public CompletableFuture<String> translateToArabic(String text) {
        if (text == null || text.isBlank()) {
            return CompletableFuture.completedFuture("");
        }
        String key = text.trim();
        String cached = cache.get(key);
        if (cached != null) {
            return CompletableFuture.completedFuture(cached);
        }
        return CompletableFuture
                .supplyAsync(() -> callMyMemory(key))
                .exceptionally(ex -> null)
                .thenApply(remote -> {
                    String value = (remote != null && !remote.isBlank()) ? remote : translateOffline(key);
                    cache.put(key, value);
                    return value;
                });
    }

    /**
     * Variante synchrone (utile pour des cas où on a déjà un thread non-UI).
     */
    public String translateToArabicBlocking(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        try {
            return translateToArabic(text).get();
        } catch (Exception e) {
            return translateOffline(text);
        }
    }

    private String callMyMemory(String text) {
        try {
            String url = "https://api.mymemory.translated.net/get?q="
                    + URLEncoder.encode(text, StandardCharsets.UTF_8)
                    + "&langpair=fr|ar";
            HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(4))
                    .header("Accept", "application/json")
                    .GET()
                    .build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (resp.statusCode() / 100 != 2) {
                return null;
            }
            JsonObject root = JsonParser.parseString(resp.body()).getAsJsonObject();
            if (!root.has("responseData")) {
                return null;
            }
            JsonObject data = root.getAsJsonObject("responseData");
            if (!data.has("translatedText")) {
                return null;
            }
            String t = data.get("translatedText").getAsString();
            return t == null || t.isBlank() ? null : t;
        } catch (Exception e) {
            return null;
        }
    }

    private String translateOffline(String text) {
        // Petit moteur de remplacement par phrase puis par mot.
        String key = text.toLowerCase().trim();
        if (offlineDict.containsKey(key)) {
            return offlineDict.get(key);
        }
        // Mot-à-mot — produit une approximation utile en démo.
        StringBuilder out = new StringBuilder();
        boolean any = false;
        for (String token : text.split("\\s+")) {
            String norm = token.toLowerCase().replaceAll("[\\p{Punct}]", "");
            String tr = offlineDict.get(norm);
            if (tr != null) {
                out.append(tr).append(' ');
                any = true;
            } else {
                out.append(token).append(' ');
            }
        }
        String result = out.toString().trim();
        if (!any || result.isEmpty()) {
            return "(الترجمة غير متوفرة) " + text;
        }
        return result;
    }

    private static Map<String, String> buildOfflineDict() {
        Map<String, String> m = new LinkedHashMap<>();
        // Phrases courantes
        m.put("bonjour", "مرحبا");
        m.put("salut", "أهلا");
        m.put("bonsoir", "مساء الخير");
        m.put("comment ça va", "كيف حالك");
        m.put("comment vas-tu", "كيف حالك");
        m.put("ça va", "بخير");
        m.put("merci", "شكرا");
        m.put("merci beaucoup", "شكرا جزيلا");
        m.put("s'il vous plaît", "من فضلك");
        m.put("s'il te plaît", "من فضلك");
        m.put("oui", "نعم");
        m.put("non", "لا");
        m.put("d'accord", "موافق");
        m.put("ok", "حسنا");
        m.put("au revoir", "وداعا");
        m.put("à bientôt", "إلى اللقاء");
        m.put("bonne nuit", "تصبح على خير");
        m.put("excusez-moi", "اعذرني");
        m.put("je ne sais pas", "لا أعرف");
        m.put("je comprends", "فهمت");
        m.put("aujourd'hui", "اليوم");
        m.put("demain", "غدا");
        m.put("hier", "أمس");
        m.put("séance", "حصة");
        m.put("rendez-vous", "موعد");
        m.put("planning", "جدول");
        m.put("objectif", "هدف");
        m.put("entraînement", "تدريب");
        m.put("encadrant", "مدرب");
        m.put("client", "زبون");
        m.put("message", "رسالة");
        m.put("conversation", "محادثة");
        // Mots usuels
        m.put("je", "أنا");
        m.put("tu", "أنت");
        m.put("vous", "أنتم");
        m.put("nous", "نحن");
        m.put("est", "هو");
        m.put("suis", "أنا");
        m.put("avoir", "يملك");
        m.put("être", "يكون");
        m.put("le", "الـ");
        m.put("la", "الـ");
        m.put("les", "الـ");
        m.put("un", "واحد");
        m.put("une", "واحدة");
        m.put("et", "و");
        m.put("ou", "أو");
        m.put("avec", "مع");
        m.put("pour", "لـ");
        m.put("dans", "في");
        m.put("sur", "على");
        m.put("très", "جدا");
        m.put("bien", "جيد");
        m.put("mal", "سيء");
        m.put("eau", "ماء");
        m.put("café", "قهوة");
        m.put("petit", "صغير");
        m.put("grand", "كبير");
        m.put("nouveau", "جديد");
        m.put("ancien", "قديم");
        m.put("temps", "وقت");
        m.put("jour", "يوم");
        m.put("semaine", "أسبوع");
        m.put("mois", "شهر");
        m.put("année", "سنة");
        return m;
    }
}
