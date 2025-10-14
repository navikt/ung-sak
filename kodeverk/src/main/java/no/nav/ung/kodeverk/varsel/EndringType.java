package no.nav.ung.kodeverk.varsel;

import com.fasterxml.jackson.annotation.JsonValue;
import no.nav.ung.kodeverk.LegacyKodeverdiJsonValue;
import no.nav.ung.kodeverk.api.Kodeverdi;

import java.util.LinkedHashMap;
import java.util.Map;

@LegacyKodeverdiJsonValue // Serialiserast som kode string i default object mapper
public enum EndringType implements Kodeverdi {

    ENDRET_INNTEKT("ENDRET_INNTEKT", "Endret registerinntekt"),
    ENDRET_STARTDATO("ENDRET_STARTDATO", "Endret startdato"),
    ENDRET_SLUTTDATO("ENDRET_SLUTTDATO", "Endret sluttdato"),

    ;

    @JsonValue
    private final String kode;
    private final String navn;


    private static final Map<String, EndringType> KODER = new LinkedHashMap<>();

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }


    EndringType(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    public static EndringType fraKode(String kode) {
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent EndringType: " + kode);
        }
        return ad;
    }

    @Override
    public String getKode() {
        return kode;
    }

    @Override
    public String getOffisiellKode() {
        return kode;
    }

    @Override
    public String getKodeverk() {
        return "ENDRING_TYPE";
    }

    @Override
    public String getNavn() {
        return navn;
    }


}
