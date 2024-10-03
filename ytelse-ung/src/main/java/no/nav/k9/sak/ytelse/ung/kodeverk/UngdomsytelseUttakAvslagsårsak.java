package no.nav.k9.sak.ytelse.ung.kodeverk;

import java.util.LinkedHashMap;
import java.util.Map;

import no.nav.k9.kodeverk.api.Kodeverdi;

public enum UngdomsytelseUttakAvslagsårsak implements Kodeverdi {

    IKKE_NOK_DAGER("IKKE_NOK_DAGER", "Ikke nok dager");


    private static final Map<String, UngdomsytelseUttakAvslagsårsak> KODER = new LinkedHashMap<>();

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

    private String kode;
    private String navn;

    UngdomsytelseUttakAvslagsårsak(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    public static UngdomsytelseUttakAvslagsårsak fraKode(String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent UngdomsytelseUttakAvslagsårsak: " + kode);
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
        return "UNG_UTTAK_AVSLAGSÅRSAK";
    }

    @Override
    public String getNavn() {
        return navn;
    }
}
