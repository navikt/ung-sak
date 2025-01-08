package no.nav.ung.sak.økonomi.tilbakekreving.samkjøring;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateSegmentCombinator;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.felles.konfigurasjon.env.Environment;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.ytelse.DagsatsOgUtbetalingsgrad;
import no.nav.ung.sak.ytelse.beregning.TilkjentYtelseUtleder;
import no.nav.ung.sak.ytelse.beregning.UngdomsytelseTilkjentYtelseUtleder;

@Dependent
public class SjekkEndringUtbetalingTilBrukerTjeneste {

    private static final Logger logger = LoggerFactory.getLogger(SjekkEndringUtbetalingTilBrukerTjeneste.class);

    private final BehandlingRepository behandlingRepository;
    private final TilkjentYtelseUtleder tilkjentYtelseUtleder;

    @Inject
    public SjekkEndringUtbetalingTilBrukerTjeneste(BehandlingRepository behandlingRepository,
                                                   UngdomsytelseTilkjentYtelseUtleder utledTilkjentYtelse) {
        this.behandlingRepository = behandlingRepository;
        this.tilkjentYtelseUtleder = utledTilkjentYtelse;
    }

    public LocalDateTimeline<Boolean> endringerUtbetalingTilBruker(Behandling behandling) {
        Behandling forrigeBehandling = finnSisteIkkeHenlagteBehandling(behandling).orElse(null);
        var resultatNå = tilkjentYtelseUtleder.utledTilkjentYtelseTidslinje(behandling.getId());
        var resultatFør = forrigeBehandling != null
            ? tilkjentYtelseUtleder.utledTilkjentYtelseTidslinje(forrigeBehandling.getId())
            : null;

        LocalDateTimeline<Boolean> endringYtelse = endringerUtbetalingYtelseTilBruker(resultatNå, resultatFør);
        if (Environment.current().isDev()) {
            logger.info("Sammenlignet resultat fra behandling {} med inneværende {}", (forrigeBehandling != null ? forrigeBehandling.getId() : null), behandling.getId());
            logger.info("Endring ytelse {}", endringYtelse);
        }
        return endringYtelse;
    }

    private LocalDateTimeline<Boolean> endringerUtbetalingYtelseTilBruker(LocalDateTimeline<DagsatsOgUtbetalingsgrad> resultatNå, LocalDateTimeline<DagsatsOgUtbetalingsgrad> resultatFør) {
        LocalDateTimeline<Long> differanse = resultatNå.crossJoin(resultatFør, DIFFERANSE);
        return differanse.filterValue(p -> p != 0L).mapValue(v -> true);
    }

    private Optional<Behandling> finnSisteIkkeHenlagteBehandling(Behandling aktuellBehandling) {
        Optional<Behandling> forrige = aktuellBehandling.getOriginalBehandlingId().map(behandlingRepository::hentBehandling);
        while (forrige.isPresent() && forrige.get().erHenlagt()) {
            forrige = forrige.get().getOriginalBehandlingId().map(behandlingRepository::hentBehandling);
        }
        return forrige;
    }

    private static final LocalDateSegmentCombinator<DagsatsOgUtbetalingsgrad, DagsatsOgUtbetalingsgrad, Long> DIFFERANSE = (intervall, lhs, rhs) -> new LocalDateSegment<>(intervall, (lhs != null ? lhs.getValue().dagsats() : 0L) - (rhs != null ? rhs.getValue().dagsats() : 0));

}
