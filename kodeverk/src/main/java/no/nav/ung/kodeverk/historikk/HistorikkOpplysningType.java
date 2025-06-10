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
public enum HistorikkOpplysningType implements Kodeverdi {

    UDEFINIERT("-", "Ikke definert"),
    ;

    private static final Map<String, HistorikkOpplysningType> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "HISTORIKK_OPPLYSNING_TYPE";

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

    private String navn;

    private String kode;

    private HistorikkOpplysningType(String kode) {
        this.kode = kode;
    }

    private HistorikkOpplysningType(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    public static HistorikkOpplysningType  fraKode(final String kode)  {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent HistorikkOpplysningType: " + kode);
        }
        return ad;
    }

    public static Map<String, HistorikkOpplysningType> kodeMap() {
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
}
