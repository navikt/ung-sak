package no.nav.k9.kodeverk.opptjening;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator.Mode;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import no.nav.k9.kodeverk.TempAvledeKode;
import no.nav.k9.kodeverk.api.Kodeverdi;
import no.nav.k9.kodeverk.arbeidsforhold.ArbeidType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;

import java.util.*;
import java.util.stream.Collectors;

@JsonFormat(shape = Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public enum OpptjeningAktivitetType implements Kodeverdi {

    ARBEIDSAVKLARING("AAP", "Arbeidsavklaringspenger",
        Set.of(),
        Set.of(FagsakYtelseType.ARBEIDSAVKLARINGSPENGER)
    ),
    ARBEID("ARBEID", "Arbeid",
        Set.of(ArbeidType.FORENKLET_OPPGJØRSORDNING, ArbeidType.MARITIMT_ARBEIDSFORHOLD, ArbeidType.ORDINÆRT_ARBEIDSFORHOLD),
        Set.of()
    ),
    DAGPENGER("DAGPENGER", "Dagpenger",
        Set.of(),
        Set.of(FagsakYtelseType.DAGPENGER)
    ),
    FORELDREPENGER("FORELDREPENGER", "Foreldrepenger",
        Set.of(),
        Set.of(FagsakYtelseType.FORELDREPENGER)
    ),
    FRILANS("FRILANS", "Frilans",
        Set.of(ArbeidType.FRILANSER),
        Set.of()
    ),
    MILITÆR_ELLER_SIVILTJENESTE("MILITÆR_ELLER_SIVILTJENESTE", "Militær- eller siviltjeneste",
        Set.of(ArbeidType.MILITÆR_ELLER_SIVILTJENESTE),
        Set.of()
    ),
    NÆRING("NÆRING", "Næring",
        Set.of(ArbeidType.SELVSTENDIG_NÆRINGSDRIVENDE),
        Set.of()
    ),
    OMSORGSPENGER("OMSORGSPENGER", "Omsorgspenger",
        Set.of(),
        Set.of(FagsakYtelseType.OMSORGSPENGER)
    ),
    OPPLÆRINGSPENGER("OPPLÆRINGSPENGER", "Opplæringspenger",
        Set.of(),
        Set.of(FagsakYtelseType.OPPLÆRINGSPENGER)
    ),
    PLEIEPENGER("PLEIEPENGER", "Pleiepenger",
        Set.of(),
        Set.of(FagsakYtelseType.PLEIEPENGER_SYKT_BARN, FagsakYtelseType.PLEIEPENGER_NÆRSTÅENDE)
    ),
    ETTERLØNN_SLUTTPAKKE("ETTERLØNN_SLUTTPAKKE", "Etterlønn eller sluttpakke",
        Set.of(ArbeidType.ETTERLØNN_SLUTTPAKKE),
        Set.of()
    ),
    SVANGERSKAPSPENGER("SVANGERSKAPSPENGER", "Svangerskapspenger",
        Set.of(),
        Set.of(FagsakYtelseType.SVANGERSKAPSPENGER)
    ),
    SYKEPENGER("SYKEPENGER", "Sykepenger",
        Set.of(),
        Set.of(FagsakYtelseType.SYKEPENGER)
    ),
    SYKEPENGER_AV_DAGPENGER("SYKEPENGER_AV_DAGPENGER", "Sykepenger",
        Set.of(),
        Set.of(FagsakYtelseType.SYKEPENGER)
    ),
    PLEIEPENGER_AV_DAGPENGER("PLEIEPENGER_AV_DAGPENGER", "Pleiepenger",
        Set.of(),
        Set.of(FagsakYtelseType.PLEIEPENGER_SYKT_BARN, FagsakYtelseType.PLEIEPENGER_NÆRSTÅENDE)
    ),
    VENTELØNN_VARTPENGER("VENTELØNN_VARTPENGER", "Ventelønn eller vartpenger",
        Set.of(ArbeidType.VENTELØNN_VARTPENGER),
        Set.of()
    ),
    VIDERE_ETTERUTDANNING("VIDERE_ETTERUTDANNING", "Videre- og etterutdanning",
        Set.of(ArbeidType.LØNN_UNDER_UTDANNING),
        Set.of()
    ),
    UTENLANDSK_ARBEIDSFORHOLD("UTENLANDSK_ARBEIDSFORHOLD", "Arbeid i utlandet",
        Set.of(ArbeidType.UTENLANDSK_ARBEIDSFORHOLD),
        Set.of()
    ),
    FRISINN("FRISINN", "FRISINN",
        Set.of(),
        Set.of(FagsakYtelseType.FRISINN)
    ),
    UTDANNINGSPERMISJON("UTDANNINGSPERMISJON", "Utdanningspermisjon",
        Set.of(), Set.of()),
    MELLOM_ARBEID("MELLOM_ARBEID", "Mellom arbeidsforhold",
        Set.of(), Set.of()),
    UDEFINERT("-", "UDEFINERT",
        Set.of(),
        Set.of()
    ),
    ;

    public static final Set<OpptjeningAktivitetType> YTELSE = Set.of(
        SYKEPENGER,
        FORELDREPENGER,
        PLEIEPENGER,
        SVANGERSKAPSPENGER,
        OPPLÆRINGSPENGER,
        FRISINN,
        OMSORGSPENGER);
    public static final Set<OpptjeningAktivitetType> K9_YTELSER = Set.of(
        PLEIEPENGER, PLEIEPENGER_AV_DAGPENGER,
        OPPLÆRINGSPENGER);
    public static final String KODEVERK = "OPPTJENING_AKTIVITET_TYPE";
    public static final Set<OpptjeningAktivitetType> ANNEN_OPPTJENING = Set.of(VENTELØNN_VARTPENGER, MILITÆR_ELLER_SIVILTJENESTE, ETTERLØNN_SLUTTPAKKE,
        VIDERE_ETTERUTDANNING, UTENLANDSK_ARBEIDSFORHOLD, FRILANS);
    private static final Map<String, OpptjeningAktivitetType> KODER = new LinkedHashMap<>();
    private static final Map<OpptjeningAktivitetType, Set<ArbeidType>> INDEKS_OPPTJ_ARBEID = new LinkedHashMap<>();
    private static final Map<OpptjeningAktivitetType, Set<FagsakYtelseType>> INDEKS_OPPTJ_RELYT = new LinkedHashMap<>();

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
            INDEKS_OPPTJ_ARBEID.put(v, v.arbeidType);
            INDEKS_OPPTJ_RELYT.put(v, v.relaterYtelseType);

        }
    }

    private String kode;

    @JsonIgnore
    private String navn;

    @JsonIgnore
    private Set<ArbeidType> arbeidType;

    @JsonIgnore
    private Set<FagsakYtelseType> relaterYtelseType;

    private OpptjeningAktivitetType(String kode, String navn, Set<ArbeidType> arbeidType, Set<FagsakYtelseType> relaterYtelseType) {
        this.kode = kode;
        this.navn = navn;
        this.arbeidType = arbeidType;
        this.relaterYtelseType = relaterYtelseType;
    }

    @JsonCreator(mode = Mode.DELEGATING)
    public static OpptjeningAktivitetType fraKode(Object node) {
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

    public static Map<OpptjeningAktivitetType, Set<ArbeidType>> hentTilArbeidTypeRelasjoner() {
        return Collections.unmodifiableMap(INDEKS_OPPTJ_ARBEID);
    }

    private static Map<OpptjeningAktivitetType, Set<FagsakYtelseType>> hentTilFagsakYtelseTyper() {
        return Collections.unmodifiableMap(INDEKS_OPPTJ_RELYT);
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

}
