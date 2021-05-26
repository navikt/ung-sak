package no.nav.k9.sak.web.app.tjenester.behandling.tilsyn;

import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.k9.sak.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.k9.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.k9.sak.kontrakt.tilsyn.aksjonspunkt.VurderingBeredskapDto;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
@DtoTilServiceAdapter(dto = VurderingBeredskapDto.class, adapter = AksjonspunktOppdaterer.class)
public class VurderBeredskap implements AksjonspunktOppdaterer<VurderingBeredskapDto> {

    private UnntakEtablertTilsynOppdateringService unntakEtablertTilsynOppdateringService;

    VurderBeredskap() {
        // for CDI proxy
    }

    @Inject
    public VurderBeredskap(UnntakEtablertTilsynOppdateringService unntakEtablertTilsynOppdateringService) {
        this.unntakEtablertTilsynOppdateringService = unntakEtablertTilsynOppdateringService;
    }


    @Override
    public OppdateringResultat oppdater(VurderingBeredskapDto dto, AksjonspunktOppdaterParameter param) {
        return unntakEtablertTilsynOppdateringService.oppdater(dto.getVurderinger(), Vurderingstype.BEREDSKAP, param.getBehandlingId(), param.getAktørId());
    }
}
