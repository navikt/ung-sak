package no.nav.ung.kodeverk.vilkår;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

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
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;

@JsonFormat(shape = Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)

public enum Avslagsårsak implements Kodeverdi {

    SØKT_FOR_SENT("1007", "Søkt for sent",
            Map.of(FagsakYtelseType.OMSORGSPENGER, "22-13, 3. ledd")),
    MANGLENDE_DOKUMENTASJON("1019", "Manglende dokumentasjon",
            Map.of(FagsakYtelseType.FP, "21-3,21-7",
                FagsakYtelseType.OMP, "21-3, 9-5",
                FagsakYtelseType.PPN, "21-3, 9-13",
                FagsakYtelseType.OPPLÆRINGSPENGER, "21-3, 9-14",
                FagsakYtelseType.PSB, "21-3, 9-10",
                FagsakYtelseType.OMSORGSPENGER_KS, "21-3, 9-6")),
    SØKER_ER_IKKE_MEDLEM("1020", "Søker er ikke medlem",
            Map.of(FagsakYtelseType.FP, "14-2",
                FagsakYtelseType.OMP, "9-2 og 9-3, jamfør kapittel 2")),
    SØKER_ER_UTVANDRET("1021", "Søker er utvandret",
            Map.of(FagsakYtelseType.FP, "14-2",
                FagsakYtelseType.OMP, "9-2 og 9-3, jamfør kapittel 2")),
    SØKER_HAR_IKKE_LOVLIG_OPPHOLD("1023", "Søker har ikke lovlig opphold",
            Map.of(FagsakYtelseType.FP, "14-2",
                FagsakYtelseType.OMP, "9-2 og 9-3, jamfør kapittel 2")),
    SØKER_HAR_IKKE_OPPHOLDSRETT("1024", "Søker har ikke oppholdsrett",
            Map.of(FagsakYtelseType.FP, "14-2",
                FagsakYtelseType.OMP, "9-2 og 9-3, jamfør kapittel 2")),
    SØKER_ER_IKKE_BOSATT("1025", "Søker er ikke bosatt",
            Map.of(FagsakYtelseType.FP, "14-2",
                FagsakYtelseType.OMP, "9-2 og 9-3, jamfør kapittel 2")),
    IKKE_TILSTREKKELIG_OPPTJENING("1035", "Ikke tilstrekkelig opptjening",
            Map.of(FagsakYtelseType.FP, "9-2",
                FagsakYtelseType.OMP, "9-2, jamfør 8-2")),
    FYLLER_IKKE_ORDINÆRE_OPPTJENINGSREGLER("1036", "Ikke tilstrekkelig opptjening",
            Map.of(FagsakYtelseType.OMP, "8-47 1. ledd")),
    FOR_LAVT_BEREGNINGSGRUNNLAG("1041", "For lavt brutto beregningsgrunnlag",
            Map.of(FagsakYtelseType.OMP, "9-3 2. ledd",
                FagsakYtelseType.PSB, "9-3 2. ledd",
                FagsakYtelseType.FRISINN, "koronaloven § 2 1. ledd")),
    MANGLENDE_INNTEKTSGRUNNLAG("1043", "Manglende inntektsgrunnlag for periode",
            Map.of(FagsakYtelseType.OMP, "§§ 21-3 og 8-28",
                FagsakYtelseType.PSB, "§§ 21-3 og 8-28")),
    FOR_LAVT_BEREGNINGSGRUNNLAG_8_47("1042", "For lavt brutto beregningsgrunnlag",
            Map.of(FagsakYtelseType.OMP, "8-47 5. ledd",
                FagsakYtelseType.PSB, "8-47 5. ledd")),
    SØKT_FRILANS_UTEN_FRILANS_INNTEKT("8000", "Søkt frilans uten frilansinntekt",
            Map.of(FagsakYtelseType.FRISINN, "koronaloven § 1 1. ledd")),
    AVKORTET_GRUNNET_ANNEN_INNTEKT("8001", "Avkortet grunnet annen inntekt",
            Map.of(FagsakYtelseType.FRISINN, "koronaloven § 2 2. ledd")),
    INGEN_STØNADSDAGER_I_SØKNADSPERIODEN("8002", "Ingen stønadsdager i søknadsperioden",
            Map.of(FagsakYtelseType.FRISINN, "21-3")),

    IKKE_DOKUMENTERT_SYKDOM_SKADE_ELLER_LYTE("1067", "Ikke dokumentert sykdom, skade eller lyte.",
            Map.of(FagsakYtelseType.PSB, "9-10 1. ledd")),
    DOKUMENTASJON_IKKE_FRA_RETT_ORGAN("1068", "Ikke mottatt dokumentasjon fra rett organ.",
            Map.of(FagsakYtelseType.PSB, "9-16")),
    IKKE_BEHOV_FOR_KONTINUERLIG_TILSYN_OG_PLEIE_PÅ_BAKGRUNN_AV_SYKDOM("1069", "Ikke behov for kontinuerlig pleie.",
            Map.of(FagsakYtelseType.PSB, "9-10 1. ledd")),
    IKKE_DOKUMENTERT_OMSORGEN_FOR("1071", "Ikke dokumentert omsorgen for.",
            Map.of(
                FagsakYtelseType.PSB, "9-10",
                FagsakYtelseType.OMSORGSPENGER_AO, "9-6 1. ledd",
                FagsakYtelseType.OMSORGSPENGER_KS, "9-6 2. ledd",
                FagsakYtelseType.OMSORGSPENGER_MA, "9-6 3. ledd")),

