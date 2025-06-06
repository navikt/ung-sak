package no.nav.ung.kodeverk.opptjening;

import com.fasterxml.jackson.annotation.JsonValue;
import no.nav.ung.kodeverk.api.Kodeverdi;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public enum OpptjeningAktivitetKlassifisering implements Kodeverdi {

    BEKREFTET_GODKJENT("BEKREFTET_GODKJENT", "Bekreftet godkjent"),
    BEKREFTET_AVVIST("BEKREFTET_AVVIST", "Bekreftet avvist"),
    ANTATT_GODKJENT("ANTATT_GODKJENT", "Antatt godkjent"),
    MELLOMLIGGENDE_PERIODE("MELLOMLIGGENDE_PERIODE", "Mellomliggende periode"),
    UDEFINERT("-", "UDEFINERT"),
    ;

    private static final Map<String, OpptjeningAktivitetKlassifisering> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "OPPTJENING_AKTIVITET_KLASSIFISERING";

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

    private String navn;

    private String kode;

    private OpptjeningAktivitetKlassifisering(String kode) {
        this.kode = kode;
    }

    private OpptjeningAktivitetKlassifisering(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    public static OpptjeningAktivitetKlassifisering  fraKode(final String kode)  {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent OpptjeningAktivitetKlassifisering: " + kode);
        }
        return ad;
    }

    public static Map<String, OpptjeningAktivitetKlassifisering> kodeMap() {
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
