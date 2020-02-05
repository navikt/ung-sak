package no.nav.k9.sak.kontrakt.medlem;

import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class AvklarFortsattMedlemskapAksjonspunktDto {

    @JsonProperty(value = "perioder")
    @Valid
    @Size(max = 100)
    private List<BekreftedePerioderAdapter> perioder;

    public AvklarFortsattMedlemskapAksjonspunktDto(List<BekreftedePerioderAdapter> perioder) {

        this.perioder = perioder;
    }

    public List<BekreftedePerioderAdapter> getPerioder() {
        return perioder;
    }
}
