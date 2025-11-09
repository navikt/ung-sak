package no.nav.ung.kodeverk.hjemmel;

import com.fasterxml.jackson.annotation.JsonValue;
import no.nav.ung.kodeverk.LegacyKodeverdiJsonValue;
import no.nav.ung.kodeverk.api.Kodeverdi;

import java.util.LinkedHashMap;
import java.util.Map;

@LegacyKodeverdiJsonValue // Serialiserast som kode string i default object mapper
public enum Hjemmel implements Kodeverdi {

    UNG_FORSKRIFT_PARAGRAF_1("UNG_FRSKRFT_1", "Forskrift om forsøk med ungdomsprogram og ungdomsprogramytelse § 1"),
    UNG_FORSKRIFT_PARAGRAF_2("UNG_FRSKRFT_2", "Forskrift om forsøk med ungdomsprogram og ungdomsprogramytelse § 2"),
    UNG_FORSKRIFT_PARAGRAF_3("UNG_FRSKRFT_3", "Forskrift om forsøk med ungdomsprogram og ungdomsprogramytelse § 3"),
    UNG_FORSKRIFT_PARAGRAF_4("UNG_FRSKRFT_4", "Forskrift om forsøk med ungdomsprogram og ungdomsprogramytelse § 4"),
    UNG_FORSKRIFT_PARAGRAF_6("UNG_FRSKRFT_6", "Forskrift om forsøk med ungdomsprogram og ungdomsprogramytelse § 6"),
    UNG_FORSKRIFT_PARAGRAF_7("UNG_FRSKRFT_7", "Forskrift om forsøk med ungdomsprogram og ungdomsprogramytelse § 7"),
    UNG_FORSKRIFT_PARAGRAF_8("UNG_FRSKRFT_8", "Forskrift om forsøk med ungdomsprogram og ungdomsprogramytelse § 8"),
    UNG_FORSKRIFT_PARAGRAF_9("UNG_FRSKRFT_9", "Forskrift om forsøk med ungdomsprogram og ungdomsprogramytelse § 9"),
    UNG_FORSKRIFT_PARAGRAF_10("UNG_FRSKRFT_10", "Forskrift om forsøk med ungdomsprogram og ungdomsprogramytelse § 10"),
    UNG_FORSKRIFT_PARAGRAF_11("UNG_FRSKRFT_11", "Forskrift om forsøk med ungdomsprogram og ungdomsprogramytelse § 11"),
    UNG_FORSKRIFT_PARAGRAF_12("UNG_FRSKRFT_12", "Forskrift om forsøk med ungdomsprogram og ungdomsprogramytelse § 12"),
    UNG_FORSKRIFT_PARAGRAF_14("UNG_FRSKRFT_14", "Forskrift om forsøk med ungdomsprogram og ungdomsprogramytelse § 14"),

    ARBEIDSMARKEDSLOVEN_PARAGRAF_12("AML_12", "Arbeidsmarkedsloven § 12"),
    ARBEIDSMARKEDSLOVEN_PARAGRAF_13("AML_13", "Arbeidsmarkedsloven § 13"),
    ARBEIDSMARKEDSLOVEN_PARAGRAF_17("AML_17", "Arbeidsmarkedsloven § 17"),

    FORVALTNINGSLOVEN_PARAGRAF_11("FVL_11", "Forvaltningsloven § 11"),
    FORVALTNINGSLOVEN_PARAGRAF_28("FVL_28", "Forvaltningsloven § 28"),
    FORVALTNINGSLOVEN_PARAGRAF_29("FVL_29", "Forvaltningsloven § 29"),
    FORVALTNINGSLOVEN_PARAGRAF_30("FVL_30", "Forvaltningsloven § 30"),
    FORVALTNINGSLOVEN_PARAGRAF_31("FVL_31", "Forvaltningsloven § 31"),
    FORVALTNINGSLOVEN_PARAGRAF_32("FVL_32", "Forvaltningsloven § 32"),
    FORVALTNINGSLOVEN_PARAGRAF_33("FVL_33", "Forvaltningsloven § 33"),
    FORVALTNINGSLOVEN_PARAGRAF_34("FVL_34", "Forvaltningsloven § 34"),
    FORVALTNINGSLOVEN_PARAGRAF_35("FVL_35", "Forvaltningsloven § 35"),

    FOLKETRYGDLOVEN_PARAGRAF_22_15("FTRL_22_15", "Folketrygdloven § 22-15"),
    FOLKETRYGDLOVEN_PARAGRAF_22_17("FTRL_22_17", "Folketrygdloven § 22-17"),
    FOLKETRYGDLOVEN_PARAGRAF_22_17_A("FTRL_22_17A", "Folketrygdloven § 22-17 a"),

    MANGLER("-", "MANGLER"); // Mangler etter kontrakt fra kabal-api hjemmel enum.

    private static final Map<String, Hjemmel> KODER = new LinkedHashMap<>();

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

    private String kode;
    private String navn;

    Hjemmel(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    public static Hjemmel fraKode(String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent Hjemmel: " + kode);
        }
        return ad;
    }


    @Override
    @JsonValue
    public String getKode() {
        return kode;
    }

    @Override
    public String getOffisiellKode() {
        return getKode();
    }

    @Override
    public String getKodeverk() {
        return "UNG_HJEMMEL";
    }

    @Override
    public String getNavn() {
        return navn;
    }
}
