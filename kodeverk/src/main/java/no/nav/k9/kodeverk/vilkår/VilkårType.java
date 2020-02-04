package no.nav.k9.kodeverk.vilkår;

import java.util.Collections;
import java.util.HashSet;
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
public enum VilkårType implements Kodeverdi {

    MEDLEMSKAPSVILKÅRET(VilkårTypeKoder.FP_VK_2,
        "Medlemskapsvilkåret",
        Map.of(FagsakYtelseType.FORELDREPENGER, "§ 14-2"),
        Avslagsårsak.SØKER_ER_IKKE_MEDLEM,
        Avslagsårsak.SØKER_ER_UTVANDRET,
        Avslagsårsak.SØKER_HAR_IKKE_LOVLIG_OPPHOLD,
        Avslagsårsak.SØKER_HAR_IKKE_OPPHOLDSRETT,
        Avslagsårsak.SØKER_ER_IKKE_BOSATT),
    MEDLEMSKAPSVILKÅRET_LØPENDE(VilkårTypeKoder.FP_VK_2_L,
        "Løpende medlemskapsvilkår",
        Map.of(FagsakYtelseType.FORELDREPENGER, "§ 14-2"),
        Avslagsårsak.SØKER_ER_IKKE_MEDLEM,
        Avslagsårsak.SØKER_ER_UTVANDRET,
        Avslagsårsak.SØKER_HAR_IKKE_LOVLIG_OPPHOLD,
        Avslagsårsak.SØKER_HAR_IKKE_OPPHOLDSRETT,
        Avslagsårsak.SØKER_ER_IKKE_BOSATT),
    MEDISINSKEVILKÅR(VilkårTypeKoder.PSB_VK_1,
        "Medisinskevilkår",
        Map.of(FagsakYtelseType.PLEIEPENGER_SYKT_BARN, "§ 14-2"),
        Avslagsårsak.IKKE_MEDISINSK_BEHOV),
    SØKERSOPPLYSNINGSPLIKT(VilkårTypeKoder.FP_VK_34,
        "Søkers opplysningsplikt",
        Map.of(FagsakYtelseType.ENGANGSTØNAD, "§§ 21-3 og 21-7", FagsakYtelseType.FORELDREPENGER, "§§ 21-3 og 21-7"),
        Avslagsårsak.MANGLENDE_DOKUMENTASJON),
    OPPTJENINGSPERIODEVILKÅR(VilkårTypeKoder.FP_VK_21,
        "Opptjeningsperiode",
        Map.of(FagsakYtelseType.FORELDREPENGER, "§ 14-6 og 14-10"),
        Avslagsårsak.IKKE_TILSTREKKELIG_OPPTJENING),
    OPPTJENINGSVILKÅRET(VilkårTypeKoder.FP_VK_23,
        "Opptjeningsvilkåret",
        Map.of(FagsakYtelseType.FORELDREPENGER, "§ 14-6"),
        Avslagsårsak.IKKE_TILSTREKKELIG_OPPTJENING),
    BEREGNINGSGRUNNLAGVILKÅR(VilkårTypeKoder.FP_VK_41,
        "Beregning",
        Map.of(FagsakYtelseType.FORELDREPENGER, "§ 14-7"),
        Avslagsårsak.FOR_LAVT_BEREGNINGSGRUNNLAG),

    /**
     * Brukes i stedet for null der det er optional.
     */
    UDEFINERT("-", "Ikke definert", Map.of()),

    ;

    private static final Map<String, VilkårType> KODER = new LinkedHashMap<>();
    private static final Map<VilkårType, Set<Avslagsårsak>> INDEKS_VILKÅR_AVSLAGSÅRSAKER = new LinkedHashMap<>(); // NOSONAR
    private static final Map<Avslagsårsak, Set<VilkårType>> INDEKS_AVSLAGSÅRSAK_VILKÅR = new LinkedHashMap<>(); // NOSONAR
    public static final String KODEVERK = "VILKAR_TYPE";

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

    public String getLovReferanse(FagsakYtelseType fagsakYtelseType) {
        return lovReferanser.get(fagsakYtelseType);
    }

    @Override
    public String toString() {
        return super.toString() + lovReferanser;
    }

    @Override
    public String getNavn() {
        return navn;
    }

    @JsonCreator
    public static VilkårType fraKode(@JsonProperty("kode") String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent VilkårType: " + kode);
        }
        return ad;
    }

    public static Map<String, VilkårType> kodeMap() {
        return Collections.unmodifiableMap(KODER);
    }

    public Set<Avslagsårsak> getAvslagsårsaker() {
        return avslagsårsaker;
    }

    public static Map<VilkårType, Set<Avslagsårsak>> finnAvslagårsakerGruppertPåVilkårType() {
        return Collections.unmodifiableMap(INDEKS_VILKÅR_AVSLAGSÅRSAKER);
    }

    public static Set<VilkårType> getVilkårTyper(Avslagsårsak avslagsårsak) {
        return INDEKS_AVSLAGSÅRSAK_VILKÅR.get(avslagsårsak);
    }

    @JsonProperty
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

            INDEKS_VILKÅR_AVSLAGSÅRSAKER.put(v, v.avslagsårsaker);
            v.avslagsårsaker.forEach(a -> INDEKS_AVSLAGSÅRSAK_VILKÅR.computeIfAbsent(a, (k) -> new HashSet<>(4)).add(v));
        }
    }

}
