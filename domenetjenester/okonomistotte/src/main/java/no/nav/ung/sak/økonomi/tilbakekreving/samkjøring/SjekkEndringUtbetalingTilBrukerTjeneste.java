package no.nav.ung.sak.økonomi.tilbakekreving.samkjøring;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateSegmentCombinator;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.felles.konfigurasjon.env.Environment;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.ytelse.beregning.TilkjentYtelsePeriode;
import no.nav.ung.sak.ytelse.beregning.UngdomsytelseUtledTilkjentYtelse;
import no.nav.ung.sak.ytelse.beregning.UtledTilkjentYtelse;

@Dependent
public class SjekkEndringUtbetalingTilBrukerTjeneste {

    private static final Logger logger = LoggerFactory.getLogger(SjekkEndringUtbetalingTilBrukerTjeneste.class);

    private final BehandlingRepository behandlingRepository;
    private final UtledTilkjentYtelse utledTilkjentYtelse;

    @Inject
    public SjekkEndringUtbetalingTilBrukerTjeneste(BehandlingRepository behandlingRepository,
                                                   UngdomsytelseUtledTilkjentYtelse utledTilkjentYtelse) {
        this.behandlingRepository = behandlingRepository;
        this.utledTilkjentYtelse = utledTilkjentYtelse;
    }

    public LocalDateTimeline<Boolean> endringerUtbetalingTilBruker(Behandling behandling) {
        Behandling forrigeBehandling = finnSisteIkkeHenlagteBehandling(behandling).orElse(null);
        var resultatNå = utledTilkjentYtelse.utledTilkjentYtelsePerioder(behandling.getId()).orElse(null);
        var resultatFør = forrigeBehandling != null
            ? utledTilkjentYtelse.utledTilkjentYtelsePerioder(forrigeBehandling.getId()).orElse(null)
            : null;

        LocalDateTimeline<Boolean> endringYtelse = endringerUtbetalingYtelseTilBruker(resultatNå, resultatFør);
        if (Environment.current().isDev()) {
            logger.info("Sammenlignet resultat fra behandling {} med inneværende {}", (forrigeBehandling != null ? forrigeBehandling.getId() : null), behandling.getId());
            logger.info("Endring ytelse {}", endringYtelse);
        }
        return endringYtelse;
    }

    private LocalDateTimeline<Boolean> endringerUtbetalingYtelseTilBruker(List<TilkjentYtelsePeriode> resultatNå, List<TilkjentYtelsePeriode> resultatFør) {
        LocalDateTimeline<Long> ytelseTilBrukerNå = ytelseTilBrukerTidsinje(resultatNå);
        LocalDateTimeline<Long> ytelseTilBrukerFør = ytelseTilBrukerTidsinje(resultatFør);
        LocalDateTimeline<Long> differanse = ytelseTilBrukerNå.crossJoin(ytelseTilBrukerFør, DIFFERANSE);
        return differanse.filterValue(p -> p != 0L).mapValue(v -> true);
    }


    private LocalDateTimeline<Long> ytelseTilBrukerTidsinje(List<TilkjentYtelsePeriode> perioder) {
        if (perioder == null) {
            return LocalDateTimeline.empty();
        }
        return perioder.stream().map(p -> new LocalDateTimeline<>(p.periode().getFomDato(), p.periode().getTomDato(), p.dagsats().longValue()))
            .reduce(LocalDateTimeline::crossJoin)
            .orElse(LocalDateTimeline.empty());
    }


    private Optional<Behandling> finnSisteIkkeHenlagteBehandling(Behandling aktuellBehandling) {
        Optional<Behandling> forrige = aktuellBehandling.getOriginalBehandlingId().map(behandlingRepository::hentBehandling);
        while (forrige.isPresent() && forrige.get().erHenlagt()) {
            forrige = forrige.get().getOriginalBehandlingId().map(behandlingRepository::hentBehandling);
        }
        return forrige;
    }

    private static final LocalDateSegmentCombinator<Long, Long, Long> DIFFERANSE = (intervall, lhs, rhs) -> new LocalDateSegment<>(intervall, (lhs != null ? lhs.getValue() : 0L) - (rhs != null ? rhs.getValue() : 0));

}
