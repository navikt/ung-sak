package no.nav.ung.kodeverk.historikk;

import com.fasterxml.jackson.annotation.JsonValue;
import no.nav.ung.kodeverk.api.Kodeverdi;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public enum HistorikkAktør implements Kodeverdi {

    BESLUTTER("BESL", "Beslutter"),
    SAKSBEHANDLER("SBH", "Saksbehandler"),
    SØKER("SOKER", "Søker"),
    ARBEIDSGIVER("ARBEIDSGIVER", "Arbeidsgiver"),
    VEDTAKSLØSNINGEN("VL", "Vedtaksløsningen"),
    UDEFINERT("-", "Ikke definert"),
    ;

    private static final Map<String, HistorikkAktør> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "HISTORIKK_AKTOER";

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

    private String navn;

    private String kode;

    private HistorikkAktør(String kode) {
        this.kode = kode;
    }

    private HistorikkAktør(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    public static HistorikkAktør  fraKode(final String kode)  {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent HistorikkAktør: " + kode);
        }
        return ad;
    }

    public static Map<String, HistorikkAktør> kodeMap() {
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

    public static void main(String[] args) {
        System.out.println(KODER.keySet());
    }
}
