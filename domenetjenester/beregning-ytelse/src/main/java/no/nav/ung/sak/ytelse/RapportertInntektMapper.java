package no.nav.ung.sak.ytelse;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.ung.kodeverk.arbeidsforhold.InntektspostType;
import no.nav.ung.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.ung.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.ung.sak.domene.iay.modell.Inntektspost;
import no.nav.ung.sak.domene.iay.modell.OppgittArbeidsforhold;
import no.nav.ung.sak.domene.iay.modell.OppgittOpptjening;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.domene.typer.tid.Virkedager;
import no.nav.ung.sak.ytelse.uttalelse.BrukersUttalelseForRegisterinntekt;
import no.nav.ung.sak.ytelse.uttalelse.BrukersUttalelsePeriode;
import no.nav.ung.sak.ytelse.uttalelse.Status;
import no.nav.ung.sak.ytelseperioder.YtelseperiodeUtleder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Dependent
public class RapportertInntektMapper {

    private static final Logger LOGGER = LoggerFactory.getLogger(RapportertInntektMapper.class);

    private final InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;
    private final YtelseperiodeUtleder ytelseperiodeUtleder;

    @Inject
    public RapportertInntektMapper(InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste, YtelseperiodeUtleder ytelseperiodeUtleder) {
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
        this.ytelseperiodeUtleder = ytelseperiodeUtleder;
    }

    public LocalDateTimeline<RapporterteInntekter> mapAlleGjeldendeRegisterOgBrukersInntekter(Long behandlingId) {
        // Henter iay-grunnlag (kall til abakus)
        final var iayGrunnlag = inntektArbeidYtelseTjeneste.hentGrunnlag(behandlingId);
        final var ytelseTidslinje = ytelseperiodeUtleder.utledYtelsestidslinje(behandlingId);


        final var brukersRapporterteInntekter = finnBrukersRapporterteInntekter(iayGrunnlag, ytelseTidslinje);
        validerBrukersRapporterteInntekterTidslinje(brukersRapporterteInntekter, ytelseTidslinje);

        final var grupperteInntekter = grupperInntekter(iayGrunnlag);

        final var registerTidslinje = finnRegisterInntektTidslinje(ytelseTidslinje, grupperteInntekter);
        return kombinerTidslinjer(brukersRapporterteInntekter, registerTidslinje);

    }

    public LocalDateTimeline<BrukersUttalelseForRegisterinntekt> finnRegisterinntekterForUttalelse(Long behandlingId,
                                                                                                   List<BrukersUttalelsePeriode> brukersUttalelsePerioder) {
        final var relevanteUttalelser = brukersUttalelsePerioder.stream()
            .filter(it -> Set.of(Status.BEKREFTET, Status.VENTER).contains(it.status())).toList();
        final var unikeGrunnlagsIder = relevanteUttalelser
            .stream()
            .map(no.nav.ung.sak.ytelse.uttalelse.BrukersUttalelsePeriode::iayGrunnlagUUID)
            .collect(Collectors.toSet());

        final var grunnlagPrUUID = unikeGrunnlagsIder.stream().collect(Collectors.toMap(it -> it, it -> inntektArbeidYtelseTjeneste.hentGrunnlagForGrunnlagId(behandlingId, it)));

        return relevanteUttalelser.stream()
            .map(it -> finnRegisterinntekterVurdertIUttalelse(it, grunnlagPrUUID))
            .reduce(LocalDateTimeline::crossJoin)
            .orElse(LocalDateTimeline.empty());

    }

    private Map<InntektType, List<Inntektspost>> grupperInntekter(InntektArbeidYtelseGrunnlag iayGrunnlag) {
        return iayGrunnlag.getRegisterVersjon().stream().flatMap(it -> it.getAktørInntekt().stream())
            .flatMap(it -> it.getInntekt().stream())
            .flatMap(it -> it.getAlleInntektsposter().stream())
            .collect(Collectors.groupingBy(this::mapTilInntektType));
    }

    private LocalDateTimeline<BrukersUttalelseForRegisterinntekt> finnRegisterinntekterVurdertIUttalelse(BrukersUttalelsePeriode it, Map<UUID, InntektArbeidYtelseGrunnlag> grunnlagPrUUID) {
        final var iayGrunnlag = grunnlagPrUUID.get(it.iayGrunnlagUUID());
        final var grupperteInntekter = grupperInntekter(iayGrunnlag);
        final var registerTidslinje = finnRegisterinntektForPeriode(grupperteInntekter, it.periode().toLocalDateInterval());
        return registerTidslinje.mapValue(registerinntekter -> new BrukersUttalelseForRegisterinntekt(it.status(), registerinntekter, it.uttalelse()));
    }

