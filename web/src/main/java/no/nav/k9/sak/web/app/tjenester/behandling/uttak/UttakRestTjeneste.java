package no.nav.k9.sak.web.app.tjenester.behandling.uttak;

import static no.nav.k9.abac.BeskyttetRessursKoder.FAGSAK;
import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;

import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessurs;
import no.nav.k9.felles.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.k9.sak.kontrakt.behandling.BehandlingUuidDto;
import no.nav.k9.sak.kontrakt.uttak.FastsattUttakDto;
import no.nav.k9.sak.kontrakt.uttak.OppgittUttakDto;
import no.nav.k9.sak.web.server.abac.AbacAttributtSupplier;

@ApplicationScoped
@Transactional
@Path("")
@Produces(MediaType.APPLICATION_JSON)
public class UttakRestTjeneste {

    static final String BASE_PATH = "/behandling/uttak";

    public static final String UTTAK_OPPGITT = BASE_PATH + "/oppgitt";
    public static final String UTTAK_FASTSATT = BASE_PATH + "/fastsatt";

    private MapUttak mapUttak;

    public UttakRestTjeneste() {
        // for proxying
    }

    @Inject
    public UttakRestTjeneste(MapUttak mapOppgittUttak) {
        this.mapUttak = mapOppgittUttak;
    }

    /**
     * Hent oppgitt uttak for angitt behandling.
     */
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Path(UTTAK_OPPGITT)
    @Operation(description = "Hent oppgitt uttak for behandling", tags = "behandling - uttak", responses = {
        @ApiResponse(responseCode = "200", description = "Returnerer Oppgitt uttak fra søknad, null hvis ikke finnes noe", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = OppgittUttakDto.class)))
    })
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public OppgittUttakDto getOppgittUttak(@NotNull @QueryParam(BehandlingUuidDto.NAME) @Parameter(description = BehandlingUuidDto.DESC) @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingUuidDto behandlingIdDto) {
        UUID behandlingId = behandlingIdDto.getBehandlingUuid();
        return mapUttak.mapOppgittUttak(behandlingId);
    }

    /**
     * Hent fastsatt uttak for angitt behandling.
     */
    @GET
    @Path(UTTAK_FASTSATT)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Hent Fastsatt uttak for behandling", tags = "behandling - uttak", responses = {
        @ApiResponse(responseCode = "200", description = "Returnerer uttak fastsatt av saksbehandler (fakta avklart før vurdering av uttak), null hvis ikke finnes noe", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = FastsattUttakDto.class)))
    })
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public FastsattUttakDto getFastsattUttak(@NotNull @QueryParam(BehandlingUuidDto.NAME) @Parameter(description = BehandlingUuidDto.DESC) @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingUuidDto behandlingIdDto) {
        UUID behandlingId = behandlingIdDto.getBehandlingUuid();
        return mapUttak.mapFastsattUttak(behandlingId);
    }

}
