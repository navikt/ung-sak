package no.nav.k9.sak.kontrakt.vedtak;

import java.util.Collections;
import java.util.Set;

import javax.validation.Valid;
import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.VurderÅrsak;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class AksjonspunktGodkjenningDto {

    @JsonProperty(value = "aksjonspunktKode", required = true)
    @Size(min = 4, max = 10)
    @Pattern(regexp = "^[\\p{L}\\p{N}_\\.\\-/]+$")
    private String aksjonspunktKode;

    @JsonInclude(value = Include.NON_EMPTY)
    @JsonProperty(value = "arsaker")
    @Valid
    @Size(max = 20)
    private Set<VurderÅrsak> arsaker = Collections.emptySet();

    @JsonInclude(value = Include.NON_EMPTY)
    @JsonProperty(value = "begrunnelse")
    @Size(max = 4000)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{L}\\p{M}\\p{N}§]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    private String begrunnelse;

    @JsonProperty(value = "godkjent", required = true)
    @NotNull
    private boolean godkjent;

    public AksjonspunktGodkjenningDto() {
        //
    }

    @AssertTrue(message = "begrunnelse er påkrevd om det ikke er godkjent")
    private boolean isOk() {
        return godkjent || begrunnelse != null;
    }

    public String getAksjonspunktKode() {
        return aksjonspunktKode;
    }

    public Set<VurderÅrsak> getArsaker() {
        return arsaker == null ? Collections.emptySet() : Collections.unmodifiableSet(arsaker);
    }

    public String getBegrunnelse() {
        return begrunnelse;
    }

    public boolean isGodkjent() {
        return godkjent;
    }

    public void setAksjonspunktKode(AksjonspunktDefinisjon aksjonspunktDefinisjon) {
        this.aksjonspunktKode = aksjonspunktDefinisjon.getKode();

    }

    public void setAksjonspunktKode(String aksjonspunktKode) {
        this.aksjonspunktKode = aksjonspunktKode;
    }

    public void setArsaker(Set<VurderÅrsak> arsaker) {
        this.arsaker = arsaker == null ? null : Set.copyOf(arsaker);
    }

    public void setBegrunnelse(String begrunnelse) {
        this.begrunnelse = begrunnelse;
    }

    public void setGodkjent(boolean godkjent) {
        this.godkjent = godkjent;
    }
}
