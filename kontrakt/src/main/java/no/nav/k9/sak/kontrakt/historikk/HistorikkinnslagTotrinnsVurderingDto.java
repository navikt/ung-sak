package no.nav.k9.sak.kontrakt.historikk;

import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class HistorikkinnslagTotrinnsVurderingDto {

    @JsonProperty(value = "aksjonspunktBegrunnelse")
    @Size(max = 5000)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{L}\\p{N}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String aksjonspunktBegrunnelse;

    @JsonProperty(value = "godkjent")
    private boolean godkjent;

    @JsonProperty(value = "aksjonspunktKode")
    @Size(max = 100)
    @Pattern(regexp = "^[\\p{Alnum}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String aksjonspunktKode;

    public HistorikkinnslagTotrinnsVurderingDto() {
        //
    }

    public String getBegrunnelse() {
        return aksjonspunktBegrunnelse;
    }

    public void setBegrunnelse(String aksjonspunktBegrunnelse) {
        this.aksjonspunktBegrunnelse = aksjonspunktBegrunnelse;
    }

    public boolean isGodkjent() {
        return godkjent;
    }

    public void setGodkjent(boolean godkjent) {
        this.godkjent = godkjent;
    }

    public String getAksjonspunktKode() {
        return aksjonspunktKode;
    }

    public void setAksjonspunktKode(String aksjonspunktKode) {
        this.aksjonspunktKode = aksjonspunktKode;
    }

}
