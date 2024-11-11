package no.nav.k9.sak.grunnbeløp;

import java.math.BigDecimal;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.sak.behandlingslager.grunnbeløp.GrunnbeløpRepository;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;


@Dependent
public class GrunnbeløpTjeneste {

    private final GrunnbeløpRepository grunnbeløpRepository;

    @Inject
    public GrunnbeløpTjeneste(GrunnbeløpRepository grunnbeløpRepository) {
        this.grunnbeløpRepository = grunnbeløpRepository;
    }

    public LocalDateTimeline<BigDecimal> hentGrunnbeløpTidslinje(LocalDateTimeline<Boolean> tidslinje) {
        var grunnbeløpSatser = grunnbeløpRepository.hentGrunnbeløpForPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(tidslinje.getMinLocalDate(), tidslinje.getMaxLocalDate()));
        return grunnbeløpSatser.stream()
            .map(s -> new LocalDateTimeline<>(s.getPeriode().getFomDato(), s.getPeriode().getTomDato(), BigDecimal.valueOf(s.getVerdi())))
            .reduce(LocalDateTimeline::crossJoin)
            .orElse(LocalDateTimeline.empty());
    }


}
