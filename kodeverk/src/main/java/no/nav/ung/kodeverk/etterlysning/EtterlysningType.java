package no.nav.ung.kodeverk.etterlysning;

import com.fasterxml.jackson.annotation.JsonValue;
import no.nav.ung.kodeverk.api.Kodeverdi;

import java.util.LinkedHashMap;
import java.util.Map;

public enum EtterlysningType implements Kodeverdi {

    UTTALELSE_KONTROLL_INNTEKT("UTTALELSE_KONTROLL_INNTEKT", "Uttalelse kontroll av inntekt"),
    UTTALELSE_ENDRET_STARTDATO("UTTALELSE_ENDRET_STARTDATO", "Uttalelse endret startdato"),
    UTTALELSE_ENDRET_SLUTTDATO("UTTALELSE_ENDRET_SLUTTDATO", "Uttalelse endret sluttdato"),
    ;

    @JsonValue
    private final String kode;
    private final String navn;


    private static final Map<String, EtterlysningType> KODER = new LinkedHashMap<>();

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }


    EtterlysningType(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    public static EtterlysningType fraKode(String kode) {
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent EtterlysningType: " + kode);
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
        return "ETTERLYSNING_TYPE";
    }

    @Override
    public String getNavn() {
        return navn;
    }


}
