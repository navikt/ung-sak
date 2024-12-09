package no.nav.ung.sak.web.app.proxy.oppdrag;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import no.nav.ung.abac.BeskyttetRessursKoder;
import no.nav.k9.felles.sikkerhet.abac.*;
import no.nav.k9.oppdrag.kontrakt.simulering.v1.SimuleringResultatDto;
import no.nav.ung.sak.kontrakt.behandling.*;
import no.nav.ung.sak.web.server.abac.AbacAttributtSupplier;
import no.nav.ung.sak.økonomi.simulering.klient.K9OppdragRestKlient;

import java.util.*;

import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;

@Path("/proxy/oppdrag")
@ApplicationScoped
@Transactional
public class OppdragProxyRestTjeneste {

    public static final String SIMULERING_RESULTAT_URL = "/proxy/oppdrag/simulering/detaljert-barnetilleggTidslinje";

    private K9OppdragRestKlient restKlient;

    public OppdragProxyRestTjeneste() {
        // For Rest-CDI
    }

    @Inject
    public OppdragProxyRestTjeneste(K9OppdragRestKlient restKlient) {
        this.restKlient = restKlient;
    }

    @GET
    @Path("/simulering/detaljert-resultat")
    @Operation(description = "Hent detaljert barnetilleggTidslinje av simulering mot økonomi med og uten inntrekk", summary = ("Returnerer simuleringsresultat."), tags = "simulering")
    @BeskyttetRessurs(action = READ, resource = BeskyttetRessursKoder.FAGSAK)
    public Optional<SimuleringResultatDto> hentSimuleringResultat(@NotNull @QueryParam(BehandlingUuidDto.NAME) @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingUuidDto behandlingIdDto) {
        return restKlient.hentSimuleringResultat(behandlingIdDto.getBehandlingUuid());
    }


}
