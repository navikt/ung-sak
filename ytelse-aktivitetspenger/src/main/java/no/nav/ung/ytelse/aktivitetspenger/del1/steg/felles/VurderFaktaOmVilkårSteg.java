package no.nav.ung.ytelse.aktivitetspenger.del1.steg.felles;

import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.ung.sak.behandlingskontroll.BehandlingSteg;
import no.nav.ung.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.perioder.ProsessTriggerPeriodeUtleder;

import java.util.List;

public abstract class VurderFaktaOmVilkårSteg implements BehandlingSteg {

    private Instance<ProsessTriggerPeriodeUtleder> prosessTriggerPeriodeUtledere;
    private BehandlingRepository behandlingRepository;
    private BehandlingÅrsakType avklaringsårsak;
    private AksjonspunktDefinisjon aksjonspunkt;

    protected VurderFaktaOmVilkårSteg() {
        // for CDI proxy
    }

    protected VurderFaktaOmVilkårSteg(BehandlingRepository behandlingRepository,
                                       @Any Instance<ProsessTriggerPeriodeUtleder> prosessTriggerPeriodeUtledere,
                                       BehandlingÅrsakType avklaringsårsak,
                                       AksjonspunktDefinisjon aksjonspunkt) {
        this.behandlingRepository = behandlingRepository;
        this.prosessTriggerPeriodeUtledere = prosessTriggerPeriodeUtledere;
        this.avklaringsårsak = avklaringsårsak;
        this.aksjonspunkt = aksjonspunkt;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        long behandlingId = kontekst.getBehandlingId();
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        LocalDateTimeline<Boolean> tidslinjeForManuellFaktavurdering = finnTidslinjeForManuellFaktavurdering(behandling, behandlingId);
        // Saksbehandler må vurdere fakta manuelt for perioder uten grunnlag — prioritert over vent
        if (!tidslinjeForManuellFaktavurdering.isEmpty()) {
            return BehandleStegResultat.utførtMedAksjonspunkter(List.of(aksjonspunkt));
        }

        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

    private LocalDateTimeline<Boolean> finnTidslinjeForManuellFaktavurdering(Behandling behandling, long behandlingId) {
        return ProsessTriggerPeriodeUtleder.finnTjeneste(prosessTriggerPeriodeUtledere, behandling.getFagsakYtelseType())
            .utledTidslinje(behandlingId)
            .filterValue(it -> it.contains(avklaringsårsak))
            .mapValue(_ -> true);
    }
}
