package no.nav.ung.kodeverk;

import com.fasterxml.jackson.annotation.JsonValue;
import no.nav.ung.kodeverk.api.Kodeverdi;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public enum Fagsystem implements Kodeverdi {
    UNG_SAK("UNG_SAK", "Vedtaksløsning Ungdomsprogramytelse", "UNG_SAK"),
    K9SAK("K9SAK", "Vedtaksløsning K9 - Pleiepenger", "K9"),
    FPSAK("FPSAK", "Vedtaksløsning Foreldrepenger", "FS36"),
    TPS("TPS", "TPS", "FS03"),
    VLSP("VLSP", "Vedtaksløsning Sykepenger", "VLSP"),
    JOARK("JOARK", "Joark", "AS36"),
    INFOTRYGD("INFOTRYGD", "Infotrygd", "IT01"),
    ARENA("ARENA", "Arena", "AO01"),
    INNTEKT("INNTEKT", "INNTEKT", "FS28"),
    MEDL("MEDL", "MEDL", "FS18"),
    GOSYS("GOSYS", "Gosys", "FS22"),
    ENHETSREGISTERET("ENHETSREGISTERET", "Enhetsregisteret", "ER01"),
    AAREGISTERET("AAREGISTERET", "AAregisteret", "AR01"),

    /**
     * Alle kodeverk må ha en verdi, det kan ikke være null i databasen. Denne koden gjør samme nytten.
     */
    UDEFINERT("-", "Ikke definert", null),
    ;

    public static final String KODEVERK = "FAGSYSTEM";

    private static final Map<String, Fagsystem> KODER = new LinkedHashMap<>();

    private String navn;

    private String offisiellKode;

    private String kode;

    Fagsystem() {
        // Hibernate trenger den
    }

    private Fagsystem(String kode) {
        this.kode = kode;
    }

    private Fagsystem(String kode, String navn, String offisiellKode) {
        this.kode = kode;
        this.navn = navn;
        this.offisiellKode = offisiellKode;
    }

    public static Fagsystem fraKode(final String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent Fagsystem: for input " + kode);
        }
        return ad;
    }

    public static Map<String, Fagsystem> kodeMap() {
        return Collections.unmodifiableMap(KODER);
    }

    @Override
    public String getNavn() {
        return navn;
    }

    @Override
    public String getOffisiellKode() {
        return offisiellKode;
    }

    public static void main(String[] args) {
        System.out.println(KODER.keySet());
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

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

}
