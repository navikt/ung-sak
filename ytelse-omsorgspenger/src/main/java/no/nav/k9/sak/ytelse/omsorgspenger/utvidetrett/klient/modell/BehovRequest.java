package no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.klient.modell;

import java.util.EnumMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonValue;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.NONE, getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, isGetterVisibility = JsonAutoDetect.Visibility.NONE, creatorVisibility = JsonAutoDetect.Visibility.NONE)
public class BehovRequest {

    @JsonValue
    private Map<BehovType, RawJson> behov = new EnumMap<>(BehovType.class);

    public BehovRequest() {
    }

    public BehovRequest setBehov(Map<BehovType, RawJson> behov) {
        this.behov = behov;
        return this;
    }

    public Map<BehovType, RawJson> getBehov() {
        return behov;
    }

    @JsonAnySetter
    public void setBehov(String propertyKey, RawJson rawJson) {
        behov.put(BehovType.valueOf(propertyKey), rawJson);
    }
}
