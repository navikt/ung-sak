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
import no.nav.k9.felles.integrasjon.pdl.Pdl;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessurs;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionType;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursResourceType;
import no.nav.k9.felles.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.k9.sikkerhet.context.SubjectHandler;
import no.nav.ung.sak.kontrakt.oppgaver.OpprettSøkYtelseOppgaveDto;
import no.nav.ung.sak.oppgave.veileder.VeilederOppgaveTjeneste;
import no.nav.ung.sak.oppgave.veileder.VeilederOppgaveTjenesteImpl;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.oppgave.brukerdialog.BrukerdialogOppgaveTjeneste;
import no.nav.ung.sak.kontrakt.oppgaver.BrukerdialogOppgaveDto;
import no.nav.ung.sak.web.server.abac.AbacAttributtSupplier;

import java.util.List;
import java.util.UUID;

@Path(BrukerdialogOppgaveRestTjeneste.BASE_PATH)
@ApplicationScoped
@Transactional
@Produces(MediaType.APPLICATION_JSON)
public class BrukerdialogOppgaveRestTjeneste {
    static final String BASE_PATH = "/oppgave";

    private BrukerdialogOppgaveTjeneste oppgaveTjeneste;
    private VeilederOppgaveTjeneste veilederOppgaveTjeneste;
    private Pdl pdl;

    public BrukerdialogOppgaveRestTjeneste() {
        // CDI proxy
    }

    @Inject
    public BrukerdialogOppgaveRestTjeneste(BrukerdialogOppgaveTjeneste oppgaveTjeneste,
                                           VeilederOppgaveTjeneste veilederOppgaveTjeneste, Pdl pdl) {
        this.oppgaveTjeneste = oppgaveTjeneste;
        this.veilederOppgaveTjeneste = veilederOppgaveTjeneste;
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


    @POST
    @Path("/opprett/sok-ytelse")
    @Operation(summary = "Oppretter oppgave for å søke ytelse", tags = "brukerdialog-oppgave")
    @BeskyttetRessurs(action = BeskyttetRessursActionType.CREATE, resource = BeskyttetRessursResourceType.UNGDOMSPROGRAM)
    public BrukerdialogOppgaveDto opprettSøkYtelseOppgave(
        @NotNull @Parameter(description = "Data om hvem og hva det søkes om") @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class)
        OpprettSøkYtelseOppgaveDto opprettSøkYtelseOppgaveDto) {
        return veilederOppgaveTjeneste.opprettSøkYtelseOppgave(opprettSøkYtelseOppgaveDto);
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

