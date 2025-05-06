package no.nav.ung.kodeverk.vilkår;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator.Mode;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import no.nav.ung.kodeverk.TempAvledeKode;
import no.nav.ung.kodeverk.api.Kodeverdi;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;

import java.util.*;

@JsonFormat(shape = Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public enum VilkårType implements Kodeverdi {
    ALDERSVILKÅR("K9_VK_3",
        "Aldersvilkåret",
        Map.of(
            FagsakYtelseType.PLEIEPENGER_SYKT_BARN, "§ 9-3 første ledd",
            FagsakYtelseType.PLEIEPENGER_NÆRSTÅENDE, "§ 9-3 første ledd",
            FagsakYtelseType.OMSORGSPENGER_AO, "§ 9-3 første ledd",
            FagsakYtelseType.OMSORGSPENGER_KS, "§ 9-3 første ledd",
            FagsakYtelseType.OMSORGSPENGER_MA, "§ 9-3 første ledd",
            FagsakYtelseType.UNGDOMSYTELSE, ""), // TODO: Finn riktig paragraf. Vurder å trekke ut til egen VilkårsType.
        Avslagsårsak.SØKER_OVER_HØYESTE_ALDER),
    SØKNADSFRIST("FP_VK_3",
        "Søknadsfristvilkåret",
        Map.of(FagsakYtelseType.OMSORGSPENGER, "§ 22-13 tredje ledd",
            FagsakYtelseType.PLEIEPENGER_SYKT_BARN, "§ 22-13 tredje ledd",
            FagsakYtelseType.OPPLÆRINGSPENGER, "§ 22-13 tredje ledd",
            FagsakYtelseType.PLEIEPENGER_NÆRSTÅENDE, "§ 22-13 tredje ledd"),
        Avslagsårsak.SØKT_FOR_SENT),
    SØKERSOPPLYSNINGSPLIKT("FP_VK_34",
        "Søkers opplysningsplikt",
        Map.of(FagsakYtelseType.UNGDOMSYTELSE, "forskrift om ungdomsprogrammet § 4"),
        Avslagsårsak.MANGLENDE_DOKUMENTASJON),
    // TODO: Gå over dette før lansering
    UNGDOMSPROGRAMVILKÅRET(
        "UNG_VK_XXX",
        "Deltar i ungdomsprogrammet",
        Map.of(FagsakYtelseType.UNGDOMSYTELSE, "§ xxx"),
        Avslagsårsak.OPPHØRT_UNGDOMSPROGRAM
    ),
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

    // Overstyr standard toString() slik at generert openapi spesifikasjon blir enum med kode verdi, samtidig som legacy
    // serialisering fungerer som før.
    public String toString() {
        return this.getKode();
    }

    @Override
    public String getOffisiellKode() {
        return getKode();
    }

}
