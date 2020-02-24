package no.nav.k9.sak.kontrakt;

import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonValue;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public enum FeilType {
    BEHANDLING_ENDRET_FEIL("BEHANDLING_ENDRET_FEIL"),
    GENERELL_FEIL("GENERELL_FEIL"),
    MANGLER_TILGANG_FEIL("MANGLER_TILGANG_FEIL"),
    TOMT_RESULTAT_FEIL("TOMT_RESULTAT_FEIL");

    private static final Map<String, FeilType> KODER = new LinkedHashMap<>();

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

    @JsonValue
    private final String kode;

    FeilType(String navn) {
        this.kode = navn;
    }

    @JsonCreator
    public static FeilType fraKode(String kode) {
        return KODER.get(kode);
    }

    @Override
    public String toString() {
        return kode;
    }
}
