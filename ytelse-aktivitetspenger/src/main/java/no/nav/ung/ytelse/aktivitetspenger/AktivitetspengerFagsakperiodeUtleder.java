package no.nav.ung.ytelse.aktivitetspenger;

import jakarta.enterprise.context.Dependent;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.tid.DatoIntervallEntitet;
import no.nav.ung.sak.tid.TidslinjeUtil;
import no.nav.ung.sak.typer.Periode;

import java.util.List;

@Dependent
public class AktivitetspengerFagsakperiodeUtleder {

    public DatoIntervallEntitet utledNyPeriodeForFagsak(Behandling behandling, Periode søknadsperiode) {
        LocalDateTimeline<Boolean> eksisterendePeriode = TidslinjeUtil.tilTidslinje(List.of(behandling.getFagsak().getPeriode()));
        LocalDateTimeline<Boolean> nyPeriode = new LocalDateTimeline<>(søknadsperiode.getFom(), søknadsperiode.getTom(), true);
        LocalDateTimeline<Boolean> tidslinje = eksisterendePeriode.crossJoin(nyPeriode, StandardCombinators::alwaysTrueForMatch);
        return DatoIntervallEntitet.fraOgMedTilOgMed(tidslinje.getMinLocalDate(), tidslinje.getMaxLocalDate());
    }

}
