package no.nav.ung.sak.web.app.tjenester.behandling.beregningsresultat;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.kontrakt.beregningsresultat.BeregningsresultatDto;
import no.nav.ung.sak.kontrakt.beregningsresultat.BeregningsresultatMedUtbetaltePeriodeDto;
import no.nav.ung.sak.ytelse.DagsatsOgUtbetalingsgrad;
import no.nav.ung.sak.ytelse.beregning.BeregningsresultatMapper;
import no.nav.ung.sak.ytelse.beregning.TilkjentYtelseUtleder;
import no.nav.ung.sak.ytelse.beregning.UngdomsytelseBeregningsresultatMapper;
import no.nav.ung.sak.ytelse.beregning.UngdomsytelseTilkjentYtelseUtleder;

@ApplicationScoped
public class BeregningsresultatTjeneste {

    private BeregningsresultatMapper mapper;
    private TilkjentYtelseUtleder tilkjentYtelseUtleder;

    public BeregningsresultatTjeneste() {
        // For CDI
    }

    @Inject
    public BeregningsresultatTjeneste(UngdomsytelseBeregningsresultatMapper ungdomsytelseBeregningsresultatMapper,
                                      UngdomsytelseTilkjentYtelseUtleder ungdomsytelseUtledTilkjentYtelse) {
        this.tilkjentYtelseUtleder = ungdomsytelseUtledTilkjentYtelse;
        this.mapper = ungdomsytelseBeregningsresultatMapper;
    }

    public Optional<BeregningsresultatDto> lagBeregningsresultatMedUttaksplan(Behandling behandling) {
        var tidslinje = tilkjentYtelseUtleder.utledTilkjentYtelseTidslinje(behandling.getId());
        return tidslinje.isEmpty() ? Optional.empty() : Optional.of(mapper.map(behandling, tidslinje));
    }

    public Optional<BeregningsresultatMedUtbetaltePeriodeDto> lagBeregningsresultatMedUtbetaltePerioder(Behandling behandling) {
        var tidslinje = tilkjentYtelseUtleder.utledTilkjentYtelseTidslinje(behandling.getId());
        return tidslinje.isEmpty() ? Optional.empty() : Optional.of(mapper.mapMedUtbetaltePerioder(behandling, tidslinje));
    }
}
