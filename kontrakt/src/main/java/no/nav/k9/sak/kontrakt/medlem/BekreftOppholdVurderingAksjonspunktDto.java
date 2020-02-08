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
public class BekreftOppholdVurderingAksjonspunktDto {
    
    @JsonProperty(value = "oppholdsrettVurdering")
    private Boolean oppholdsrettVurdering;
    
    @JsonProperty(value = "lovligOppholdVurdering")
    private Boolean lovligOppholdVurdering;
    
    @JsonProperty(value = "erEosBorger")
    private Boolean erEosBorger;
    
    @JsonProperty(value = "begrunnelse")
    @Size(max = 4000)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{L}\\p{N}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String begrunnelse;

    public BekreftOppholdVurderingAksjonspunktDto(Boolean oppholdsrettVurdering, Boolean lovligOppholdVurdering, Boolean erEosBorger, String begrunnelse) {
        this.oppholdsrettVurdering = oppholdsrettVurdering;
        this.lovligOppholdVurdering = lovligOppholdVurdering;
        this.erEosBorger = erEosBorger;
        this.begrunnelse = begrunnelse;
    }

    public String getBegrunnelse() {
        return begrunnelse;
    }

    public Boolean getOppholdsrettVurdering() {
        return oppholdsrettVurdering;
    }

    public Boolean getLovligOppholdVurdering() {
        return lovligOppholdVurdering;
    }

    public Boolean getErEosBorger() {
        return erEosBorger;
    }
}
