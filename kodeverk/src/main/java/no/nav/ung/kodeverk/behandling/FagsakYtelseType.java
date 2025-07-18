package no.nav.ung.kodeverk.behandling;

import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import no.nav.ung.kodeverk.api.Kodeverdi;

import java.io.IOException;
import java.util.*;

public enum FagsakYtelseType implements Kodeverdi {

    /** Folketrygdloven K4 ytelser. */
    DAGPENGER("DAG", "Dagpenger", null, null),

    /** Ny ytelse for kompenasasjon for koronatiltak for Selvstendig næringsdrivende og Frilansere (Anmodning 10). */
    FRISINN("FRISINN", "FRIlansere og Selvstendig næringsdrivendes INNtektskompensasjon", "FRI", "FRI"),

    /** Folketrygdloven K8 ytelser. */
    SYKEPENGER("SP", "Sykepenger", null, null),

    /** Folketrygdloven K9 ytelser. */
    PLEIEPENGER_SYKT_BARN("PSB", "Pleiepenger sykt barn", "PN", "OMS"),
    PLEIEPENGER_NÆRSTÅENDE("PPN", "Pleiepenger livets sluttfase", "PP", "OMS"),
    OMSORGSPENGER("OMP", "Omsorgspenger", "OM", "OMS"),
    OMSORGSPENGER_KS("OMP_KS", "Ekstra omsorgsdager kronisk syk", "OM", "OMS"),
    OMSORGSPENGER_MA("OMP_MA", "Ekstra omsorgsdager midlertidig alene", "OM", "OMS"),
    OMSORGSPENGER_AO("OMP_AO", "Alene om omsorgen", "OM", "OMS"),

    OPPLÆRINGSPENGER("OLP", "Opplæringspenger", "OP", "OMS"),

    /** Folketrygdloven K11 ytelser. */
    ARBEIDSAVKLARINGSPENGER("AAP", "Arbeidsavklaringspenger", null, null),

    /** Folketrygdloven K14 ytelser. */
    ENGANGSTØNAD("ES", "Engangsstønad", null, null),
    FORELDREPENGER("FP", "Foreldrepenger", null, null),
    SVANGERSKAPSPENGER("SVP", "Svangerskapspenger", null, null),

    /** Folketrygdloven K15 ytelser. */
    ENSLIG_FORSØRGER("EF", "Enslig forsørger", null, null),

    /** Folketrygdloven ?? ytelser. */
    UNGDOMSYTELSE("UNG", "Ungdomsprogramytelse", null, "UNG"){
        @Override
        public boolean vurderÅpneOppgaverFørVedtak() {
            return false;
        }
    },



    OBSOLETE("OBSOLETE", "Kun brukt for å markere noen som utgått - ikke en gyldig type i seg selv", null, null),
    UDEFINERT("-", "Ikke definert", null, null),
    ;

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
    public static final FagsakYtelseType PPN = PLEIEPENGER_NÆRSTÅENDE;
    public static final FagsakYtelseType OLP = OPPLÆRINGSPENGER;
    public static final FagsakYtelseType OMP = OMSORGSPENGER;
    public static final FagsakYtelseType FP = FORELDREPENGER;
    public static final FagsakYtelseType SVP = SVANGERSKAPSPENGER;

    /** Ytelser som er relatert til søker, for samlet innhenting etc.. */
    public static final Set<FagsakYtelseType> RELATERT_YTELSE_TYPER_FOR_SØKER = Collections.unmodifiableSet(EnumSet.complementOf(EnumSet.of(OBSOLETE, UDEFINERT)));

    private String navn;

    private String kode;

    private String infotrygdBehandlingstema;

    private String oppgavetema;

    private FagsakYtelseType(String kode) {
        this.kode = kode;
    }

    private FagsakYtelseType(String kode, String navn, String infotrygdBehandlingstema, String oppgavetema) {
        this.kode = kode;
        this.navn = navn;
        this.infotrygdBehandlingstema = infotrygdBehandlingstema;
        this.oppgavetema = oppgavetema;
    }

