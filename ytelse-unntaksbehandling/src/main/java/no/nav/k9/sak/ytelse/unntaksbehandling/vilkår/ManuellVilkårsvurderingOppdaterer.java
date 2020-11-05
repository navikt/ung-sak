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
import no.nav.k9.sak.ytelse.unntaksbehandling.repo.ManuellVilkårsvurderingGrunnlagRepository;
import no.nav.k9.sak.ytelse.unntaksbehandling.repo.VilkårsvurderingFritekst;

@ApplicationScoped
@DtoTilServiceAdapter(dto = VurderVilkårManueltDto.class, adapter = AksjonspunktOppdaterer.class)
public class ManuellVilkårsvurderingOppdaterer implements AksjonspunktOppdaterer<VurderVilkårManueltDto> {

    private BehandlingRepository behandlingRepository;
    private ManuellVilkårsvurderingGrunnlagRepository vilkårsvurderingRepository;

    @SuppressWarnings("unused")
    ManuellVilkårsvurderingOppdaterer() {
        // for CDI
    }

    @Inject
    public ManuellVilkårsvurderingOppdaterer(BehandlingRepositoryProvider repositoryProvider, ManuellVilkårsvurderingGrunnlagRepository vilkårsvurderingRepository) {
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();  //TODO Sjekk ut provider
        this.vilkårsvurderingRepository = vilkårsvurderingRepository;
    }

    // TODO: Lage lese-tjeneste som henter ut manuell vilkårsvurdering

    @Override
    public OppdateringResultat oppdater(VurderVilkårManueltDto dto, AksjonspunktOppdaterParameter param) {
        var behandlingId = param.getBehandlingId();
        var behandling = behandlingRepository.hentBehandling(behandlingId);

        behandling.setBehandlingResultatType(dto.getBehandlingResultatType());
        vilkårsvurderingRepository.lagreOgFlushFritekst(behandlingId, new VilkårsvurderingFritekst(dto.getFritekst()));

        return OppdateringResultat.utenOveropp();
    }
}
