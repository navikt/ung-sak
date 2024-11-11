package no.nav.ung.sak.kontrakt.beregninginput;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonValue;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;


@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.STRING)
@JsonInclude(value = JsonInclude.Include.NON_ABSENT, content = JsonInclude.Include.NON_EMPTY)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.NONE, getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, isGetterVisibility = JsonAutoDetect.Visibility.NONE, creatorVisibility = JsonAutoDetect.Visibility.NONE)
public class OverstyrOpphørsdatoRefusjon {

    @JsonValue
    @Valid
    @NotNull
    private LocalDate opphørRefusjon;

    OverstyrOpphørsdatoRefusjon() {
    }

    public OverstyrOpphørsdatoRefusjon(LocalDate opphørRefusjon) {
        this.opphørRefusjon = opphørRefusjon;
    }

    @JsonCreator
    public OverstyrOpphørsdatoRefusjon(String opphørsdatoRefusjon) {
        this.opphørRefusjon = LocalDate.parse(opphørsdatoRefusjon);
    }

    public LocalDate getOpphørRefusjon() {
        return opphørRefusjon;
    }


    @Override
    public String toString() {
        return "OverstyrOpphørsdatoRefusjon{" +
            ", opphørRefusjon=" + opphørRefusjon +
            '}';
    }


}
