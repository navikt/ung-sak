package no.nav.ung.fordel.kodeverdi;

import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonFormat(shape = Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public enum BehandlingType implements Kodeverdi {

    DIGITAL_SØKNAD("DIGITAL_SØKNAD","ae0227", "Digital søknad"),
    UTBETALING("UTBETALING","ae0007", "Utbetaling"),
    OVERFØRING("OVERFØRING","ae0085", "Overføring"),
    DIGITAL_ETTERSENDELSE("DIGITAL_ETTERSENDELSE", "ae0246", "Digital ettersendelse"),
    ANSATTE("ANSATTE", "ae0249", "Ansatte"),
    UTLAND("UTLAND", "ae0106", "Utland"),
    UDEFINERT("-", null, "Ikke definert"),
    ;

    private static final Map<String, BehandlingType> KODER = new LinkedHashMap<>();
    private static final Map<String, BehandlingType> OFFISIELLE_KODER = new LinkedHashMap<>();
    private static final Map<String, BehandlingType> ALLE_TERMNAVN = new LinkedHashMap<>();

    public static final String KODEVERK = "BEHANDLING_TEMA";

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
            if (v.offisiellKode != null) {
                OFFISIELLE_KODER.putIfAbsent(v.offisiellKode, v);
            }
            if (v.termnavn != null) {
                ALLE_TERMNAVN.putIfAbsent(v.termnavn, v);
            }
        }
    }

    private String kode;

    @JsonIgnore
    private String offisiellKode;

    @JsonIgnore
    private String termnavn;

    private BehandlingType(String kode, String offisiellKode, String termnavn) {
        this.kode = kode;
        this.offisiellKode = offisiellKode;
        this.termnavn = termnavn;
    }

    @JsonCreator
    public static BehandlingType fraKode(@JsonProperty("kode") String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent Tema: " + kode);
        }
        return ad;
    }

    public static BehandlingType fraKodeDefaultUdefinert(@JsonProperty("kode") String kode) {
        if (kode == null) {
            return UDEFINERT;
        }
        return KODER.getOrDefault(kode, UDEFINERT);
    }

    public static BehandlingType fraOffisiellKode(String kode) {
        if (kode == null) {
            return UDEFINERT;
        }
        return OFFISIELLE_KODER.getOrDefault(kode, UDEFINERT);
    }

    public static BehandlingType fraTermNavn(String termnavn) {
        if (termnavn == null) {
            return UDEFINERT;
        }
        return ALLE_TERMNAVN.getOrDefault(termnavn, UDEFINERT);
    }

    @JsonProperty
    @Override
    public String getKodeverk() {
        return KODEVERK;
    }

    @JsonProperty
    @Override
    public String getKode() {
        return kode;
    }

    public String getOffisiellKode() {
        return offisiellKode;
    }

    public String getTermNavn() {
        return termnavn;
    }
}