    public static FagsakYtelseType fraKode(final String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent FagsakYtelseType: for input " + kode);
        }
        return ad;
    }

    public static Map<String, FagsakYtelseType> kodeMap() {
        return Collections.unmodifiableMap(KODER);
    }

    @JsonValue
    @Override
    public String getKode() {
        return kode;
    }

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

    public String getInfotrygdBehandlingstema() {
        return infotrygdBehandlingstema;
    }

    public String getOppgavetema() {
        return oppgavetema;
    }

    public static FagsakYtelseType fromString(String kode) {
        return fraKode(kode);
    }

    /** Hvilke ytelser som gir opptjening for angitt ytelse. */
    private static final Map<FagsakYtelseType, Set<FagsakYtelseType>> OPPTJENING_RELATERTYTELSE = Map.of(
        FRISINN, Set.of(SYKEPENGER,
            SVANGERSKAPSPENGER,
            FORELDREPENGER,
            DAGPENGER,
            ARBEIDSAVKLARINGSPENGER,
            PLEIEPENGER_SYKT_BARN,
            PLEIEPENGER_NÆRSTÅENDE,
            OMSORGSPENGER,
            OPPLÆRINGSPENGER),
        PLEIEPENGER_SYKT_BARN, Set.of(SYKEPENGER,
            SVANGERSKAPSPENGER,
            FORELDREPENGER,
            DAGPENGER,
            ENSLIG_FORSØRGER,
            PLEIEPENGER_SYKT_BARN,
            PLEIEPENGER_NÆRSTÅENDE,
            OMSORGSPENGER,
            OPPLÆRINGSPENGER,
            FRISINN),
        OPPLÆRINGSPENGER, Set.of(SYKEPENGER,
            SVANGERSKAPSPENGER,
            FORELDREPENGER,
            DAGPENGER,
            ENSLIG_FORSØRGER,
            PLEIEPENGER_SYKT_BARN,
            PLEIEPENGER_NÆRSTÅENDE,
            OMSORGSPENGER,
            OPPLÆRINGSPENGER,
            FRISINN),
        PLEIEPENGER_NÆRSTÅENDE, Set.of(SYKEPENGER,
            SVANGERSKAPSPENGER,
            FORELDREPENGER,
            DAGPENGER,
            ENSLIG_FORSØRGER,
            PLEIEPENGER_SYKT_BARN,
            PLEIEPENGER_NÆRSTÅENDE,
            OMSORGSPENGER,
            OPPLÆRINGSPENGER,
            FRISINN),
        OMSORGSPENGER, Set.of(SYKEPENGER,
            SVANGERSKAPSPENGER,
            FORELDREPENGER,
            DAGPENGER,
            ENSLIG_FORSØRGER,
            PLEIEPENGER_SYKT_BARN,
            PLEIEPENGER_NÆRSTÅENDE,
            OMSORGSPENGER,
            OPPLÆRINGSPENGER,
            FRISINN));

    /** Hvilke K9-ytelser som skal sjekkes mot overlapp */
    private static final Map<FagsakYtelseType, Set<FagsakYtelseType>> OVERLAPPSJEKK_RELATERT_YTELSE_K9 = Map.of(
        PLEIEPENGER_SYKT_BARN, Set.of(
            PLEIEPENGER_SYKT_BARN,
            PLEIEPENGER_NÆRSTÅENDE,
            OMSORGSPENGER,
            OPPLÆRINGSPENGER),
        PLEIEPENGER_NÆRSTÅENDE, Set.of(
            PLEIEPENGER_SYKT_BARN,
            PLEIEPENGER_NÆRSTÅENDE,
            OMSORGSPENGER,
            OPPLÆRINGSPENGER),
        OPPLÆRINGSPENGER, Set.of(
            PLEIEPENGER_SYKT_BARN,
            PLEIEPENGER_NÆRSTÅENDE,
            OMSORGSPENGER,
            OPPLÆRINGSPENGER),
        OMSORGSPENGER, Set.of(
            PLEIEPENGER_NÆRSTÅENDE,
            PLEIEPENGER_SYKT_BARN,
            OPPLÆRINGSPENGER)
    );

    /** Hvilke eksterne ytelser som skal sjekkes mot overlapp */
    private static final Map<FagsakYtelseType, Set<FagsakYtelseType>> OVERLAPPSJEKK_RELATERT_YTELSE_EKSTERN = Map.of(
        PLEIEPENGER_SYKT_BARN, Set.of(
            SYKEPENGER,
            FORELDREPENGER),
        PLEIEPENGER_NÆRSTÅENDE, Set.of(
            SYKEPENGER),
        OPPLÆRINGSPENGER, Set.of(
            SYKEPENGER,
            FORELDREPENGER),
        OMSORGSPENGER, Set.of(
            SYKEPENGER,
            FORELDREPENGER)
    );

    /** Hvorvidt data for ektefelle/samboer og pleietrengende skal benyttes for ytelsen. */
    private static final Set<FagsakYtelseType> HAR_RELATERTE_PERSONER = Collections.unmodifiableSet(EnumSet.of(
        PLEIEPENGER_SYKT_BARN,
        PLEIEPENGER_NÆRSTÅENDE,
        OPPLÆRINGSPENGER,
        OMSORGSPENGER,
        OMSORGSPENGER_KS,
        OMSORGSPENGER_MA,
        OMSORGSPENGER_AO,
        FORELDREPENGER,
        ENGANGSTØNAD));
    private static final Set<FagsakYtelseType> BEHANDLES_I_K9_SAK = Collections.unmodifiableSet(EnumSet.of(
        PLEIEPENGER_SYKT_BARN,
        PLEIEPENGER_NÆRSTÅENDE,
        OPPLÆRINGSPENGER,
        OMSORGSPENGER,
        OMSORGSPENGER_KS,
        OMSORGSPENGER_MA,
        OMSORGSPENGER_AO));

    /** Hvorvidt ytelsetypen omfattes av kap 8 i folketrygdloven. */
    private static final Set<FagsakYtelseType> OMFATTES_AV_KAP_8 = Collections.unmodifiableSet(EnumSet.of(
        PLEIEPENGER_SYKT_BARN,
        PLEIEPENGER_NÆRSTÅENDE,
        OPPLÆRINGSPENGER,
        OMSORGSPENGER,
        FORELDREPENGER,
        SVANGERSKAPSPENGER,
        SYKEPENGER));

    public boolean girOpptjeningsTid(FagsakYtelseType ytelseType) {
        final var relatertYtelseTypeSet = OPPTJENING_RELATERTYTELSE.get(ytelseType);
        if (relatertYtelseTypeSet == null) {
            throw new IllegalStateException("Støtter ikke fagsakYtelseType" + ytelseType);
        }
        return relatertYtelseTypeSet.contains(this);
    }

    public Set<FagsakYtelseType> hentK9YtelserForOverlappSjekk() {
        return OVERLAPPSJEKK_RELATERT_YTELSE_K9.getOrDefault(this, Set.of());
    }

    public Set<FagsakYtelseType> hentEksterneYtelserForOverlappSjekk() {
        return OVERLAPPSJEKK_RELATERT_YTELSE_EKSTERN.getOrDefault(this, Set.of());
    }

    // Kan igrunn fjernast viss dei typer som spesifiserer at denne skal brukast blir serialisert med openapi-compat, eller
    // anna ObjectMapper som ikkje har overstyring av Kodeverdi til objekt serialisering, sidan annotasjoner på denne type no
    // spesifiserer @JsonValue på kode property.
    public static final class PlainYtelseSerializer extends StdSerializer<FagsakYtelseType> {
        public PlainYtelseSerializer() {
            super(FagsakYtelseType.class);
        }

        @Override
        public void serialize(FagsakYtelseType value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writeString(value.getKode());
        }
    }

    // Kan igrunn fjernast viss dei typer som spesifiserer at denne skal brukast blir deserialisert med openapi-compat, eller
    // anna ObjectMapper som ikkje har overstyring av Kodeverdi til objekt serialisering, sidan annotasjoner på denne type no
    // spesifiserer @JsonValue på kode property.
    public static final class PlainYtelseDeserializer extends StdDeserializer<FagsakYtelseType> {

        public PlainYtelseDeserializer() {
            super(FagsakYtelseType.class);
        }

        @Override
        public FagsakYtelseType deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
            return FagsakYtelseType.fraKode(p.getText());
        }

    }

    public boolean harRelatertePersoner() {
        return HAR_RELATERTE_PERSONER.contains(this);
    }

    public boolean erRammevedtak() {
        return this == OMSORGSPENGER_KS || this == OMSORGSPENGER_MA || this == OMSORGSPENGER_AO;
    }

    public boolean erK9Ytelse() {
        return BEHANDLES_I_K9_SAK.contains(this);
    }

    public boolean omfattesAvK8() {
        return OMFATTES_AV_KAP_8.contains(this);
    }

    public boolean vurderÅpneOppgaverFørVedtak() {
        return true;
    }

    void requireNull(String value, String type) {
        if (value != null) {
            throw new IllegalArgumentException(type + " må være null, fikk: " + value + ", for ytelseType=" + this);
        }

    }
}
