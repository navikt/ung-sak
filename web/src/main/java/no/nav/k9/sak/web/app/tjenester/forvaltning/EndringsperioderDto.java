package no.nav.k9.sak.web.app.tjenester.forvaltning;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import no.nav.k9.sak.typer.Periode;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.NONE, getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, isGetterVisibility = JsonAutoDetect.Visibility.NONE, creatorVisibility = JsonAutoDetect.Visibility.NONE)
public record EndringsperioderDto(
    @NotNull
    @Valid
    @JsonProperty("harEndretSykdomPerioder")
    List<Periode> harEndretSykdomPerioder,
    @NotNull
    @Valid
    @JsonProperty("harEndretEtablertTilsynPerioder")
    List<Periode> harEndretEtablertTilsynPerioder,
    @NotNull
    @Valid
    @JsonProperty("harEndretNattevåkOgBeredskapPerioder")
    List<Periode> harEndretNattevåkOgBeredskapPerioder,
    @NotNull
    @Valid
    @JsonProperty("harEndretUttakPerioder")
    List<Periode> harEndretUttakPerioder,
    @NotNull
    @Valid
    @JsonProperty("harEndringSomPåvirkerSakPerioder")
    List<Periode> harEndringSomPåvirkerSakPerioder
) {
}
