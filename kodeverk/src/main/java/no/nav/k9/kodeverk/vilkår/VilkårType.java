package no.nav.k9.kodeverk.vilkår;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
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
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import no.nav.k9.kodeverk.TempAvledeKode;
import no.nav.k9.kodeverk.api.Kodeverdi;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.vilkår.VilkårType.Serializer;

@JsonSerialize(contentUsing = Serializer.class)
@JsonFormat(shape = Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public enum VilkårType implements Kodeverdi {
    K9_VILKÅRET("FP_VK_0",
        "K9-vilkåret", // for unntaksbehandling
        Map.of(FagsakYtelseType.PLEIEPENGER_SYKT_BARN, "§ 8",
            FagsakYtelseType.PLEIEPENGER_NÆRSTÅENDE, "§ 8",
            FagsakYtelseType.OPPLÆRINGSPENGER, "§ 8",
            FagsakYtelseType.OMP, "§ 8",
            FagsakYtelseType.FRISINN, "koronaloven § 1-3"),
        Avslagsårsak.UDEFINERT),
    MEDLEMSKAPSVILKÅRET("FP_VK_2",
        "Medlemskapsvilkåret",
        Map.of(FagsakYtelseType.OMSORGSPENGER, "Kap. 2",
            FagsakYtelseType.PLEIEPENGER_SYKT_BARN, "Kap. 2",
            FagsakYtelseType.PLEIEPENGER_NÆRSTÅENDE, "Kap. 2",
            FagsakYtelseType.OPPLÆRINGSPENGER, "Kap. 2"),
        Avslagsårsak.SØKER_ER_IKKE_MEDLEM,
        Avslagsårsak.SØKER_ER_UTVANDRET,
        Avslagsårsak.SØKER_HAR_IKKE_LOVLIG_OPPHOLD,
        Avslagsårsak.SØKER_HAR_IKKE_OPPHOLDSRETT,
        Avslagsårsak.SØKER_ER_IKKE_BOSATT),
    OMSORGEN_FOR("K9_VK_1",
        "Omsorgen for",
        Map.of(FagsakYtelseType.PLEIEPENGER_SYKT_BARN, "§ 9-10",
            FagsakYtelseType.OMSORGSPENGER_AO, "§ 9-10",
            FagsakYtelseType.OMSORGSPENGER_KS, "§ 9-10",
            FagsakYtelseType.OMSORGSPENGER_MA, "§ 9-10"),
        Avslagsårsak.IKKE_DOKUMENTERT_SYKDOM_SKADE_ELLER_LYTE,
        Avslagsårsak.IKKE_DOKUMENTERT_OMSORGEN_FOR,
        Avslagsårsak.IKKE_BEHOV_FOR_KONTINUERLIG_TILSYN_OG_PLEIE_PÅ_BAKGRUNN_AV_SYKDOM,
        Avslagsårsak.DOKUMENTASJON_IKKE_FRA_RETT_ORGAN),
    ALDERSVILKÅR("K9_VK_3",
        "Aldersvilkåret",
        Map.of(
            FagsakYtelseType.PLEIEPENGER_SYKT_BARN, "§ 9-3 første ledd",
            FagsakYtelseType.PLEIEPENGER_NÆRSTÅENDE, "§ 9-3 første ledd",
            FagsakYtelseType.OMSORGSPENGER_AO, "§ 9-3 første ledd",
            FagsakYtelseType.OMSORGSPENGER_KS, "§ 9-3 første ledd",
            FagsakYtelseType.OMSORGSPENGER_MA, "§ 9-3 første ledd"),
        Avslagsårsak.SØKER_OVER_HØYESTE_ALDER),
    ALDERSVILKÅR_BARN("K9_VK_5_3", "Aldersvilkår for barn",
        Map.of(
            FagsakYtelseType.OMSORGSPENGER_AO, "§ 9-5 tredje ledd",
            FagsakYtelseType.OMSORGSPENGER_KS, "§ 9-5 tredje ledd",
            FagsakYtelseType.OMSORGSPENGER_MA, "§ 9-5 tredje ledd"),
        Avslagsårsak.BARN_OVER_HØYESTE_ALDER),
    MEDISINSKEVILKÅR_UNDER_18_ÅR("K9_VK_2_a",
        "Medisinske vilkår under 18 år",
        Map.of(FagsakYtelseType.PLEIEPENGER_SYKT_BARN, "§ 9-10 første ledd"),
        Avslagsårsak.IKKE_DOKUMENTERT_OMSORGEN_FOR,
        Avslagsårsak.IKKE_DOKUMENTERT_SYKDOM_SKADE_ELLER_LYTE,
        Avslagsårsak.IKKE_BEHOV_FOR_KONTINUERLIG_TILSYN_OG_PLEIE_PÅ_BAKGRUNN_AV_SYKDOM,
        Avslagsårsak.DOKUMENTASJON_IKKE_FRA_RETT_ORGAN,
        Avslagsårsak.MANGLENDE_DOKUMENTASJON),
    MEDISINSKEVILKÅR_18_ÅR("K9_VK_2_b",
        "Medisinske vilkår 18 år eller eldre",
        Map.of(FagsakYtelseType.PLEIEPENGER_SYKT_BARN, "§ 9-10 tredje ledd"),
        Avslagsårsak.IKKE_DOKUMENTERT_OMSORGEN_FOR,
        Avslagsårsak.IKKE_DOKUMENTERT_SYKDOM_SKADE_ELLER_LYTE,
        Avslagsårsak.IKKE_BEHOV_FOR_KONTINUERLIG_TILSYN_OG_PLEIE_PÅ_BAKGRUNN_AV_SYKDOM,
        Avslagsårsak.DOKUMENTASJON_IKKE_FRA_RETT_ORGAN,
        Avslagsårsak.MANGLENDE_DOKUMENTASJON),
    SØKNADSFRIST("FP_VK_3",
        "Søknadsfristvilkåret",
        Map.of(FagsakYtelseType.OMSORGSPENGER, "§ 22-13, 2. ledd",
            FagsakYtelseType.PLEIEPENGER_SYKT_BARN, "§ 22-13, 2. ledd",
            FagsakYtelseType.OPPLÆRINGSPENGER, "§ 22-13, 2. ledd",
            FagsakYtelseType.PLEIEPENGER_NÆRSTÅENDE, "§ 22-13, 2. ledd"),
        Avslagsårsak.SØKT_FOR_SENT),
    SØKERSOPPLYSNINGSPLIKT("FP_VK_34",
        "Søkers opplysningsplikt",
        Map.of(FagsakYtelseType.PLEIEPENGER_SYKT_BARN, "§§ 21-3 og 21-7",
            FagsakYtelseType.PLEIEPENGER_NÆRSTÅENDE, "§§ 21-3 og 21-7",
            FagsakYtelseType.OPPLÆRINGSPENGER, "§§ 21-3 og 21-7",
            FagsakYtelseType.OMSORGSPENGER, "§§ 21-3 og 21-7"),
        Avslagsårsak.MANGLENDE_DOKUMENTASJON),
    OPPTJENINGSPERIODEVILKÅR("FP_VK_21",
        "Opptjeningsperiode",
        Map.of(FagsakYtelseType.PLEIEPENGER_SYKT_BARN, "§ 9-2 jamfør 8-2",
            FagsakYtelseType.PLEIEPENGER_NÆRSTÅENDE, "§ 9-2 jamfør 8-2",
            FagsakYtelseType.OPPLÆRINGSPENGER, "§ 9-2 jamfør 8-2",
            FagsakYtelseType.OMP, "§ 9-2 jamfør 8-2")),
    OPPTJENINGSVILKÅRET("FP_VK_23",
        "Opptjeningsvilkåret",
        Map.of(FagsakYtelseType.PLEIEPENGER_SYKT_BARN, "§ 9-2 jamfør 8-2",
            FagsakYtelseType.PLEIEPENGER_NÆRSTÅENDE, "§ 9-2 jamfør 8-2",
            FagsakYtelseType.OPPLÆRINGSPENGER, "§ 9-2 jamfør 8-2",
            FagsakYtelseType.OMP, "§ 9-2 jamfør 8-2"),
        Avslagsårsak.IKKE_TILSTREKKELIG_OPPTJENING,
        Avslagsårsak.FYLLER_IKKE_ORDINÆRE_OPPTJENINGSREGLER),
    BEREGNINGSGRUNNLAGVILKÅR("FP_VK_41",
        "Beregning",
        Map.of(FagsakYtelseType.PLEIEPENGER_SYKT_BARN, "§ 8",
            FagsakYtelseType.PLEIEPENGER_NÆRSTÅENDE, "§ 8",
            FagsakYtelseType.OPPLÆRINGSPENGER, "§ 8",
            FagsakYtelseType.OMP, "§ 8",
            FagsakYtelseType.FRISINN, "koronaloven § 1-3"),
        Avslagsårsak.FOR_LAVT_BEREGNINGSGRUNNLAG,
        Avslagsårsak.FOR_LAVT_BEREGNINGSGRUNNLAG_8_47,
        Avslagsårsak.SØKT_FRILANS_UTEN_FRILANS_INNTEKT,
        Avslagsårsak.AVKORTET_GRUNNET_ANNEN_INNTEKT),
    I_LIVETS_SLUTTFASE("K9_VK_16",
        "I livets sluttfase",
        Map.of(FagsakYtelseType.PLEIEPENGER_NÆRSTÅENDE, "§ 9-13"),
        Avslagsårsak.MANGLENDE_DOKUMENTASJON,
        Avslagsårsak.IKKE_I_LIVETS_SLUTTFASE,
        Avslagsårsak.PLEIETRENGENDE_INNLAGT_I_STEDET_FOR_HJEMME
    ),
    NØDVENDIG_OPPLÆRING("K9_VK_20",
        "Nødvendig opplæring",
        Map.of(FagsakYtelseType.OPPLÆRINGSPENGER, "§ 9-14"),
        Avslagsårsak.MANGLENDE_DOKUMENTASJON,
        Avslagsårsak.IKKE_NØDVENDIG,
        Avslagsårsak.IKKE_GODKJENT_INSTITUSJON
    ),
    LANGVARIG_SYKDOM("K9_VK_17",
        "Nødvendig opplæring",
        Map.of(FagsakYtelseType.OPPLÆRINGSPENGER, "§ 9-14"),
        Avslagsårsak.MANGLENDE_DOKUMENTASJON,
        Avslagsårsak.IKKE_NØDVENDIG,
        Avslagsårsak.IKKE_GODKJENT_INSTITUSJON
    ),
    UTVIDETRETT("K9_VK_9_6", "Utvidet rett",
        Map.of(FagsakYtelseType.OMSORGSPENGER_AO, "§ 9-6 1. ledd",
            FagsakYtelseType.OMSORGSPENGER_KS, "§ 9-6 2. ledd",
            FagsakYtelseType.OMSORGSPENGER_MA, "§ 9-6 3. ledd"),
        Avslagsårsak.IKKE_UTVIDETRETT, Avslagsårsak.IKKE_UTVIDETRETT_IKKE_KRONISK_SYK, Avslagsårsak.IKKE_UTVIDETRETT_IKKE_ØKT_RISIKO_FRAVÆR, Avslagsårsak.IKKE_UTVIDETRETT_REGNES_IKKE_SOM_Å_HA_ALENEOMSORG,
        Avslagsårsak.IKKE_UTVIDETRETT_VARIGHET_UNDER_SEKS_MÅN),
    /**
     * Brukes i stedet for null der det er optional.
     */
    UDEFINERT("-", "Ikke definert", Map.of()),

    ;

    public static final String KODEVERK = "VILKAR_TYPE";
    private static final Map<String, VilkårType> KODER = new LinkedHashMap<>();
    private static final Map<VilkårType, Set<Avslagsårsak>> INDEKS_VILKÅR_AVSLAGSÅRSAKER = new LinkedHashMap<>(); // NOSONAR
    private static final Map<Avslagsårsak, Set<VilkårType>> INDEKS_AVSLAGSÅRSAK_VILKÅR = new LinkedHashMap<>(); // NOSONAR

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }

            // hack for jackson < 2.11. Klarer ikke å serialisere enum key i maps riktig (ignorerer JsonProperty)
            // fra 2.11 er følgende workaround (respekterer fortsatt ikke JsonProperty på enum)
            // https://github.com/FasterXML/jackson-databind/issues/2503
            KODER.putIfAbsent(v.name(), v);

            INDEKS_VILKÅR_AVSLAGSÅRSAKER.put(v, v.avslagsårsaker);
            v.avslagsårsaker.forEach(a -> INDEKS_AVSLAGSÅRSAK_VILKÅR.computeIfAbsent(a, (k) -> new HashSet<>(4)).add(v));
        }
    }

    @JsonIgnore
    private Map<FagsakYtelseType, String> lovReferanser = Map.of();
    @JsonIgnore
    private String navn;
    @JsonIgnore
    private Set<Avslagsårsak> avslagsårsaker;
    private String kode;

    private VilkårType(String kode) {
        this.kode = kode;
    }

    private VilkårType(String kode,
                       String navn,
                       Map<FagsakYtelseType, String> lovReferanser,
                       Avslagsårsak... avslagsårsaker) {
        this.kode = kode;
        this.navn = navn;
        this.lovReferanser = lovReferanser;
        this.avslagsårsaker = Set.of(avslagsårsaker);

    }

    @JsonCreator(mode = Mode.DELEGATING)
    public static VilkårType fraKode(Object node) {
        if (node == null) {
            return null;
        }
        String kode = TempAvledeKode.getVerdi(VilkårType.class, node, "kode");
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent VilkårType: for input " + node);
        }
        return ad;
    }

    public static Map<String, VilkårType> kodeMap() {
        return Collections.unmodifiableMap(KODER);
    }

    public static Map<VilkårType, Set<Avslagsårsak>> finnAvslagårsakerGruppertPåVilkårType() {
        return Collections.unmodifiableMap(INDEKS_VILKÅR_AVSLAGSÅRSAKER);
    }

    public static Set<VilkårType> getVilkårTyper(Avslagsårsak avslagsårsak) {
        return INDEKS_AVSLAGSÅRSAK_VILKÅR.get(avslagsårsak);
    }

    /**
     * for JAX-RS.
     */
    public static VilkårType fromString(String kode) {
        return fraKode(kode);
    }

    public String getLovReferanse(FagsakYtelseType fagsakYtelseType) {
        return lovReferanser.get(fagsakYtelseType);
    }

    @Override
    public String getNavn() {
        return navn;
    }

    public Set<Avslagsårsak> getAvslagsårsaker() {
        return avslagsårsaker;
    }

    @JsonProperty(value = "kodeverk", access = JsonProperty.Access.READ_ONLY)
    @Override
    public String getKodeverk() {
        return KODEVERK;
    }

    @JsonProperty(value = "kode")
    @Override
    public String getKode() {
        return kode;
    }

    @Override
    public String getOffisiellKode() {
        return getKode();
    }

    static class Serializer extends StdSerializer<VilkårType> {

        public Serializer() {
            super(VilkårType.class);
        }

        @Override
        public void serialize(VilkårType value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
            jgen.writeStartObject();
            jgen.writeStringField("kode", value.getKode());
            jgen.writeStringField("kodeverk", value.getKodeverk());
            jgen.writeEndObject();
        }

    }

}
