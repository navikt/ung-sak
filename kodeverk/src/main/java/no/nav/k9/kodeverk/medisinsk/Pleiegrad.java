package no.nav.k9.kodeverk.medisinsk;

import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonCreator.Mode;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import no.nav.k9.kodeverk.TempAvledeKode;
import no.nav.k9.kodeverk.api.Kodeverdi;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public enum Pleiegrad implements Kodeverdi {
    /**
     * Alle kodeverk må ha en verdi, det kan ikke være null i databasen. Denne koden gjør samme nytten.
     */
    KONTINUERLIG_TILSYN("KONTINUERLIG_TILSYN", "Kontinuerlig tilsyn", null, 100),
    UTVIDET_KONTINUERLIG_TILSYN("UTVIDET_KONTINUERLIG_TILSYN", "Utvidet kontinuerlig tilsyn", null, 200),
    INNLEGGELSE("INNLEGGELSE", "Innleggelse", null, 200),
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

    @JsonIgnore
    private String navn;
    @JsonIgnore
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

    @JsonCreator(mode = Mode.DELEGATING)
    public static Pleiegrad  fraKode(Object node)  {
        if (node == null) {
            return null;
        }
        String kode = TempAvledeKode.getVerdi(Pleiegrad.class, node, "kode");
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent Pleiegrad: " + kode);
        }
        return ad;
    }

    @JsonProperty
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

    @JsonProperty
    @Override
    public String getKodeverk() {
        return KODEVERK;
    }

    @Override
    public String getNavn() {
        return navn;
    }
}
