package no.nav.k9.sak.web.app.tjenester.forvaltning;


import static no.nav.k9.abac.BeskyttetRessursKoder.DRIFT;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessurs;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt;
import no.nav.k9.kodeverk.behandling.BehandlingStatus;
import no.nav.k9.kodeverk.behandling.BehandlingType;


@Path("/unntaksbehandling/forvaltning")
@ApplicationScoped
@Transactional
public class ForvaltningUnntaksløypaRestTjeneste {

    private EntityManager entityManager;

    public ForvaltningUnntaksløypaRestTjeneste() {
        // For Rest-CDI
    }

    @Inject
    public ForvaltningUnntaksløypaRestTjeneste(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @GET
    @Path("finn-ubehandlede")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Henter liste av saker med ubehandlede unntaksbehandlinger")
    @Produces(MediaType.TEXT_PLAIN)
    @BeskyttetRessurs(action = BeskyttetRessursActionAttributt.READ, resource = DRIFT)
    public Response finnUbehandledeUnntaksbehandlnger() {
        String preparedStatement = """
            select saksnummer
             from behandling b
             join fagsak f on b.fagsak_id = f.id
             where b.behandling_type = :behandlingType and behandling_status <> :behandlingAvsluttetStatus
            """;
        Query query = entityManager.createNativeQuery(preparedStatement)
            .setParameter("behandlingType", BehandlingType.UNNTAKSBEHANDLING.getKode())
            .setParameter("behandlingAvsluttetStatus", BehandlingStatus.AVSLUTTET.getKode());
        List resultat = query.getResultList();
        return Response.ok("Saker med ubehandlede unntaksbehandlinger:  " + resultat).build();
    }

}
