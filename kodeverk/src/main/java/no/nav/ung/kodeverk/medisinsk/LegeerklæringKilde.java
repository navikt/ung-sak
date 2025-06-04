package no.nav.ung.kodeverk.medisinsk;

import com.fasterxml.jackson.annotation.JsonValue;
import no.nav.ung.kodeverk.api.Kodeverdi;

import java.util.LinkedHashMap;
import java.util.Map;

public enum LegeerklæringKilde implements Kodeverdi {
    /**
     * Alle kodeverk må ha en verdi, det kan ikke være null i databasen. Denne koden gjør samme nytten.
     */
    SYKEHUSLEGE("SYKEHUSLEGE", "Sykehuslege", null),
    SPESIALISTHELSETJENESTE("SPESIALISTHELSETJENESTE", "Spesialisthelsetjenesten", null),
    FASTLEGE("FASTLEGE", "Fastlege", null),
    ANNET("ANNET", "Annet", null),
    UDEFINERT("-", "Ikke definert", null),
    ;

    public static final String KODEVERK = "LEGEERKLÆRING_KILDE";

    private static final Map<String, LegeerklæringKilde> KODER = new LinkedHashMap<>();

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

    private String navn;
    private String offisiellKode;
    private String kode;

    LegeerklæringKilde() {
        // Hibernate trenger den
    }

    private LegeerklæringKilde(String kode) {
        this.kode = kode;
    }

    private LegeerklæringKilde(String kode, String navn, String offisiellKode) {
        this.kode = kode;
        this.navn = navn;
        this.offisiellKode = offisiellKode;
    }

    public static LegeerklæringKilde  fraKode(final String kode)  {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent LegeerklæringKilde: " + kode);
        }
        return ad;
    }

    @JsonValue
    @Override
    public String getKode() {
        return kode;
    }

    @Override
    public String getOffisiellKode() {
        return offisiellKode;
    }

    @Override
    public String getKodeverk() {
        return KODEVERK;
    }

    @Override
    public String getNavn() {
        return navn;
    }
}
