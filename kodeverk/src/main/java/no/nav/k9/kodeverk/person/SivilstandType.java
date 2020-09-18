package no.nav.k9.kodeverk.person;

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

/** Fra offisielt kodeverk (kodeverkklienten). */
@JsonFormat(shape = Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public enum SivilstandType implements Kodeverdi {

    ENKEMANN("ENKE", "Enke/-mann"),
    GIFT("GIFT", "Gift"),
    GJENLEVENDE_PARTNER("GJPA", "Gjenlevende partner"),
    GIFT_ADSKILT("GLAD", "Gift, lever adskilt"),
    UOPPGITT("NULL", "Uoppgitt"),
    REGISTRERT_PARTNER("REPA", "Registrert partner"),
    SAMBOER("SAMB", "Samboer"),
    SEPARERT_PARTNER("SEPA", "Separert partner"),
    SEPARERT("SEPR", "Separert"),
    SKILT("SKIL", "Skilt"),
    SKILT_PARTNER("SKPA", "Skilt partner"),
    UGIFT("UGIF", "Ugift"),
    ;

    public static final String KODEVERK = "SIVILSTAND_TYPE";

    private static final Map<String, SivilstandType> KODER = new LinkedHashMap<>();

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

    private SivilstandType(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    @JsonProperty
    @Override
    public String getKode() {
        return kode;
    }

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Override
    public String getKodeverk() {
        return KODEVERK;
    }

    @Override
    public String getNavn() {
        return navn;
    }

    @Override
    public String getOffisiellKode() {
        return getKode();
    }

    @JsonCreator(mode = Mode.DELEGATING)
    public static SivilstandType  fraKode(Object node)  {
        if (node == null) {
            return null;
        }
        String kode = TempAvledeKode.getVerdi(SivilstandType.class, node, "kode");
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent FagsakYtelseType: " + kode);
        }
        return ad;
    }

    public static Map<String, SivilstandType> kodeMap() {
        return Collections.unmodifiableMap(KODER);
    }

}
