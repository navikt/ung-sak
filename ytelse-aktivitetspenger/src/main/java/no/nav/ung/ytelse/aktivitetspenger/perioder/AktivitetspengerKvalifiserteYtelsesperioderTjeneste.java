package no.nav.ung.ytelse.aktivitetspenger.perioder;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.domene.typer.tid.TidslinjeUtil;
import no.nav.ung.sak.ytelseperioder.KvalifiserteYtelsesperioderTjeneste;
import no.nav.ung.ytelse.aktivitetspenger.del1.AktivitetspengerVilkårsPerioderTilVurderingTjeneste;

@ApplicationScoped
@FagsakYtelseTypeRef(FagsakYtelseType.AKTIVITETSPENGER)
public class AktivitetspengerKvalifiserteYtelsesperioderTjeneste implements KvalifiserteYtelsesperioderTjeneste {

    private AktivitetspengerVilkårsPerioderTilVurderingTjeneste aktivitetspengerVilkårsPerioderTilVurderingTjeneste;

    @Inject
     public AktivitetspengerKvalifiserteYtelsesperioderTjeneste(@FagsakYtelseTypeRef(FagsakYtelseType.AKTIVITETSPENGER) AktivitetspengerVilkårsPerioderTilVurderingTjeneste aktivitetspengerVilkårsPerioderTilVurderingTjeneste) {
        this.aktivitetspengerVilkårsPerioderTilVurderingTjeneste = aktivitetspengerVilkårsPerioderTilVurderingTjeneste;
    }

    public AktivitetspengerKvalifiserteYtelsesperioderTjeneste() {
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
