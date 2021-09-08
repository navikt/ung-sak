package no.nav.k9.sak.kontrakt.uttak;

import java.util.List;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY, isGetterVisibility = JsonAutoDetect.Visibility.NONE)
public class ManglendeArbeidstidDto {

    @JsonProperty(value = "mangler")
    @Valid
    private List<ArbeidsgiverMedPerioderSomManglerDto> mangler;

    public ManglendeArbeidstidDto() {
    }

    public ManglendeArbeidstidDto(List<ArbeidsgiverMedPerioderSomManglerDto> mangler) {
        this.mangler = mangler;
    }

    public List<ArbeidsgiverMedPerioderSomManglerDto> getMangler() {
        return mangler;
    }
}
