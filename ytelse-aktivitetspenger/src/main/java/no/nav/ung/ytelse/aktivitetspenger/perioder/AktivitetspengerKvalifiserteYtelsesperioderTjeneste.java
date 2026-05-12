package no.nav.ung.ytelse.aktivitetspenger.perioder;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.vilkår.VilkårTjeneste;
import no.nav.ung.sak.ytelseperioder.KvalifiserteYtelsesperioderTjeneste;

@ApplicationScoped
@FagsakYtelseTypeRef(FagsakYtelseType.AKTIVITETSPENGER)
public class AktivitetspengerKvalifiserteYtelsesperioderTjeneste implements KvalifiserteYtelsesperioderTjeneste {

    private VilkårTjeneste vilkårTjeneste;
    private BehandlingRepository behandlingRepository;

    @Inject
    public AktivitetspengerKvalifiserteYtelsesperioderTjeneste(VilkårTjeneste vilkårTjeneste, BehandlingRepository behandlingRepository) {
        this.vilkårTjeneste = vilkårTjeneste;
        this.behandlingRepository = behandlingRepository;
    }

    public AktivitetspengerKvalifiserteYtelsesperioderTjeneste() {
    }

    @Override
    public LocalDateTimeline<Boolean> finnPeriodeTidslinje(Long behandlingId) {
        var samletResultat = vilkårTjeneste.samletVilkårsresultat(behandlingId);
        if (!samletResultat.filterValue(v -> v.getSamletUtfall() == Utfall.IKKE_VURDERT).isEmpty()) {
            return finnInitiellPeriodeTidslinje(behandlingId);
        }
        return samletResultat.filterValue(v -> v.getSamletUtfall() == Utfall.OPPFYLT)
            .mapValue(_ -> true);
    }

    @Override
    public LocalDateTimeline<Boolean> finnInitiellPeriodeTidslinje(Long behandlingId) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        if (behandling.erRevurdering()) {
            return finnPeriodeTidslinje(behandling.getOriginalBehandlingId().orElseThrow(() -> new IllegalStateException("Forventer å finne original behandling")));
        }
        return LocalDateTimeline.empty();
    }
}
