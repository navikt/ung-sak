package no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Respons fra GET /behandling/bosatt.
 * Returnerer saksbehandlers foreslåtte og eventuelle fastsatte bostedavklaringer per periode.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class BostedGrunnlagResponseDto {

    @JsonProperty("perioder")
    @NotNull
    @Valid
    private List<BostedGrunnlagPeriodeDto> perioder;

    public BostedGrunnlagResponseDto() {
        // for jackson
    }

    public BostedGrunnlagResponseDto(List<BostedGrunnlagPeriodeDto> perioder) {
        this.perioder = perioder;
    }

    public List<BostedGrunnlagPeriodeDto> getPerioder() {
        return perioder;
    }
}
