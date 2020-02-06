package no.nav.k9.sak.kontrakt.økonomi;

import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotNull;

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
@JsonTypeName(AksjonspunktKodeDefinisjon.VURDER_INNTREKK_KODE)
public class VurderFeilutbetalingOgInntrekkDto extends BekreftetAksjonspunktDto {

    @JsonProperty(value = "erTilbakekrevingVilkårOppfylt", required = true)
    @NotNull
    private boolean erTilbakekrevingVilkårOppfylt;

    @JsonProperty(value = "grunnerTilReduksjon", required = false)
    private Boolean grunnerTilReduksjon; // null når !erTilbakekrevingVilkårOppfylt

    private TilbakekrevingVidereBehandling videreBehandling; // null når erTilbakekrevingVilkårOppfylt

    protected VurderFeilutbetalingOgInntrekkDto() {
        //
    }

    public VurderFeilutbetalingOgInntrekkDto(String begrunnelse, boolean erTilbakekrevingVilkårOppfylt, Boolean grunnerTilReduksjon,
                                             TilbakekrevingVidereBehandling videreBehandling) {
        super(begrunnelse);
        this.erTilbakekrevingVilkårOppfylt = erTilbakekrevingVilkårOppfylt;
        this.grunnerTilReduksjon = grunnerTilReduksjon;
        this.videreBehandling = videreBehandling;
    }

    @AssertTrue(message = "Kan kun ha grunnerTilReduksjon når erTilbakekrevingVilkårOppfylt=true")
    private boolean okGrunner() {
        return erTilbakekrevingVilkårOppfylt || grunnerTilReduksjon == null;
    }

    @AssertTrue(message = "Kan kun ha videreBehandling når erTilbakekrevingVilkårOppfylt=false")
    private boolean okVidereBehandling() {
        return !erTilbakekrevingVilkårOppfylt || videreBehandling == null;
    }

    public boolean getErTilbakekrevingVilkårOppfylt() {
        return erTilbakekrevingVilkårOppfylt;
    }

    public Boolean getGrunnerTilReduksjon() {
        return grunnerTilReduksjon;
    }

    public TilbakekrevingVidereBehandling getVidereBehandling() {
        return videreBehandling;
    }

}
