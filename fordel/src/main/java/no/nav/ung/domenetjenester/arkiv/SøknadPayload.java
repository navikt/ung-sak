package no.nav.ung.domenetjenester.arkiv;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

import no.nav.k9.søknad.JsonUtils;

public class SøknadPayload {

    private final JsonNode payload;

    private final Søknadsformat format;

    public SøknadPayload(JsonNode payload, Søknadsformat format) {
        this.payload = payload;
        this.format = format;
    }

    public JsonNode getPayload() {
        return payload;
    }

    public Søknadsformat getFormat() {
        return format;
    }

    public String getPayloadAsString() {
        if (payload == null) {
            return null;
        }
        try {
            return JsonUtils.getObjectMapper().writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Ugyldig payload", e);
        }
    }

}
