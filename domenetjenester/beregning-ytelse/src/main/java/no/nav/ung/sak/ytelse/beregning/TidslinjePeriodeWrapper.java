package no.nav.ung.sak.ytelse.beregning;

import no.nav.ung.sak.behandlingslager.behandling.beregning.BeregningsresultatPeriode;

class TidslinjePeriodeWrapper {

    private BeregningsresultatPeriode revurderingPeriode;
    private BeregningsresultatPeriode originalPeriode;

    TidslinjePeriodeWrapper(BeregningsresultatPeriode revurderingPeriode, BeregningsresultatPeriode originalPeriode){
        this.revurderingPeriode = revurderingPeriode;
        this.originalPeriode = originalPeriode;
    }

    BeregningsresultatPeriode getRevurderingPeriode() {
        return revurderingPeriode;
    }

    BeregningsresultatPeriode getOriginalPeriode() {
        return originalPeriode;
    }

}
