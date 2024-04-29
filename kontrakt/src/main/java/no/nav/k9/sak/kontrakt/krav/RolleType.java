package no.nav.k9.sak.kontrakt.krav;

import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.k9.sak.kontrakt.kompletthet.Status;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public enum RolleType {
    BRUKER("BRUKER"),
    ARBEIDSGIVER("ARBEIDSGIVER");

    @JsonValue
    private final String kode;

    RolleType(String kode) {
        this.kode = kode;
    }

    private static final Map<String, RolleType> KODER = new LinkedHashMap<>();

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }


    @JsonCreator
    public static RolleType fraKode(String kode) {
        return KODER.get(kode);
    }

}

