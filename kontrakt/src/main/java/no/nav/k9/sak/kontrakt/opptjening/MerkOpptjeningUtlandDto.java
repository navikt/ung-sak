package no.nav.k9.sak.kontrakt.opptjening;

import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.k9.sak.kontrakt.aksjonspunkt.BekreftetAksjonspunktDto;

@JsonTypeName(AksjonspunktKodeDefinisjon.AUTOMATISK_MARKERING_AV_UTENLANDSSAK_KODE)
public class MerkOpptjeningUtlandDto extends BekreftetAksjonspunktDto {

    public MerkOpptjeningUtlandDto() {
        //For Jackson
    }

}
