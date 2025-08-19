package no.nav.ung.sak.web.app.tjenester.behandling.beregningsresultat;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.kontrakt.beregningsresultat.BeregningsresultatDto;
import no.nav.ung.sak.ytelse.beregning.BeregningsresultatMapper;
import no.nav.ung.sak.ytelse.beregning.TilkjentYtelseUtleder;
import no.nav.ung.sak.ytelse.beregning.UngdomsytelseBeregningsresultatMapper;
import no.nav.ung.sak.ytelse.beregning.UngdomsytelseTilkjentYtelseUtleder;

import java.util.Optional;

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

    public Optional<BeregningsresultatDto> lagBeregningsresultat(Behandling behandling) {
        var tidslinje = tilkjentYtelseUtleder.utledTilkjentYtelseTidslinje(behandling.getId());
        return tidslinje.isEmpty() ? Optional.empty() : Optional.of(mapper.map(behandling, tidslinje));
    }

}
