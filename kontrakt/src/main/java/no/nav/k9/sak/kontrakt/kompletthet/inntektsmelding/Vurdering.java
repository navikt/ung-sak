package no.nav.k9.sak.kontrakt.kompletthet.inntektsmelding;

import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonValue;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public enum Vurdering {
    I_BRUK("I_BRUK"),
    ERSTATTET_AV_NYERE("ERSTATTET_AV_NYERE"),
    IKKE_RELEVANT("IKKE_RELEVANT"),
    MANGLER_DATO("MANGLER_DATO");

    private static final Map<String, Vurdering> KODER = new LinkedHashMap<>();

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

    @JsonValue
    private final String kode;

    private Vurdering(String kode) {
        this.kode = kode;
    }

    @JsonCreator
    public static Vurdering fraKode(String kode) {
        return KODER.get(kode);
    }

}
