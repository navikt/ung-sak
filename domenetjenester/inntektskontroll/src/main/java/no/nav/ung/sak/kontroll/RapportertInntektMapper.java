package no.nav.ung.sak.kontroll;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.ung.kodeverk.arbeidsforhold.InntektsKilde;
import no.nav.ung.kodeverk.arbeidsforhold.InntektspostType;
import no.nav.ung.kodeverk.varsel.EtterlysningStatus;
import no.nav.ung.sak.domene.iay.modell.*;
import no.nav.ung.sak.felles.tid.DatoIntervallEntitet;
import no.nav.ung.sak.felles.tid.Virkedager;
import no.nav.ung.sak.felles.typer.Beløp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

@Dependent
public class RapportertInntektMapper {

    private static final Logger LOGGER = LoggerFactory.getLogger(RapportertInntektMapper.class);

    private final InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;

    @Inject
    public RapportertInntektMapper(InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste) {
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
    }

    public LocalDateTimeline<RapporterteInntekter> mapAlleGjeldendeRegisterOgBrukersInntekter(Long behandlingId, LocalDateTimeline<Boolean> relevantTidslinje) {
        // Henter iay-grunnlag (kall til abakus)
        final var iayGrunnlag = inntektArbeidYtelseTjeneste.finnGrunnlag(behandlingId);
        if (iayGrunnlag.isEmpty()) {
            return LocalDateTimeline.empty();
        }

        final var brukersRapporterteInntekter = finnBrukersRapporterteInntekter(iayGrunnlag.get(), relevantTidslinje);
        validerBrukersRapporterteInntekterTidslinje(brukersRapporterteInntekter, relevantTidslinje);

        final var grupperteInntekter = grupperInntekter(iayGrunnlag.get());

        final var registerTidslinje = finnRegisterInntektTidslinje(relevantTidslinje, grupperteInntekter);
        return kombinerTidslinjer(brukersRapporterteInntekter, registerTidslinje);

    }

    public LocalDateTimeline<EtterlysningOgRegisterinntekt> finnRegisterinntekterForEtterlysninger(
        Long behandlingId,
        List<InntektskontrollEtterlysningsPeriode> etterlysningsperioder) {

        return etterlysningsperioder.stream()
            .filter(it -> !it.inntektskontrollEtterlysningInfo().etterlysningStatus().equals(EtterlysningStatus.AVBRUTT) && !it.inntektskontrollEtterlysningInfo().etterlysningStatus().equals(EtterlysningStatus.SKAL_AVBRYTES))
            .map(it -> finnRegisterinntekterVurdertIUttalelse(behandlingId, it))
            .reduce(LocalDateTimeline::crossJoin)
            .orElse(LocalDateTimeline.empty());

    }

    private List<InntekterForKilde> grupperInntekter(InntektArbeidYtelseGrunnlag iayGrunnlag) {
        final var inntekter = iayGrunnlag.getRegisterVersjon().map(InntektArbeidYtelseAggregat::getInntekter);
        return new InntektFilter(inntekter)
            .getAlleInntekter(InntektsKilde.INNTEKT_UNGDOMSYTELSE)
            .stream()
            .filter(it -> !it.getAlleInntektsposter().isEmpty())
            .map(it -> {
                // Antar at alle inntektsposter for en inntekt har samme inntektstype
                Inntektspost førsteInntektspost = it.getAlleInntektsposter().iterator().next();
                return new InntekterForKilde(
                    mapTilInntektType(førsteInntektspost),
                    it.getArbeidsgiver(),
                    førsteInntektspost.getInntektYtelseType(),
                    it.getAlleInntektsposter().stream()
                        .map(ip -> new Inntektsperiode(ip.getBeløp(), ip.getPeriode()))
                        .toList()
                );
            }).toList();
    }


    private LocalDateTimeline<EtterlysningOgRegisterinntekt> finnRegisterinntekterVurdertIUttalelse(Long behandlingId, InntektskontrollEtterlysningsPeriode it) {
        final var registerinntekter = finnRegisterinntekterForPeriodeOgGrunnlag(behandlingId, it.iayGrunnlagUUID(), it.periode());
        Set<RapportertInntekt> rapportertInntekter = mapTilRapporterteInntekterPrType(registerinntekter);
        return new LocalDateTimeline<>(it.periode(), new EtterlysningOgRegisterinntekt(rapportertInntekter, it.inntektskontrollEtterlysningInfo()));
    }

    /**
     * Finner registerinntekt innenfor gitt periode for oppgitt IAY-grunnlag
     *
     * @param behandlingId BehandlingId
     * @param grunnlagUuid Referanse til IAY-grunnlag
     * @param periode      Aktuell periode
     * @return
     */
    public List<InntekterForKilde> finnRegisterinntekterForPeriodeOgGrunnlag(Long behandlingId, UUID grunnlagUuid, LocalDateInterval periode) {
        final var iayGrunnlag = inntektArbeidYtelseTjeneste.hentGrunnlagForGrunnlagId(behandlingId, grunnlagUuid);
        final var grupperteInntekter = grupperInntekter(iayGrunnlag);
        final var registerinntekter = finnOverlappendeInntekter(periode, grupperteInntekter);
        return registerinntekter;
    }

    private static LocalDateTimeline<Set<RapportertInntekt>> finnRegisterInntektTidslinje(LocalDateTimeline<Boolean> relevantTidslinje, List<InntekterForKilde> grupperteInntekter) {
        var registerTidslinje = new LocalDateTimeline<Set<RapportertInntekt>>(Set.of());

        for (var intervall : relevantTidslinje.getLocalDateIntervals()) {

            final var inntekterForPeriode = finnRegisterinntektForPeriode(grupperteInntekter, intervall);
            if (!inntekterForPeriode.isEmpty()) {
                registerTidslinje = registerTidslinje.crossJoin(new LocalDateTimeline<>(intervall, inntekterForPeriode));
            }

        }
        return registerTidslinje;
    }

