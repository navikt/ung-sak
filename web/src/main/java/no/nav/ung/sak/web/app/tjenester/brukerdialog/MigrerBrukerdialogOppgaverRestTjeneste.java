package no.nav.ung.sak.web.app.tjenester.brukerdialog;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessurs;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionType;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursResourceType;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.ung.sak.kontrakt.oppgaver.MigrerOppgaveDto;
import no.nav.ung.sak.kontrakt.oppgaver.MigreringsRequest;
import no.nav.ung.sak.kontrakt.oppgaver.MigreringsResultat;
import no.nav.ung.sak.oppgave.BrukerdialogOppgaveRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * REST-tjeneste for migrering av brukerdialogoppgaver fra annen applikasjon.
 * Tilgjengelig kun for systemtoken.
 */
@Path("/forvaltning/oppgave/migrer")
@ApplicationScoped
@Transactional
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Forvaltning", description = "API for forvaltning av brukerdialog oppgaver")
public class MigrerBrukerdialogOppgaverRestTjeneste {

    private static final Logger log = LoggerFactory.getLogger(MigrerBrukerdialogOppgaverRestTjeneste.class);

    private BrukerdialogOppgaveRepository repository;
    private ProsessTaskTjeneste prosessTaskTjeneste;
    private ObjectMapper objectMapper;

    public MigrerBrukerdialogOppgaverRestTjeneste() {
        // CDI proxy
    }

    @Inject
    public MigrerBrukerdialogOppgaverRestTjeneste(
        BrukerdialogOppgaveRepository repository,
        ProsessTaskTjeneste prosessTaskTjeneste) {
        this.repository = repository;
        this.prosessTaskTjeneste = prosessTaskTjeneste;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.findAndRegisterModules();
    }

    /**
     * Migrerer oppgaver fra annen applikasjon.
     * Idempotent - oppgaver som allerede eksisterer hoppes over.
     *
     * @param request Liste med oppgaver som skal migreres
     * @return Resultat med statistikk over migrering
     */
    @POST
    @Operation(
        summary = "Migrer brukerdialogoppgaver fra annen applikasjon",
        description = "Oppretter en prosess-task per oppgave for migrering. " +
            "Idempotent - gjør ingenting hvis oppgave med samme referanse allerede eksisterer."
    )
    @BeskyttetRessurs(action = BeskyttetRessursActionType.CREATE, resource = BeskyttetRessursResourceType.APPLIKASJON)
    public Response migrerOppgaver(@Valid @NotNull MigreringsRequest request) {
        log.info("Mottatt forespørsel om å migrere {} oppgaver", request.oppgaver().size());

        int antallOpprettet = 0;
        int antallHoppetOver = 0;

        for (MigrerOppgaveDto oppgave : request.oppgaver()) {
            // Sjekk om oppgave allerede eksisterer
            var eksisterende = repository.hentOppgaveForOppgavereferanse(oppgave.oppgaveReferanse());

            if (eksisterende.isPresent()) {
                log.debug("Oppgave med referanse {} eksisterer allerede, hopper over", oppgave.oppgaveReferanse());
                antallHoppetOver++;
            } else {
                // Opprett task for opprettelse
                opprettMigreringsTask(oppgave);
                antallOpprettet++;
            }
        }

        var resultat = new MigreringsResultat(antallOpprettet, antallHoppetOver);
        log.info("Migrering fullført: {} opprettet, {} hoppet over, {} totalt",
            resultat.antallOpprettet(), resultat.antallHoppetOver(), resultat.antallTotalt());

        return Response.ok(resultat).build();
    }

    private void opprettMigreringsTask(MigrerOppgaveDto oppgave) {
        try {
            ProsessTaskData task = ProsessTaskData.forProsessTask(MigrerBrukerdialogOppgaveTask.class);
            task.setProperty(MigrerBrukerdialogOppgaveTask.OPPGAVE_DATA,
                objectMapper.writeValueAsString(oppgave));
            task.setCallIdFraEksisterende();
            prosessTaskTjeneste.lagre(task);
            log.debug("Opprettet migreringstask for oppgave {}", oppgave.oppgaveReferanse());
        } catch (Exception e) {
            log.error("Feil ved opprettelse av migreringstask for oppgave {}", oppgave.oppgaveReferanse(), e);
            throw new RuntimeException("Feil ved opprettelse av migreringstask", e);
        }
    }
}

