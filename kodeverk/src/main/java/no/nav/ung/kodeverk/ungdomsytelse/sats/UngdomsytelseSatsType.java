package no.nav.ung.kodeverk.ungdomsytelse.sats;

import java.util.LinkedHashMap;
import java.util.Map;

import no.nav.ung.kodeverk.api.Kodeverdi;

public enum UngdomsytelseSatsType implements Kodeverdi {

    LAV("LAV", "Lav"),
    HØY("HOY", "Høy");

    private static final Map<String, UngdomsytelseSatsType> KODER = new LinkedHashMap<>();

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

    private String kode;
    private String navn;

    UngdomsytelseSatsType(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    public static UngdomsytelseSatsType fraKode(String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent UngdomsytelseSatsType: " + kode);
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
        return "UNG_SATSTYPE";
    }

    @Override
    public String getNavn() {
        return navn;
    }
}
