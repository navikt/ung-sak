package no.nav.k9.sak.kontrakt.vedtak;

import java.util.Set;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.VurderÅrsak;


@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class AksjonspunktGodkjenningDto {

    @JsonProperty(value="godkent", required = true)
    @NotNull
    private boolean godkjent;

    @JsonProperty(value="begrunnelse", required = true)
    @Size(max = 4000)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{L}\\p{M}\\p{N}]+$", message="'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String begrunnelse;

    @JsonProperty(value="aksjonspunktKode", required = true)
    @Size(min = 4, max = 10)
    @Pattern(regexp = "^[\\p{L}\\p{N}_\\.\\-/]+$")
    private String aksjonspunktKode;

    @JsonProperty(value="arsaker", required = true)
    @Valid
    @NotNull
    @Size(max = 20)
    private Set<no.nav.k9.kodeverk.behandling.aksjonspunkt.VurderÅrsak> arsaker;

    public AksjonspunktGodkjenningDto() { 
        // 
    }

    public boolean isGodkjent() {
        return godkjent;
    }

    public void setGodkjent(boolean godkjent) {
        this.godkjent = godkjent;
    }

    public String getBegrunnelse() {
        return begrunnelse;
    }

    public void setBegrunnelse(String begrunnelse) {
        this.begrunnelse = begrunnelse;
    }

    public Set<VurderÅrsak> getArsaker() {
        return arsaker;
    }

    public void setArsaker(Set<VurderÅrsak> arsaker) {
        this.arsaker = arsaker;
    }

    public void setAksjonspunktKode(String aksjonspunktKode) {
        this.aksjonspunktKode = aksjonspunktKode;
    }

    public String getAksjonspunktKode() {
        return aksjonspunktKode;
    }

    public void setAksjonspunktKode(AksjonspunktDefinisjon aksjonspunktDefinisjon) {
        this.aksjonspunktKode = aksjonspunktDefinisjon.getKode();

    }
}
