package no.nav.k9.kodeverk.vilkår;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.kodeverk.api.Kodeverdi;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;

@JsonFormat(shape = Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)

public enum Avslagsårsak implements Kodeverdi {

    SØKT_FOR_SENT("1007", "Søkt for sent", Map.of()),
    MANGLENDE_DOKUMENTASJON("1019", "Manglende dokumentasjon", Map.of(FagsakYtelseType.FP, "21-3,21-7", FagsakYtelseType.OMP, "21-3")),
    SØKER_ER_IKKE_MEDLEM("1020", "Søker er ikke medlem", Map.of(FagsakYtelseType.FP, "14-2", FagsakYtelseType.OMP, "9-2 og 9-3, jamfør kapittel 2")),
    SØKER_ER_UTVANDRET("1021", "Søker er utvandret", Map.of(FagsakYtelseType.FP, "14-2", FagsakYtelseType.OMP, "9-2 og 9-3, jamfør kapittel 2")),
    SØKER_HAR_IKKE_LOVLIG_OPPHOLD("1023", "Søker har ikke lovlig opphold", Map.of(FagsakYtelseType.FP, "14-2", FagsakYtelseType.OMP, "9-2 og 9-3, jamfør kapittel 2")),
    SØKER_HAR_IKKE_OPPHOLDSRETT("1024", "Søker har ikke oppholdsrett", Map.of(FagsakYtelseType.FP, "14-2", FagsakYtelseType.OMP, "9-2 og 9-3, jamfør kapittel 2")),
    SØKER_ER_IKKE_BOSATT("1025", "Søker er ikke bosatt", Map.of(FagsakYtelseType.FP, "14-2", FagsakYtelseType.OMP, "9-2 og 9-3, jamfør kapittel 2")),
    IKKE_TILSTREKKELIG_OPPTJENING("1035", "Ikke tilstrekkelig opptjening", Map.of(FagsakYtelseType.FP, "9-2", FagsakYtelseType.OMP, "9-2, jamfør 8-2")),
    FYLLER_IKKE_ORDINÆRE_OPPTJENINGSREGLER("1036", "Ikke tilstrekkelig opptjening", Map.of(FagsakYtelseType.OMP, "8-47 1. ledd")),
    FOR_LAVT_BEREGNINGSGRUNNLAG("1041", "For lavt brutto beregningsgrunnlag", Map.of(FagsakYtelseType.OMP, "9-3 2. ledd", FagsakYtelseType.PSB, "9-3 2. ledd")),
    FOR_LAVT_BEREGNINGSGRUNNLAG_8_47("1042", "For lavt brutto beregningsgrunnlag", Map.of(FagsakYtelseType.OMP, "8-47 5. ledd", FagsakYtelseType.PSB, "8-47 5. ledd")),

    IKKE_DOKUMENTERT_SYKDOM_SKADE_ELLER_LYTE("1067", "Ikke dokumentert sykdom, skade eller lyte.", Map.of(FagsakYtelseType.PSB, "9-10 1. ledd")),
    DOKUMENTASJON_IKKE_FRA_RETT_ORGAN("1068", "Ikke mottatt dokumentasjon fra rett organ.", Map.of(FagsakYtelseType.PSB, "9-16")),
    IKKE_BEHOV_FOR_KONTINUERLIG_TILSYN_OG_PLEIE_PÅ_BAKGRUNN_AV_SYKDOM("1069", "Ikke behov for kontinuerlig pleie.", Map.of(FagsakYtelseType.PSB, "9-10 1. ledd")),
    IKKE_DOKUMENTERT_OMSORGEN_FOR("1071", "Ikke dokumentert omsorgen for.", Map.of(FagsakYtelseType.PSB, "9-10")),

    ÅRSKVANTUM_AVSLÅTT_IKKE_RETT("1072", "Ikke tett til omsorgsp.", Map.of(FagsakYtelseType.OMP, "9-6")),
    ÅRSKVANTUM_IKKE_FLERE_DAGER("1073", "Ikke nok dager i årskvantum.", Map.of(FagsakYtelseType.OMP, "9-6")),
    ÅRSKVANTUM_AVSLÅTT_OPPTJENING("1074", "Ikke nok opptejning til årskvantum.", Map.of(FagsakYtelseType.OMP, "9-6")),
    ÅRSKVANTUM_AVSLÅTT_MEDLEMSKAP("1075", "Ikke nok medlemskap til årskvantum.", Map.of(FagsakYtelseType.OMP, "9-6")),
    ÅRSKVANTUM_AVSLÅTT_70ÅR("1076", "Ikke ung nok for årskvantum.", Map.of(FagsakYtelseType.OMP, "9-6")),
    ÅRSKVANTUM_AVSLÅTT_UIDENTIFISERT_RAMMEVEDTAK("1077", "Uidentifisert rammevedtak", Map.of(FagsakYtelseType.OMP, "9-6")),

    INGEN_BEREGNINGSREGLER_TILGJENGELIG_I_LØSNINGEN("1099", "Ingen beregningsregler tilgjengelig i løsningen", Map.of()),
    UDEFINERT("-", "Ikke definert", Map.of()),

    ;

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

    @JsonCreator
    public static Avslagsårsak fraKode(@JsonProperty("kode") String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent Avslagsårsak: " + kode);
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
