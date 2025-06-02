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
    ALDERSVILKÅR("UNG_VK_1",
        "Aldersvilkåret",
        Map.of(FagsakYtelseType.UNGDOMSYTELSE, "Forskrift om ungdomsprogram og ungdomsprogramytelse § 3 bokstav a"), // TODO: Finn riktig paragraf
        Avslagsårsak.SØKER_OVER_HØYESTE_ALDER,
        Avslagsårsak.SØKER_UNDER_MINSTE_ALDER),
    SØKNADSFRIST("UNG_VK_3",
        "Søknadsfristvilkåret",
        Map.of(FagsakYtelseType.UNGDOMSYTELSE, ""), // TODO: Legg til lovreferanse fra arbeidsmarkedsloven
        Avslagsårsak.SØKT_FOR_SENT),
    SØKERSOPPLYSNINGSPLIKT("UNG_VK_4",
        "Søkers opplysningsplikt",
        Map.of(FagsakYtelseType.UNGDOMSYTELSE, "Forskrift om ungdomsprogram og ungdomsprogramytelse § 4"),
        Avslagsårsak.MANGLENDE_DOKUMENTASJON),
    UNGDOMSPROGRAMVILKÅRET(
        "UNG_VK_2",
        "Deltar i ungdomsprogrammet",
        Map.of(FagsakYtelseType.UNGDOMSYTELSE, "Forskrift om ungdomsprogram og ungdomsprogramytelse § 8"),
        Avslagsårsak.ENDRET_STARTDATO_UNGDOMSPROGRAM,
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
