package org.example.realtime;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.time.Instant;
import java.util.Optional;

/**
 * Représentation immuable d'un évènement temps réel reçu (ou à publier) sur
 * un topic Mercure.
 *
 * <p>Le {@link #data()} est un {@link JsonElement} Gson — laissé brut pour
 * que chaque service métier puisse l'interpréter à sa façon.</p>
 */
public final class RealtimeEvent {

    private final String topic;
    private final JsonElement data;
    private final Instant receivedAt;

    public RealtimeEvent(String topic, JsonElement data) {
        this(topic, data, Instant.now());
    }

    public RealtimeEvent(String topic, JsonElement data, Instant receivedAt) {
        this.topic = topic;
        this.data = data == null ? JsonNull.INSTANCE : data;
        this.receivedAt = receivedAt == null ? Instant.now() : receivedAt;
    }

    public String topic() {
        return topic;
    }

    public JsonElement data() {
        return data;
    }

    public Instant receivedAt() {
        return receivedAt;
    }

    /** Renvoie l'objet JSON racine si la donnée en est un. */
    public Optional<JsonObject> asObject() {
        if (data.isJsonObject()) {
            return Optional.of(data.getAsJsonObject());
        }
        return Optional.empty();
    }

    /**
     * Tente de récupérer un champ texte de l'objet racine.
     */
    public Optional<String> stringField(String key) {
        return asObject()
                .filter(o -> o.has(key) && !o.get(key).isJsonNull())
                .map(o -> o.get(key).getAsString());
    }

    /**
     * Construit un {@link RealtimeEvent} depuis un payload texte brut.
     * Si le payload n'est pas du JSON valide il est encapsulé sous forme
     * {@code {"raw": "..."}} pour ne jamais perdre l'information.
     */
    public static RealtimeEvent fromRawPayload(String topic, String payload) {
        JsonElement element;
        try {
            element = JsonParser.parseString(payload == null ? "" : payload);
        } catch (Exception e) {
            JsonObject wrapper = new JsonObject();
            wrapper.addProperty("raw", payload == null ? "" : payload);
            element = wrapper;
        }
        return new RealtimeEvent(topic, element);
    }

    @Override
    public String toString() {
        return "RealtimeEvent{topic=" + topic + ", data=" + data + "}";
    }
}
