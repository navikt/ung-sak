package no.nav.ung.sak.web.app.tjenester.behandling.beregningsresultat;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.kontrakt.beregningsresultat.BeregningsresultatDto;
import no.nav.ung.sak.kontrakt.beregningsresultat.BeregningsresultatMedUtbetaltePeriodeDto;
import no.nav.ung.sak.ytelse.beregning.BeregningsresultatMapper;
import no.nav.ung.sak.ytelse.beregning.UtledTilkjentYtelse;
import no.nav.ung.sak.ytelse.beregning.UngdomsytelseBeregningsresultatMapper;
import no.nav.ung.sak.ytelse.beregning.UngdomsytelseUtledTilkjentYtelse;

@ApplicationScoped
public class BeregningsresultatTjeneste {

    private BeregningsresultatMapper mapper;
    private UtledTilkjentYtelse utledTilkjentYtelse;

    public BeregningsresultatTjeneste() {
        // For CDI
    }

    @Inject
    public BeregningsresultatTjeneste(UngdomsytelseBeregningsresultatMapper ungdomsytelseBeregningsresultatMapper,
                                      UngdomsytelseUtledTilkjentYtelse ungdomsytelseUtledTilkjentYtelse) {
        this.utledTilkjentYtelse = ungdomsytelseUtledTilkjentYtelse;
        this.mapper = ungdomsytelseBeregningsresultatMapper;
    }

    public Optional<BeregningsresultatDto> lagBeregningsresultatMedUttaksplan(Behandling behandling) {
        return utledTilkjentYtelse.utledTilkjentYtelsePerioder(behandling.getId())
            .map(it -> mapper.map(behandling, it));
    }

    public Optional<BeregningsresultatMedUtbetaltePeriodeDto> lagBeregningsresultatMedUtbetaltePerioder(Behandling behandling) {
        return utledTilkjentYtelse.utledTilkjentYtelsePerioder(behandling.getId())
            .map(it -> mapper.mapMedUtbetaltePerioder(behandling, it));
    }
}
