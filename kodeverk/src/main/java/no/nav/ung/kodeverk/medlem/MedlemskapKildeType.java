package no.nav.ung.kodeverk.medlem;

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
import no.nav.ung.kodeverk.TempAvledeKode;
import no.nav.ung.kodeverk.api.Kodeverdi;

@JsonFormat(shape = Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public enum MedlemskapKildeType implements Kodeverdi {

    E500("E500", "E-500"),
    INFOTR("INFOTR", "Infotrygd"),
    AVGSYS("AVGSYS", "Avgiftsystemet"),
    APPBRK("APPBRK", "Applikasjonsbruker"),
    PP01("PP01", "Pensjon"),
    FS22("FS22", "Gosys"),
    SRVGOSYS("srvgosys", "Gosys, ikke standard"),
    SRVMELOSYS("srvmelosys", "Melosys, ikke standard"),
    MEDL("MEDL", "MEDL"),
    TPS("TPS", "TPS"),
    TP("TP", "TP"),
    LAANEKASSEN("LAANEKASSEN", "Laanekassen"),
    ANNEN("ANNEN", "Annen"),
    UDEFINERT("-", "Ikke definert"),
    ;

    private static final Map<String, MedlemskapKildeType> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "MEDLEMSKAP_KILDE";

    @Deprecated
    public static final String DISCRIMINATOR = "MEDLEMSKAP_KILDE";

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

    private MedlemskapKildeType(String kode) {
        this.kode = kode;
    }

    private MedlemskapKildeType(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    @JsonCreator(mode = Mode.DELEGATING)
    public static MedlemskapKildeType  fraKode(Object node)  {
        if (node == null) {
            return null;
        }
        String kode = TempAvledeKode.getVerdi(MedlemskapKildeType.class, node, "kode");
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent MedlemskapKildeType: " + kode);
        }
        return ad;
    }

    public static Map<String, MedlemskapKildeType> kodeMap() {
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

    public static void main(String[] args) {
        System.out.println(KODER.keySet());
    }
}
