package no.nav.k9.sak.kontrakt.uttak;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY, isGetterVisibility = JsonAutoDetect.Visibility.NONE)
public class ArbeidsgiverMedPerioderSomManglerDto {

    @JsonProperty(value = "arbeidsgiver")
    @Valid
    private UttakArbeidsforhold arbeidsgiver;

    @JsonProperty(value = "manglendePerioder")
    @Valid
    @Size
    private List<Periode> manglendePerioder;

    public ArbeidsgiverMedPerioderSomManglerDto() {
    }

    public ArbeidsgiverMedPerioderSomManglerDto(UttakArbeidsforhold arbeidsgiver, List<Periode> manglendePerioder) {
        this.arbeidsgiver = arbeidsgiver;
        this.manglendePerioder = manglendePerioder;
    }

    public UttakArbeidsforhold getArbeidsgiver() {
        return arbeidsgiver;
    }

    public List<Periode> getManglendePerioder() {
        return manglendePerioder;
    }
}
