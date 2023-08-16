package no.nav.k9.sak.kontrakt.uttak.overstyring;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class OverstyrUttakSlettPeriodeDto {

    @JsonProperty(value = "id", required = true)
    @NotNull
    @Min(value = 999951) //sekvens begynner på 1M og tar 50 om gangen
    @Max(value = Integer.MAX_VALUE)
    private Long id;

    public OverstyrUttakSlettPeriodeDto() {
        //
    }

    public OverstyrUttakSlettPeriodeDto(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }
}