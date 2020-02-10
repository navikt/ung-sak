package no.nav.k9.sak.kontrakt.mottak;

import java.util.Arrays;
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
public enum ForsendelseStatus {

    INNVILGET("INNVILGET"),
    AVSLÅTT("AVSLÅTT"),
    PÅGÅR("PÅGÅR"),
    PÅ_VENT("PÅ_VENT");

    private static final Map<String, ForsendelseStatus> KODER = new LinkedHashMap<>();

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

    @JsonValue
    private final String kode;

    ForsendelseStatus(String value) {
        this.kode = value;
    }

    public String getKode() {
        return kode;
    }
    
    /**
     * @return the Enum representation for the given string.
     * @throws IllegalArgumentException if unknown string.
     */
    @JsonCreator
    public static ForsendelseStatus fraKode(String kode) {
        if (kode == null) {
            return null;
        }
        return Arrays.stream(values())
            .filter(v -> v.kode.equals(kode))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("ugyldig verdi: " + kode));
    }
}
