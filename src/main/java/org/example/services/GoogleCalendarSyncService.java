package org.example.services;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.example.entities.Evenement;
import org.example.entities.InscriptionEvenement;
import org.example.utils.MyDataBase;

import java.awt.Desktop;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class GoogleCalendarSyncService {

    private static final String USER_TOKEN_TABLE = "google_calendar_user_tokens";
    private static final String USER_LINK_TABLE = "google_calendar_event_links";
    private static final String ADMIN_TOKEN_TABLE = "google_calendar_admin_tokens";
    private static final String ADMIN_LINK_TABLE = "google_calendar_admin_event_links";
    private static final String CALENDAR_SCOPE = "https://www.googleapis.com/auth/calendar.events";
    private static final DateTimeFormatter ISO_OFFSET = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private final Connection con;
    private final HttpClient httpClient;
    private final Gson gson;

    public GoogleCalendarSyncService() {
        this.con = MyDataBase.getInstance().getConnection();
        this.httpClient = HttpClient.newHttpClient();
        this.gson = new Gson();
        ensureTables();
    }

    public SyncResult syncRegistration(Evenement event, InscriptionEvenement inscription) {
        if (event == null || inscription == null || inscription.getId() <= 0 || inscription.getIdUser() <= 0) {
            return SyncResult.withWarning("Inscription enregistrée, mais la synchronisation Google Calendar a été ignorée.");
        }

        try {
            OAuthClientConfig config = loadClientConfig();
            TokenRecord token = ensureAuthorizedToken(inscription.getIdUser(), config);
            String googleEventId = createGoogleCalendarEvent(token.accessToken(), event);

            try {
                upsertEventLink(inscription, "primary", googleEventId, "synced");
            } catch (SQLException sqlException) {
                deleteGoogleCalendarEvent(token.accessToken(), "primary", googleEventId);
                throw sqlException;
            }

            return SyncResult.ok();
        } catch (Exception exception) {
            return SyncResult.withWarning(buildRegistrationWarning(exception));
        }
    }

    public SyncResult removeRegistrationSync(int localUserId, int inscriptionId) {
        if (localUserId <= 0 || inscriptionId <= 0) {
            return SyncResult.ok();
        }

        EventLinkRecord link;
        try {
            link = findEventLink(inscriptionId);
        } catch (SQLException exception) {
            return SyncResult.withWarning("Désinscription enregistrée, mais le lien Google Calendar n'a pas pu être vérifié.");
        }

        if (link == null || link.googleEventId() == null || link.googleEventId().isBlank()) {
            deleteEventLinkQuietly(inscriptionId);
            return SyncResult.ok();
        }

        try {
            OAuthClientConfig config = loadClientConfig();
            TokenRecord token = ensureAuthorizedToken(localUserId, config);
            deleteGoogleCalendarEvent(token.accessToken(), link.calendarId(), link.googleEventId());
            deleteEventLink(inscriptionId);
            return SyncResult.ok();
        } catch (Exception exception) {
            deleteEventLinkQuietly(inscriptionId);
            return SyncResult.withWarning(buildRemovalWarning(exception));
        }
    }

    private void ensureTables() {
        // User tables (already created by schema)
        // Admin tables (already created by schema)
        // No-op here, schema migration is handled by SQL
    }

    // ================= ADMIN SYNC METHODS =================

    public SyncResult syncAdminEvent(Evenement event, String adminEmail) {
        if (event == null || adminEmail == null || adminEmail.isBlank() || event.getId() <= 0) {
            return SyncResult.withWarning("Événement enregistré, mais la synchronisation Google Calendar admin a été ignorée.");
        }
        try {
            OAuthClientConfig config = loadClientConfig();
            AdminTokenRecord token = ensureAuthorizedAdminToken(adminEmail, config);
            AdminEventLinkRecord link = findAdminEventLink(event.getId());
            String googleEventId;
            if (link != null && link.googleEventId() != null && !link.googleEventId().isBlank()) {
                // Update existing event
                updateGoogleCalendarEvent(token.accessToken(), link.googleEventId(), event);
                googleEventId = link.googleEventId();
            } else {
                // Create new event
                googleEventId = createGoogleCalendarEvent(token.accessToken(), event);
                try {
                    upsertAdminEventLink(event.getId(), adminEmail, "primary", googleEventId, "synced");
                } catch (SQLException sqlException) {
                    deleteGoogleCalendarEvent(token.accessToken(), "primary", googleEventId);
                    throw sqlException;
                }
            }
            return SyncResult.ok();
        } catch (Exception exception) {
            return SyncResult.withWarning("Ajout admin Google Calendar échoué: " + sanitizeMessage(exception));
        }
    }

    // Update an existing Google Calendar event by ID.
    private void updateGoogleCalendarEvent(String accessToken, String googleEventId, Evenement event) throws Exception {
        JsonObject payload = new JsonObject();
        payload.addProperty("summary", safe(event.getTitre(), "Événement OXYN"));
        payload.addProperty("description", buildEventDescription(event));
        payload.addProperty("location", buildLocation(event));

        JsonObject start = new JsonObject();
        start.addProperty("dateTime", toOffsetDateTime(event.getDateDebut()));
        start.addProperty("timeZone", ZoneId.systemDefault().getId());
        payload.add("start", start);

        JsonObject end = new JsonObject();
        end.addProperty("dateTime", toOffsetDateTime(resolveEndDate(event)));
        end.addProperty("timeZone", ZoneId.systemDefault().getId());
        payload.add("end", end);

        JsonObject reminders = new JsonObject();
        reminders.addProperty("useDefault", true);
        payload.add("reminders", reminders);

        String url = "https://www.googleapis.com/calendar/v3/calendars/primary/events/"
                + URLEncoder.encode(googleEventId, StandardCharsets.UTF_8);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(gson.toJson(payload), StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        ensureSuccess(response, "Impossible de mettre à jour l'événement Google Calendar.");
    }

    public SyncResult removeAdminEventSync(int eventId, String adminEmail) {
        if (eventId <= 0 || adminEmail == null || adminEmail.isBlank()) {
            return SyncResult.ok();
        }
        AdminEventLinkRecord link;
        try {
            link = findAdminEventLink(eventId);
        } catch (SQLException exception) {
            return SyncResult.withWarning("Suppression admin Google Calendar: lien introuvable.");
        }
        if (link == null || link.googleEventId() == null || link.googleEventId().isBlank()) {
            deleteAdminEventLinkQuietly(eventId);
            return SyncResult.ok();
        }
        try {
            OAuthClientConfig config = loadClientConfig();
            AdminTokenRecord token = ensureAuthorizedAdminToken(adminEmail, config);
            deleteGoogleCalendarEvent(token.accessToken(), link.calendarId(), link.googleEventId());
            deleteAdminEventLink(eventId);
            return SyncResult.ok();
        } catch (Exception exception) {
            deleteAdminEventLinkQuietly(eventId);
            return SyncResult.withWarning("Suppression admin Google Calendar échouée: " + sanitizeMessage(exception));
        }
    }

    // ========== ADMIN TOKEN MANAGEMENT ==========
    private AdminTokenRecord ensureAuthorizedAdminToken(String adminEmail, OAuthClientConfig config) throws Exception {
        AdminTokenRecord current = findAdminToken(adminEmail);
        if (current == null) {
            return authorizeAdmin(adminEmail, config);
        }
        if (!current.isExpiredSoon()) {
            return current;
        }
        if (current.refreshToken() == null || current.refreshToken().isBlank()) {
            return authorizeAdmin(adminEmail, config);
        }
        try {
            return refreshAdminToken(adminEmail, config, current);
        } catch (IOException exception) {
            return authorizeAdmin(adminEmail, config);
        }
    }

    private AdminTokenRecord authorizeAdmin(String adminEmail, OAuthClientConfig config) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        CompletableFuture<AuthorizationResponse> authFuture = new CompletableFuture<>();
        String state = UUID.randomUUID().toString();
        int port = server.getAddress().getPort();
        String redirectUri = "http://localhost:" + port + "/oauth2/callback";
        server.createContext("/oauth2/callback", exchange -> handleOAuthCallback(exchange, state, authFuture));
        server.start();
        try {
            openAuthorizationPage(buildAuthorizationUrl(config, redirectUri, state));
            AuthorizationResponse response = authFuture.get(180, TimeUnit.SECONDS);
            if (response.error() != null && !response.error().isBlank()) {
                throw new IOException("Autorisation Google refusée ou interrompue.");
            }
            if (response.code() == null || response.code().isBlank()) {
                throw new IOException("Code d'autorisation Google introuvable.");
            }
            return exchangeAdminAuthorizationCode(adminEmail, config, response.code(), redirectUri);
        } finally {
            server.stop(0);
        }
    }

    private AdminTokenRecord exchangeAdminAuthorizationCode(String adminEmail, OAuthClientConfig config, String code, String redirectUri) throws Exception {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("code", code);
        body.put("client_id", config.clientId());
        body.put("client_secret", config.clientSecret());
        body.put("redirect_uri", redirectUri);
        body.put("grant_type", "authorization_code");
        JsonObject json = postForm(config.tokenUri(), body);
        AdminTokenRecord token = toAdminTokenRecord(adminEmail, json, null);
        upsertAdminToken(token);
        return token;
    }

    private AdminTokenRecord refreshAdminToken(String adminEmail, OAuthClientConfig config, AdminTokenRecord existing) throws Exception {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("client_id", config.clientId());
        body.put("client_secret", config.clientSecret());
        body.put("refresh_token", existing.refreshToken());
        body.put("grant_type", "refresh_token");
        JsonObject json = postForm(config.tokenUri(), body);
        AdminTokenRecord refreshed = toAdminTokenRecord(adminEmail, json, existing);
        upsertAdminToken(refreshed);
        return refreshed;
    }

    private AdminTokenRecord findAdminToken(String adminEmail) throws SQLException {
        String sql = "SELECT access_token_admin_token, refresh_token_admin_token, access_token_expires_at_admin_token, "
                + "scope_admin_token, token_type_admin_token FROM " + ADMIN_TOKEN_TABLE + " WHERE admin_email = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, adminEmail);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                Timestamp expiresAt = rs.getTimestamp("access_token_expires_at_admin_token");
                return new AdminTokenRecord(
                        adminEmail,
                        rs.getString("access_token_admin_token"),
                        rs.getString("refresh_token_admin_token"),
                        expiresAt == null ? null : expiresAt.toInstant(),
                        rs.getString("scope_admin_token"),
                        rs.getString("token_type_admin_token")
                );
            }
        }
    }

    private void upsertAdminToken(AdminTokenRecord token) throws SQLException {
        String sql = "INSERT INTO " + ADMIN_TOKEN_TABLE + " ("
                + "admin_email, access_token_admin_token, refresh_token_admin_token, "
                + "access_token_expires_at_admin_token, scope_admin_token, token_type_admin_token"
                + ") VALUES (?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE "
                + "access_token_admin_token = VALUES(access_token_admin_token), "
                + "refresh_token_admin_token = VALUES(refresh_token_admin_token), "
                + "access_token_expires_at_admin_token = VALUES(access_token_expires_at_admin_token), "
                + "scope_admin_token = VALUES(scope_admin_token), "
                + "token_type_admin_token = VALUES(token_type_admin_token)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, token.adminEmail());
            ps.setString(2, token.accessToken());
            ps.setString(3, token.refreshToken());
            if (token.expiresAt() == null) {
                ps.setTimestamp(4, null);
            } else {
                ps.setTimestamp(4, Timestamp.from(token.expiresAt()));
            }
            ps.setString(5, token.scope());
            ps.setString(6, token.tokenType());
            ps.executeUpdate();
        }
    }

    // ========== ADMIN EVENT LINK MANAGEMENT ==========
    private AdminEventLinkRecord findAdminEventLink(int eventId) throws SQLException {
        String sql = "SELECT google_calendar_id, google_event_id, sync_status_admin_link FROM " + ADMIN_LINK_TABLE + " WHERE id_evenement_admin_link = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, eventId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return new AdminEventLinkRecord(
                        rs.getString("google_calendar_id"),
                        rs.getString("google_event_id"),
                        rs.getString("sync_status_admin_link")
                );
            }
        }
    }

    private void upsertAdminEventLink(int eventId, String adminEmail, String calendarId, String googleEventId, String status) throws SQLException {
        String sql = "INSERT INTO " + ADMIN_LINK_TABLE + " ("
                + "id_evenement_admin_link, admin_email, google_calendar_id, google_event_id, sync_status_admin_link"
                + ") VALUES (?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE "
                + "admin_email = VALUES(admin_email), "
                + "google_calendar_id = VALUES(google_calendar_id), "
                + "google_event_id = VALUES(google_event_id), "
                + "sync_status_admin_link = VALUES(sync_status_admin_link)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, eventId);
            ps.setString(2, adminEmail);
            ps.setString(3, calendarId);
            ps.setString(4, googleEventId);
            ps.setString(5, status);
            ps.executeUpdate();
        }
    }

    private void deleteAdminEventLink(int eventId) throws SQLException {
        String sql = "DELETE FROM " + ADMIN_LINK_TABLE + " WHERE id_evenement_admin_link = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, eventId);
            ps.executeUpdate();
        }
    }

    private void deleteAdminEventLinkQuietly(int eventId) {
        try {
            deleteAdminEventLink(eventId);
        } catch (SQLException ignored) {
        }
    }

    private AdminTokenRecord toAdminTokenRecord(String adminEmail, JsonObject json, AdminTokenRecord fallback) {
        String accessToken = getRequiredString(json, "access_token");
        String refreshToken = getOptionalString(json, "refresh_token", fallback == null ? null : fallback.refreshToken());
        long expiresIn = json.has("expires_in") && !json.get("expires_in").isJsonNull()
                ? json.get("expires_in").getAsLong()
                : 3600L;
        Instant expiresAt = Instant.now().plusSeconds(Math.max(60L, expiresIn - 60L));
        String scope = getOptionalString(json, "scope", fallback == null ? CALENDAR_SCOPE : fallback.scope());
        String tokenType = getOptionalString(json, "token_type", fallback == null ? "Bearer" : fallback.tokenType());
        return new AdminTokenRecord(adminEmail, accessToken, refreshToken, expiresAt, scope, tokenType);
    }

    private record AdminTokenRecord(String adminEmail, String accessToken, String refreshToken, Instant expiresAt, String scope, String tokenType) {
        private boolean isExpiredSoon() {
            return expiresAt == null || Instant.now().plusSeconds(60).isAfter(expiresAt);
        }
    }

    private record AdminEventLinkRecord(String calendarId, String googleEventId, String syncStatus) {
    }

    private TokenRecord ensureAuthorizedToken(int localUserId, OAuthClientConfig config) throws Exception {
        TokenRecord current = findToken(localUserId);

        if (current == null) {
            return authorizeUser(localUserId, config);
        }

        if (!current.isExpiredSoon()) {
            return current;
        }

        if (current.refreshToken() == null || current.refreshToken().isBlank()) {
            return authorizeUser(localUserId, config);
        }

        try {
            return refreshToken(localUserId, config, current);
        } catch (IOException exception) {
            return authorizeUser(localUserId, config);
        }
    }

    private TokenRecord authorizeUser(int localUserId, OAuthClientConfig config) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        CompletableFuture<AuthorizationResponse> authFuture = new CompletableFuture<>();
        String state = UUID.randomUUID().toString();
        int port = server.getAddress().getPort();
        String redirectUri = "http://localhost:" + port + "/oauth2/callback";

        server.createContext("/oauth2/callback", exchange -> handleOAuthCallback(exchange, state, authFuture));
        server.start();

        try {
            openAuthorizationPage(buildAuthorizationUrl(config, redirectUri, state));
            AuthorizationResponse response = authFuture.get(180, TimeUnit.SECONDS);
            if (response.error() != null && !response.error().isBlank()) {
                throw new IOException("Autorisation Google refusée ou interrompue.");
            }
            if (response.code() == null || response.code().isBlank()) {
                throw new IOException("Code d'autorisation Google introuvable.");
            }
            return exchangeAuthorizationCode(localUserId, config, response.code(), redirectUri);
        } finally {
            server.stop(0);
        }
    }

    private void handleOAuthCallback(HttpExchange exchange,
                                     String expectedState,
                                     CompletableFuture<AuthorizationResponse> authFuture) throws IOException {
        Map<String, String> params = splitQuery(exchange.getRequestURI().getRawQuery());
        String state = params.get("state");
        String error = params.get("error");
        String code = params.get("code");

        String html;
        if (error != null && !error.isBlank()) {
            authFuture.complete(new AuthorizationResponse(null, error));
            html = successPage("Connexion Google annulée", "Retournez dans OXYN pour continuer.");
        } else if (!expectedState.equals(state)) {
            authFuture.complete(new AuthorizationResponse(null, "invalid_state"));
            html = successPage("Connexion Google invalide", "La validation a échoué. Retournez dans OXYN.");
        } else {
            authFuture.complete(new AuthorizationResponse(code, null));
            html = successPage("Google Calendar connecté", "Vous pouvez fermer cet onglet et revenir dans OXYN.");
        }

        byte[] body = html.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        exchange.sendResponseHeaders(200, body.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(body);
        }
    }

    private String buildAuthorizationUrl(OAuthClientConfig config, String redirectUri, String state) {
        Map<String, String> query = new LinkedHashMap<>();
        query.put("client_id", config.clientId());
        query.put("redirect_uri", redirectUri);
        query.put("response_type", "code");
        query.put("scope", CALENDAR_SCOPE);
        query.put("access_type", "offline");
        query.put("prompt", "consent");
        query.put("state", state);

        return config.authUri() + "?" + buildFormBody(query);
    }

    private void openAuthorizationPage(String authorizationUrl) throws IOException {
        if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            throw new IOException("Impossible d'ouvrir le navigateur pour la connexion Google.");
        }
        Desktop.getDesktop().browse(URI.create(authorizationUrl));
    }

    private TokenRecord exchangeAuthorizationCode(int localUserId,
                                                  OAuthClientConfig config,
                                                  String code,
                                                  String redirectUri) throws Exception {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("code", code);
        body.put("client_id", config.clientId());
        body.put("client_secret", config.clientSecret());
        body.put("redirect_uri", redirectUri);
        body.put("grant_type", "authorization_code");

        JsonObject json = postForm(config.tokenUri(), body);
        TokenRecord token = toTokenRecord(localUserId, json, null);
        upsertToken(token);
        return token;
    }

    private TokenRecord refreshToken(int localUserId,
                                     OAuthClientConfig config,
                                     TokenRecord existing) throws Exception {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("client_id", config.clientId());
        body.put("client_secret", config.clientSecret());
        body.put("refresh_token", existing.refreshToken());
        body.put("grant_type", "refresh_token");

        JsonObject json = postForm(config.tokenUri(), body);
        TokenRecord refreshed = toTokenRecord(localUserId, json, existing);
        upsertToken(refreshed);
        return refreshed;
    }

    private String createGoogleCalendarEvent(String accessToken, Evenement event) throws Exception {
        JsonObject payload = new JsonObject();
        payload.addProperty("summary", safe(event.getTitre(), "Événement OXYN"));
        payload.addProperty("description", buildEventDescription(event));
        payload.addProperty("location", buildLocation(event));

        JsonObject start = new JsonObject();
        start.addProperty("dateTime", toOffsetDateTime(event.getDateDebut()));
        start.addProperty("timeZone", ZoneId.systemDefault().getId());
        payload.add("start", start);

        JsonObject end = new JsonObject();
        end.addProperty("dateTime", toOffsetDateTime(resolveEndDate(event)));
        end.addProperty("timeZone", ZoneId.systemDefault().getId());
        payload.add("end", end);

        JsonObject reminders = new JsonObject();
        reminders.addProperty("useDefault", true);
        payload.add("reminders", reminders);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://www.googleapis.com/calendar/v3/calendars/primary/events"))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(payload), StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        ensureSuccess(response, "Impossible de créer l'événement Google Calendar.");

        JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
        if (!json.has("id") || json.get("id").isJsonNull()) {
            throw new IOException("Réponse Google Calendar invalide après création d'événement.");
        }
        return json.get("id").getAsString();
    }

    private void deleteGoogleCalendarEvent(String accessToken, String calendarId, String googleEventId) throws Exception {
        String url = "https://www.googleapis.com/calendar/v3/calendars/"
                + URLEncoder.encode(safe(calendarId, "primary"), StandardCharsets.UTF_8)
                + "/events/"
                + URLEncoder.encode(googleEventId, StandardCharsets.UTF_8);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + accessToken)
                .DELETE()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() == 404 || response.statusCode() == 410) {
            return;
        }
        ensureSuccess(response, "Impossible de supprimer l'événement Google Calendar.");
    }

    private JsonObject postForm(String url, Map<String, String> body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(buildFormBody(body), StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        ensureSuccess(response, "Impossible de communiquer avec Google OAuth.");
        return JsonParser.parseString(response.body()).getAsJsonObject();
    }

    private void ensureSuccess(HttpResponse<String> response, String fallbackMessage) throws IOException {
        int status = response.statusCode();
        if (status >= 200 && status < 300) {
            return;
        }

        String message = fallbackMessage;
        if (response.body() != null && !response.body().isBlank()) {
            try {
                JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
                if (json.has("error_description") && !json.get("error_description").isJsonNull()) {
                    message = json.get("error_description").getAsString();
                } else if (json.has("error") && json.get("error").isJsonObject()) {
                    JsonObject error = json.getAsJsonObject("error");
                    if (error.has("message") && !error.get("message").isJsonNull()) {
                        message = error.get("message").getAsString();
                    }
                }
            } catch (Exception ignored) {
            }
        }

        throw new IOException(message + " (HTTP " + status + ")");
    }

    private OAuthClientConfig loadClientConfig() throws IOException {
        Path path = findClientSecretPath();
        if (path == null) {
            throw new IOException("Fichier OAuth Google introuvable. Placez le JSON client_secret dans la racine du projet ou dans le dossier secrets.");
        }

        String raw = Files.readString(path, StandardCharsets.UTF_8);
        JsonObject root = JsonParser.parseString(raw).getAsJsonObject();
        JsonObject installed = root.has("installed") ? root.getAsJsonObject("installed") : null;
        if (installed == null) {
            throw new IOException("Le fichier OAuth Google ne contient pas de section installed.");
        }

        String clientId = getRequiredString(installed, "client_id");
        String clientSecret = getRequiredString(installed, "client_secret");
        String authUri = getOptionalString(installed, "auth_uri", "https://accounts.google.com/o/oauth2/auth");
        String tokenUri = getOptionalString(installed, "token_uri", "https://oauth2.googleapis.com/token");

        String redirectUri = "http://localhost";
        if (installed.has("redirect_uris") && installed.get("redirect_uris").isJsonArray()) {
            JsonArray redirectUris = installed.getAsJsonArray("redirect_uris");
            if (!redirectUris.isEmpty() && !redirectUris.get(0).isJsonNull()) {
                redirectUri = redirectUris.get(0).getAsString();
            }
        }

        return new OAuthClientConfig(clientId, clientSecret, authUri, tokenUri, redirectUri);
    }

    private Path findClientSecretPath() throws IOException {
        Path[] directCandidates = new Path[] {
                Paths.get("google-oauth-client.json"),
                Paths.get("secrets", "google-oauth-client.json"),
            Paths.get("secrets", "google-oauth-client-local.json"),
            Paths.get("oxyn_java", "google-oauth-client.json"),
            Paths.get("oxyn_java", "secrets", "google-oauth-client.json"),
            Paths.get("oxyn_java", "secrets", "google-oauth-client-local.json")
        };

        for (Path candidate : directCandidates) {
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get("."), "client_secret_*.json")) {
            for (Path candidate : stream) {
                if (Files.isRegularFile(candidate)) {
                    return candidate;
                }
            }
        }

        Path secretsDir = Paths.get("secrets");
        if (Files.isDirectory(secretsDir)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(secretsDir, "client_secret_*.json")) {
                for (Path candidate : stream) {
                    if (Files.isRegularFile(candidate)) {
                        return candidate;
                    }
                }
            }
        }

        Path oxynJavaDir = Paths.get("oxyn_java");
        if (Files.isDirectory(oxynJavaDir)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(oxynJavaDir, "client_secret_*.json")) {
                for (Path candidate : stream) {
                    if (Files.isRegularFile(candidate)) {
                        return candidate;
                    }
                }
            }

            Path oxynSecretsDir = oxynJavaDir.resolve("secrets");
            if (Files.isDirectory(oxynSecretsDir)) {
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(oxynSecretsDir, "client_secret_*.json")) {
                    for (Path candidate : stream) {
                        if (Files.isRegularFile(candidate)) {
                            return candidate;
                        }
                    }
                }
            }
        }

        return null;
    }

    private TokenRecord findToken(int localUserId) throws SQLException {
        String sql = "SELECT access_token_google_token, refresh_token_google_token, access_token_expires_at_google_token, "
            + "scope_google_token, token_type_google_token FROM " + USER_TOKEN_TABLE + " WHERE id_user_google_token = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, localUserId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }

                Timestamp expiresAt = rs.getTimestamp("access_token_expires_at_google_token");
                return new TokenRecord(
                        localUserId,
                        rs.getString("access_token_google_token"),
                        rs.getString("refresh_token_google_token"),
                        expiresAt == null ? null : expiresAt.toInstant(),
                        rs.getString("scope_google_token"),
                        rs.getString("token_type_google_token")
                );
            }
        }
    }

    private void upsertToken(TokenRecord token) throws SQLException {
        String sql = "INSERT INTO " + USER_TOKEN_TABLE + " ("
                + "id_user_google_token, access_token_google_token, refresh_token_google_token, "
                + "access_token_expires_at_google_token, scope_google_token, token_type_google_token"
                + ") VALUES (?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE "
                + "access_token_google_token = VALUES(access_token_google_token), "
                + "refresh_token_google_token = VALUES(refresh_token_google_token), "
                + "access_token_expires_at_google_token = VALUES(access_token_expires_at_google_token), "
                + "scope_google_token = VALUES(scope_google_token), "
                + "token_type_google_token = VALUES(token_type_google_token)";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, token.localUserId());
            ps.setString(2, token.accessToken());
            ps.setString(3, token.refreshToken());
            if (token.expiresAt() == null) {
                ps.setTimestamp(4, null);
            } else {
                ps.setTimestamp(4, Timestamp.from(token.expiresAt()));
            }
            ps.setString(5, token.scope());
            ps.setString(6, token.tokenType());
            ps.executeUpdate();
        }
    }

    private EventLinkRecord findEventLink(int inscriptionId) throws SQLException {
        String sql = "SELECT google_calendar_id, google_event_id, sync_status_google_link FROM " + USER_LINK_TABLE + " WHERE id_inscription_google_link = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, inscriptionId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return new EventLinkRecord(
                        rs.getString("google_calendar_id"),
                        rs.getString("google_event_id"),
                        rs.getString("sync_status_google_link")
                );
            }
        }
    }

    private void upsertEventLink(InscriptionEvenement inscription,
                                 String calendarId,
                                 String googleEventId,
                                 String status) throws SQLException {
        String sql = "INSERT INTO " + USER_LINK_TABLE + " ("
                + "id_inscription_google_link, id_user_google_link, id_evenement_google_link, google_calendar_id, google_event_id, sync_status_google_link"
                + ") VALUES (?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE "
                + "id_user_google_link = VALUES(id_user_google_link), "
                + "id_evenement_google_link = VALUES(id_evenement_google_link), "
                + "google_calendar_id = VALUES(google_calendar_id), "
                + "google_event_id = VALUES(google_event_id), "
                + "sync_status_google_link = VALUES(sync_status_google_link)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, inscription.getId());
            ps.setInt(2, inscription.getIdUser());
            ps.setInt(3, inscription.getIdEvenement());
            ps.setString(4, calendarId);
            ps.setString(5, googleEventId);
            ps.setString(6, status);
            ps.executeUpdate();
        }
    }

    private void deleteEventLink(int inscriptionId) throws SQLException {
        String sql = "DELETE FROM " + USER_LINK_TABLE + " WHERE id_inscription_google_link = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, inscriptionId);
            ps.executeUpdate();
        }
    }

    private void deleteEventLinkQuietly(int inscriptionId) {
        try {
            deleteEventLink(inscriptionId);
        } catch (SQLException ignored) {
        }
    }

    private TokenRecord toTokenRecord(int localUserId, JsonObject json, TokenRecord fallback) {
        String accessToken = getRequiredString(json, "access_token");
        String refreshToken = getOptionalString(json, "refresh_token", fallback == null ? null : fallback.refreshToken());
        long expiresIn = json.has("expires_in") && !json.get("expires_in").isJsonNull()
                ? json.get("expires_in").getAsLong()
                : 3600L;
        Instant expiresAt = Instant.now().plusSeconds(Math.max(60L, expiresIn - 60L));
        String scope = getOptionalString(json, "scope", fallback == null ? CALENDAR_SCOPE : fallback.scope());
        String tokenType = getOptionalString(json, "token_type", fallback == null ? "Bearer" : fallback.tokenType());
        return new TokenRecord(localUserId, accessToken, refreshToken, expiresAt, scope, tokenType);
    }

    private String buildRegistrationWarning(Exception exception) {
        return "Inscription enregistrée, mais l'ajout à Google Calendar a échoué. " + sanitizeMessage(exception);
    }

    private String buildRemovalWarning(Exception exception) {
        return "Désinscription enregistrée, mais la suppression depuis Google Calendar a échoué. " + sanitizeMessage(exception);
    }

    private String sanitizeMessage(Exception exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return "Vérifiez votre connexion Google puis réessayez.";
        }
        return message;
    }

    private String buildEventDescription(Evenement event) {
        String description = safe(event.getDescription(), "Aucune description fournie.");
        return description + "\n\nAjouté automatiquement depuis OXYN.";
    }

    private String buildLocation(Evenement event) {
        String lieu = event.getLieu() == null ? "" : event.getLieu().trim();
        String ville = event.getVille() == null ? "" : event.getVille().trim();
        if (lieu.isBlank()) {
            return ville;
        }
        if (ville.isBlank()) {
            return lieu;
        }
        return lieu + ", " + ville;
    }

    private Date resolveEndDate(Evenement event) {
        if (event.getDateFin() != null) {
            return event.getDateFin();
        }
        Date start = event.getDateDebut() == null ? new Date() : event.getDateDebut();
        return new Date(start.getTime() + TimeUnit.HOURS.toMillis(1));
    }

    private String toOffsetDateTime(Date date) {
        Date safeDate = date == null ? new Date() : date;
        return safeDate.toInstant().atZone(ZoneId.systemDefault()).toOffsetDateTime().format(ISO_OFFSET);
    }

    private String buildFormBody(Map<String, String> values) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : values.entrySet()) {
            if (sb.length() > 0) {
                sb.append('&');
            }
            sb.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8));
            sb.append('=');
            sb.append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
        }
        return sb.toString();
    }

    private Map<String, String> splitQuery(String rawQuery) {
        Map<String, String> params = new LinkedHashMap<>();
        if (rawQuery == null || rawQuery.isBlank()) {
            return params;
        }

        String[] pairs = rawQuery.split("&");
        for (String pair : pairs) {
            if (pair == null || pair.isBlank()) {
                continue;
            }

            int idx = pair.indexOf('=');
            String key = idx >= 0 ? pair.substring(0, idx) : pair;
            String value = idx >= 0 ? pair.substring(idx + 1) : "";
            params.put(urlDecode(key), urlDecode(value));
        }
        return params;
    }

    private String urlDecode(String value) {
        return java.net.URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private String successPage(String title, String message) {
        return "<html><head><meta charset=\"utf-8\"></head><body style=\"font-family:Segoe UI,Arial,sans-serif;padding:32px;background:#f4fbff;color:#153b59;\">"
                + "<h2>" + escapeHtml(title) + "</h2><p>" + escapeHtml(message) + "</p></body></html>";
    }

    private String escapeHtml(String value) {
        return safe(value, "")
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private String getRequiredString(JsonObject object, String member) {
        if (!object.has(member) || object.get(member).isJsonNull()) {
            throw new IllegalStateException("Champ Google OAuth manquant: " + member);
        }
        return object.get(member).getAsString();
    }

    private String getOptionalString(JsonObject object, String member, String fallback) {
        if (!object.has(member) || object.get(member).isJsonNull()) {
            return fallback;
        }
        String value = object.get(member).getAsString();
        return value == null || value.isBlank() ? fallback : value;
    }

    private String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private record OAuthClientConfig(String clientId,
                                     String clientSecret,
                                     String authUri,
                                     String tokenUri,
                                     String redirectUri) {
    }

    private record AuthorizationResponse(String code, String error) {
    }

    private record TokenRecord(int localUserId,
                               String accessToken,
                               String refreshToken,
                               Instant expiresAt,
                               String scope,
                               String tokenType) {
        private boolean isExpiredSoon() {
            return expiresAt == null || Instant.now().plusSeconds(60).isAfter(expiresAt);
        }
    }

    private record EventLinkRecord(String calendarId, String googleEventId, String syncStatus) {
    }

    public record SyncResult(boolean success, String message) {
        public static SyncResult ok() {
            return new SyncResult(true, null);
        }

        public static SyncResult withWarning(String message) {
            return new SyncResult(false, message);
        }

        public boolean hasMessage() {
            return message != null && !message.isBlank();
        }
    }
}