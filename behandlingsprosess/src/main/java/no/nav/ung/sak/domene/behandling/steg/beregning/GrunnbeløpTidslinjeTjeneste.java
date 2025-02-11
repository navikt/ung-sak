package no.nav.ung.sak.domene.behandling.steg.beregning;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.sak.grunnbeløp.GrunnbeløpTjeneste;

import java.math.BigDecimal;

@Dependent
public class GrunnbeløpTidslinjeTjeneste {

    private GrunnbeløpTjeneste grunnbeløpTjeneste;

    @Inject
    public GrunnbeløpTidslinjeTjeneste(GrunnbeløpTjeneste grunnbeløpTjeneste) {
        this.grunnbeløpTjeneste = grunnbeløpTjeneste;
    }

    public LocalDateTimeline<BigDecimal> hentGrunnbeløpTidslinje() {
        return grunnbeløpTjeneste.hentGrunnbeløpTidslinje();
    }
}
