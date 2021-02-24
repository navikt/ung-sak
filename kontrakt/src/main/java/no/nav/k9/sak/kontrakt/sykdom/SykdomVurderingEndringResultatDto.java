package no.nav.k9.sak.kontrakt.sykdom;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.validation.Valid;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class SykdomVurderingEndringResultatDto {

    @JsonProperty(value = "perioderMedEndringer", required = true)
    @Size(max = 100)
    @Valid
    private List<SykdomPeriodeMedEndringDto> perioderMedEndringer = new ArrayList<>();

    
    SykdomVurderingEndringResultatDto() {
        
    }
    
    public SykdomVurderingEndringResultatDto(List<SykdomPeriodeMedEndringDto> perioderMedEndringer) {
        this.perioderMedEndringer = Objects.requireNonNull(perioderMedEndringer, "perioderMedEndringer");
    }
        
    public List<SykdomPeriodeMedEndringDto> getPerioderMedEndringer() {
        return perioderMedEndringer;
    }
}