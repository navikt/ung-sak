package no.nav.k9.sak.kontrakt.historikk;

import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import no.nav.k9.sak.kontrakt.Patterns;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class HistorikkinnslagTotrinnsVurderingDto {

    @JsonProperty(value = "aksjonspunktBegrunnelse")
    @Size(max = 4000)
    @Pattern(regexp = Patterns.FRITEKST, message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    private String aksjonspunktBegrunnelse;

    @JsonProperty(value = "aksjonspunktKode")
    @Size(max = 100)
    @Pattern(regexp = "^[\\p{Alnum}]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    private String aksjonspunktKode;

    @JsonProperty(value = "godkjent")
    private boolean godkjent;

    public HistorikkinnslagTotrinnsVurderingDto() {
        //
    }

    public String getAksjonspunktBegrunnelse() {
        return aksjonspunktBegrunnelse;
    }

    public String getAksjonspunktKode() {
        return aksjonspunktKode;
    }

    public String getBegrunnelse() {
        return aksjonspunktBegrunnelse;
    }

    public boolean isGodkjent() {
        return godkjent;
    }

    public void setAksjonspunktBegrunnelse(String aksjonspunktBegrunnelse) {
        this.aksjonspunktBegrunnelse = aksjonspunktBegrunnelse;
    }

    public void setAksjonspunktKode(String aksjonspunktKode) {
        this.aksjonspunktKode = aksjonspunktKode;
    }

    public void setBegrunnelse(String aksjonspunktBegrunnelse) {
        this.aksjonspunktBegrunnelse = aksjonspunktBegrunnelse;
    }

    public void setGodkjent(boolean godkjent) {
        this.godkjent = godkjent;
    }

}
