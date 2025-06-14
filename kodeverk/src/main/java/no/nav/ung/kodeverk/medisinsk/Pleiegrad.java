package no.nav.ung.kodeverk.medisinsk;

import com.fasterxml.jackson.annotation.JsonValue;
import no.nav.ung.kodeverk.api.Kodeverdi;

import java.util.LinkedHashMap;
import java.util.Map;

public enum Pleiegrad implements Kodeverdi {
    /**
     * Alle kodeverk må ha en verdi, det kan ikke være null i databasen. Denne koden gjør samme nytten.
     */
    KONTINUERLIG_TILSYN("KONTINUERLIG_TILSYN", "Kontinuerlig tilsyn", null, 100),
    UTVIDET_KONTINUERLIG_TILSYN("UTVIDET_KONTINUERLIG_TILSYN", "Utvidet kontinuerlig tilsyn", null, 200),
    INNLEGGELSE("INNLEGGELSE", "Innleggelse", null, 200),
    LIVETS_SLUTT_TILSYN("LIVETS_SLUTT_TILSYN", "Livets slutt tilsyn", null, 100),
    LIVETS_SLUTT_TILSYN_FOM2023("LIVETS_SLUTT_TILSYN_2023", "Livets slutt tilsyn fom 2023", null, 200),
    NØDVENDIG_OPPLÆRING("NØDVENDIG_OPPLÆRING", "Nødvendig opplæring", null, 100),
    INGEN("INGEN", "Ingen kontinuerlig tilsyn", null, 0),
    UDEFINERT("-", "Ikke definert", null, 0),
    ;

    public static final String KODEVERK = "PLEIEGRAD";

    private static final Map<String, Pleiegrad> KODER = new LinkedHashMap<>();

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

    private int prosent;

    Pleiegrad() {
        // Hibernate trenger den
    }

    private Pleiegrad(String kode) {
        this.kode = kode;
    }

    private Pleiegrad(String kode, String navn, String offisiellKode, int prosent) {
        this.kode = kode;
        this.navn = navn;
        this.offisiellKode = offisiellKode;
        this.prosent = prosent;
    }

    public static Pleiegrad  fraKode(final String kode)  {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent Pleiegrad: " + kode);
        }
        return ad;
    }

    @JsonValue
    @Override
    public String getKode() {
        return kode;
    }

    public int getProsent() {
        return prosent;
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
