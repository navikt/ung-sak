package no.nav.k9.kodeverk.geografisk;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;

import no.nav.k9.kodeverk.api.Kodeverdi;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class Landkoder implements Kodeverdi {
    private static final String KODEVERK = "LANDKODER";

    private static final Map<String, Landkoder> KODER = initKoder();

    public static final Landkoder NOR = fraKode("NOR");
    public static final Landkoder SWE = fraKode("SWE");
    public static final Landkoder USA = fraKode("USA");
    public static final Landkoder PNG = fraKode("PNG");
    public static final Landkoder BEL = fraKode("BEL");
    public static final Landkoder FIN = fraKode("FIN");
    public static final Landkoder CAN = fraKode("CAN");
    public static final Landkoder ESP = fraKode("ESP");

    /** Kodeverkklient spesifikk konstant. Bruker oppgir ikke land */
    public static final Landkoder UOPPGITT_UKJENT = fraKode("???");

    /** Egendefinert konstant - ikke definert (null object pattern) for bruk i modeller som krever non-null. */
    public static final Landkoder UDEFINERT = fraKode("-");

    /** ISO 3166 alpha 3-letter code. */
    @JsonProperty(value = "kode")
    private String kode;

    Landkoder() {
    }

    private Landkoder(String kode) {
        this.kode = kode;
    }

    private static Map<String, Landkoder> initKoder() {
        var map = new LinkedHashMap<String, Landkoder>();
        for (var c : Locale.getISOCountries()) {
            Locale locale = new Locale("", c);
            String iso3cc = locale.getISO3Country().toUpperCase();
            Landkoder landkode = new Landkoder(iso3cc);
            map.put(c, landkode);
            map.put(iso3cc, landkode);
        }
        // udefinerte koder
        List.of("-", "???")
            .forEach(c -> map.put(c, new Landkoder(c)));

        // ISO user defined codes (NAV spesifikke)
        // @see https://en.wikipedia.org/wiki/ISO_3166-1_alpha-3#User-assigned_code_elements
        List.of(
            "XXX", // STATSLØS
            "XUK", // UKJENT
            "XXK",  // KOSOVO
            // Historiske land
            "DDR" // Tyskland (Øst)
        ).forEach(c -> map.put(c, new Landkoder(c)));

        // ISO transitional codes.
        // @see https://en.wikipedia.org/wiki/ISO_3166-1_alpha-3#Transitional_reservations
        List.of("ANT", "BUR", "BYS", "CSK", "ROM", "SCG", "TMP", "YUG", "ZAR", "SUN")
            .forEach(c -> map.put(c, new Landkoder(c)));

        return Collections.unmodifiableMap(map);
    }

    @Override
    public String getOffisiellKode() {
        return kode;
    }

    @JsonProperty(value = "navn", access = Access.READ_ONLY)
    @Override
    public String getNavn() {
        return kode;
    }

    @Override
    public String getKode() {
        return kode;
    }

    @JsonProperty(value = "kodeverk", access = Access.READ_ONLY)
    @Override
    public String getKodeverk() {
        return KODEVERK;
    }

    public static boolean erNorge(String kode) {
        return NOR.getKode().equals(kode);
    }

    public static Map<String, Landkoder> kodeMap() {
        return Collections.unmodifiableMap(KODER);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var other = (Landkoder) obj;
        return Objects.equals(kode, other.kode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(kode);
    }

    @JsonCreator
    public static Landkoder fraKode(@JsonProperty("kode") String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent Landkode: " + kode);
        }
        return ad;
    }

    @Override
    public String toString() {
        return kode;
    }
}
