package no.nav.ung.kodeverk.opptjening;

import com.fasterxml.jackson.annotation.JsonValue;
import no.nav.ung.kodeverk.api.Kodeverdi;
import no.nav.ung.kodeverk.arbeidsforhold.ArbeidType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;

import java.util.*;
import java.util.stream.Collectors;

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
    @Deprecated(since = "2024-06-17", forRemoval = false) // Brukes for løpende saker i uttak, men skal aldri opprettes for nye vurderinger
    SYKEPENGER_AV_DAGPENGER("SYKEPENGER_AV_DAGPENGER", "Sykepenger",
        Set.of(),
        Set.of(FagsakYtelseType.SYKEPENGER)
    ),
    @Deprecated(since = "2024-06-17", forRemoval = false) // Brukes for løpende saker i uttak, men skal aldri opprettes for nye vurderinger
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

    private String navn;

    private Set<ArbeidType> arbeidType;

    private Set<FagsakYtelseType> relaterYtelseType;

    private OpptjeningAktivitetType(String kode, String navn, Set<ArbeidType> arbeidType, Set<FagsakYtelseType> relaterYtelseType) {
        this.kode = kode;
        this.navn = navn;
        this.arbeidType = arbeidType;
        this.relaterYtelseType = relaterYtelseType;
    }

    public static OpptjeningAktivitetType fraKode(final String kode) {
        if (kode == null) {
            return null;
        }
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

    @Override
    public String getKodeverk() {
        return KODEVERK;
    }

    @JsonValue
    @Override
    public String getKode() {
        return kode;
    }

    @Override
    public String getOffisiellKode() {
        return getKode();
    }

}
