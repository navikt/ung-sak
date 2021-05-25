package no.nav.k9.kodeverk.hendelser;

import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.kodeverk.TempAvledeKode;
import no.nav.k9.kodeverk.api.Kodeverdi;

@JsonFormat(shape = Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public enum HendelseType implements Kodeverdi {

    PDL_DØDFØDSEL("PDL_DØDFØDSEL"),
    PDL_FØDSEL("PDL_FØDSEL"),
    PDL_DØDSFALL("PDL_DØDSFALL"),

    UDEFINERT("-"),
    ;

    public static final String KODEVERK = "HENDELSE_TYPE";

    private static final Map<String, HendelseType> KODER = new LinkedHashMap<>();

    private String kode;

    HendelseType() {
        // Hibernate trenger den
    }

    private HendelseType(String kode) {
        this.kode = kode;
    }


    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static HendelseType fraKode(Object node) {
        if (node == null) {
            return null;
        }
        String kode = TempAvledeKode.getVerdi(HendelseType.class, node, "kode");
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent HendelseType: for input " + node);
        }
        return ad;
    }


    public static void main(String[] args) {
        System.out.println(KODER.keySet());
    }

    @JsonProperty
    @Override
    public String getKodeverk() {
        return KODEVERK;
    }

    @Override
    public String getNavn() {
        return null;
    }

    @JsonProperty
    @Override
    public String getKode() {
        return kode;
    }

    @Override
    public String getOffisiellKode() {
        return null;
    }

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }
}
