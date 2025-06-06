package no.nav.ung.kodeverk.historikk;

import com.fasterxml.jackson.annotation.JsonValue;
import no.nav.ung.kodeverk.api.Kodeverdi;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @deprecated har ingen gyldige verdier?.
 */
@Deprecated(forRemoval = true)
public enum HistorikkResultatType implements Kodeverdi {

    UDEFINIERT("-", "Ikke definert"),
    ;

    private static final Map<String, HistorikkResultatType> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "HISTORIKK_RESULTAT_TYPE";

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

    private String navn;

    private String kode;

    private HistorikkResultatType(String kode) {
        this.kode = kode;
    }

    private HistorikkResultatType(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    public static HistorikkResultatType  fraKode(final String kode)  {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent HistorikkResultatType: " + kode);
        }
        return ad;
    }

    public static Map<String, HistorikkResultatType> kodeMap() {
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

    @Override
    public String getOffisiellKode() {
        return getKode();
    }

    @JsonValue
    @Override
    public String getKode() {
        return kode;
    }

    public static void main(String[] args) {
        System.out.println(KODER.keySet());
    }
}
