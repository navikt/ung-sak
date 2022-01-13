package no.nav.k9.sak.web.app.tjenester.behandling.tilsyn;

import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.k9.sak.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.k9.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.k9.sak.kontrakt.tilsyn.aksjonspunkt.VurderingNattevåkDto;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
@DtoTilServiceAdapter(dto = VurderingNattevåkDto.class, adapter = AksjonspunktOppdaterer.class)
public class VurderNattevåk implements AksjonspunktOppdaterer<VurderingNattevåkDto> {

    private UnntakEtablertTilsynOppdateringService unntakEtablertTilsynOppdateringService;

    VurderNattevåk() {
        // for CDI proxy
    }

    @Inject
    public VurderNattevåk(UnntakEtablertTilsynOppdateringService unntakEtablertTilsynOppdateringService) {
        this.unntakEtablertTilsynOppdateringService = unntakEtablertTilsynOppdateringService;
    }

    @Override
    public OppdateringResultat oppdater(VurderingNattevåkDto dto, AksjonspunktOppdaterParameter param) {
        return unntakEtablertTilsynOppdateringService.oppdater(dto.getVurderinger(), Vurderingstype.NATTEVÅK, param.getBehandlingId(), param.getAktørId());
    }
}
