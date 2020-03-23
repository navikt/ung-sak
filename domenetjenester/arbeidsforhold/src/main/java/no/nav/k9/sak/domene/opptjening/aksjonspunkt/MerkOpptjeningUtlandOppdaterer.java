package no.nav.k9.sak.domene.opptjening.aksjonspunkt;

import javax.enterprise.context.ApplicationScoped;

import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.k9.sak.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.k9.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.k9.sak.kontrakt.opptjening.MerkOpptjeningUtlandDto;

@ApplicationScoped
@DtoTilServiceAdapter(dto = MerkOpptjeningUtlandDto.class, adapter = AksjonspunktOppdaterer.class)
class MerkOpptjeningUtlandOppdaterer implements AksjonspunktOppdaterer<MerkOpptjeningUtlandDto> {

    @Override
    public OppdateringResultat oppdater(MerkOpptjeningUtlandDto dto, AksjonspunktOppdaterParameter param) {
        return OppdateringResultat.utenOveropp();
    }
}
