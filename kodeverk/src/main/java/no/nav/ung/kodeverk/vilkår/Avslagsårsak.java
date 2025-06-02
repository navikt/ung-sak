package no.nav.ung.kodeverk.vilkår;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator.Mode;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import no.nav.ung.kodeverk.TempAvledeKode;
import no.nav.ung.kodeverk.api.Kodeverdi;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@JsonFormat(shape = Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)

public enum Avslagsårsak implements Kodeverdi {

    SØKT_FOR_SENT("1007", "Søkt for sent",
        Map.of(FagsakYtelseType.UNGDOMSYTELSE, "22-13, 3. ledd")),
    MANGLENDE_DOKUMENTASJON("1019", "Manglende dokumentasjon",
        Map.of(FagsakYtelseType.UNGDOMSYTELSE, "forskrift om ungdomsprogrammet § 4")),
    SØKER_UNDER_MINSTE_ALDER("1089", "Søker er yngre enn minste tillate alder.",
        Map.of(FagsakYtelseType.UNGDOMSYTELSE, "Forskrift om ungdomsprogram og ungdomsprogramytelse § 3 bokstav a")),
    SØKER_OVER_HØYESTE_ALDER("1090", "Søker er eldre enn høyeste tillate alder.",
        Map.of(FagsakYtelseType.UNGDOMSYTELSE, "Forskrift om ungdomsprogram og ungdomsprogramytelse § 3 bokstav a")),
    SØKER_HAR_AVGÅTT_MED_DØDEN("1091", "Søker har avgått med døden.",
        Map.of(FagsakYtelseType.UNGDOMSYTELSE, "??")),

    OPPHØRT_UNGDOMSPROGRAM("2001", "Opphørt ungdomsprogram",
        Map.of(FagsakYtelseType.UNGDOMSYTELSE, "Forskriften for ungomsprogram")),
    ENDRET_STARTDATO_UNGDOMSPROGRAM("2002", "Endret start av ungdomsprogram",
        Map.of(FagsakYtelseType.UNGDOMSYTELSE, "Endret startdato for ungdomsprogram")),

    UDEFINERT("-", "Ikke definert",
        Map.of());

    public static final String KODEVERK = "AVSLAGSARSAK"; //$NON-NLS-1$
    private static final Map<String, Avslagsårsak> KODER = new LinkedHashMap<>();

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

    @JsonIgnore
    private Map<FagsakYtelseType, String> lovReferanser;

    private Avslagsårsak(String kode, String navn, Map<FagsakYtelseType, String> lovReferanser) {
        this.kode = kode;
        this.navn = navn;
        this.lovReferanser = lovReferanser;
    }

    @JsonCreator(mode = Mode.DELEGATING)
    public static Avslagsårsak fraKode(Object node) {
        if (node == null) {
            return null;
        }
        String kode = TempAvledeKode.getVerdi(Avslagsårsak.class, node, "kode");
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent Avslagsårsak: for input " + node);
        }
        return ad;
    }

    public static Map<String, Avslagsårsak> kodeMap() {
        return Collections.unmodifiableMap(KODER);
    }

    @Override
    public String getNavn() {
        return navn;
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
    public String getOffisiellKode() {
        return getKode();
    }

    /**
     * Get vilkår dette avslaget kan opptre i.
     */
    public Set<VilkårType> getVilkårTyper() {
        return VilkårType.getVilkårTyper(this);
    }

    public String getLovHjemmelData(FagsakYtelseType ytelseType) {
        return lovReferanser.getOrDefault(ytelseType, "<mangler knytning lovhjemmel>");
    }

}
