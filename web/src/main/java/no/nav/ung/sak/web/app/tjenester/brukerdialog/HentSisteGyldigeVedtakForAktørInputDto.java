package no.nav.ung.sak.web.app.tjenester.brukerdialog;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import no.nav.ung.abac.AbacAttributt;
import no.nav.k9.felles.sikkerhet.abac.AbacDataAttributter;
import no.nav.k9.felles.sikkerhet.abac.AbacDto;
import no.nav.ung.sak.sikkerhet.abac.AppAbacAttributtType;
import no.nav.ung.sak.typer.AktørId;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public record HentSisteGyldigeVedtakForAktørInputDto(
    @NotNull
    @Valid
    @JsonProperty("pleietrengendeAktørId")
    @AbacAttributt(value = "pleietrengendeAktørId", masker = true)
    AktørId pleietrengendeAktørId
) implements AbacDto {

    @Override
    public AbacDataAttributter abacAttributter() {
        return AbacDataAttributter.opprett()
            .leggTil(AppAbacAttributtType.AKTØR_ID, pleietrengendeAktørId.getAktørId());
    }
}
