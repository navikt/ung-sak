package no.nav.k9.sak.kontrakt.omsorg;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.sak.kontrakt.sykdom.Resultat;
import no.nav.k9.sak.typer.Periode;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class OmsorgenForOppdateringDto {

    @JsonProperty(value = "periode")
    @Valid
    private Periode periode;
    
    @JsonProperty(value = "begrunnelse")
    @Size(max = 4000)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{L}\\p{M}\\p{N}§]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    @Valid
    private String begrunnelse;
    
    @JsonProperty(value = "resultat")
    @Valid
    private Resultat resultat;

    
    OmsorgenForOppdateringDto() {
        
    }

    public OmsorgenForOppdateringDto(Periode periode, String begrunnelse, Resultat resultat) {
        this.periode = periode;
        this.begrunnelse = begrunnelse;
        this.resultat = resultat;
    }

    
    public Periode getPeriode() {
        return periode;
    }
    
    public String getBegrunnelse() {
        return begrunnelse;
    }

    public Resultat getResultat() {
        return resultat;
    }
}
