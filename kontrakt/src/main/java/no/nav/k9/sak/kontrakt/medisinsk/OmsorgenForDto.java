package no.nav.k9.sak.kontrakt.medisinsk;

import javax.validation.Valid;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.sak.kontrakt.sykdom.Resultat;
import no.nav.k9.sak.typer.Periode;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class OmsorgenForDto {

    @JsonProperty(value = "periode")
    @Valid
    private Periode periode;

    // TODO Omsorg: Endre til enum.
    @JsonProperty(value = "relasjon")
    @Size(max = 4000)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{L}\\p{M}\\p{N}§]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    @Valid
    private String relasjon;

    @JsonProperty(value = "relasjonsbeskrivelse")
    @Size(max = 4000)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{L}\\p{M}\\p{N}§]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    @Valid
    private String relasjonsbeskrivelse;
    
    @JsonProperty(value = "begrunnelse")
    @Size(max = 4000)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{L}\\p{M}\\p{N}§]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    @Valid
    private String begrunnelse;
    
    @JsonProperty(value = "resultat")
    @Valid
    private Resultat resultat;

    
    OmsorgenForDto() {
        
    }

    public OmsorgenForDto(Periode periode, String begrunnelse, String relasjon, String relasjonsbeskrivelse, Resultat resultat) {
        this.periode = periode;
        this.begrunnelse = begrunnelse;
        this.relasjon = relasjon;
        this.relasjonsbeskrivelse = relasjonsbeskrivelse;
        this.resultat = resultat;
    }

    
    public Periode getPeriode() {
        return periode;
    }
    
    public String getBegrunnelse() {
        return begrunnelse;
    }
    
    public String getRelasjon() {
        return relasjon;
    }
    
    public String getRelasjonsbeskrivelse() {
        return relasjonsbeskrivelse;
    }
    
    public Resultat getResultat() {
        return resultat;
    }
}
