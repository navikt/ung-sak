package no.nav.k9.kodeverk.behandling;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.util.Collections;
import java.util.EnumSet;
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
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import no.nav.k9.kodeverk.TempAvledeKode;
import no.nav.k9.kodeverk.api.Kodeverdi;
import no.nav.k9.sak.typer.AktørId;

@JsonFormat(shape = Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public enum FagsakYtelseType implements Kodeverdi {

    /** Folketrygdloven K4 ytelser. */
    DAGPENGER("DAG", "Dagpenger", null, null),

    /** Ny ytelse for kompenasasjon for koronatiltak for Selvstendig næringsdrivende og Frilansere (Anmodning 10). */
    FRISINN("FRISINN", "FRIlansere og Selvstendig næringsdrivendes INNtektskompensasjon", "FRI", "FRI") {
        @Override
        public void validerNøkkelParametere(String pleietrengendeAktørId, String relatertPersonAktørId) {
            requireNull(pleietrengendeAktørId, "pleietrengende");
            requireNull(relatertPersonAktørId, "relatertPerson");
        }

        @Override
        public boolean vurderÅpneOppgaverFørVedtak() {
            return false;
        }
    },

    /** Folketrygdloven K8 ytelser. */
    SYKEPENGER("SP", "Sykepenger", null, null),

    /** Folketrygdloven K9 ytelser. */
    PLEIEPENGER_SYKT_BARN("PSB", "Pleiepenger sykt barn", "PN", "OMS") {
        @Override
        public void validerNøkkelParametere(String pleietrengendeAktørId, String relatertPersonAktørId) {
            requireNonNull(pleietrengendeAktørId, "pleietrengende");
            requireNull(relatertPersonAktørId, "relatertPerson");
        }
    },
    PLEIEPENGER_NÆRSTÅENDE("PPN", "Pleiepenger nærstående", null, "OMS") {
        @Override
        public void validerNøkkelParametere(String pleietrengendeAktørId, String relatertPersonAktørId) {
            requireNonNull(pleietrengendeAktørId, "pleietrengende");
            requireNull(relatertPersonAktørId, "relatertPerson");
        }
    },
    OMSORGSPENGER("OMP", "Omsorgspenger", "OM", "OMS") {
        @Override
        public void validerNøkkelParametere(String pleietrengendeAktørId, String relatertPersonAktørId) {
            requireNull(pleietrengendeAktørId, "pleietrengende");
            requireNull(relatertPersonAktørId, "relatertPerson");
        }

    },
    OMSORGSPENGER_KS("OMP_KS", "Ekstra omsorgsdager kronisk syk", "OM", "OMS") {
        @Override
        public void validerNøkkelParametere(String pleietrengendeAktørId, String relatertPersonAktørId) {
            requireNonNull(pleietrengendeAktørId, "pleietrengende");
            requireNull(relatertPersonAktørId, "relatertPerson");
        }

        @Override
        public boolean vurderÅpneOppgaverFørVedtak() {
            return false;
        }
    },
    OMSORGSPENGER_MA("OMP_MA", "Ekstra omsorgsdager midlertidig alene", "OM", "OMS") {
        @Override
        public void validerNøkkelParametere(String pleietrengendeAktørId, String relatertPersonAktørId) {
            requireNull(pleietrengendeAktørId, "pleietrengende");
            requireNonNull(relatertPersonAktørId, "relatertPerson");
        }

        @Override
        public boolean vurderÅpneOppgaverFørVedtak() {
            return false;
        }
    },
    OMSORGSPENGER_AO("OMP_AO", "Alene om omsorgen", "OM", "OMS") {
        @Override
        public void validerNøkkelParametere(String pleietrengendeAktørId, String relatertPersonAktørId) {
            requireNonNull(pleietrengendeAktørId, "pleietrengende");
            requireNull(relatertPersonAktørId, "relatertPerson");
        }

        @Override
        public boolean vurderÅpneOppgaverFørVedtak() {
            return false;
        }
    },

    OPPLÆRINGSPENGER("OLP", "Opplæringspenger", null, "OMS"),

    /** @deprecated Gammel infotrygd kode for K9 ytelser. Må tolkes om til ovenstående sammen med TemaUnderkategori. */
    @Deprecated
    PÅRØRENDESYKDOM("PS", "Pårørende sykdom", null, null),

    /** Folketrygdloven K11 ytelser. */
    ARBEIDSAVKLARINGSPENGER("AAP", "Arbeidsavklaringspenger", null, null),

    /** Folketrygdloven K14 ytelser. */
    ENGANGSTØNAD("ES", "Engangsstønad", null, null),
    FORELDREPENGER("FP", "Foreldrepenger", null, null),
    SVANGERSKAPSPENGER("SVP", "Svangerskapspenger", null, null),

    /** Folketrygdloven K15 ytelser. */
    ENSLIG_FORSØRGER("EF", "Enslig forsørger", null, null),

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
    public static final FagsakYtelseType OMP = OMSORGSPENGER;
    public static final FagsakYtelseType FP = FORELDREPENGER;
    public static final FagsakYtelseType SVP = SVANGERSKAPSPENGER;

    /** Ytelser som er relatert til søker, for samlet innhenting etc.. */
    public static final Set<FagsakYtelseType> RELATERT_YTELSE_TYPER_FOR_SØKER = Collections.unmodifiableSet(EnumSet.complementOf(EnumSet.of(PÅRØRENDESYKDOM, OBSOLETE, UDEFINERT)));

    @JsonIgnore
    private String navn;

    private String kode;

    @JsonIgnore
    private String infotrygdBehandlingstema;

    @JsonIgnore
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

    @JsonCreator(mode = Mode.DELEGATING)
    public static FagsakYtelseType fraKode(Object node) {
        if (node == null) {
            return null;
        }
        String kode = TempAvledeKode.getVerdi(FagsakYtelseType.class, node, "kode");
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent FagsakYtelseType: for input " + node);
        }
        return ad;
    }

    public static Map<String, FagsakYtelseType> kodeMap() {
        return Collections.unmodifiableMap(KODER);
    }

    @JsonProperty(value = "kode")
    @Override
    public String getKode() {
        return kode;
    }

    @JsonProperty(value = "kodeverk", access = JsonProperty.Access.READ_ONLY)
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
            PÅRØRENDESYKDOM,
            PLEIEPENGER_SYKT_BARN,
            PLEIEPENGER_NÆRSTÅENDE,
            OMSORGSPENGER,
            OPPLÆRINGSPENGER),
        PLEIEPENGER_SYKT_BARN, Set.of(SYKEPENGER,
            SVANGERSKAPSPENGER,
            FORELDREPENGER,
            DAGPENGER,
            ENSLIG_FORSØRGER,
            PÅRØRENDESYKDOM,
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
            PÅRØRENDESYKDOM,
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

    public static final class PlainYtelseSerializer extends StdSerializer<FagsakYtelseType> {
        public PlainYtelseSerializer() {
            super(FagsakYtelseType.class);
        }

        @Override
        public void serialize(FagsakYtelseType value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writeString(value.getKode());
        }
    }

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

    @SuppressWarnings("unused")
    public void validerNøkkelParametere(String pleietrengendeAktørId, String relatertPersonAktørId) {
        throw new UnsupportedOperationException("støtter ikke ytelsetype: " + this);
    }

    public void validerNøkkelParametere(AktørId pleietrengendeAktørId, AktørId relatertPersonAktørId) {
        validerNøkkelParametere(pleietrengendeAktørId == null ? null : pleietrengendeAktørId.getAktørId(), relatertPersonAktørId == null ? null : relatertPersonAktørId.getAktørId());
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
