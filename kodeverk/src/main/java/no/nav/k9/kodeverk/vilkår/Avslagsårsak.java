package no.nav.k9.kodeverk.vilkår;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@JsonFormat(shape = Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public enum Avslagsårsak implements ÅrsakskodeMedLovreferanse {

    SØKT_FOR_SENT("1007", "Søkt for sent", null),
    MANGLENDE_DOKUMENTASJON("1019", "Manglende dokumentasjon", "{\"fagsakYtelseType\": [{\"FP\": [{\"kategori\": \"FP_VK_34\", \"lovreferanse\": \"21-3,21-7\"}]}]}"),
    SØKER_ER_IKKE_MEDLEM("1020", "Søker er ikke medlem", "{\"fagsakYtelseType\": [{\"FP\": [{\"kategori\": \"FP_VK_2\", \"lovreferanse\": \"14-2\"}]}]}"),
    SØKER_ER_UTVANDRET("1021", "Søker er utvandret", "{\"fagsakYtelseType\": [{\"FP\": [{\"kategori\": \"FP_VK_2\", \"lovreferanse\": \"14-2\"}]}]}"),
    SØKER_HAR_IKKE_LOVLIG_OPPHOLD("1023", "Søker har ikke lovlig opphold", "{\"fagsakYtelseType\": [{\"FP\": [{\"kategori\": \"FP_VK_2\", \"lovreferanse\": \"14-2\"}]}]}"),
    SØKER_HAR_IKKE_OPPHOLDSRETT("1024", "Søker har ikke oppholdsrett", "{\"fagsakYtelseType\": [{\"FP\": [{\"kategori\": \"FP_VK_2\", \"lovreferanse\": \"14-2\"}]}]}"),
    SØKER_ER_IKKE_BOSATT("1025", "Søker er ikke bosatt", "{\"fagsakYtelseType\": [{\"FP\": [{\"kategori\": \"FP_VK_2\", \"lovreferanse\": \"14-2\"}]}]}"),
    IKKE_TILSTREKKELIG_OPPTJENING("1035", "Ikke tilstrekkelig opptjening", "{\"fagsakYtelseType\": [{\"FP\": [{\"kategori\": \"FP_VK_23\", \"lovreferanse\": \"9-2\"}]}]}"),
    FOR_LAVT_BEREGNINGSGRUNNLAG("1041", "For lavt brutto beregningsgrunnlag", "{\"fagsakYtelseType\": [{\"FP\": [{\"kategori\": \"FP_VK_41\", \"lovreferanse\": \"14-7\"}]}]}"),
    STEBARNSADOPSJON_IKKE_FLERE_DAGER_IGJEN("1051", "Stebarnsadopsjon ikke flere dager igjen", "{\"fagsakYtelseType\": [{\"FP\": [{\"kategori\": \"FP_VK_16\", \"lovreferanse\": \"14-5\"}]}]}"),
    SØKER_IKKE_GRAVID_KVINNE("1060", "§14-4 første ledd: Søker er ikke gravid kvinne", "{\"fagsakYtelseType\": [{\"SVP\": [{\"kategori\": \"SVP_VK_1\", \"lovreferanse\": \"14-4 1. ledd\"}]}]}"),
    SØKER_ER_IKKE_I_ARBEID("1061", "§14-4 tredje ledd: Søker er ikke i arbeid/har ikke tap av pensjonsgivende inntekt", "{\"fagsakYtelseType\": [{\"SVP\": [{\"kategori\": \"SVP_VK_1\", \"lovreferanse\": \"14-4 3. ledd\"}]}]}"),
    ARBEIDSTAKER_HAR_IKKE_DOKUMENTERT_RISIKOFAKTORER("1063", "§14-4 første ledd: Arbeidstaker har ikke dokumentert risikofaktorer", "{\"fagsakYtelseType\": [{\"SVP\": [{\"kategori\": \"SVP_VK_1\", \"lovreferanse\": \"14-4 1. ledd\"}]}]}"),
    ARBEIDSTAKER_KAN_OMPLASSERES("1064", "§14-4 første ledd: Arbeidstaker kan omplasseres til annet høvelig arbeid", "{\"fagsakYtelseType\": [{\"SVP\": [{\"kategori\": \"SVP_VK_1\", \"lovreferanse\": \"14-4 1. ledd\"}]}]}"),
    SN_FL_HAR_IKKE_DOKUMENTERT_RISIKOFAKTORER("1065", "§14-4 andre ledd: Næringsdrivende/frilanser har ikke dokumentert risikofaktorer", "{\"fagsakYtelseType\": [{\"SVP\": [{\"kategori\": \"SVP_VK_1\", \"lovreferanse\": \"14-4 2. ledd\"}]}]}"),
    IKKE_DOKUMENTERT_SYKDOM_SKADE_ELLER_LYTE("1067", "Ikke dokumentert sykdom, skade eller lyte.", "{\"fagsakYtelseType\": [{\"PSB\": [{\"kategori\": \"PSB_VK_2a\", \"lovreferanse\": \"9-10 1. ledd\"}]}]}"),
    DOKUMENTASJON_IKKE_FRA_RETT_ORGAN("1068", "Ikke mottatt dokumentasjon fra rett organ.", "{\"fagsakYtelseType\": [{\"PSB\": [{\"kategori\": \"PSB_VK_2a\", \"lovreferanse\": \"9-16\"}]}]}"),
    IKKE_BEHOV_FOR_KONTINUERLIG_TILSYN_OG_PLEIE_PÅ_BAKGRUNN_AV_SYKDOM("1069", "Ikke behov for kontinuerlig pleie.", "{\"fagsakYtelseType\": [{\"PSB\": [{\"kategori\": \"PSB_VK_2a\", \"lovreferanse\": \"9-10 1. ledd\"}]}]}"),
    IKKE_DOKUMENTERT_OMSORGEN_FOR("1071", "Ikke dokumentert omsorgen for.", "{\"fagsakYtelseType\": [{\"PSB\": [{\"kategori\": \"PSB_VK_1\", \"lovreferanse\": \"9-10\"}]}]}"),
    ÅRSKVANTUM_AVSLÅTT_IKKE_RETT("1072", "Ikke tett til omsorgsp.", "{\"fagsakYtelseType\": [{\"OMS\": [{\"kategori\": \"OMS_VK_1\", \"lovreferanse\": \"9-6\"}]}]}"),
    ÅRSKVANTUM_IKKE_FLERE_DAGER("1073", "Ikke nok dager i årskvantum.", "{\"fagsakYtelseType\": [{\"OMS\": [{\"kategori\": \"OMS_VK_1\", \"lovreferanse\": \"9-6\"}]}]}"),
    ÅRSKVANTUM_AVSLÅTT_OPPTJENING("1074", "Ikke nok opptejning til årskvantum.", "{\"fagsakYtelseType\": [{\"OMS\": [{\"kategori\": \"OMS_VK_1\", \"lovreferanse\": \"9-6\"}]}]}"),
    ÅRSKVANTUM_AVSLÅTT_MEDLEMSKAP("1075", "Ikke nok medlemskap til årskvantum.", "{\"fagsakYtelseType\": [{\"OMS\": [{\"kategori\": \"OMS_VK_1\", \"lovreferanse\": \"9-6\"}]}]}"),
    ÅRSKVANTUM_AVSLÅTT_70ÅR("1076", "Ikke ung nok for årskvantum.", "{\"fagsakYtelseType\": [{\"OMS\": [{\"kategori\": \"OMS_VK_1\", \"lovreferanse\": \"9-6\"}]}]}"),
    INGEN_BEREGNINGSREGLER_TILGJENGELIG_I_LØSNINGEN("1099", "Ingen beregningsregler tilgjengelig i løsningen", null),
    UDEFINERT("-", "Ikke definert", null),

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

    // TODO endre fra raw json
    @JsonIgnore
    private String lovReferanse;

    @JsonIgnore
    private String navn;

    private String kode;

    private Avslagsårsak(String kode, String navn, String lovReferanse) {
        this.kode = kode;
        this.navn = navn;
        this.lovReferanse = lovReferanse;
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

    public static void main(String[] args) {
        System.out.println(KODER.keySet().stream().map(a -> "\"" + a + "\"").collect(Collectors.toList()));
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

    @Override
    public String getLovHjemmelData() {
        return lovReferanse;
    }


}
