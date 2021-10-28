package no.nav.k9.sak.web.app.tjenester.behandling.kompletthet;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.BehandlingResultatType;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.k9.sak.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.k9.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.k9.sak.kontrakt.kompletthet.aksjonspunkt.BekreftHenleggelseUkompletthetSøknadsgrunnlagDto;

@ApplicationScoped
@DtoTilServiceAdapter(dto = BekreftHenleggelseUkompletthetSøknadsgrunnlagDto.class, adapter = AksjonspunktOppdaterer.class)
public class BekreftHenleggelseUkomplettSøknadsgrunnlagOppdaterer implements AksjonspunktOppdaterer<BekreftHenleggelseUkompletthetSøknadsgrunnlagDto> {

    @Inject
    public BekreftHenleggelseUkomplettSøknadsgrunnlagOppdaterer() {
        // CDI
    }

    @Override
    public OppdateringResultat oppdater(BekreftHenleggelseUkompletthetSøknadsgrunnlagDto dto, AksjonspunktOppdaterParameter param) {
        return OppdateringResultat.medHenleggelse(BehandlingResultatType.HENLAGT_IKKE_KOMPLETT, dto.getBegrunnelse());
    }
}
