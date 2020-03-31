package no.nav.k9.sak.web.app.tjenester.behandling.uttak;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import no.nav.k9.sak.domene.uttak.UttakTjeneste;
import no.nav.k9.sak.kontrakt.behandling.BehandlingUuidDto;
import no.nav.k9.sak.kontrakt.behandling.SaksnummerDto;
import no.nav.k9.sak.kontrakt.uttak.FastsattUttakDto;
import no.nav.k9.sak.kontrakt.uttak.OppgittUttakDto;
import no.nav.k9.sak.kontrakt.uttak.UttaksplanDto;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.k9.sak.web.server.abac.AbacAttributtSupplier;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.TilpassetAbacAttributt;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static no.nav.k9.abac.BeskyttetRessursKoder.FAGSAK;
import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;

@ApplicationScoped
@Transactional
@Path("")
@Produces(MediaType.APPLICATION_JSON)
public class UttakRestTjeneste {

    static final String BASE_PATH = "/behandling/uttak";

    public static final String UTTAKSPLANER = BASE_PATH + "/uttaksplaner";
    public static final String UTTAK_OPPGITT = BASE_PATH + "/oppgitt";
    public static final String UTTAK_FASTSATT = BASE_PATH + "/fastsatt";

    private UttakTjeneste uttakTjeneste;


    private MapUttak mapUttak;

    public UttakRestTjeneste() {
        // for proxying
    }

    @Inject
    public UttakRestTjeneste(UttakTjeneste uttakTjeneste, MapUttak mapOppgittUttak) {
        this.uttakTjeneste = uttakTjeneste;
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

    /**
     * Hent uttaksplan for angitt behandling og andre parters relevante saker. Sender inn saksnummer så de vil avsjekkes mot Abac enkelt istdf.
     * å slå opp her.
     */
    @GET
    @Path(UTTAKSPLANER)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Hent uttaksplaner", tags = "behandling - uttak", responses = {
            @ApiResponse(responseCode = "200", description = "Returnerer Uttaksplaner, tom liste hvis ikke finnes noe. Dette er faktisk uttaksplan fastsatt og vurdert.", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = UttaksplanDto.class)))
    })
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public UttaksplanDto getUttaksplaner(@NotNull @QueryParam(BehandlingUuidDto.NAME) @Parameter(description = BehandlingUuidDto.DESC)  @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingUuidDto behandlingIdDto,
                                         @QueryParam("saksnummer") @Parameter(description = "Saksnummer for tilknyttede saker") @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) List<SaksnummerDto> andreParterSaker) {
        UUID behandlingId = behandlingIdDto.getBehandlingUuid();
        List<Saksnummer> andrePartersSaknummer = andreParterSaker == null ? Collections.emptyList() : andreParterSaker.stream().map(SaksnummerDto::getVerdi).collect(Collectors.toList());

        String uttaksplanBehandling = uttakTjeneste.hentUttaksplanerRaw(behandlingId);
        String andreParterUttaksplaner = uttakTjeneste.hentUttaksplanerRaw(andrePartersSaknummer);

        return new UttaksplanDto(uttaksplanBehandling, andreParterUttaksplaner);
    }

}
