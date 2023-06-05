package no.nav.k9.sak.web.app.tjenester.etterkontroll;

import static no.nav.k9.abac.BeskyttetRessursKoder.DRIFT;
import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt.CREATE;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessurs;
import no.nav.k9.sak.behandling.revurdering.etterkontroll.batch.AutomatiskEtterkontrollBatchTask;

@Path("")
@ApplicationScoped
@Transactional
public class EtterkontrollRestTjeneste {

    public static final String KJØR_ETTERKONTROLL_BATCH_PATH = "/etterkontroll/batch";
    public static final Logger log = LoggerFactory.getLogger(EtterkontrollRestTjeneste.class);

    private AutomatiskEtterkontrollBatchTask batchTask;

    public EtterkontrollRestTjeneste(){}

    @Inject
    public EtterkontrollRestTjeneste(@Any AutomatiskEtterkontrollBatchTask batchTask) {
        this.batchTask = batchTask;
    }

    @POST
    @Path(KJØR_ETTERKONTROLL_BATCH_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Utfører klare etterkontroller uten å vente på fast kjøring", tags = "etterkontroll")
    @BeskyttetRessurs(action = CREATE, resource = DRIFT) //TODO er det ok?
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public void kjørEtterkontrollBatch() { // NOSONAR
        log.info("Kjører etterkontroll batch manuelt");
        batchTask.utfør();
    }
}
