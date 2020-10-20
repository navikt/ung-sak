package no.nav.k9.sak.ytelse.beregning.beregningsresultat;

import java.util.Optional;

import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatEntitet;

public interface BeregningsresultatProvider {
    Optional<BeregningsresultatEntitet> hentBeregningsresultat(Long behandlingId);

    Optional<BeregningsresultatEntitet> hentUtbetBeregningsresultat(Long behandlingId);
}
