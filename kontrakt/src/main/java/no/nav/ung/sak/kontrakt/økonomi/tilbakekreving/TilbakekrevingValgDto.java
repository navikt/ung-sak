package no.nav.ung.sak.kontrakt.økonomi.tilbakekreving;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import no.nav.ung.kodeverk.økonomi.tilbakekreving.TilbakekrevingVidereBehandling;

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
    @Size(max = 12000)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{L}\\p{M}\\p{P}\\p{N}]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
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

}
