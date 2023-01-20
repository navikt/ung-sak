package no.nav.k9.sak.økonomi.tilbakekreving.samkjøring;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateSegmentCombinator;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.domene.typer.tid.Hjelpetidslinjer;

@Dependent
public class SjekkEndringUtbetalingTilBrukerTjeneste {

    private BeregningsresultatRepository beregningsresultatRepository;
    private BehandlingRepository behandlingRepository;

    @Inject
    public SjekkEndringUtbetalingTilBrukerTjeneste(BeregningsresultatRepository beregningsresultatRepository, BehandlingRepository behandlingRepository) {
        this.beregningsresultatRepository = beregningsresultatRepository;
        this.behandlingRepository = behandlingRepository;
    }

    public LocalDateTimeline<Boolean> endringerUtbetalingTilBruker(Behandling behandling) {
        Behandling forrigeBehandling = finnSisteIkkeHenlagteBehandling(behandling).orElse(null);
        BeregningsresultatEntitet resultatNå = beregningsresultatRepository.hentEndeligBeregningsresultat(behandling.getId()).orElse(null);
        BeregningsresultatEntitet resultatFør = forrigeBehandling != null
            ? beregningsresultatRepository.hentEndeligBeregningsresultat(forrigeBehandling.getId()).orElse(null)
            : null;

        FagsakYtelseType fagsakYtelseType = behandling.getFagsakYtelseType();
        LocalDateTimeline<Boolean> endringYtelse = endringerUtbetalingYtelseTilBruker(fagsakYtelseType, resultatNå, resultatFør);
        LocalDateTimeline<Boolean> endringFeriepenger = endringerUtbetalingFeriepengerTilBruker(resultatNå, resultatFør);
        return endringYtelse.crossJoin(endringFeriepenger, StandardCombinators::alwaysTrueForMatch);
    }

    private LocalDateTimeline<Boolean> endringerUtbetalingYtelseTilBruker(FagsakYtelseType fagsakYtelseType, BeregningsresultatEntitet resultatNå, BeregningsresultatEntitet resultatFør) {
        LocalDateTimeline<Long> ytelseTilBrukerNå = ytelseTilBrukerTidsinje(resultatNå);
        LocalDateTimeline<Long> ytelseTilBrukerFør = ytelseTilBrukerTidsinje(resultatFør);

        LocalDateTimeline<Long> differanse = ytelseTilBrukerNå.crossJoin(ytelseTilBrukerFør, DIFFERANSE);
        LocalDateTimeline<Boolean> harDifferanse = differanse.filterValue(p -> p != 0L).mapValue(v -> true);
        return fagsakYtelseType == FagsakYtelseType.OMP
            ? harDifferanse
            : Hjelpetidslinjer.fjernHelger(harDifferanse);
    }

    private LocalDateTimeline<Boolean> endringerUtbetalingFeriepengerTilBruker(BeregningsresultatEntitet resultatNå, BeregningsresultatEntitet resultatFør) {
        LocalDateTimeline<Long> ytelseTilBrukerNå = feriepengerTilBrukerTidsinje(resultatNå);
        LocalDateTimeline<Long> ytelseTilBrukerFør = feriepengerTilBrukerTidsinje(resultatFør);
        LocalDateTimeline<Long> differanse = ytelseTilBrukerNå.crossJoin(ytelseTilBrukerFør, DIFFERANSE);
        return differanse.filterValue(p -> p != 0L).mapValue(v -> true);
    }

    private LocalDateTimeline<Long> ytelseTilBrukerTidsinje(BeregningsresultatEntitet beregningsresultat) {
        if (beregningsresultat == null) {
            return LocalDateTimeline.empty();
        }
        List<LocalDateSegment<Long>> segmenter = new ArrayList<>();
        for (BeregningsresultatPeriode periode : beregningsresultat.getBeregningsresultatPerioder()) {
            LocalDateInterval intervall = periode.getPeriode().toLocalDateInterval();
            for (BeregningsresultatAndel andel : periode.getBeregningsresultatAndelList()) {
                if (andel.erBrukerMottaker()) {
                    segmenter.add(new LocalDateSegment<>(intervall, (long) andel.getDagsats()));
                }
            }
        }

        return new LocalDateTimeline<>(segmenter, SUM);
    }

    private LocalDateTimeline<Long> feriepengerTilBrukerTidsinje(BeregningsresultatEntitet beregningsresultat) {
        if (beregningsresultat == null) {
            return LocalDateTimeline.empty();
        }
        List<LocalDateSegment<Long>> segmenter = new ArrayList<>();
        for (BeregningsresultatPeriode periode : beregningsresultat.getBeregningsresultatPerioder()) {
            int feriepengeOpptjeningsÅr = periode.getPeriode().getFomDato().getYear();
            LocalDateInterval feriepengerUtbetales = new LocalDateInterval(LocalDate.of(feriepengeOpptjeningsÅr + 1, 5, 1), LocalDate.of(feriepengeOpptjeningsÅr + 1, 5, 31));
            for (BeregningsresultatAndel andel : periode.getBeregningsresultatAndelList()) {
                if (andel.erBrukerMottaker()) {
                    segmenter.add(new LocalDateSegment<>(feriepengerUtbetales, (long) andel.getDagsats()));
                }
            }
        }
        return new LocalDateTimeline<>(segmenter, (interval, lhs, rhs) -> new LocalDateSegment<>(interval, lhs.getValue() + rhs.getValue()));
    }

    private Optional<Behandling> finnSisteIkkeHenlagteBehandling(Behandling aktuellBehandling) {
        Optional<Behandling> forrige = aktuellBehandling.getOriginalBehandlingId().map(behandlingRepository::hentBehandling);
        while (forrige.isPresent() && forrige.get().erHenlagt()) {
            forrige = forrige.get().getOriginalBehandlingId().map(behandlingRepository::hentBehandling);
        }
        return forrige;
    }

    private static final LocalDateSegmentCombinator<Long, Long, Long> DIFFERANSE = (intervall, lhs, rhs) -> new LocalDateSegment<>(intervall, (lhs != null ? lhs.getValue() : 0L) - (rhs != null ? rhs.getValue() : 0));
    private static final LocalDateSegmentCombinator<Long, Long, Long> SUM = (interval, lhs, rhs) -> new LocalDateSegment<>(interval, lhs.getValue() + rhs.getValue());

}
