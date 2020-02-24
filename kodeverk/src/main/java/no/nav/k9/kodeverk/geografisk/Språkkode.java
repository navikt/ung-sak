package no.nav.k9.kodeverk.geografisk;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;

import no.nav.k9.kodeverk.api.Kodeverdi;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class Språkkode implements Kodeverdi {
    private static final String KODEVERK = "SPRAAK_KODE";

    private static final Map<String, Språkkode> KODER = initSpråkkoder();

    public static final Språkkode nb = fraKode("NB");
    public static final Språkkode no = fraKode("NO");
    public static final Språkkode nn = fraKode("NN");
    public static final Språkkode en = fraKode("EN");

    public static final Språkkode UDEFINERT = fraKode("-"); //$NON-NLS-1$
    
    @JsonProperty(value = "kode")
    private String kode;

    @JsonIgnore
    private String offisielIso2Kode;

    Språkkode() {
    }

    private Språkkode(String kode, String offisielIso2Kode) {
        this.kode = kode;
        this.offisielIso2Kode = offisielIso2Kode;
    }

    @Override
    public String getOffisiellKode() {
        return offisielIso2Kode;
    }

    @JsonIgnore
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

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var other = (Språkkode) obj;
        return Objects.equals(kode, other.kode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(kode);
    }

    @JsonCreator
    public static Språkkode fraKode(@JsonProperty("kode") String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent Språkkode: " + kode);
        }
        return ad;
    }

    public static Optional<Språkkode> fraKodeOptional(@JsonProperty("kode") String kode) {
        if (kode == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(KODER.get(kode));
    }

    private static Map<String, Språkkode> initSpråkkoder() {
        var map = new LinkedHashMap<String, Språkkode>();
        for (var c : Locale.getISOLanguages()) {
            Språkkode språkkode = new Språkkode(c.toUpperCase(), c);
            map.put(c.toUpperCase(), språkkode);
            map.put(c, språkkode);
        }
        map.put("-", new Språkkode("-", "Ikke definert"));
        return Collections.unmodifiableMap(map);
    }

    public static Map<String, Språkkode> kodeMap() {
        return Collections.unmodifiableMap(KODER);
    }
}
