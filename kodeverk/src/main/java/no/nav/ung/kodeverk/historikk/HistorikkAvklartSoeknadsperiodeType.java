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
public enum HistorikkAvklartSoeknadsperiodeType implements Kodeverdi {

    UDEFINERT("-", "Ikke definert"),
    ;

    private static final Map<String, HistorikkAvklartSoeknadsperiodeType> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "HISTORIKK_AVKLART_SOEKNADSPERIODE_TYPE";

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

    private String navn;

    private String kode;

    private HistorikkAvklartSoeknadsperiodeType(String kode) {
        this.kode = kode;
    }

    private HistorikkAvklartSoeknadsperiodeType(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    public static HistorikkAvklartSoeknadsperiodeType  fraKode(final String kode)  {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent HistorikkAvklartSoeknadsperiodeType: " + kode);
        }
        return ad;
    }

    public static Map<String, HistorikkAvklartSoeknadsperiodeType> kodeMap() {
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
