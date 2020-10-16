package no.nav.k9.sak.ytelse.unntaksbehandling.vilkår;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.k9.sak.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.k9.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.kontrakt.vilkår.VurderVilkårManueltDto;

@ApplicationScoped
@DtoTilServiceAdapter(dto = VurderVilkårManueltDto.class, adapter = AksjonspunktOppdaterer.class)
public class ManuellVilkårsvurderingOppdaterer implements AksjonspunktOppdaterer<VurderVilkårManueltDto> {

    private BehandlingRepository behandlingRepository;

    ManuellVilkårsvurderingOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public ManuellVilkårsvurderingOppdaterer(BehandlingRepositoryProvider repositoryProvider) {
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
    }

    // TODO: Lage lese-tjeneste som henter ut manuell vilkårsvurdering

    @Override
    public OppdateringResultat oppdater(VurderVilkårManueltDto dto, AksjonspunktOppdaterParameter param) {
        var behandlingId = param.getBehandlingId();
        var behandling = behandlingRepository.hentBehandling(behandlingId);

        behandling.setBehandlingResultatType(dto.getBehandlingResultatType());
        // TODO: Lagre fritekst fra saksbehandler

        return OppdateringResultat.utenTransisjon().build();
    }
}
