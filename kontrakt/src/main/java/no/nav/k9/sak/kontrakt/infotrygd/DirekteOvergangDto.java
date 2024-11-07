package no.nav.k9.sak.kontrakt.infotrygd;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class DirekteOvergangDto {

    @JsonProperty(value = "skjæringstidspunkter", required = true)
    @Valid
    @NotNull
    @Size(min=1)
    private List<LocalDate> skjæringstidspunkter;


    public DirekteOvergangDto() {
    }

    public DirekteOvergangDto(List<LocalDate> skjæringstidspunkter) {
        this.skjæringstidspunkter = skjæringstidspunkter;
    }

    public List<LocalDate> getSkjæringstidspunkter() {
        return skjæringstidspunkter;
    }
}
