package no.nav.ung.ytelse.aktivitetspenger.perioder;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.sak.domene.typer.tid.TidslinjeUtil;
import no.nav.ung.sak.ytelseperioder.KvalifiserteYtelsesperioderTjeneste;
import no.nav.ung.ytelse.aktivitetspenger.del1.AktivitetspengerVilkårsPerioderTilVurderingTjeneste;

@Dependent
public class AktivitetspengerKvalifiserteYtelsesperioderTjeneste implements KvalifiserteYtelsesperioderTjeneste {

    private final AktivitetspengerVilkårsPerioderTilVurderingTjeneste aktivitetspengerVilkårsPerioderTilVurderingTjeneste;

    @Inject
     public AktivitetspengerKvalifiserteYtelsesperioderTjeneste(AktivitetspengerVilkårsPerioderTilVurderingTjeneste aktivitetspengerVilkårsPerioderTilVurderingTjeneste) {
        this.aktivitetspengerVilkårsPerioderTilVurderingTjeneste = aktivitetspengerVilkårsPerioderTilVurderingTjeneste;
    }


    @Override
    public LocalDateTimeline<Boolean> finnPeriodeTidslinje(Long behandlingId) {
        return TidslinjeUtil.tilTidslinjeKomprimert(aktivitetspengerVilkårsPerioderTilVurderingTjeneste.utledFraDefinerendeVilkår(behandlingId));
    }

    @Override
    public LocalDateTimeline<Boolean> finnInitiellPeriodeTidslinje(Long behandlingId) {
        return finnPeriodeTidslinje(behandlingId);
    }
}
