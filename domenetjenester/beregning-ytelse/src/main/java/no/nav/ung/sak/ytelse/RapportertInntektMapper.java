package no.nav.ung.sak.ytelse;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.ung.kodeverk.arbeidsforhold.InntektsKilde;
import no.nav.ung.kodeverk.arbeidsforhold.InntektspostType;
import no.nav.ung.kodeverk.etterlysning.EtterlysningStatus;
import no.nav.ung.sak.domene.iay.modell.*;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.domene.typer.tid.Virkedager;
import no.nav.ung.sak.uttalelse.EtterlysningsPeriode;
import no.nav.ung.sak.ytelseperioder.MånedsvisTidslinjeUtleder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Dependent
public class RapportertInntektMapper {

    private static final Logger LOGGER = LoggerFactory.getLogger(RapportertInntektMapper.class);

    private final InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;
    private final MånedsvisTidslinjeUtleder ytelsesperiodeutleder;

    @Inject
    public RapportertInntektMapper(InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste, MånedsvisTidslinjeUtleder ytelsesperiodeutleder) {
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
        this.ytelsesperiodeutleder = ytelsesperiodeutleder;
    }

    public LocalDateTimeline<RapporterteInntekter> mapAlleGjeldendeRegisterOgBrukersInntekter(Long behandlingId) {
        // Henter iay-grunnlag (kall til abakus)
        final var iayGrunnlag = inntektArbeidYtelseTjeneste.finnGrunnlag(behandlingId);
        if (iayGrunnlag.isEmpty()) {
            return LocalDateTimeline.empty();
        }

        final var månedsvisYtelseTidslinje = ytelsesperiodeutleder.periodiserMånedsvis(behandlingId);


        final var brukersRapporterteInntekter = finnBrukersRapporterteInntekter(iayGrunnlag.get(), månedsvisYtelseTidslinje);
        validerBrukersRapporterteInntekterTidslinje(brukersRapporterteInntekter, månedsvisYtelseTidslinje);

        final var grupperteInntekter = grupperInntekter(iayGrunnlag.get());

        final var registerTidslinje = finnRegisterInntektTidslinje(månedsvisYtelseTidslinje, grupperteInntekter);
        return kombinerTidslinjer(brukersRapporterteInntekter, registerTidslinje);

    }

    public LocalDateTimeline<EtterlysningOgRegisterinntekt> finnRegisterinntekterForEtterlysninger(
        Long behandlingId,
        List<EtterlysningsPeriode> etterlysningsperioder) {

        var svarteEllerVentendeStatuser = Set.of(EtterlysningStatus.MOTTATT_SVAR, EtterlysningStatus.OPPRETTET, EtterlysningStatus.VENTER);
        return etterlysningsperioder.stream()
            .filter(it -> svarteEllerVentendeStatuser.contains(it.etterlysningInfo().etterlysningStatus()))
            .map(it -> finnRegisterinntekterVurdertIUttalelse(behandlingId, it))
            .reduce(LocalDateTimeline::crossJoin)
            .orElse(LocalDateTimeline.empty());

    }

    private Map<InntektType, List<Inntektspost>> grupperInntekter(InntektArbeidYtelseGrunnlag iayGrunnlag) {
        final var aktørInntekt = iayGrunnlag.getRegisterVersjon().stream().flatMap(it -> it.getAktørInntekt().stream()).findFirst();
        return new InntektFilter(aktørInntekt)
            .getInntektsposter(InntektsKilde.INNTEKT_UNGDOMSYTELSE)
            .stream()
            .collect(Collectors.groupingBy(this::mapTilInntektType));
    }


    private LocalDateTimeline<EtterlysningOgRegisterinntekt> finnRegisterinntekterVurdertIUttalelse(Long behandlingId, EtterlysningsPeriode it) {
        final var iayGrunnlag = inntektArbeidYtelseTjeneste.hentGrunnlagForGrunnlagId(behandlingId, it.iayGrunnlagUUID());
        final var grupperteInntekter = grupperInntekter(iayGrunnlag);
        final var registerTidslinje = finnRegisterinntektForPeriode(grupperteInntekter, it.periode());
        return registerTidslinje.mapValue(registerinntekter -> new EtterlysningOgRegisterinntekt(registerinntekter, it.etterlysningInfo()));
    }

    private static LocalDateTimeline<Set<RapportertInntekt>> finnRegisterInntektTidslinje(LocalDateTimeline<YearMonth> månedsvisYtelseTidslinje, Map<InntektType, List<Inntektspost>> grupperteInntekter) {
        var registerTidslinje = new LocalDateTimeline<Set<RapportertInntekt>>(Set.of());

        for (var intervall : månedsvisYtelseTidslinje.getLocalDateIntervals()) {

            final var tidslinjeForPeriode = finnRegisterinntektForPeriode(grupperteInntekter, intervall);
            registerTidslinje = registerTidslinje.crossJoin(tidslinjeForPeriode);

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


        if (inntekterForPeriode.isEmpty()) {
            return LocalDateTimeline.empty();
        }
        final var tidslinjeForPeriode = new LocalDateTimeline<Set<RapportertInntekt>>(intervall, inntekterForPeriode);
        return tidslinjeForPeriode;
    }

    private static void validerBrukersRapporterteInntekterTidslinje(LocalDateTimeline<Set<RapportertInntekt>> brukersRapporterteInntekter, LocalDateTimeline<YearMonth> ytelseTidslinje) {
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

    private LocalDateTimeline<Set<RapportertInntekt>> finnBrukersRapporterteInntekter(InntektArbeidYtelseGrunnlag iayGrunnlag, LocalDateTimeline<YearMonth> månedsvisYtelseTidslinje) {
        // Finner rapporterte inntekter pr journalpost sortert på mottattdato med siste mottatt journalpost først
        final var sorterteInntekttidslinjerPåMottattdato = finnSorterteInntektstidslinjer(iayGrunnlag, månedsvisYtelseTidslinje);

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

    private List<InntektForMottattidspunkt> finnSorterteInntektstidslinjer(InntektArbeidYtelseGrunnlag iayGrunnlag, LocalDateTimeline<YearMonth> månedsvisYtelseTidslinje) {
        return iayGrunnlag.getOppgittOpptjeningAggregat()
            .stream()
            .flatMap(o -> o.getOppgitteOpptjeninger().stream())
            .filter(o -> erRapportertForGyldigPeriode(o, månedsvisYtelseTidslinje))
            .map(RapportertInntektMapper::finnInntekterPrMottattidspunkt)
            .sorted(Comparator.reverseOrder())
            .toList();
    }

    private boolean erRapportertForGyldigPeriode(OppgittOpptjening o, LocalDateTimeline<YearMonth> månedsvisYtelseTidslinje) {
        return o.getOppgittArbeidsforhold().stream().map(OppgittArbeidsforhold::getPeriode)
            .map(DatoIntervallEntitet::toLocalDateInterval)
            .allMatch(p -> månedsvisYtelseTidslinje.getLocalDateIntervals().stream().anyMatch(p::equals));
    }

    private static InntektForMottattidspunkt finnInntekterPrMottattidspunkt(OppgittOpptjening o) {
        final var res = new ArrayList<>(finnArbeidOgFrilansSegmenter(o));
        return new InntektForMottattidspunkt(o.getInnsendingstidspunkt(), new LocalDateTimeline<>(res, StandardCombinators::union));
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
