package no.nav.k9.kodeverk.behandling;

import java.io.IOException;
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
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import no.nav.k9.kodeverk.api.Kodeverdi;

@JsonFormat(shape = Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public enum FagsakYtelseType implements Kodeverdi {

    /** Folketrygdloven K4 ytelser. */
    DAGPENGER("DAG", "Dagpenger"),

    /** Ny ytelse for kompenasasjon for koronatiltak for Selvstendig næringsdrivende og Frilansere (Anmodning 10). */
    FRISINN("FRISINN", "FRIlansere og Selstendig næringsdrivendes INNtektskompensasjon"),

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

    OBSOLETE("OBSOLETE", "Kun brukt for å markere noen som utgått - ikke en gyldig type i seg selv"),
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

    @JsonProperty(value="kode")
    @Override
    public String getKode() {
        return kode;
    }

    @JsonProperty(value="kodeverk", access = JsonProperty.Access.READ_ONLY)
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

    public static FagsakYtelseType fromString(String kode) {
        return fraKode(kode);
    }

    private static final Map<FagsakYtelseType, Set<FagsakYtelseType>> OPPTJENING_RELATERTYTELSE_CONFIG = Map.of(
        FORELDREPENGER,
        Set.of(ENSLIG_FORSØRGER, SYKEPENGER, SVANGERSKAPSPENGER, FORELDREPENGER, DAGPENGER,
            PÅRØRENDESYKDOM, PLEIEPENGER_SYKT_BARN, PLEIEPENGER_NÆRSTÅENDE, OMSORGSPENGER, OPPLÆRINGSPENGER),
        FRISINN, Set.of(SYKEPENGER, SVANGERSKAPSPENGER, FORELDREPENGER, DAGPENGER, ARBEIDSAVKLARINGSPENGER,
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

}
