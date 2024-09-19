package no.nav.k9.sak.ytelse.ung.beregning;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.sak.behandlingslager.diff.DiffEntity;
import no.nav.k9.sak.ytelse.beregning.grunnlag.BeregningsgrunnlagPeriode;
import no.nav.k9.sak.ytelse.beregning.grunnlag.BeregningsgrunnlagPerioderGrunnlag;

class UngdomsytelseGrunnlagBuilder {

    private static final Logger log = LoggerFactory.getLogger(UngdomsytelseGrunnlagBuilder.class);
    private final UngdomsytelseGrunnlag kladd;
    private boolean built = false;

    UngdomsytelseGrunnlagBuilder(UngdomsytelseGrunnlag kladd) {
        this.kladd = (kladd != null) ? new UngdomsytelseGrunnlag(kladd) : new UngdomsytelseGrunnlag();
    }

    UngdomsytelseGrunnlagBuilder medSatsPerioder(UngdomsytelseSatsPerioder perioder) {
        validerState();
        Objects.requireNonNull(perioder);
        kladd.setSatsPerioder(perioder);
        return this;
    }

    UngdomsytelseGrunnlag build() {
        validerState();
        this.built = true;

        return kladd;
    }

    boolean erForskjellig(UngdomsytelseGrunnlag grunnlag, DiffEntity differ) {
        return differ.areDifferent(grunnlag, kladd);
    }

    private void validerState() {
        if (built) {
            throw new IllegalStateException("[Utviklerfeil] Skal ikke gjenbruke builder!");
        }
    }


}
