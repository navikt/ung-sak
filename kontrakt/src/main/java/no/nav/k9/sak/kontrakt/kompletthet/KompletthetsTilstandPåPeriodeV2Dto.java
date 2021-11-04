package no.nav.k9.sak.kontrakt.kompletthet;

import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.sak.kontrakt.uttak.Periode;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class KompletthetsTilstandPåPeriodeV2Dto {

    @NotNull
    @Valid
    @JsonProperty("periode")
    private Periode periode;

    @NotNull
    @Size()
    @Valid
    @JsonProperty("status")
    private List<ArbeidsgiverArbeidsforholdStatusV2> status;

    @JsonCreator
    public KompletthetsTilstandPåPeriodeV2Dto(@JsonProperty("periode") Periode periode,
                                              @JsonProperty("status") List<ArbeidsgiverArbeidsforholdStatusV2> status) {
        this.periode = periode;
        this.status = status;
    }

    public Periode getPeriode() {
        return periode;
    }

    public List<ArbeidsgiverArbeidsforholdStatusV2> getStatus() {
        return status;
    }
}
