package no.nav.ung.sak.web.app.tjenester.forvaltning;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessurs;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionType;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursResourceType;
import no.nav.ung.sak.typer.Saksnummer;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;


@Path("/produksjonsstyring/forvaltning")
@ApplicationScoped
@Transactional
public class ForvaltningProduksjonsstyringRestTjeneste {

    private EntityManager entityManager;

    @Inject
    public ForvaltningProduksjonsstyringRestTjeneste(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public ForvaltningProduksjonsstyringRestTjeneste() {
        // For Rest-CDI
    }

    @GET
    @Path("saker-med-aksjonspunkt")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Henter alle saker som venter på aksjonspunkt, sortert etter eldste behandling først", summary = ("Brukes for produksjonsstyring"), tags = "produksjonsstyring")
    @BeskyttetRessurs(action = BeskyttetRessursActionType.READ, resource = BeskyttetRessursResourceType.DRIFT)
    public List<Saksnummer> sakerPåAksjonspunkt() {
        List<String> resultat = (List<String>) entityManager.createNativeQuery("""
                select saksnummer
                 from aksjonspunkt a
                 join behandling b on a.behandling_id = b.id
                 join fagsak f on b.fagsak_id = f.id
                 where a.aksjonspunkt_status = 'OPPR'
                   and (a.vent_aarsak = '-' or a.vent_aarsak is null) -- filtrerer bort autopunkt
                 order by b.opprettet_tid;
                """)
            .getResultList();
        return resultat.stream()
            .map(Saksnummer::new)
            .collect(Collectors.toCollection(LinkedHashSet::new)) //for å fjerne evt. duplikate og beholde rekkefølge
            .stream()
            .toList();
    }


}
