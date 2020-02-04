package no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt;

import java.util.Collection;

import javax.validation.Valid;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.foreldrepenger.behandling.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.vedtak.aksjonspunkt.AksjonspunktGodkjenningDto;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;

@JsonTypeName(AksjonspunktKodeDefinisjon.FATTER_VEDTAK_KODE)
public class FatterVedtakAksjonspunktDto extends BekreftetAksjonspunktDto {

    @Valid
    @Size(max = 10)
    private Collection<AksjonspunktGodkjenningDto> aksjonspunktGodkjenningDtos;

    FatterVedtakAksjonspunktDto() {
        // For Jackson
    }

    public FatterVedtakAksjonspunktDto(String begrunnelse, Collection<AksjonspunktGodkjenningDto> aksjonspunktGodkjenningDtos) {
        super(begrunnelse);
        this.aksjonspunktGodkjenningDtos = aksjonspunktGodkjenningDtos;
    }


    public Collection<AksjonspunktGodkjenningDto> getAksjonspunktGodkjenningDtos() {
        return aksjonspunktGodkjenningDtos;
    }
}
