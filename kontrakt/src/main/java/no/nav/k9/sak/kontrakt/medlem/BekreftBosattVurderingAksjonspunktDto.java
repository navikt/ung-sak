package no.nav.k9.sak.kontrakt.medlem;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import no.nav.k9.sak.kontrakt.Patterns;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class BekreftBosattVurderingAksjonspunktDto {

    @JsonProperty(value = "begrunnelse")
    @Size(max = 4000)
    @Pattern(regexp = Patterns.FRITEKST, message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    private String begrunnelse;

    @JsonProperty(value = "bosattVurdering")
    private Boolean bosattVurdering;

    public BekreftBosattVurderingAksjonspunktDto() {
        //
    }

    public BekreftBosattVurderingAksjonspunktDto(Boolean bosattVurdering, String begrunnelse) {
        this.bosattVurdering = bosattVurdering;
        this.begrunnelse = begrunnelse;
    }

    public String getBegrunnelse() {
        return begrunnelse;
    }

    public Boolean getBosattVurdering() {
        return bosattVurdering;
    }

    public void setBegrunnelse(String begrunnelse) {
        this.begrunnelse = begrunnelse;
    }

    public void setBosattVurdering(Boolean bosattVurdering) {
        this.bosattVurdering = bosattVurdering;
    }
}
