package no.nav.k9.sak.kontrakt.medlem;

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
public class BekreftBosattVurderingAksjonspunktDto {

    @JsonProperty(value = "bosattVurdering")
    private Boolean bosattVurdering;

    @JsonProperty(value = "begrunnelse")
    @Size(max = 5000)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{L}\\p{N}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String begrunnelse;

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
}
