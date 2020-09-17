package no.nav.k9.kodeverk.person;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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

@JsonFormat(shape = Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public enum PersonstatusType implements Kodeverdi {

    ABNR("ABNR", "Aktivt BOSTNR", false),
    ADNR("ADNR", "Aktivt D-nummer", false),
    BOSA("BOSA", "Bosatt", true),
    DØD("DØD", "Død", true),
    DØDD("DØDD", "Dødd", false),
    FOSV("FOSV", "Forsvunnet/savnet", false),
    FØDR("FØDR", "Fødselregistrert", false),
    UFUL("UFUL", "Ufullstendig fødselsnr", false),
    UREG("UREG", "Uregistrert person", false),
    UTAN("UTAN", "Utgått person annullert tilgang Fnr", false),
    UTPE("UTPE", "Utgått person", false),
    UTVA("UTVA", "Utvandret", true),

    UDEFINERT("-", "Ikke definert", false),

    ;

    private static final Map<String, PersonstatusType> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "PERSONSTATUS_TYPE";

    @JsonIgnore
    private String navn;

    private String kode;

    @JsonIgnore
    private boolean fortsettBehandling;

    private PersonstatusType(String kode, String navn, boolean fortsettBehandling) {
        this.kode = kode;
        this.navn = navn;
        this.fortsettBehandling = fortsettBehandling;
    }

    public static boolean erDød(PersonstatusType personstatus) {
        return DØD.equals(personstatus) || DØDD.equals(personstatus);
    }

    @JsonCreator(mode = Mode.DELEGATING)
    public static PersonstatusType  fraKode(@JsonProperty("kode") Object node)  {
        if (node == null) {
            return null;
        }
        String kode = TempAvledeKode.getVerdi(PersonstatusType.class, node, "kode");
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent PersonstatusType: " + kode);
        }
        return ad;
    }

    public static Map<String, PersonstatusType> kodeMap() {
        return Collections.unmodifiableMap(KODER);
    }

    @Override
    public String getNavn() {
        return navn;
    }

    public static void main(String[] args) {
        System.out.println(KODER.keySet());
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

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

    public static Set<PersonstatusType> personstatusTyperFortsattBehandling() {
        return List.of(values()).stream().filter(s -> s.fortsettBehandling).collect(Collectors.toSet());
    }

}
