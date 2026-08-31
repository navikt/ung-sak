package no.nav.ung.ytelse.aktivitetspenger.del1.steg.bistandsvilkår;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.sak.behandlingskontroll.*;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.perioder.ProsessTriggerPeriodeUtleder;

import java.util.List;

import static no.nav.ung.kodeverk.behandling.BehandlingStegType.VURDER_FAKTA_OM_BISTAND;

@ApplicationScoped
@BehandlingStegRef(value = VURDER_FAKTA_OM_BISTAND)
@BehandlingTypeRef
@FagsakYtelseTypeRef(FagsakYtelseType.AKTIVITETSPENGER)
public class VurderFaktaBistandSteg implements BehandlingSteg {

    private Instance<ProsessTriggerPeriodeUtleder> prosessTriggerPeriodeUtledere;
    private BehandlingRepository behandlingRepository;

    VurderFaktaBistandSteg() {
        // for CDI proxy
    }

    @Inject
    public VurderFaktaBistandSteg(BehandlingRepository behandlingRepository,
                                  @Any Instance<ProsessTriggerPeriodeUtleder> prosessTriggerPeriodeUtledere) {
        this.prosessTriggerPeriodeUtledere = prosessTriggerPeriodeUtledere;
        this.behandlingRepository = behandlingRepository;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        long behandlingId = kontekst.getBehandlingId();
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        LocalDateTimeline<Boolean> tidslinjeForManuellFaktavurdering = finnTidslinjeForManuellFaktavurdering(behandling, behandlingId);
        // Saksbehandler må avklare bistandsbehovet for perioder med endret bistandsbehov før vilkåret vurderes
        if (!tidslinjeForManuellFaktavurdering.isEmpty()) {
            return BehandleStegResultat.utførtMedAksjonspunkter(List.of(AksjonspunktDefinisjon.VURDER_FAKTA_OM_BISTAND));
        }

        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

    private LocalDateTimeline<Boolean> finnTidslinjeForManuellFaktavurdering(Behandling behandling, long behandlingId) {
        return ProsessTriggerPeriodeUtleder.finnTjeneste(prosessTriggerPeriodeUtledere, behandling.getFagsakYtelseType())
            .utledTidslinje(behandlingId)
            .filterValue(it -> it.contains(BehandlingÅrsakType.ENDRET_BISTANDSBEHOV))
            .mapValue(_ -> true);
    }
}
