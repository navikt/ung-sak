package no.nav.k9.sak.ytelse.ung.beregning.barnetillegg;

import java.math.BigDecimal;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.sak.behandling.BehandlingReferanse;

@Dependent
public class LagBarnetilleggTidslinje {

    public static final BigDecimal BARNETILLEGG_DAGSATS = BigDecimal.valueOf(36);
    private final LagAntallBarnTidslinje lagAntallBarnTidslinje;

    @Inject
    public LagBarnetilleggTidslinje(LagAntallBarnTidslinje lagAntallBarnTidslinje) {
        this.lagAntallBarnTidslinje = lagAntallBarnTidslinje;
    }

    /** Utleder tidslinje for barnetillegg
     * @param behandlingReferanse Behandlingreferanse
     * @return Tidslinje for barnetillegg
     */
    public LocalDateTimeline<Barnetillegg> lagTidslinje(BehandlingReferanse behandlingReferanse) {
        var antallBarnTidslinje = lagAntallBarnTidslinje.lagAntallBarnTidslinje(behandlingReferanse);
        return antallBarnTidslinje.mapValue(antallBarn -> new Barnetillegg(BigDecimal.valueOf(antallBarn).multiply(BARNETILLEGG_DAGSATS), antallBarn));
    }

    public record Barnetillegg(BigDecimal dagsats, int antallBarn) {
    }

}
