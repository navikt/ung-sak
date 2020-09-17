package no.nav.k9.kodeverk.opptjening;

import java.util.AbstractMap;
/**
 * <h3>Internt kodeverk</h3>
 * Definerer aktiviteter benyttet til å vurdere Opptjening.
 * <p>
 * Kodeverket sammenstiller data fra {@link ArbeidType} og {@link RelatertYtelseType}.<br>
 * Senere benyttes dette i mapping til bla. Beregningsgrunnlag.
 *
 */
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonCreator.Mode;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import no.nav.k9.kodeverk.TempAvledeKode;
import no.nav.k9.kodeverk.api.Kodeverdi;
import no.nav.k9.kodeverk.arbeidsforhold.ArbeidType;
import no.nav.k9.kodeverk.arbeidsforhold.TemaUnderkategori;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;

@JsonFormat(shape = Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public enum OpptjeningAktivitetType implements Kodeverdi {

    ARBEIDSAVKLARING("AAP", "Arbeidsavklaringspenger",
            Set.of(),
            Set.of(FagsakYtelseType.ARBEIDSAVKLARINGSPENGER),
            Set.of()),
    ARBEID("ARBEID", "Arbeid",
            Set.of(ArbeidType.FORENKLET_OPPGJØRSORDNING, ArbeidType.MARITIMT_ARBEIDSFORHOLD, ArbeidType.ORDINÆRT_ARBEIDSFORHOLD),
            Set.of(),
            Set.of()),
    DAGPENGER("DAGPENGER", "Dagpenger",
            Set.of(),
            Set.of(FagsakYtelseType.DAGPENGER),
            Set.of()),
    FORELDREPENGER("FORELDREPENGER", "Foreldrepenger",
            Set.of(),
            Set.of(FagsakYtelseType.FORELDREPENGER),
            Set.of()),
    FRILANS("FRILANS", "Frilans",
            Set.of(ArbeidType.FRILANSER),
            Set.of(),
            Set.of()),
    MILITÆR_ELLER_SIVILTJENESTE("MILITÆR_ELLER_SIVILTJENESTE", "Militær- eller siviltjeneste",
            Set.of(ArbeidType.MILITÆR_ELLER_SIVILTJENESTE),
            Set.of(),
            Set.of()),
    NÆRING("NÆRING", "Næring",
            Set.of(ArbeidType.SELVSTENDIG_NÆRINGSDRIVENDE),
            Set.of(),
            Set.of()),
    OMSORGSPENGER("OMSORGSPENGER", "Omsorgspenger",
            Set.of(),
            Set.of(FagsakYtelseType.OMSORGSPENGER),
            Set.of(TemaUnderkategori.PÅRØRENDE_OMSORGSPENGER)),
    OPPLÆRINGSPENGER("OPPLÆRINGSPENGER", "Opplæringspenger",
            Set.of(),
            Set.of(FagsakYtelseType.OPPLÆRINGSPENGER),
            Set.of(TemaUnderkategori.PÅRØRENDE_OPPLÆRINGSPENGER)),
    PLEIEPENGER("PLEIEPENGER", "Pleiepenger",
            Set.of(),
            Set.of(FagsakYtelseType.PLEIEPENGER_SYKT_BARN, FagsakYtelseType.PLEIEPENGER_NÆRSTÅENDE),
            Set.of(TemaUnderkategori.PÅRØRENDE_PLEIETRENGENDE_SYKT_BARN, TemaUnderkategori.PÅRØRENDE_PLEIETRENGENDE,
                TemaUnderkategori.PÅRØRENDE_PLEIETRENGENDE_PÅRØRENDE, TemaUnderkategori.PÅRØRENDE_PLEIEPENGER)),
    ETTERLØNN_SLUTTPAKKE("ETTERLØNN_SLUTTPAKKE", "Etterlønn eller sluttpakke",
            Set.of(ArbeidType.ETTERLØNN_SLUTTPAKKE),
            Set.of(),
            Set.of()),
    SVANGERSKAPSPENGER("SVANGERSKAPSPENGER", "Svangerskapspenger",
            Set.of(),
            Set.of(FagsakYtelseType.SVANGERSKAPSPENGER),
            Set.of()),
    SYKEPENGER("SYKEPENGER", "Sykepenger",
            Set.of(),
            Set.of(FagsakYtelseType.SYKEPENGER),
            Set.of()),
    VENTELØNN_VARTPENGER("VENTELØNN_VARTPENGER", "Ventelønn eller vartpenger",
            Set.of(ArbeidType.VENTELØNN_VARTPENGER),
            Set.of(),
            Set.of()),
    VIDERE_ETTERUTDANNING("VIDERE_ETTERUTDANNING", "Videre- og etterutdanning",
            Set.of(ArbeidType.LØNN_UNDER_UTDANNING),
            Set.of(),
            Set.of()),
    UTENLANDSK_ARBEIDSFORHOLD("UTENLANDSK_ARBEIDSFORHOLD", "Arbeid i utlandet",
            Set.of(ArbeidType.UTENLANDSK_ARBEIDSFORHOLD),
            Set.of(),
            Set.of()),
    FRISINN("FRISINN", "FRISINN",
        Set.of(),
        Set.of(FagsakYtelseType.FRISINN),
        Set.of()),
    UTDANNINGSPERMISJON("UTDANNINGSPERMISJON", "Utdanningspermisjon",
            Set.of(), Set.of(), Set.of()),
    MELLOM_ARBEID("MELLOM_ARBEID", "Mellom arbeidsforhold",
            Set.of(), Set.of(), Set.of()),
    UDEFINERT("-", "UDEFINERT",
            Set.of(),
            Set.of(),
            Set.of()),
            ;

    private static final Map<String, OpptjeningAktivitetType> KODER = new LinkedHashMap<>();

    private static final Map<OpptjeningAktivitetType, Set<ArbeidType>> INDEKS_OPPTJ_ARBEID = new LinkedHashMap<>();
    private static final Map<OpptjeningAktivitetType, Set<FagsakYtelseType>> INDEKS_OPPTJ_RELYT = new LinkedHashMap<>();
    private static final Map<OpptjeningAktivitetType, Set<TemaUnderkategori>> INDEKS_OPPTJ_TEMAUN = new LinkedHashMap<>();

    public static final String KODEVERK = "OPPTJENING_AKTIVITET_TYPE";

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
            INDEKS_OPPTJ_ARBEID.put(v, v.arbeidType);
            INDEKS_OPPTJ_RELYT.put(v, v.relaterYtelseType);
            INDEKS_OPPTJ_TEMAUN.put(v, v.temaUnderkategori);

        }
    }

    public static final Set<OpptjeningAktivitetType> ANNEN_OPPTJENING = Set.of(VENTELØNN_VARTPENGER, MILITÆR_ELLER_SIVILTJENESTE, ETTERLØNN_SLUTTPAKKE,
        VIDERE_ETTERUTDANNING, UTENLANDSK_ARBEIDSFORHOLD, FRILANS);

    private String kode;

    @JsonIgnore
    private String navn;

    @JsonIgnore
    private Set<ArbeidType> arbeidType;

    @JsonIgnore
    private FagsakYtelseType relatertYtelseType;

    @JsonIgnore
    private Set<TemaUnderkategori> temaUnderkategori;

    @JsonIgnore
    private Set<FagsakYtelseType> relaterYtelseType;

    private OpptjeningAktivitetType(String kode, String navn, Set<ArbeidType> arbeidType, Set<FagsakYtelseType> relaterYtelseType,
                                    Set<TemaUnderkategori> temaUnderkategori) {
        this.kode = kode;
        this.navn = navn;
        this.arbeidType = arbeidType;
        this.relaterYtelseType = relaterYtelseType;
        this.temaUnderkategori = temaUnderkategori;
    }

    @JsonCreator(mode = Mode.DELEGATING)
    public static OpptjeningAktivitetType  fraKode(@JsonProperty("kode") Object node)  {
        if (node == null) {
            return null;
        }
        String kode = TempAvledeKode.getVerdi(OpptjeningAktivitetType.class, node, "kode");
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent OpptjeningAktivitetType: " + kode);
        }
        return ad;
    }

    public static Map<String, OpptjeningAktivitetType> kodeMap() {
        return Collections.unmodifiableMap(KODER);
    }

    @Override
    public String getNavn() {
        return navn;
    }

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
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

    public static Map<OpptjeningAktivitetType, Set<ArbeidType>> hentTilArbeidTypeRelasjoner() {
        return Collections.unmodifiableMap(INDEKS_OPPTJ_ARBEID);
    }

    private static Map<OpptjeningAktivitetType, Set<FagsakYtelseType>> hentTilFagsakYtelseTyper() {
        return Collections.unmodifiableMap(INDEKS_OPPTJ_RELYT);
    }

    private static Map<OpptjeningAktivitetType, Set<TemaUnderkategori>> hentTilTemaUnderkategori() {
        return Collections.unmodifiableMap(INDEKS_OPPTJ_TEMAUN);
    }

    public static Map<TemaUnderkategori, Set<OpptjeningAktivitetType>> hentFraTemaUnderkategori() {
        return hentTilTemaUnderkategori().entrySet().stream()
            .flatMap(entry -> entry.getValue().stream()
                .map(v -> new AbstractMap.SimpleEntry<>(v, entry.getKey())))
            .collect(Collectors.groupingBy(Map.Entry::getKey, Collectors.mapping(Map.Entry::getValue, Collectors.toSet())));
    }

    public static Map<ArbeidType, Set<OpptjeningAktivitetType>> hentFraArbeidTypeRelasjoner() {
        return hentTilArbeidTypeRelasjoner().entrySet().stream()
            .flatMap(entry -> entry.getValue().stream()
                .map(v -> new AbstractMap.SimpleEntry<>(v, entry.getKey())))
            .collect(Collectors.groupingBy(Map.Entry::getKey, Collectors.mapping(Map.Entry::getValue, Collectors.toSet())));
    }

    public static Map<FagsakYtelseType, Set<OpptjeningAktivitetType>> hentFraFagsakYtelseTyper() {
        return hentTilFagsakYtelseTyper().entrySet().stream()
            .flatMap(entry -> entry.getValue().stream()
                .map(v -> new AbstractMap.SimpleEntry<>(v, entry.getKey())))
            .collect(Collectors.groupingBy(Map.Entry::getKey, Collectors.mapping(Map.Entry::getValue, Collectors.toSet())));
    }

}
