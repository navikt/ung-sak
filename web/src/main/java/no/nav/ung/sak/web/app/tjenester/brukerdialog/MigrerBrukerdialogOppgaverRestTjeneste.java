package no.nav.ung.sak.web.app.tjenester.brukerdialog;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessurs;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionType;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursResourceType;
import no.nav.ung.sak.kontrakt.oppgaver.MigrerOppgaveDto;
import no.nav.ung.sak.kontrakt.oppgaver.MigreringsRequest;
import no.nav.ung.sak.kontrakt.oppgaver.MigreringsResultat;
import no.nav.ung.sak.oppgave.BrukerdialogOppgaveEntitet;
import no.nav.ung.sak.oppgave.BrukerdialogOppgaveRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * REST-tjeneste for migrering av brukerdialogoppgaver fra annen applikasjon.
 * Tilgjengelig kun for driftstilgang.
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

    public MigrerBrukerdialogOppgaverRestTjeneste() {
        // CDI proxy
    }

    @Inject
    public MigrerBrukerdialogOppgaverRestTjeneste(
        BrukerdialogOppgaveRepository repository) {
        this.repository = repository;
    }

    /**
     * Migrerer oppgaver fra en annen app.
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
    @BeskyttetRessurs(action = BeskyttetRessursActionType.CREATE, resource = BeskyttetRessursResourceType.DRIFT)
    public Response migrerOppgaver(@Valid @NotNull MigreringsRequest request) {
        log.info("Mottatt forespørsel om å migrere {} oppgaver", request.oppgaver().size());

        int antallOpprettet = 0;
        int antallHoppetOver = 0;

        for (MigrerOppgaveDto oppgaveDto : request.oppgaver()) {
            // Sjekk om oppgave allerede eksisterer
            var eksisterende = repository.hentOppgaveForOppgavereferanse(oppgaveDto.oppgaveReferanse());

            if (eksisterende.isPresent()) {
                log.debug("Oppgave med referanse {} eksisterer allerede, hopper over", oppgaveDto.oppgaveReferanse());
                antallHoppetOver++;
            } else {

                // Opprett ny oppgave med alle felter fra migrering
                LocalDateTime frist = oppgaveDto.frist() != null
                    ? oppgaveDto.frist().withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime()
                    : null;

                LocalDateTime løstDato = oppgaveDto.løstDato() != null
                    ? oppgaveDto.løstDato().withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime()
                    : null;

                LocalDateTime åpnetDato = oppgaveDto.åpnetDato() != null
                    ? oppgaveDto.åpnetDato().withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime()
                    : null;

                LocalDateTime lukketDato = oppgaveDto.lukketDato() != null
                    ? oppgaveDto.lukketDato().withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime()
                    : null;

                var nyOppgave = new BrukerdialogOppgaveEntitet(
                    oppgaveDto.oppgaveReferanse(),
                    oppgaveDto.oppgavetype(),
                    oppgaveDto.aktørId(),
                    oppgaveDto.oppgavetypeData(),
                    oppgaveDto.bekreftelse(),
                    oppgaveDto.status(),
                    frist,
                    løstDato,
                    åpnetDato,
                    lukketDato
                );
                repository.persister(nyOppgave);
                antallOpprettet++;
            }
        }

        var resultat = new MigreringsResultat(antallOpprettet, antallHoppetOver);
        log.info("Migrering fullført: {} opprettet, {} hoppet over, {} totalt",
            resultat.antallOpprettet(), resultat.antallHoppetOver(), resultat.antallTotalt());

        return Response.ok(resultat).build();
    }

}

