package no.nav.ung.sak.kontrakt.behandling.part;

import com.fasterxml.jackson.annotation.*;
import jakarta.validation.Valid;
import no.nav.ung.sak.felles.typer.Identifikasjon;
import no.nav.ung.sak.felles.typer.RolleType;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class PartDto {

    @Valid
    public final Identifikasjon identifikasjon;

    @Valid
    public final RolleType rolleType;

    @JsonCreator
    public PartDto(@JsonProperty("identifikasjon") Identifikasjon identifikasjon,
                   @JsonProperty("rolleType") RolleType rolleType) {
        this.identifikasjon = identifikasjon;
        this.rolleType = rolleType;
    }
}
