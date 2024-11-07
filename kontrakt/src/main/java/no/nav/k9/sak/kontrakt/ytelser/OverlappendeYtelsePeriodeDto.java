package no.nav.k9.sak.kontrakt.ytelser;

import java.time.LocalDate;

import jakarta.validation.Valid;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class OverlappendeYtelsePeriodeDto {


    @JsonProperty(value = "fom", required = true)
    @Valid
    private LocalDate fom;

    @JsonProperty(value = "tom", required = true)
    @Valid
    private LocalDate tom;

    public OverlappendeYtelsePeriodeDto(LocalDate fom, LocalDate tom) {
        this.fom = fom;
        this.tom = tom;
    }
}
