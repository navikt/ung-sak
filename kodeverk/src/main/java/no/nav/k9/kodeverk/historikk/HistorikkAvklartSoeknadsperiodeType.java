package no.nav.k9.kodeverk.historikk;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonCreator.Mode;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import no.nav.k9.kodeverk.TempAvledeKode;
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

    @JsonCreator(mode = Mode.DELEGATING)
    public static HistorikkAvklartSoeknadsperiodeType  fraKode(@JsonProperty("kode") Object node)  {
        if (node == null) {
            return null;
        }
        String kode = TempAvledeKode.getVerdi(HistorikkAvklartSoeknadsperiodeType.class, node, "kode");
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
