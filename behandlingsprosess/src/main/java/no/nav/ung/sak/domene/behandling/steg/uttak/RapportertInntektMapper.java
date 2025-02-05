package no.nav.ung.sak.domene.behandling.steg.uttak;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.ung.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.ung.sak.domene.behandling.steg.uttak.regler.InntektType;
import no.nav.ung.sak.domene.behandling.steg.uttak.regler.RapportertInntekt;
import no.nav.ung.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.ung.sak.domene.iay.modell.OppgittOpptjening;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Dependent
public class RapportertInntektMapper {

    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;

    @Inject
    public RapportertInntektMapper(InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste) {
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
    }

    public LocalDateTimeline<Set<RapportertInntekt>> map(Long behandlingId) {
        // Henter iay-grunnlag (kall til abakus)
        final var iayGrunnlag = inntektArbeidYtelseTjeneste.hentGrunnlag(behandlingId);
        // Finner rapporterte inntekter pr journalpost sortert på mottattdato med siste mottatt journalpost først
        final var sorterteInntekttidslinjerPåMottattdato = finnSorterteInntektstidslinjer(iayGrunnlag);

        var resultatTidslinje = new LocalDateTimeline<Set<RapportertInntekt>>(List.of());
        for (InntektForMottattidspunkt journalpostMedInntekttidslinje : sorterteInntekttidslinjerPåMottattdato) {
            // Dersom vi ikke allerede har lagt til en rapportert inntekt for perioden legger vi den til i resultattidslinjen
            if (!resultatTidslinje.intersects(journalpostMedInntekttidslinje.tidslinje())) {
                // Trenger ikkje å håndtere overlapp sidan vi aldri går inn her med overlapp
                resultatTidslinje = resultatTidslinje.crossJoin(journalpostMedInntekttidslinje.tidslinje());
            }
        }

        return resultatTidslinje;
    }

    private List<InntektForMottattidspunkt> finnSorterteInntektstidslinjer(InntektArbeidYtelseGrunnlag iayGrunnlag) {
        return iayGrunnlag.getOppgittOpptjeningAggregat()
            .stream()
            .flatMap(o -> o.getOppgitteOpptjeninger().stream().map(RapportertInntektMapper::finnInntekterPrMottattidspunkt))
            .sorted(Comparator.reverseOrder())
            .toList();
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
        public int compareTo(@NotNull InntektForMottattidspunkt o) {
            return this.mottattTidspunkt.compareTo(o.mottattTidspunkt);
        }
    }

}
