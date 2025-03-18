package no.nav.ung.kodeverk.kontroll;

import no.nav.ung.kodeverk.api.Kodeverdi;

import java.util.LinkedHashMap;
import java.util.Map;

public enum KontrollertInntektKilde implements Kodeverdi {

    BRUKER("BRUKER", "Bruker"),
    REGISTER("REGISTER", "Register"),
    SAKSBEHANDLER("SAKSBEHANDLER", "Saksbehandler");


    private static final Map<String, KontrollertInntektKilde> KODER = new LinkedHashMap<>();

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

    private String kode;
    private String navn;

    KontrollertInntektKilde(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    public static KontrollertInntektKilde fraKode(String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent KontrollertInntektKilde: " + kode);
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
        return "UNG_KONTROLLERT_INNTEKT_KILDE";
    }

    @Override
    public String getNavn() {
        return navn;
    }
}
