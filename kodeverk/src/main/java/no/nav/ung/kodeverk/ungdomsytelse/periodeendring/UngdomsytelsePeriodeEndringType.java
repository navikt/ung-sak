package no.nav.ung.kodeverk.ungdomsytelse.periodeendring;

import no.nav.ung.kodeverk.api.Kodeverdi;

import java.util.LinkedHashMap;
import java.util.Map;

public enum UngdomsytelsePeriodeEndringType implements Kodeverdi {

    ENDRET_STARTDATO("ENDRET_START", "Endret startdato"),
    ENDRET_OPPHØRSDATO("ENDRET_OPPHOER", "Endret opphørsdatp");

    private static final Map<String, UngdomsytelsePeriodeEndringType> KODER = new LinkedHashMap<>();

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

    private String kode;
    private String navn;

    UngdomsytelsePeriodeEndringType(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    public static UngdomsytelsePeriodeEndringType fraKode(String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent PeriodeEndringType: " + kode);
        }
        return ad;
    }


    @Override
    public String getKode() {
        return kode;
    }

    @Override
    public String getOffisiellKode() {
        return getKode();
    }

    @Override
    public String getKodeverk() {
        return "UNG_PERIODE_ENDRING_TYPE";
    }

    @Override
    public String getNavn() {
        return navn;
    }
}
