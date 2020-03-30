package no.nav.k9.kodeverk.behandling;

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

@JsonFormat(shape = Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public enum FagsakYtelseType implements Kodeverdi {

    /** Folketrygdloven K4 ytelser. */
    DAGPENGER("DAG", "Dagpenger"),

    /** Folketrygdloven K8 ytelser. */
    SYKEPENGER("SP", "Sykepenger"),

    /** Folketrygdloven K9 ytelser. */
    PLEIEPENGER_SYKT_BARN("PSB", "Pleiepenger sykt barn"),
    PLEIEPENGER_NÆRSTÅENDE("PPN", "Pleiepenger nærstående"),
    OMSORGSPENGER("OMP", "Omsorgspenger"),
    OPPLÆRINGSPENGER("OLP", "Opplæringspenger"),

    /** @deprecated Gammel infotrygd kode for K9 ytelser. Må tolkes om til ovenstående sammen med TemaUnderkategori. */
    @Deprecated
    PÅRØRENDESYKDOM("PS", "Pårørende sykdom"),

    /** Folketrygdloven K11 ytelser. */
    ARBEIDSAVKLARINGSPENGER("AAP"),

    /** Folketrygdloven K14 ytelser. */
    ENGANGSTØNAD("ES", "Engangsstønad"),
    FORELDREPENGER("FP", "Foreldrepenger"),
    SVANGERSKAPSPENGER("SVP", "Svangerskapspenger"),

    /** Folketrygdloven K15 ytelser. */
    ENSLIG_FORSØRGER("EF", "Enslig forsørger"),

    UDEFINERT("-", "Ikke definert"),;

    public static final String KODEVERK = "FAGSAK_YTELSE"; //$NON-NLS-1$

    private static final Map<String, FagsakYtelseType> KODER = new LinkedHashMap<>();

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

    // shortcut konstanter

    public static final FagsakYtelseType ES = ENGANGSTØNAD;
    public static final FagsakYtelseType PSB = PLEIEPENGER_SYKT_BARN;
    public static final FagsakYtelseType OMP = OMSORGSPENGER;
    public static final FagsakYtelseType FP = FORELDREPENGER;
    public static final FagsakYtelseType SVP = SVANGERSKAPSPENGER;

    @JsonIgnore
    private String navn;

    private String kode;

    private FagsakYtelseType(String kode) {
        this.kode = kode;
    }

    private FagsakYtelseType(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    @JsonCreator
    public static FagsakYtelseType fraKode(@JsonProperty("kode") String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent FagsakYtelseType: " + kode);
        }
        return ad;
    }

    public static Map<String, FagsakYtelseType> kodeMap() {
        return Collections.unmodifiableMap(KODER);
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

    public static void main(String[] args) {
        System.out.println(KODER.keySet());
    }

    /**
     * @deprecated Ikke switch på dette i koden. Marker heller klasse og pakke for angitt ytelse (eks. behandlingssteg, aksjonspuntutleder,
     *             kompletthetsjekk).
     *             Til nød bruk en negativ guard
     *             <code>if(!ENGANGSSTØNAD.getKode().equals(this.getKode())) throw IllegalStateException("No trespassing in this code"); </code>
     */
    @Deprecated
    public final boolean gjelderEngangsstønad() {
        return ENGANGSTØNAD.getKode().equals(this.getKode());
    }

    /**
     * @deprecated Ikke switch på dette i koden. Marker heller klasse og pakke for angitt ytelse (eks. behandlingssteg, aksjonspuntutleder,
     *             kompletthetsjekk)
     *             Til nød bruk en negativ guard
     *             <code>if(!FORELDREPENGER.getKode().equals(this.getKode())) throw IllegalStateException("No trespassing in this code"); </code>
     */
    @Deprecated
    public final boolean gjelderForeldrepenger() {
        return FORELDREPENGER.getKode().equals(this.getKode());
    }

    /**
     * @deprecated Ikke switch på dette i koden. Marker heller klasse og pakke for angitt ytelse (eks. behandlingssteg, aksjonspuntutleder,
     *             kompletthetsjekk)
     *             Til nød bruk en negativ guard
     *             <code>if(!SVANGERSKAPSPENGER.getKode().equals(this.getKode())) throw IllegalStateException("No trespassing in this code"); </code>
     */
    @Deprecated
    public final boolean gjelderSvangerskapspenger() {
        return SVANGERSKAPSPENGER.getKode().equals(this.getKode());
    }

    private static final Map<FagsakYtelseType, Set<FagsakYtelseType>> OPPTJENING_RELATERTYTELSE_CONFIG = Map.of(
        FORELDREPENGER,
        Set.of(ENSLIG_FORSØRGER, SYKEPENGER, SVANGERSKAPSPENGER, FORELDREPENGER, DAGPENGER,
            PÅRØRENDESYKDOM, PLEIEPENGER_SYKT_BARN, PLEIEPENGER_NÆRSTÅENDE, OMSORGSPENGER, OPPLÆRINGSPENGER),

        SVANGERSKAPSPENGER,
        Set.of(SYKEPENGER, SVANGERSKAPSPENGER, FORELDREPENGER, DAGPENGER,
            PÅRØRENDESYKDOM, PLEIEPENGER_SYKT_BARN, PLEIEPENGER_NÆRSTÅENDE, OMSORGSPENGER, OPPLÆRINGSPENGER),

        // FIXME K9 Verdiene under er høyst sannsynlig feil -- kun lagt inn for å komme videre i verdikjedetest.
        PLEIEPENGER_SYKT_BARN,
        Set.of(SYKEPENGER, SVANGERSKAPSPENGER, FORELDREPENGER, DAGPENGER, ENSLIG_FORSØRGER,
            PÅRØRENDESYKDOM, PLEIEPENGER_SYKT_BARN, PLEIEPENGER_NÆRSTÅENDE, OMSORGSPENGER, OPPLÆRINGSPENGER),
        // FIXME K9 Verdiene under er høyst sannsynlig feil -- kun lagt inn for å komme videre i verdikjedetest.
        OMSORGSPENGER,
        Set.of(SYKEPENGER, SVANGERSKAPSPENGER, FORELDREPENGER, DAGPENGER, ENSLIG_FORSØRGER,
            PÅRØRENDESYKDOM, PLEIEPENGER_SYKT_BARN, PLEIEPENGER_NÆRSTÅENDE, OMSORGSPENGER, OPPLÆRINGSPENGER));

    public boolean girOpptjeningsTid(FagsakYtelseType ytelseType) {
        final var relatertYtelseTypeSet = OPPTJENING_RELATERTYTELSE_CONFIG.get(ytelseType);
        if (relatertYtelseTypeSet == null) {
            throw new IllegalStateException("Støtter ikke fagsakYtelseType" + ytelseType);
        }
        return relatertYtelseTypeSet.contains(this);
    }

}
