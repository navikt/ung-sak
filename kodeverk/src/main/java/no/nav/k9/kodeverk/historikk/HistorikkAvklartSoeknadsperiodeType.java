package no.nav.k9.kodeverk.historikk;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import no.nav.k9.kodeverk.api.Kodeverdi;

/**
 * @deprecated har ingen gyldige verdier?.
 */
@Deprecated(forRemoval = true)
@JsonFormat(shape = Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
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

    @JsonIgnore
    private String navn;

    private String kode;

    private HistorikkAvklartSoeknadsperiodeType(String kode) {
        this.kode = kode;
    }

    private HistorikkAvklartSoeknadsperiodeType(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    @JsonCreator
    public static HistorikkAvklartSoeknadsperiodeType  fraKode(String kode)  {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            return valueOf(kode); // Sjekk om vi finner verdi viss søker etter name, brukast for openapi deserialisering inntil @JsonValue er på plass.
        }
        return ad;
    }

    @JsonCreator
    public static HistorikkAvklartSoeknadsperiodeType fraObjektProp(@JsonProperty("kode") String kode) {
        return fraKode(kode);
    }

    public static Map<String, HistorikkAvklartSoeknadsperiodeType> kodeMap() {
        return Collections.unmodifiableMap(KODER);
    }

    @Override
    public String getNavn() {
        return navn;
    }

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Override
    public String getKodeverk() {
        return KODEVERK;
    }

    @JsonProperty
    @Override
    public String getKode() {
        return kode;
    }

    @Override
    public String getOffisiellKode() {
        return getKode();
    }

}
