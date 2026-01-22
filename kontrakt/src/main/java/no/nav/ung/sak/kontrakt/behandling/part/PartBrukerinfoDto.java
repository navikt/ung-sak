package no.nav.ung.sak.kontrakt.behandling.part;


import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import no.nav.ung.sak.felles.typer.AktørId;
import no.nav.ung.sak.felles.typer.Identifikasjon;

import static no.nav.ung.sak.felles.typer.RolleType.BRUKER;

public class PartBrukerinfoDto extends PartDto {

    /** Navn privatperson som har sendt inn klagen. */
    @JsonProperty(value = "navn")
    @Size(max = 200)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{L}\\p{M}\\p{N}]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    public final String navn;

    public PartBrukerinfoDto(String navn, Identifikasjon identifikasjon) {
        super(identifikasjon, BRUKER);
        this.navn = navn;
    }

    public static PartBrukerinfoDto av(String navn, AktørId aktørId) {
        return new PartBrukerinfoDto(navn, Identifikasjon.av(aktørId));
    }
}
