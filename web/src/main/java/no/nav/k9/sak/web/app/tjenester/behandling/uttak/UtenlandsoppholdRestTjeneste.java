package no.nav.k9.sak.web.app.tjenester.behandling.uttak;

import static no.nav.k9.abac.BeskyttetRessursKoder.FAGSAK;
import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;

import java.util.Optional;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessurs;
import no.nav.k9.felles.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.kontrakt.behandling.BehandlingUuidDto;
import no.nav.k9.sak.kontrakt.uttak.FastsattUttakDto;
import no.nav.k9.sak.kontrakt.uttak.UtenlandsoppholdDto;
import no.nav.k9.sak.web.server.abac.AbacAttributtSupplier;
import no.nav.k9.sak.ytelse.pleiepengerbarn.inngangsvilkår.søknadsfrist.PSBVurdererSøknadsfristTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.PeriodeFraSøknadForBrukerTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.UttakPerioderGrunnlagRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.UttaksPerioderGrunnlag;
import no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.input.utenlandsopphold.MapUtenlandsopphold;

@ApplicationScoped
@Transactional
@Path("")
@Produces(MediaType.APPLICATION_JSON)
public class UtenlandsoppholdRestTjeneste {
    static final String BASE_PATH = "/behandling/uttak";

    public static final String UTTAK_UTENLANDSOPPHOLD = BASE_PATH + "/utenlandsopphold";

    private UttakPerioderGrunnlagRepository uttakPerioderGrunnlagRepository;
    private BehandlingRepository behandlingRepository;

    private PSBVurdererSøknadsfristTjeneste søknadsfristTjeneste;
    private PeriodeFraSøknadForBrukerTjeneste periodeFraSøknadForBrukerTjeneste;

    public UtenlandsoppholdRestTjeneste() {
    }

    @Inject
    public UtenlandsoppholdRestTjeneste(
            UttakPerioderGrunnlagRepository uttakPerioderGrunnlagRepository,
            BehandlingRepository behandlingRepository,
            PSBVurdererSøknadsfristTjeneste søknadsfristTjeneste,
            PeriodeFraSøknadForBrukerTjeneste periodeFraSøknadForBrukerTjeneste) {
        this.uttakPerioderGrunnlagRepository = uttakPerioderGrunnlagRepository;
        this.behandlingRepository = behandlingRepository;
        this.søknadsfristTjeneste = søknadsfristTjeneste;
        this.periodeFraSøknadForBrukerTjeneste = periodeFraSøknadForBrukerTjeneste;
    }

    @GET
    @Path(UTTAK_UTENLANDSOPPHOLD)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Hent oppgitt utenlandsopphold", tags = "behandling - uttak",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Returnerer søkers oppgitte utenlandsopphold, tom liste hvis det ikke finnes noe",
                content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = FastsattUttakDto.class)))
    })
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    public UtenlandsoppholdDto getUtenlandsopphold(
        @NotNull @QueryParam(BehandlingUuidDto.NAME)
        @Parameter(description = BehandlingUuidDto.DESC) @Valid
        @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class)
            BehandlingUuidDto behandlingIdDto) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingIdDto.getBehandlingUuid());
        Optional<UttaksPerioderGrunnlag> uttaksPerioderGrunnlag = uttakPerioderGrunnlagRepository.hentGrunnlag(behandling.getId());
        var behandlingReferanse = BehandlingReferanse.fra(behandling);
        var vurderteSøknadsperioder = søknadsfristTjeneste.vurderSøknadsfrist(behandlingReferanse);
        var perioderFraSøknad = periodeFraSøknadForBrukerTjeneste.hentPerioderFraSøknad(behandlingReferanse);
        MapUtenlandsopphold.map(vurderteSøknadsperioder, perioderFraSøknad);

        uttaksPerioderGrunnlag.get().getOppgitteSøknadsperioder()
            .getPerioderFraSøknadene()
            .stream()
            .map(p -> p.getUtenlandsopphold())
            .map(p -> p.getUtenlandsopphold()
                .stream()
                .map(u -> new UtenlandsoppholdDto()))
    }
}
