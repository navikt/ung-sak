package no.nav.ung.sak.kontrakt.søknad;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import no.nav.k9.felles.sikkerhet.abac.StandardAbacAttributtType;
import no.nav.ung.kodeverk.abac.AppAbacAttributt;
import no.nav.ung.kodeverk.abac.AppAbacAttributtType;
import no.nav.ung.kodeverk.abac.StandardAbacAttributt;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.typer.JournalpostId;

public record SendInnPapirsøknadopplysningerRequestDto(

    @StandardAbacAttributt(StandardAbacAttributtType.FNR)
    @Size(max = 20)
    @Pattern(regexp = "^\\d+$", message = "ident [${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    String deltakerIdent,

    @StandardAbacAttributt(StandardAbacAttributtType.JOURNALPOST_ID)
    @Valid
    JournalpostId journalpostIdForPapirsøknad
) {

    /**
     * Papirsøknad-flyten gjelder kun ungdomsytelsen. PDP/ABAC krever at
     * ytelsestype kan utledes for {@code BeskyttetRessursResourceType.FAGSAK},
     * så vi eksponerer den eksplisitt siden den ikke kan utledes fra
     * eksisterende fagsak/behandling i dette steget.
     */
    @AppAbacAttributt(AppAbacAttributtType.YTELSETYPE)
    public String getYtelseTypeKode() {
        return FagsakYtelseType.UNGDOMSYTELSE.getKode();
    }
}
