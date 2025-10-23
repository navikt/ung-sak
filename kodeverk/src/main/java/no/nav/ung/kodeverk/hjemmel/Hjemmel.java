package no.nav.ung.kodeverk.hjemmel;

import com.fasterxml.jackson.annotation.JsonValue;
import no.nav.ung.kodeverk.LegacyKodeverdiJsonValue;
import no.nav.ung.kodeverk.api.Kodeverdi;

import java.util.LinkedHashMap;
import java.util.Map;

@LegacyKodeverdiJsonValue // Serialiserast som kode string i default object mapper
public enum Hjemmel implements Kodeverdi {

    UNG_FORSKRIFT_PARAGRAF_11("UNG_FRSKRFT_11", "Forskrift om forsøk med ungdomsprogram og ungdomsprogramytelse § 11"),
    UNG_FORSKRIFT_PARAGRAF_9("UNG_FRSKRFT_9", "Forskrift om forsøk med ungdomsprogram og ungdomsprogramytelse § 9"),
    UNG_FORSKRIFT_PARAGRAF_6("UNG_FRSKRFT_6", "Forskrift om forsøk med ungdomsprogram og ungdomsprogramytelse § 6"),


    FL_VEDTAK_SOM_KAN_PÅKLAGES("28", "Forvaltningsloven § 28"),
    FL_OVERSITTING_AV_KLAGEFRIST("31", "Forvaltningsloven § 31"),
    FL_ADRESSAT_FORM_OG_INNHOLD("32", "Forvaltningsloven § 32"),
    FL_SAKSFORBEREDELSE_I_KLAGESAK("33", "Forvaltningsloven § 33"),
    FTRL_KLAGE_ANKE_TRYGDESAKER("21-12", "Folketrygdloven § 21-12"),


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
