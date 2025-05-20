package no.nav.ung.sak.behandlingslager.hendelser;

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

import no.nav.ung.kodeverk.api.Kodeverdi;

@JsonFormat(shape = Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public enum StartpunktType implements Kodeverdi {

    INNHENT_REGISTEROPPLYSNINGER("INNHENT_REGISTEROPPLYSNINGER", "Innhent registeropplysninger", 1),
    INIT_PERIODER("INIT_PERIODER", "Initier perioder", 2),
    BEREGNING("BEREGNING", "Beregning", 25),
    KONTROLLER_INNTEKT("KONTROLLER_INNTEKT", "Kontroller inntekt", 30),
    UTTAK("UTTAK", "Uttak", 40),

    UDEFINERT("-", "Ikke definert", 99),
    ;

    public static final String KODEVERK = "STARTPUNKT_TYPE";
    private static final Map<String, StartpunktType> KODER = new LinkedHashMap<>();

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

    @JsonIgnore
    private int rangering;

    @JsonIgnore
    private String navn;

    private String kode;


    StartpunktType(String kode, String navn, int rangering) {
        this.kode = kode;
        this.navn = navn;
        this.rangering = rangering;
    }

    @JsonCreator
    public static StartpunktType fraKode(@JsonProperty("kode") String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent StartpunktType: " + kode);
        }
        return ad;
    }

    public static Map<String, StartpunktType> kodeMap() {
        return Collections.unmodifiableMap(KODER);
    }

    @Override
    public String getNavn() {
        return navn;
    }

    @JsonProperty
    @Override
    public String getKode() {
        return this.kode;
    }

    @JsonProperty
    @Override
    public String getKodeverk() {
        return KODEVERK;
    }

    @Override
    public String getOffisiellKode() {
        return getKode();
    }

    @Override
    public String toString() {
        return super.toString() + "('" + getKode() + "')";
    }

    public static void main(String[] args) {
        System.out.println(KODER.keySet());
    }

    public int getRangering() {
        return rangering;
    }
}
