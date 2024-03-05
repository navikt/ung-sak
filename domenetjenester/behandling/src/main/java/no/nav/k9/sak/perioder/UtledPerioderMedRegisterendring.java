package no.nav.k9.sak.perioder;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.kontrakt.krav.PeriodeMedRegisterendring;
import no.nav.k9.sak.registerendringer.Aktivitetsendringer;
import no.nav.k9.sak.registerendringer.RelevanteIAYRegisterendringerUtleder;
import no.nav.k9.sak.typer.Periode;

public class UtledPerioderMedRegisterendring {

    private Instance<RelevanteIAYRegisterendringerUtleder> relevanteIAYRegisterendringerUtledere;

    public UtledPerioderMedRegisterendring() {
    }

    @Inject
    public UtledPerioderMedRegisterendring(@Any Instance<RelevanteIAYRegisterendringerUtleder> relevanteIAYRegisterendringerUtledere) {
        this.relevanteIAYRegisterendringerUtledere = relevanteIAYRegisterendringerUtledere;
    }

    public Set<PeriodeMedRegisterendring> utledPerioderMedRegisterendring(BehandlingReferanse behandlingReferanse) {
        if (behandlingReferanse.getOriginalBehandlingId().isEmpty()) {
            return Collections.emptySet();
        }

        var relevanteIAYRegisterendringerUtleder = RelevanteIAYRegisterendringerUtleder.finnTjeneste(relevanteIAYRegisterendringerUtledere, behandlingReferanse.getFagsakYtelseType());

        var endringerIAY = relevanteIAYRegisterendringerUtleder.utledRelevanteEndringer(behandlingReferanse);

        var endringIAnsattforholdTidslinje = endringerIAY.getAnsattforholdEndringer().stream().map(Aktivitetsendringer::getEndringerForUtbetaling)
            .map(t -> t.mapValue(it -> true))
            .reduce(LocalDateTimeline.empty(), (t1, t2) -> t1.crossJoin(t2, StandardCombinators::alwaysTrueForMatch));

        var alleEndringer = endringerIAY.getInntektEndringer().stream().map(Aktivitetsendringer::getEndringerForUtbetaling)
            .map(t -> t.mapValue(it -> true))
            .reduce(endringIAnsattforholdTidslinje, (t1, t2) -> t1.crossJoin(t2, StandardCombinators::alwaysTrueForMatch));

        return alleEndringer.compress().getLocalDateIntervals()
            .stream()
            .map(p -> new PeriodeMedRegisterendring(new Periode(p.getFomDato(), p.getTomDato())))
            .collect(Collectors.toSet());
    }


}
