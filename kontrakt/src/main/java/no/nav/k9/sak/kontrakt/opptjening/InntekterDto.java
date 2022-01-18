package no.nav.k9.sak.kontrakt.opptjening;

import java.util.List;
import java.util.Objects;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class InntekterDto {

    @JsonInclude(value = Include.ALWAYS)
    @JsonProperty(value = "inntekt")
    @Size(max = 200)
    @Valid
    @NotNull
    private List<InntektDto> inntekt;

    @JsonCreator
    public InntekterDto(@JsonProperty(value = "inntekt", required = true) @NotNull @Valid List<InntektDto> inntekt) {
        this.inntekt = inntekt;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (obj == null || obj.getClass() != this.getClass())
            return false;
        var other = (InntekterDto) obj;
        return Objects.equals(inntekt, other.inntekt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(inntekt);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<inntekt[" + inntekt.size() + "]>";
    }

}
