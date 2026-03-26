package no.nav.ung.sak.web.app.tjenester.task;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import no.nav.k9.felles.konfigurasjon.env.Environment;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessurs;
import no.nav.k9.felles.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.k9.prosesstask.rest.AbacEmptySupplier;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.ung.sak.typer.Saksnummer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionType.CREATE;
import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursResourceType.DRIFT;


@Path("prosesstask-ung")
@ApplicationScoped
@Transactional
public class ForvaltningTestTaskRestTjeneste {

    private static final Logger logger = LoggerFactory.getLogger(ForvaltningTestTaskRestTjeneste.class);

    private EntityManager entityManager;
    private FagsakRepository fagsakRepository;

    public ForvaltningTestTaskRestTjeneste() {
    }

    @Inject
    public ForvaltningTestTaskRestTjeneste(EntityManager entityManager, FagsakRepository fagsakRepository) {
        this.entityManager = entityManager;
        this.fagsakRepository = fagsakRepository;
    }

    public record ØkPrioritetTaskerForSakRequest(
        Saksnummer saksnummer,
        Integer nyMinimumPrioritet
    ) {
    }

    @POST
    @Path("/økPrioritet")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Øker prioritet på tasker releatert til saksnummer", tags = "prosesstask", responses = {
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "500", description = "Feilet pga ukjent feil eller tekniske/funksjonelle feil")
    })
    @BeskyttetRessurs(action = CREATE, resource = DRIFT)
    public Response økPrioritet(@NotNull @TilpassetAbacAttributt(supplierClass = AbacEmptySupplier.class) @Valid ForvaltningTestTaskRestTjeneste.ØkPrioritetTaskerForSakRequest input) {
        if (Environment.current().isProd()) {
            throw new IllegalStateException("Denne tjenesten er ikke lansert i prod. Se på tilgangskontroll før den evt. lanseres");
        }

        Fagsak fagsak = fagsakRepository.hentSakGittSaksnummer(input.saksnummer).orElseThrow();

        entityManager.createNativeQuery("""
                update prosess_task
                 set prioritet = :nyMinimumPrioritet
                 where status in ('KLAR', 'VENTER_SVAR', 'VETO', 'FEILET')
                   and prioritet < :nyMinimumPrioritet
                   and (task_parametere like :fagsakIdSoek or task_parametere like :saksnummerSoek)
                """
            )
            .setParameter("nyMinimumPrioritet", input.nyMinimumPrioritet)
            .setParameter("saksnummerSoek", "%saksnummer=" + input.saksnummer.getVerdi() + "%")
            .setParameter("fagsakIdSoek", "%fagsakId=" + fagsak.getId() + "%")
            .executeUpdate();

        entityManager.flush();

        logger.info("Økte prioritet på tasker for {} til minst {}", input.saksnummer, input.nyMinimumPrioritet());

        return Response.ok().build();
    }

    @POST
    @Path("/endre-klar-til-kjoert-alle-tasker")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Endrer tasker som er knyttet til sak via parametre fra å være klar til å være kjørt", tags = "prosesstask", responses = {
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "500", description = "Feilet pga ukjent feil eller tekniske/funksjonelle feil")
    })
    @BeskyttetRessurs(action = CREATE, resource = DRIFT)
    public Response endreKlarTilKjørt(@NotNull @TilpassetAbacAttributt(supplierClass = AbacEmptySupplier.class) @Valid Saksnummer saksnummer) {
        if (Environment.current().isProd()) {
            throw new IllegalStateException("Denne tjenesten er ikke lansert i prod. Den skal sannsynligvis aldri i prod heller. Se på tilgangskontroll før den evt. lanseres");
        }

        Fagsak fagsak = fagsakRepository.hentSakGittSaksnummer(saksnummer).orElseThrow();

        entityManager.createNativeQuery("""
                update prosess_task
                 set status = 'KJOERT'
                 where
                  status = 'KLAR'
                 and (task_parametere like :fagsakIdSoek or task_parametere like :saksnummerSoek)
                """
            )
            .setParameter("saksnummerSoek", "%saksnummer=" + saksnummer.getVerdi() + "%")
            .setParameter("fagsakIdSoek", "%fagsakId=" + fagsak.getId() + "%")
            .executeUpdate();

        entityManager.flush();

        logger.info("Endret status på tasker som var KLAR til KJOERT for {}", saksnummer);

        return Response.ok().build();
    }

    public record EndreKlarTilKjørtRequests(
        Saksnummer saksnummer,
        List<String> taskTyperSomEndres
    ) {
    }

    @POST
    @Path("/endre-klar-til-kjoert-spesifikke-tasker")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Endrer tasker som er knyttet til sak via parametre fra å være klar til å være kjørt", tags = "prosesstask", responses = {
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "500", description = "Feilet pga ukjent feil eller tekniske/funksjonelle feil")
    })
    @BeskyttetRessurs(action = CREATE, resource = DRIFT)
    public Response endreKlarTilKjørt(@NotNull @TilpassetAbacAttributt(supplierClass = AbacEmptySupplier.class) @Valid ForvaltningTestTaskRestTjeneste.EndreKlarTilKjørtRequests input) {
        if (Environment.current().isProd()) {
            throw new IllegalStateException("Denne tjenesten er ikke lansert i prod. Den skal sannsynligvis aldri i prod heller. Se på tilgangskontroll før den evt. lanseres");
        }

        Fagsak fagsak = fagsakRepository.hentSakGittSaksnummer(input.saksnummer).orElseThrow();

        entityManager.createNativeQuery("""
                update prosess_task
                 set status = 'KJOERT'
                 where
                  status = 'KLAR'
                 and (task_parametere like :fagsakIdSoek or task_parametere like :saksnummerSoek)
                 and (task_type in :task_typer)
                """
            )
            .setParameter("saksnummerSoek", "%saksnummer=" + input.saksnummer.getVerdi() + "%")
            .setParameter("fagsakIdSoek", "%fagsakId=" + fagsak.getId() + "%")
            .setParameter("task_typer", input.taskTyperSomEndres)
            .executeUpdate();

        entityManager.flush();

        logger.info("Endret status på tasker av typer {} som var KLAR til KJOERT for {}", input.taskTyperSomEndres, input.saksnummer);

        return Response.ok().build();
    }


    public record AntallGjenståendeTaskerForSakRequest(
        Saksnummer saksnummer,
        List<String> taskTyperSomIgnoreres
    ) {
    }

    @POST
    @Path("/antall-gjenstaaende-tasker-for-sak")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Endrer tasker som er knyttet til sak via parametre fra å være klar til å være kjørt", tags = "prosesstask", responses = {
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "500", description = "Feilet pga ukjent feil eller tekniske/funksjonelle feil")
    })
    @BeskyttetRessurs(action = CREATE, resource = DRIFT)
    public Long antallGjenståendeTasker(@NotNull @TilpassetAbacAttributt(supplierClass = AbacEmptySupplier.class) @Valid AntallGjenståendeTaskerForSakRequest input) {
        if (Environment.current().isProd()) {
            throw new IllegalStateException("Denne tjenesten er ikke lansert i prod. Den skal sannsynligvis aldri i prod heller. Se på tilgangskontroll før den evt. lanseres");
        }

        Fagsak fagsak = fagsakRepository.hentSakGittSaksnummer(input.saksnummer).orElseThrow();

        Long svar = (Long) entityManager.createNativeQuery("""
                select count(*)
                 from prosess_task
                 where
                  status in ('KLAR', 'VENTER_SVAR', 'VETO', 'FEILET')
                  and (task_parametere like :fagsakIdSoek or task_parametere like :saksnummerSoek)
                  and task_type not in (:taskTyperSomIgnoreres)
                """
            )
            .setParameter("saksnummerSoek", "%saksnummer=" + input.saksnummer.getVerdi() + "%")
            .setParameter("fagsakIdSoek", "%fagsakId=" + fagsak.getId() + "%")
            .setParameter("taskTyperSomIgnoreres", input.taskTyperSomIgnoreres)
            .getSingleResult();

        entityManager.flush();

        return svar;
    }

}