    private static LocalDateTimeline<Set<RapportertInntekt>> finnRegisterInntektTidslinje(LocalDateTimeline<Boolean> ytelseTidslinje, Map<InntektType, List<Inntektspost>> grupperteInntekter) {
        final var registerTidslinje = new LocalDateTimeline<Set<RapportertInntekt>>(Set.of());

        for (var intervall : ytelseTidslinje.getLocalDateIntervals()) {

            final var tidslinjeForPeriode = finnRegisterinntektForPeriode(grupperteInntekter, intervall);
            registerTidslinje.crossJoin(tidslinjeForPeriode);

        }
        return registerTidslinje;
    }

    private static LocalDateTimeline<Set<RapportertInntekt>> finnRegisterinntektForPeriode(Map<InntektType, List<Inntektspost>> grupperteInntekter, LocalDateInterval intervall) {
        final var inntekterForPeriode = new HashSet<RapportertInntekt>();

        final var overlappendeArbeidsinntekter = finnOverlappendeInntekterForType(intervall, grupperteInntekter, InntektType.ARBEIDSTAKER_ELLER_FRILANSER);

        overlappendeArbeidsinntekter
            .stream()
            .map(it -> finnBeløpInnenforPeriode(intervall, it))
            .reduce(BigDecimal::add)
            .map(it -> new RapportertInntekt(InntektType.ARBEIDSTAKER_ELLER_FRILANSER, it))
            .ifPresent(inntekterForPeriode::add);

        final var overlappendeYtelse = finnOverlappendeInntekterForType(intervall, grupperteInntekter, InntektType.YTELSE);
        overlappendeYtelse
            .stream()
            .map(it -> finnBeløpInnenforPeriode(intervall, it))
            .reduce(BigDecimal::add)
            .map(it -> new RapportertInntekt(InntektType.YTELSE, it))
            .ifPresent(inntekterForPeriode::add);


        final var tidslinjeForPeriode = new LocalDateTimeline<Set<RapportertInntekt>>(intervall, inntekterForPeriode);
        return tidslinjeForPeriode;
    }

    private static void validerBrukersRapporterteInntekterTidslinje(LocalDateTimeline<Set<RapportertInntekt>> brukersRapporterteInntekter, LocalDateTimeline<Boolean> ytelseTidslinje) {
        final var perioderUtenforYtelsesperioder = brukersRapporterteInntekter.getLocalDateIntervals().stream()
            .filter(it -> ytelseTidslinje.getLocalDateIntervals().stream().noneMatch(it::equals))
            .toList();

        if (!perioderUtenforYtelsesperioder.isEmpty()) {
            throw new IllegalStateException("Fant rapporterte inntekter for perioder utenfor ytelsesperioder: " + perioderUtenforYtelsesperioder);

        }
    }

    private static LocalDateTimeline<RapporterteInntekter> kombinerTidslinjer(LocalDateTimeline<Set<RapportertInntekt>> brukersRapporterteInntekter, LocalDateTimeline<Set<RapportertInntekt>> registerTidslinje) {
        return brukersRapporterteInntekter.crossJoin(registerTidslinje, (di, bruker, register) -> {
            final var rapporterteInntekter = new RapporterteInntekter(bruker == null ? Set.of() : bruker.getValue(), register == null ? Set.of() : register.getValue());
            return new LocalDateSegment<>(di, rapporterteInntekter);
        });
    }

    private static BigDecimal finnBeløpInnenforPeriode(LocalDateInterval intervall, Inntektspost it) {
        final var inntektsperiode = it.getPeriode();
        final var overlapp = new LocalDateTimeline<>(intervall, true).intersection(new LocalDateInterval(inntektsperiode.getFomDato(), inntektsperiode.getTomDato()));
        final var overlappPeriode = overlapp.getLocalDateIntervals().getFirst();

        final var antallVirkedager = inntektsperiode.antallArbeidsdager();
        final var overlappAntallVirkedager = Virkedager.beregnAntallVirkedager(overlappPeriode.getFomDato(), overlappPeriode.getTomDato());

        return it.getBeløp().getVerdi().multiply(BigDecimal.valueOf(overlappAntallVirkedager).divide(BigDecimal.valueOf(antallVirkedager), 10, RoundingMode.HALF_UP));
    }

    private static List<Inntektspost> finnOverlappendeInntekterForType(LocalDateInterval intervall, Map<InntektType, List<Inntektspost>> grupperteInntekter, InntektType inntektType) {
        return grupperteInntekter.getOrDefault(inntektType, List.of())
            .stream().filter(it -> intervall.overlaps(new LocalDateInterval(it.getPeriode().getFomDato(), it.getPeriode().getTomDato())))
            .toList();
    }

    private InntektType mapTilInntektType(Inntektspost it) {
        if (it.getInntektspostType().equals(InntektspostType.LØNN)) {
            return InntektType.ARBEIDSTAKER_ELLER_FRILANSER;
        } else if (it.getInntektspostType().equals(InntektspostType.YTELSE)) {
            return InntektType.YTELSE;
        }
        throw new IllegalArgumentException("Kunne ikke håndtere inntektsposttype: " + it.getInntektspostType());
    }

