package no.nav.ung.sak.web.app.tjenester.brukerdialog;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessurs;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionType;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursResourceType;
import no.nav.k9.sikkerhet.context.SubjectHandler;
import no.nav.ung.sak.felles.typer.AktørId;
import no.nav.ung.sak.kontrakt.person.AktørIdDto;
import no.nav.ung.sak.oppgave.BrukerdialogOppgaveTjeneste;

import java.util.List;
import java.util.UUID;

@Path(BrukerdialogOppgaveRestTjeneste.BASE_PATH)
@ApplicationScoped
@Transactional
@Produces(MediaType.APPLICATION_JSON)
public class BrukerdialogOppgaveRestTjeneste {
    static final String BASE_PATH = "/brukerdialog/oppgave";

    private BrukerdialogOppgaveTjeneste oppgaveTjeneste;

    public BrukerdialogOppgaveRestTjeneste() {
        // CDI proxy
    }

    @Inject
    public BrukerdialogOppgaveRestTjeneste(BrukerdialogOppgaveTjeneste oppgaveTjeneste) {
        this.oppgaveTjeneste = oppgaveTjeneste;
    }

    @GET
    @Path("/hent/alle")
    @Operation(summary = "Henter alle oppgaver for en bruker", tags = "brukerdialog-oppgave")
    @BeskyttetRessurs(action = BeskyttetRessursActionType.READ, resource = BeskyttetRessursResourceType.TOKENX_RESOURCE)
    public List<no.nav.ung.sak.kontrakt.oppgave.BrukerdialogOppgaveDto> hentAlleOppgaver() {
        String personIdent = SubjectHandler.getSubjectHandler().getSluttBruker().getUid();
        // TODO: Implementer veksling av personident til aktørid

        AktørId aktørId;
        return oppgaveTjeneste.hentAlleOppgaverForAktør(null);
    }

    @GET
    @Path("/hent/varsler")
    @Operation(summary = "Henter alle varsler for en bruker", tags = "brukerdialog-oppgave")
    @BeskyttetRessurs(action = BeskyttetRessursActionType.READ, resource = BeskyttetRessursResourceType.TOKENX_RESOURCE)
    public List<no.nav.ung.sak.kontrakt.oppgave.BrukerdialogOppgaveDto> hentAlleVarsler(
        @NotNull @QueryParam("aktørId") @Parameter(description = "Aktør-ID for bruker") @Valid AktørIdDto aktørIdDto) {

        AktørId aktørId = aktørIdDto.getAktørId();
        return oppgaveTjeneste.hentAlleVarslerForAktør(aktørId);
    }

    @GET
    @Path("/hent/soknader")
    @Operation(summary = "Henter alle søknader for en bruker", tags = "brukerdialog-oppgave")
    @BeskyttetRessurs(action = BeskyttetRessursActionType.READ, resource = BeskyttetRessursResourceType.TOKENX_RESOURCE)
    public List<no.nav.ung.sak.kontrakt.oppgave.BrukerdialogOppgaveDto> hentAlleSøknader(
        @NotNull @QueryParam("aktørId") @Parameter(description = "Aktør-ID for bruker") @Valid AktørIdDto aktørIdDto) {

        AktørId aktørId = aktørIdDto.getAktørId();
        return oppgaveTjeneste.hentAlleSøknaderForAktør(aktørId);
    }

    @GET
    @Path("/{oppgavereferanse}")
    @Operation(summary = "Henter en spesifikk oppgave basert på oppgavereferanse", tags = "brukerdialog-oppgave")
    @BeskyttetRessurs(action = BeskyttetRessursActionType.READ, resource = BeskyttetRessursResourceType.TOKENX_RESOURCE)
    public no.nav.ung.sak.kontrakt.oppgave.BrukerdialogOppgaveDto hentOppgave(
        @NotNull @PathParam("oppgavereferanse") @Parameter(description = "Unik referanse til oppgaven") UUID oppgavereferanse) {

        return oppgaveTjeneste.hentOppgaveForOppgavereferanse(oppgavereferanse);
    }

    @GET
    @Path("/{oppgavereferanse}/lukk")
    @Operation(summary = "Lukker en oppgave", tags = "brukerdialog-oppgave")
    @BeskyttetRessurs(action = BeskyttetRessursActionType.UPDATE, resource = BeskyttetRessursResourceType.TOKENX_RESOURCE)
    public no.nav.ung.sak.kontrakt.oppgave.BrukerdialogOppgaveDto lukkOppgave(
        @NotNull @PathParam("oppgavereferanse") @Parameter(description = "Unik referanse til oppgaven") UUID oppgavereferanse) {

        return oppgaveTjeneste.lukkOppgave(oppgavereferanse);
    }

    @GET
    @Path("/{oppgavereferanse}/apnet")
    @Operation(summary = "Åpner en oppgave", tags = "brukerdialog-oppgave")
    @BeskyttetRessurs(action = BeskyttetRessursActionType.UPDATE, resource = BeskyttetRessursResourceType.TOKENX_RESOURCE)
    public no.nav.ung.sak.kontrakt.oppgave.BrukerdialogOppgaveDto åpneOppgave(
        @NotNull @PathParam("oppgavereferanse") @Parameter(description = "Unik referanse til oppgaven") UUID oppgavereferanse) {

        return oppgaveTjeneste.åpneOppgave(oppgavereferanse);
    }

    @GET
    @Path("/{oppgavereferanse}/løst")
    @Operation(summary = "Markerer en oppgave som løst", tags = "brukerdialog-oppgave")
    @BeskyttetRessurs(action = BeskyttetRessursActionType.UPDATE, resource = BeskyttetRessursResourceType.TOKENX_RESOURCE)
    public no.nav.ung.sak.kontrakt.oppgave.BrukerdialogOppgaveDto løsOppgave(
        @NotNull @PathParam("oppgavereferanse") @Parameter(description = "Unik referanse til oppgaven") UUID oppgavereferanse) {

        return oppgaveTjeneste.løsOppgave(oppgavereferanse);
    }
}

