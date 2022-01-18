package no.nav.k9.sak.kontrakt.økonomi.tilbakekreving;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.k9.kodeverk.økonomi.tilbakekreving.TilbakekrevingVidereBehandling;
import no.nav.k9.sak.kontrakt.aksjonspunkt.BekreftetAksjonspunktDto;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
@JsonTypeName(AksjonspunktKodeDefinisjon.VURDER_FEILUTBETALING_KODE)
public class VurderFeilutbetalingDto extends BekreftetAksjonspunktDto {

    @JsonProperty(value = "varseltekst")
    @Size(max = 3000)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{L}\\p{M}\\p{N}§]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    private String varseltekst;

    @JsonProperty(value = "videreBehandling", required = true)
    @NotNull
    private TilbakekrevingVidereBehandling videreBehandling;

    public VurderFeilutbetalingDto() {
        //
    }

    public VurderFeilutbetalingDto(String begrunnelse, TilbakekrevingVidereBehandling videreBehandling, String varseltekst) {
        super(begrunnelse);
        this.videreBehandling = videreBehandling;
        this.varseltekst = varseltekst;
    }

    public String getVarseltekst() {
        return varseltekst;
    }

    public TilbakekrevingVidereBehandling getVidereBehandling() {
        return videreBehandling;
    }

    public void setVarseltekst(String varseltekst) {
        this.varseltekst = varseltekst;
    }

    public void setVidereBehandling(TilbakekrevingVidereBehandling videreBehandling) {
        this.videreBehandling = videreBehandling;
    }

}