    private LocalDateTimeline<Set<RapportertInntekt>> finnBrukersRapporterteInntekter(InntektArbeidYtelseGrunnlag iayGrunnlag, LocalDateTimeline<Boolean> ytelseTidslinje) {
        // Finner rapporterte inntekter pr journalpost sortert på mottattdato med siste mottatt journalpost først
        final var sorterteInntekttidslinjerPåMottattdato = finnSorterteInntektstidslinjer(iayGrunnlag, ytelseTidslinje);

        var brukersRapporterteInntekter = new LocalDateTimeline<Set<RapportertInntekt>>(List.of());
        for (InntektForMottattidspunkt journalpostMedInntekttidslinje : sorterteInntekttidslinjerPåMottattdato) {
            // Dersom vi ikke allerede har lagt til en rapportert inntekt for perioden legger vi den til i resultattidslinjen
            if (!brukersRapporterteInntekter.intersects(journalpostMedInntekttidslinje.tidslinje())) {
                // Trenger ikkje å håndtere overlapp sidan vi aldri går inn her med overlapp
                brukersRapporterteInntekter = brukersRapporterteInntekter.crossJoin(journalpostMedInntekttidslinje.tidslinje());
                LOGGER.info("Fant rapportert inntekt for periode: " + journalpostMedInntekttidslinje);

            } else {
                LOGGER.info("Fant inntekt som ble forkastet pga overlapp med senere rapportert inntekt for samme periode: " + journalpostMedInntekttidslinje);
            }
        }
        return brukersRapporterteInntekter;
    }

    private List<InntektForMottattidspunkt> finnSorterteInntektstidslinjer(InntektArbeidYtelseGrunnlag iayGrunnlag, LocalDateTimeline<Boolean> ytelseTidslinje) {
        return iayGrunnlag.getOppgittOpptjeningAggregat()
            .stream()
            .flatMap(o -> o.getOppgitteOpptjeninger().stream())
            .filter(o -> erRapportertForGyldigPeriode(o, ytelseTidslinje))
            .map(RapportertInntektMapper::finnInntekterPrMottattidspunkt)
            .sorted(Comparator.reverseOrder())
            .toList();
    }

    private boolean erRapportertForGyldigPeriode(OppgittOpptjening o, LocalDateTimeline<Boolean> ytelseTidslinje) {
        return o.getOppgittArbeidsforhold().stream().map(OppgittArbeidsforhold::getPeriode)
            .map(DatoIntervallEntitet::toLocalDateInterval)
            .allMatch(p -> ytelseTidslinje.getLocalDateIntervals().stream().anyMatch(p::equals));
    }

    private static InntektForMottattidspunkt finnInntekterPrMottattidspunkt(OppgittOpptjening o) {
        final var res = new ArrayList<LocalDateSegment<Set<RapportertInntekt>>>();
        res.addAll(finnArbeidOgFrilansSegmenter(o));
        res.addAll(finnNæringssegmenter(o));
        return new InntektForMottattidspunkt(o.getInnsendingstidspunkt(), new LocalDateTimeline<>(res, StandardCombinators::union));
    }

    private static List<LocalDateSegment<Set<RapportertInntekt>>> finnNæringssegmenter(OppgittOpptjening o) {
        return o.getEgenNæring().stream()
            .map(it -> new LocalDateSegment<>(
                it.getPeriode().toLocalDateInterval(),
                Set.of(new RapportertInntekt(
                    InntektType.SELVSTENDIG_NÆRINGSDRIVENDE,
                    it.getBruttoInntekt())
                ))).toList();
    }

    private static List<LocalDateSegment<Set<RapportertInntekt>>> finnArbeidOgFrilansSegmenter(OppgittOpptjening o) {
        return o.getOppgittArbeidsforhold().stream()
            .map(it -> new LocalDateSegment<>(
                it.getPeriode().toLocalDateInterval(),
                Set.of(new RapportertInntekt(
                    InntektType.ARBEIDSTAKER_ELLER_FRILANSER,
                    it.getInntekt())
                ))).toList();
    }

    private record InntektForMottattidspunkt(LocalDateTime mottattTidspunkt,
                                             LocalDateTimeline<Set<RapportertInntekt>> tidslinje
    ) implements Comparable<InntektForMottattidspunkt> {

        @Override
        public String toString() {
            return "InntektForMottattidspunkt{" +
                "mottattTidspunkt=" + mottattTidspunkt +
                ", tidslinje=" + tidslinje +
                '}';
        }

        @Override
        public int compareTo(InntektForMottattidspunkt o) {
            return this.mottattTidspunkt.compareTo(o.mottattTidspunkt);
        }
    }

}
