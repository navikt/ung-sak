package no.nav.ung.kodeverk.geografisk;

import com.fasterxml.jackson.annotation.JsonValue;
import no.nav.ung.kodeverk.api.Kodeverdi;

import java.util.*;

public class Språkkode implements Kodeverdi {
    private static final String KODEVERK = "SPRAAK_KODE";

    private static final Map<String, Språkkode> KODER = initSpråkkoder();

    public static final Språkkode nb = fraKode("NB");
    public static final Språkkode no = fraKode("NO");
    public static final Språkkode nn = fraKode("NN");
    public static final Språkkode en = fraKode("EN");

    public static final Språkkode UDEFINERT = fraKode("-"); //$NON-NLS-1$

    @JsonValue
    private String kode;

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

    @Override
    public String getNavn() {
        return kode;
    }

    @Override
    public String getKode() {
        return kode;
    }

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

    public static Språkkode fraKode(final String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent Språkkode: " + kode);
        }
        return ad;
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
