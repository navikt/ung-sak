package no.nav.k9.sak.kontrakt.økonomi.tilbakekreving;

import javax.validation.Valid;
import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.kodeverk.økonomi.tilbakekreving.TilbakekrevingVidereBehandling;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class TilbakekrevingValgDto {

    @JsonProperty(value = "erTilbakekrevingVilkårOppfylt", required = true)
    @NotNull
    private Boolean erTilbakekrevingVilkårOppfylt;

    @JsonProperty(value = "grunnerTilReduksjon")
    private Boolean grunnerTilReduksjon; // null når !erTilbakekrevingVilkårOppfylt

    @JsonProperty(value = "varseltekst")
    @Size(max = 4000)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{L}\\p{M}\\p{P}\\p{N}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String varseltekst;

    @JsonProperty(value = "videreBehandling")
    @Valid
    private TilbakekrevingVidereBehandling videreBehandling; // null når erTilbakekrevingVilkårOppfylt

    public TilbakekrevingValgDto(Boolean erTilbakekrevingVilkårOppfylt, Boolean grunnerTilReduksjon, TilbakekrevingVidereBehandling videreBehandling,
                                 String varseltekst) {
        this.erTilbakekrevingVilkårOppfylt = erTilbakekrevingVilkårOppfylt;
        this.grunnerTilReduksjon = grunnerTilReduksjon;
        this.videreBehandling = videreBehandling;
        this.varseltekst = varseltekst;
    }

    protected TilbakekrevingValgDto() {
        //
    }

    public Boolean erTilbakekrevingVilkårOppfylt() {
        return erTilbakekrevingVilkårOppfylt;
    }

    public Boolean getGrunnerTilReduksjon() {
        return grunnerTilReduksjon;
    }

    public String getVarseltekst() {
        return varseltekst;
    }

    public TilbakekrevingVidereBehandling getVidereBehandling() {
        return videreBehandling;
    }

    @AssertTrue(message = "Kan kun ha grunnerTilReduksjon når erTilbakekrevingVilkårOppfylt=true")
    private boolean okGrunner() {
        return erTilbakekrevingVilkårOppfylt || grunnerTilReduksjon == null;
    }

    @AssertTrue(message = "Kan kun ha videreBehandling når erTilbakekrevingVilkårOppfylt=false")
    private boolean okVidereBehandling() {
        return !erTilbakekrevingVilkårOppfylt || videreBehandling == null;
    }
}
