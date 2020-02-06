package no.nav.k9.sak.kontrakt.beregningsgrunnlag;

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
public class RefusjonskravSomKommerForSentDto {

    @JsonProperty(value="arbeidsgiverId")
    @Size(max = 20)
    @Pattern(regexp = "^\\d+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String arbeidsgiverId;
    
    @JsonProperty(value="arbeidsgiverVisningsnavn")
    @Size(max = 200)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{L}\\p{M}\\p{N}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String arbeidsgiverVisningsnavn;
    
    @JsonProperty(value="erRefusjonskravGyldig")
    private Boolean erRefusjonskravGyldig;

    public String getArbeidsgiverId() {
        return arbeidsgiverId;
    }

    public void setArbeidsgiverId(String arbeidsgiverId) {
        this.arbeidsgiverId = arbeidsgiverId;
    }

    public String getArbeidsgiverVisningsnavn() {
        return arbeidsgiverVisningsnavn;
    }

    public void setArbeidsgiverVisningsnavn(String arbeidsgiverVisningsnavn) {
        this.arbeidsgiverVisningsnavn = arbeidsgiverVisningsnavn;
    }

    public Boolean getErRefusjonskravGyldig() {
        return erRefusjonskravGyldig;
    }

    public void setErRefusjonskravGyldig(Boolean erRefusjonskravGyldig) {
        this.erRefusjonskravGyldig = erRefusjonskravGyldig;
    }
}
