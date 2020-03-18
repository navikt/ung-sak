package no.nav.k9.sak.web.app.tjenester.behandling.uttak;

import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;
import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursResourceAttributt.FAGSAK;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import no.nav.k9.sak.domene.uttak.UttakTjeneste;
import no.nav.k9.sak.kontrakt.behandling.BehandlingIdDto;
import no.nav.k9.sak.kontrakt.behandling.SaksnummerDto;
import no.nav.k9.sak.kontrakt.uttak.FastsattUttakDto;
import no.nav.k9.sak.kontrakt.uttak.OppgittUttakDto;
import no.nav.k9.sak.kontrakt.uttak.UttaksplanDto;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.k9.sak.web.server.abac.AbacAttributtSupplier;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.TilpassetAbacAttributt;

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

    public UttakRestTjeneste() {
        // for proxying
    }

    @Inject
    public UttakRestTjeneste(UttakTjeneste uttakTjeneste) {
        this.uttakTjeneste = uttakTjeneste;
    }

    /**
     * Hent oppgitt uttak for angitt behandling.
     */
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Path(UTTAK_OPPGITT)
    @Operation(description = "Hent oppgitt uttak for behandling", tags = "behandling - uttak", responses = {
            @ApiResponse(responseCode = "200", description = "Returnerer Oppgitt uttak fra søknad, null hvis ikke finnes noe", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = UttaksplanDto.class)))
    })
    @BeskyttetRessurs(action = READ, ressurs = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public OppgittUttakDto getOppgittUttak(@NotNull @Parameter(description = "BehandlingId for aktuell behandling") @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingIdDto behandlingIdDto) {
        UUID behandlingId = behandlingIdDto.getBehandlingUuid();

        return new OppgittUttakDto(behandlingId);
    }

    /**
     * Hent fastsatt uttak for angitt behandling.
     */
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Path(UTTAK_FASTSATT)
    @Operation(description = "Hent Fastsatt uttak for behandling", tags = "behandling - uttak", responses = {
            @ApiResponse(responseCode = "200", description = "Returnerer Fastsatt uttak fra søknad, null hvis ikke finnes noe", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = UttaksplanDto.class)))
    })
    @BeskyttetRessurs(action = READ, ressurs = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public FastsattUttakDto getFastsattUttak(@NotNull @Parameter(description = "BehandlingId for aktuell behandling") @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingIdDto behandlingIdDto) {
        UUID behandlingId = behandlingIdDto.getBehandlingUuid();

        return new FastsattUttakDto(behandlingId);
    }

    /**
     * Hent uttaksplan for angitt behandling og andre parters relevante saker. Sender inn saksnummer så de vil avsjekkes mot Abac enkelt istdf.
     * å slå opp her.
     */
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Path(UTTAKSPLANER)
    @Operation(description = "Hent uttaksplaner", tags = "behandling - uttak", responses = {
            @ApiResponse(responseCode = "200", description = "Returnerer Uttaksplaner, tom liste hvis ikke finnes noe", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = UttaksplanDto.class)))
    })
    @BeskyttetRessurs(action = READ, ressurs = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public UttaksplanDto getUttaksplaner(@NotNull @Parameter(description = "BehandlingId for aktuell behandling") @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingIdDto behandlingIdDto,
                                         @Parameter(description = "Saksnummer for tilknyttede saker") @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) List<SaksnummerDto> andreParterSaker) {
        UUID behandlingId = behandlingIdDto.getBehandlingUuid();
        List<Saksnummer> andrePartersSaknummer = andreParterSaker == null ? Collections.emptyList() : andreParterSaker.stream().map(SaksnummerDto::getVerdi).collect(Collectors.toList());

        String uttaksplanBehandling = uttakTjeneste.hentUttaksplanerRaw(behandlingId);
        String andreParterUttaksplaner = uttakTjeneste.hentUttaksplanerRaw(andrePartersSaknummer);

        return new UttaksplanDto(uttaksplanBehandling, andreParterUttaksplaner);
    }

}
