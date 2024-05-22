package no.nav.k9.sak.web.app.tjenester.brukerdialog;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import no.nav.k9.sak.typer.AktørId;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public record HentSisteGyldigeVedtakForAktørInputDto(
    @NotNull
    @Valid
    @JsonProperty("aktørId")
    AktørId aktørId,

    @NotNull
    @Valid
    @JsonProperty("pleietrengendeAktørId")
    AktørId pleietrengendeAktørId
) {
}