    IKKE_UTVIDETRETT("1072", "Ikke grunnlag for utvidet rett", Map.of(
        FagsakYtelseType.OMSORGSPENGER_AO, "9-6 1. ledd",
        FagsakYtelseType.OMSORGSPENGER_KS, "9-6 2. ledd",
        FagsakYtelseType.OMSORGSPENGER_MA, "9-6 3. ledd")),
    IKKE_UTVIDETRETT_IKKE_KRONISK_SYK("1073", "Ikke kronisk syk eller funksjonshemmet", Map.of(
        FagsakYtelseType.OMSORGSPENGER_KS, "9-6 2. ledd")),
    IKKE_UTVIDETRETT_IKKE_ØKT_RISIKO_FRAVÆR("1074", "Ikke økt risiko for fravær", Map.of(
        FagsakYtelseType.OMSORGSPENGER_KS, "9-6 2. ledd")),
    IKKE_MIDLERTIDIG_ALENE_VARIGHET_UNDER_SEKS_MÅN("1075", "Varigheten er mindre enn seks måneder", Map.of(
        FagsakYtelseType.OMSORGSPENGER_MA, "9-6 3. ledd")),
    IKKE_MIDLERTIDIG_ALENE_REGNES_IKKE_SOM_Å_HA_ALENEOMSORG("1076", "Søker oppfyller ikke kravene til midlertidig alene", Map.of(
        FagsakYtelseType.OMSORGSPENGER_MA, "9-6 3. ledd")),
    IKKE_MIDLERTIDIG_ALENE("1093", "Søker oppfyller ikke midlertidig alene-vilkåret av andre grunner", Map.of(
        FagsakYtelseType.OMSORGSPENGER_MA, "9-6 3. ledd")),
    IKKE_GRUNNLAG_FOR_ALENEOMSORG("1077", "Ikke grunnlag for aleneomsorg", Map.of(
        FagsakYtelseType.OMSORGSPENGER_AO, "9-6 1. ledd")),
    IKKE_GRUNNLAG_FOR_ALENEOMSORG_FORELDRE_BOR_SAMMEN("1078", "Foreldre bor sammen", Map.of(
        FagsakYtelseType.OMSORGSPENGER_AO, "9-6 1. ledd")),
    IKKE_GRUNNLAG_FOR_ALENEOMSORG_DELT_BOSTED("1079", "Avtale om delt bosted", Map.of(
        FagsakYtelseType.OMSORGSPENGER_AO, "9-6 1. ledd")),

    PLEIETRENGENDE_INNLAGT_I_STEDET_FOR_HJEMME("1080", "Pleietrengende innlagt i stedet for hjemme",
        Map.of(FagsakYtelseType.PLEIEPENGER_NÆRSTÅENDE, "9-13")),
    IKKE_I_LIVETS_SLUTTFASE("1081", "Ikke i livets sluttfase",
        Map.of(FagsakYtelseType.PLEIEPENGER_NÆRSTÅENDE, "9-13")),
    IKKE_NØDVENDIG_OPPLÆRING("1101", "Opplæringen er ikke nødvendig for ta seg av den pleietrengende",
        Map.of(FagsakYtelseType.OPPLÆRINGSPENGER, "9-14")),
    IKKE_GODKJENT_INSTITUSJON("1102", "Institusjonen er ikke en godkjent institusjon",
        Map.of(FagsakYtelseType.OPPLÆRINGSPENGER, "9-14")),
    IKKE_GJENNOMGÅTT_OPPLÆRING("1103", "Har ikke blitt gjennomgått opplæring",
        Map.of(FagsakYtelseType.OPPLÆRINGSPENGER, "9-14")),
    IKKE_PÅ_REISE("1104", "Ikke på reise",
        Map.of(FagsakYtelseType.OPPLÆRINGSPENGER, "9-14")),
    SØKER_UNDER_MINSTE_ALDER("1089", "Søker er yngre enn minste tillate alder.",
        Map.of(FagsakYtelseType.UNGDOMSYTELSE, "?")),
    SØKER_OVER_HØYESTE_ALDER("1090", "Søker er eldre enn høyeste tillate alder.",
            Map.of(FagsakYtelseType.PSB, "9-3 1. ledd",
                FagsakYtelseType.UNGDOMSYTELSE, "?")),
    SØKER_HAR_AVGÅTT_MED_DØDEN("1091", "Søker har avgått med døden.",
            Map.of(FagsakYtelseType.PSB, "9-5")),

    BARN_OVER_HØYESTE_ALDER("1092", "Barnet er eldre enn tillatt alder.", Map.of(
            FagsakYtelseType.OMSORGSPENGER_AO, "9-5 3. ledd",
            FagsakYtelseType.OMSORGSPENGER_KS, "9-5 3. ledd",
            FagsakYtelseType.OMSORGSPENGER_MA, "9-5 3. ledd")
    ),

    INGEN_BEREGNINGSREGLER_TILGJENGELIG_I_LØSNINGEN("1099", "Ingen beregningsregler tilgjengelig i løsningen",
            Map.of()),
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
