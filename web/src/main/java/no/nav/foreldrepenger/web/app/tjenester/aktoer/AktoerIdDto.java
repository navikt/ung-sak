package no.nav.foreldrepenger.web.app.tjenester.aktoer;

import java.util.Optional;

import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import no.nav.foreldrepenger.sikkerhet.abac.AppAbacAttributtType;
import no.nav.k9.sak.typer.AktørId;
import no.nav.vedtak.sikkerhet.abac.AbacDataAttributter;
import no.nav.vedtak.sikkerhet.abac.AbacDto;

public class AktoerIdDto implements AbacDto {

    @Size(min = 0, max = 20)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{L}\\p{M}\\p{N}]+$", message="'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private final String aktørId;

    public AktoerIdDto(String aktørId) {
        this.aktørId = aktørId;
    }

    public Optional<AktørId> get() {
        if (aktørId.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new AktørId(aktørId));
    }

    @Override
    public AbacDataAttributter abacAttributter() {
        return AbacDataAttributter.opprett().leggTil(AppAbacAttributtType.AKTØR_ID, aktørId);
    }
}
