package no.nav.k9.sak.kontrakt.krav;

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
public enum ÅrsakTilVurdering {

    REVURDERER_BERØRT_PERIODE("REVURDERER_BERØRT_PERIODE"),
    ENDRING_FRA_BRUKER("ENDRING_FRA_BRUKER"),
    REVURDERER_ENDRING_FRA_ANNEN_PART("REVURDERER_ENDRING_FRA_ANNEN_PART"),
    FØRSTEGANGSVURDERING("FØRSTEGANGSVURDERING");

    private static final Map<String, ÅrsakTilVurdering> KODER = new LinkedHashMap<>();

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

    @JsonValue
    private final String kode;

    ÅrsakTilVurdering(String kode) {
        this.kode = kode;
    }

    @JsonCreator
    public static ÅrsakTilVurdering fraKode(String kode) {
        return KODER.get(kode);
    }
}
