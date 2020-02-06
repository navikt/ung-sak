package no.nav.k9.sak.kontrakt.historikk;

import javax.validation.Valid;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.kodeverk.historikk.HistorikkOpplysningType;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class HistorikkinnslagOpplysningDto {

    @JsonProperty(value="opplysningType")
    @Valid
    private HistorikkOpplysningType opplysningType;
    
    @JsonProperty(value="tilVerdi")
    @Size(max = 5000)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{L}\\p{N}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String tilVerdi;

    public HistorikkinnslagOpplysningDto(){
    }

    public HistorikkOpplysningType getOpplysningType() {
        return opplysningType;
    }

    public void setOpplysningType(HistorikkOpplysningType opplysningType) {
        this.opplysningType = opplysningType;
    }

    public String getTilVerdi() {
        return tilVerdi;
    }

    public void setTilVerdi(String tilVerdi) {
        this.tilVerdi = tilVerdi;
    }

   
}
