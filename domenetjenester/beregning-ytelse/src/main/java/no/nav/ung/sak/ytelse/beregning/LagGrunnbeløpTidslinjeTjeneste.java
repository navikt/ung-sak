package no.nav.ung.sak.ytelse.beregning;

import java.math.BigDecimal;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.sak.grunnbeløp.GrunnbeløpTjeneste;

@Dependent
public class LagGrunnbeløpTidslinjeTjeneste {

    private GrunnbeløpTjeneste grunnbeløpTjeneste;

    @Inject
    public LagGrunnbeløpTidslinjeTjeneste(GrunnbeløpTjeneste grunnbeløpTjeneste) {
        this.grunnbeløpTjeneste = grunnbeløpTjeneste;
    }

    /**
     * Lag grunnbeløptidslinje for perioder
     *
     * @param tidslinjeTilVurdering Perioder som skal lages tidslinje for
     * @return Tidslinje med grunnbeløp
     */
    public LocalDateTimeline<BigDecimal> lagGrunnbeløpTidslinjeForPeriode(LocalDateTimeline<Boolean> tidslinjeTilVurdering) {
        return grunnbeløpTjeneste.hentGrunnbeløpTidslinje(tidslinjeTilVurdering);
    }

}