    private static Set<RapportertInntekt> finnRegisterinntektForPeriode(List<InntekterForKilde> grupperteInntekter, LocalDateInterval intervall) {

        final var overlappendeInntekter = finnOverlappendeInntekter(intervall, grupperteInntekter);

        return mapTilRapporterteInntekterPrType(overlappendeInntekter);
    }

    private static Set<RapportertInntekt> mapTilRapporterteInntekterPrType(List<InntekterForKilde> overlappendeInntekter) {
        final var inntekterForPeriode = new HashSet<RapportertInntekt>();
        overlappendeInntekter
            .stream()
            .filter(it -> it.inntektType() == InntektType.ARBEIDSTAKER_ELLER_FRILANSER)
            .flatMap(it -> it.inntekter().stream())
            .map(Inntektsperiode::beløp)
            .reduce(Beløp::adder)
            .map(it -> new RapportertInntekt(InntektType.ARBEIDSTAKER_ELLER_FRILANSER, it.getVerdi()))
            .ifPresent(inntekterForPeriode::add);

        overlappendeInntekter
            .stream()
            .filter(it -> it.inntektType() == InntektType.YTELSE)
            .flatMap(it -> it.inntekter().stream())
            .map(Inntektsperiode::beløp)
            .reduce(Beløp::adder)
            .map(it -> new RapportertInntekt(InntektType.YTELSE, it.getVerdi()))
            .ifPresent(inntekterForPeriode::add);
        return inntekterForPeriode;
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

    private static Inntektsperiode finnBeløpInnenforPeriode(LocalDateInterval intervall, Inntektsperiode it) {
        final var inntektsperiode = it.periode();
        final var overlapp = new LocalDateTimeline<>(intervall, true).intersection(new LocalDateInterval(inntektsperiode.getFomDato(), inntektsperiode.getTomDato()));
        final var overlappPeriode = overlapp.getLocalDateIntervals().getFirst();

        final var antallVirkedager = inntektsperiode.antallArbeidsdager();
        final var overlappAntallVirkedager = Virkedager.beregnAntallVirkedager(overlappPeriode.getFomDato(), overlappPeriode.getTomDato());

        var beløp = new Beløp(it.beløp().getVerdi().multiply(BigDecimal.valueOf(overlappAntallVirkedager).divide(BigDecimal.valueOf(antallVirkedager), 10, RoundingMode.HALF_UP)));
        return new Inntektsperiode(beløp, inntektsperiode);
    }

    private static List<InntekterForKilde> finnOverlappendeInntekter(LocalDateInterval intervall, List<InntekterForKilde> grupperteInntekter) {
        return grupperteInntekter.stream()
            .map(inntekterForKilde ->
                {
                    List<Inntektsperiode> overlappendeInntektsperioder = inntekterForKilde.inntekter().stream()
                        .filter(it -> it.periode().toLocalDateInterval().overlaps(intervall))
                        .map(it -> finnBeløpInnenforPeriode(intervall, it))
                        .toList();
                    return new InntekterForKilde(
                        inntekterForKilde.inntektType(),
                        inntekterForKilde.arbeidsgiver(),
                        inntekterForKilde.ytelseType(),
                        overlappendeInntektsperioder);
                }
            ).toList();
    }

    private InntektType mapTilInntektType(Inntektspost it) {
        if (it.getInntektspostType().equals(InntektspostType.LØNN)) {
            return InntektType.ARBEIDSTAKER_ELLER_FRILANSER;
        } else if (it.getInntektspostType().equals(InntektspostType.YTELSE)) {
            return InntektType.YTELSE;
        }
        throw new IllegalArgumentException("Kunne ikke håndtere inntektsposttype: " + it.getInntektspostType());
    }

    private LocalDateTimeline<Set<RapportertInntekt>> finnBrukersRapporterteInntekter(InntektArbeidYtelseGrunnlag iayGrunnlag, LocalDateTimeline<Boolean> relevantTidslinje) {
        // Finner rapporterte inntekter pr journalpost sortert på mottattdato med siste mottatt journalpost først
        final var sorterteInntekttidslinjerPåMottattdato = finnSorterteInntektstidslinjer(iayGrunnlag, relevantTidslinje);

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

    private List<InntektForMottattidspunkt> finnSorterteInntektstidslinjer(InntektArbeidYtelseGrunnlag iayGrunnlag, LocalDateTimeline<Boolean> relevantTidslinje) {
        return iayGrunnlag.getOppgittOpptjeningAggregat()
            .stream()
            .flatMap(o -> o.getOppgitteOpptjeninger().stream())
            .filter(o -> erRapportertForGyldigPeriode(o, relevantTidslinje))
            .map(RapportertInntektMapper::finnInntekterPrMottattidspunkt)
            .sorted(Comparator.reverseOrder())
            .toList();
    }

    private boolean erRapportertForGyldigPeriode(OppgittOpptjening o, LocalDateTimeline<Boolean> relevantTidslinje) {
        return o.getOppgittArbeidsforhold().stream().map(OppgittArbeidsforhold::getPeriode)
            .map(DatoIntervallEntitet::toLocalDateInterval)
            .allMatch(p -> relevantTidslinje.getLocalDateIntervals().stream().anyMatch(p::equals));
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

    public record InntektForMottattidspunkt(
        LocalDateTime mottattTidspunkt,
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
