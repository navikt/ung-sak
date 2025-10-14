package no.nav.ung.sak.web.app.proxy.oppdrag;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessurs;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursResourceType;
import no.nav.k9.felles.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.k9.oppdrag.kontrakt.oppsummering.OppsummeringDto;
import no.nav.k9.oppdrag.kontrakt.simulering.v1.SimuleringDto;
import no.nav.ung.sak.kontrakt.behandling.BehandlingUuidDto;
import no.nav.ung.sak.web.server.abac.AbacAttributtSupplier;
import no.nav.ung.sak.økonomi.simulering.klient.K9OppdragRestKlient;

import java.util.Optional;

import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionType.READ;

@Path("/proxy/oppdrag")
@ApplicationScoped
@Transactional
public class OppdragProxyRestTjeneste {

    public static final String SIMULERING_RESULTAT_URL = "/proxy/oppdrag/simulering/detaljert-resultat";
    public static final String OPPSUMMERING_URL = "/proxy/oppdrag/oppsummering/v2/oppsummering";

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
    @Operation(description = "Hent detaljert resultat av simulering mot økonomi med og uten inntrekk", summary = ("Returnerer simuleringsresultat."), tags = "simulering")
    @BeskyttetRessurs(action = READ, resource = BeskyttetRessursResourceType.FAGSAK)
    public Optional<SimuleringDto> hentSimuleringResultat(@NotNull @QueryParam(BehandlingUuidDto.NAME) @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingUuidDto behandlingIdDto) {
        return restKlient.hentDetaljertSimuleringResultat(behandlingIdDto.getBehandlingUuid());
    }


    @GET
    @Path("/oppsummering/v2/oppsummering")
    @Operation(description = "Viser oppsummering av hva som sendes til OS. Både totalt opp til og med behandlingen, og differanse mot hva som fantes før behandlingen", summary = ("Oppsummering av hva som sendes til OS"), tags = "oppsummering")
    @BeskyttetRessurs(action = READ, resource = BeskyttetRessursResourceType.FAGSAK)
    public OppsummeringDto hentOppdragOppsummering(@NotNull @QueryParam(BehandlingUuidDto.NAME) @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingUuidDto behandlingIdDto) {
        return restKlient.hentOppsummering(behandlingIdDto.getBehandlingUuid());
    }

}
