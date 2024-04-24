package no.nav.k9.sak.web.app.tjenester.behandling.beregningsgrunnlag;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.NONE, getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, isGetterVisibility = JsonAutoDetect.Visibility.NONE, creatorVisibility = JsonAutoDetect.Visibility.NONE)
public record FeilInntektsmeldingIBrukResponse(
    @NotNull
    @Valid
    @JsonProperty("behandlingId")
    Long behandlingId,
    @NotNull
    @Valid
    @JsonProperty("vilkårsperioderTilRevurdering")
    List<DatoIntervallEntitet> vilkårsperioderTilRevurdering,
    @NotNull
    @Valid
    @JsonProperty("fordelPerioderEndret")
    List<DatoIntervallEntitet> fordelPerioderEndret) {
}

