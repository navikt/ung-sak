package no.nav.ung.sak.kontrakt.søknad;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import no.nav.k9.felles.sikkerhet.abac.StandardAbacAttributtType;
import no.nav.ung.kodeverk.abac.AppAbacAttributt;
import no.nav.ung.kodeverk.abac.AppAbacAttributtType;
import no.nav.ung.kodeverk.abac.StandardAbacAttributt;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.typer.JournalpostId;

public record JournalførPapirSøknadDto(

    @StandardAbacAttributt(StandardAbacAttributtType.JOURNALPOST_ID)
    @JsonProperty(value = "journalpostId", required = true)
    @NotNull
    @Valid
    JournalpostId journalpostId,

    @StandardAbacAttributt(StandardAbacAttributtType.FNR)
    @JsonProperty(value ="deltakerIdent", required = true)
    @NotNull
    @Size(max = 20)
    @Pattern(regexp = "^\\d+$", message = "ident [${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    @Valid
    String deltakerIdent

) {

    /**
     * Papirsøknad-flyten gjelder kun ungdomsytelsen, og fagsaken opprettes som
     * en del av dette endepunktet. PDP/ABAC krever at ytelsestype kan utledes
     * for {@code BeskyttetRessursResourceType.FAGSAK}, så vi eksponerer den
     * eksplisitt her siden den verken kan utledes fra eksisterende fagsak eller
     * behandling på dette tidspunktet.
     */
    @AppAbacAttributt(AppAbacAttributtType.YTELSETYPE)
    public String getYtelseTypeKode() {
        return FagsakYtelseType.UNGDOMSYTELSE.getKode();
    }
}
