package no.nav.ung.kodeverk.ytelse;

import com.fasterxml.jackson.annotation.JsonValue;
import no.nav.ung.kodeverk.LegacyKodeverdiJsonValue;
import no.nav.ung.kodeverk.api.Kodeverdi;

import java.util.LinkedHashMap;
import java.util.Map;

@LegacyKodeverdiJsonValue // Serialiserast som kode string i default object mapper
public enum KorrigertYtelseÅrsak implements Kodeverdi {

    KORRIGERING_AV_AVRUNDINGSFEIL("KORRIGERING_AV_AVRUNDINGSFEIL", "Korrigering av avrundingsfeil");
    private static final Map<String, KorrigertYtelseÅrsak> KODER = new LinkedHashMap<>();

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

    private String kode;
    private String navn;

    KorrigertYtelseÅrsak(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    public static KorrigertYtelseÅrsak fraKode(String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent KorrigertYtelseÅrsak: " + kode);
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
        return "KORRIGERT_YTELSE_AARSAK";
    }

    @Override
    public String getNavn() {
        return navn;
    }
}
