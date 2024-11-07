package no.nav.k9.sak.kontrakt.krav;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import no.nav.k9.sak.typer.Arbeidsgiver;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public record PerioderMedÅrsakPerKravstiller(
    @NotNull
    @Valid
    @JsonProperty("kravstiller")
    RolleType kravstiller,

    /**
     * Setter kun hvis kravstiller = ARBEIDSGIVER
     */
    @Valid
    @JsonProperty("arbeidsgiver")
    Arbeidsgiver arbeidsgiver,

    @Valid
    @Size
    @JsonProperty("perioderMedÅrsak")
    List<PeriodeMedÅrsaker> perioderMedÅrsak

) {
}
