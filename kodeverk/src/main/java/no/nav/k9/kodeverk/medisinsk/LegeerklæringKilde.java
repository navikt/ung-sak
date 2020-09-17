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

    @JsonIgnore
    private String navn;
    @JsonIgnore
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

    @JsonCreator(mode = Mode.DELEGATING)
    public static LegeerklæringKilde  fraKode(@JsonProperty("kode") Object node)  {
        if (node == null) {
            return null;
        }
        String kode = TempAvledeKode.getVerdi(LegeerklæringKilde.class, node, "kode");
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent LegeerklæringKilde: " + kode);
        }
        return ad;
    }

    @JsonProperty
    @Override
    public String getKode() {
        return kode;
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
