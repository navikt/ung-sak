package no.nav.ung.kodeverk.behandling;

import com.fasterxml.jackson.annotation.JsonValue;
import no.nav.ung.kodeverk.api.Kodeverdi;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public enum KonsekvensForYtelsen implements Kodeverdi{

    YTELSE_OPPHØRER("YTELSE_OPPHØRER", "Ytelsen opphører"),
    ENDRING_I_BEREGNING("ENDRING_I_BEREGNING", "Endring i beregning"),
    INGEN_ENDRING("INGEN_ENDRING", "Ingen endring"),
    UDEFINERT("-", "Udefinert"),

    ;

    private static final Map<String, KonsekvensForYtelsen> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "KONSEKVENS_FOR_YTELSEN";

    private String navn;

    private String kode;

    private KonsekvensForYtelsen(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    public static KonsekvensForYtelsen fraKode(final String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent KonsekvensForYtelsen: for input " + kode);
        }
        return ad;
    }

    public static Map<String, KonsekvensForYtelsen> kodeMap() {
        return Collections.unmodifiableMap(KODER);
    }

    @Override
    public String getNavn() {
        return navn;
    }

    @Override
    public String getKodeverk() {
        return KODEVERK;
    }

    @JsonValue
    @Override
    public String getKode() {
        return kode;
    }

    @Override
    public String getOffisiellKode() {
        return getKode();
    }

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }


}
