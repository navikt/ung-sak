package no.nav.ung.sak.web.app.tjenester.brukerdialog;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import no.nav.k9.felles.integrasjon.pdl.Pdl;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessurs;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionType;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursResourceType;
import no.nav.k9.sikkerhet.context.SubjectHandler;
import no.nav.ung.sak.felles.typer.AktørId;
import no.nav.ung.sak.oppgave.brukerdialog.BrukerdialogOppgaveTjeneste;
import no.nav.ung.sak.oppgave.kontrakt.BrukerdialogOppgaveDto;

import java.util.List;
import java.util.UUID;

@Path(BrukerdialogOppgaveRestTjeneste.BASE_PATH)
@ApplicationScoped
@Transactional
@Produces(MediaType.APPLICATION_JSON)
public class BrukerdialogOppgaveRestTjeneste {
    static final String BASE_PATH = "/brukerdialog/oppgave";

    private BrukerdialogOppgaveTjeneste oppgaveTjeneste;
    private Pdl pdl;

    public BrukerdialogOppgaveRestTjeneste() {
        // CDI proxy
    }

    @Inject
    public BrukerdialogOppgaveRestTjeneste(BrukerdialogOppgaveTjeneste oppgaveTjeneste, Pdl pdl) {
        this.oppgaveTjeneste = oppgaveTjeneste;
        this.pdl = pdl;
    }

    @GET
    @Path("/hent/alle")
    @Operation(summary = "Henter alle oppgaver for en bruker", tags = "brukerdialog-oppgave")
    @BeskyttetRessurs(action = BeskyttetRessursActionType.READ, resource = BeskyttetRessursResourceType.TOKENX_RESOURCE)
    public List<BrukerdialogOppgaveDto> hentAlleOppgaver() {
        return oppgaveTjeneste.hentAlleOppgaverForAktør(finnAktørId());
    }


    @GET
    @Path("/{oppgavereferanse}")
    @Operation(summary = "Henter en spesifikk oppgave basert på oppgavereferanse", tags = "brukerdialog-oppgave")
    @BeskyttetRessurs(action = BeskyttetRessursActionType.READ, resource = BeskyttetRessursResourceType.TOKENX_RESOURCE)
    public BrukerdialogOppgaveDto hentOppgave(
        @NotNull @PathParam("oppgavereferanse") @Parameter(description = "Unik referanse til oppgaven") UUID oppgavereferanse) {

        return oppgaveTjeneste.hentOppgaveForOppgavereferanse(oppgavereferanse, finnAktørId());
    }

    @GET
    @Path("/{oppgavereferanse}/lukk")
    @Operation(summary = "Lukker en oppgave", tags = "brukerdialog-oppgave")
    @BeskyttetRessurs(action = BeskyttetRessursActionType.UPDATE, resource = BeskyttetRessursResourceType.TOKENX_RESOURCE)
    public BrukerdialogOppgaveDto lukkOppgave(
        @NotNull @PathParam("oppgavereferanse") @Parameter(description = "Unik referanse til oppgaven") UUID oppgavereferanse) {
        return oppgaveTjeneste.lukkOppgave(oppgavereferanse,finnAktørId());
    }

    @GET
    @Path("/{oppgavereferanse}/apnet")
    @Operation(summary = "Åpner en oppgave", tags = "brukerdialog-oppgave")
    @BeskyttetRessurs(action = BeskyttetRessursActionType.UPDATE, resource = BeskyttetRessursResourceType.TOKENX_RESOURCE)
    public BrukerdialogOppgaveDto åpneOppgave(
        @NotNull @PathParam("oppgavereferanse") @Parameter(description = "Unik referanse til oppgaven") UUID oppgavereferanse) {
        return oppgaveTjeneste.åpneOppgave(oppgavereferanse, finnAktørId());
    }

    @GET
    @Path("/{oppgavereferanse}/løst")
    @Operation(summary = "Markerer en oppgave som løst", tags = "brukerdialog-oppgave")
    @BeskyttetRessurs(action = BeskyttetRessursActionType.UPDATE, resource = BeskyttetRessursResourceType.TOKENX_RESOURCE)
    public BrukerdialogOppgaveDto løsOppgave(
        @NotNull @PathParam("oppgavereferanse") @Parameter(description = "Unik referanse til oppgaven") UUID oppgavereferanse) {
        return oppgaveTjeneste.løsOppgave(oppgavereferanse, finnAktørId());
    }

    /** Veksler fra personIdent i token til aktørId ved hjelp av PDL.
     * @return AktørId til innlogget bruker
     */
    private AktørId finnAktørId() {
        String personIdent = SubjectHandler.getSubjectHandler().getSluttBruker().getUid();
        String aktørIdString = pdl.hentAktørIdForPersonIdent(personIdent, false)
            .orElseThrow(() -> new IllegalStateException("Finner ikke aktørId for personIdent"));
        return new AktørId(aktørIdString);
    }
}

