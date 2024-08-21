package no.nav.k9.sak.web.app.tjenester.behandling.uttak;

import static no.nav.k9.abac.BeskyttetRessursKoder.FAGSAK;
import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
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
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessurs;
import no.nav.k9.felles.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.kontrakt.behandling.BehandlingUuidDto;
import no.nav.k9.sak.kontrakt.uttak.UtenlandsoppholdDto;
import no.nav.k9.sak.web.server.abac.AbacAttributtSupplier;
import no.nav.k9.sak.ytelse.pleiepengerbarn.inngangsvilkår.søknadsfrist.PSBVurdererSøknadsfristTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.PeriodeFraSøknadForBrukerTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.UtenlandsoppholdTidslinjeTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.delt.UtledetUtenlandsopphold;

@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
@Path("")
@Transactional
public class UtenlandsoppholdRestTjeneste {
    static final String BASE_PATH = "/behandling/uttak";

    public static final String UTTAK_UTENLANDSOPPHOLD = BASE_PATH + "/utenlandsopphold";

    private BehandlingRepository behandlingRepository;

    private PSBVurdererSøknadsfristTjeneste søknadsfristTjeneste;
    private PeriodeFraSøknadForBrukerTjeneste periodeFraSøknadForBrukerTjeneste;

    private boolean nyUtledningAvUtenlandsopphold;

    public UtenlandsoppholdRestTjeneste() {
    }

    @Inject
    public UtenlandsoppholdRestTjeneste(
            BehandlingRepository behandlingRepository,
            @Any PSBVurdererSøknadsfristTjeneste søknadsfristTjeneste,
            PeriodeFraSøknadForBrukerTjeneste periodeFraSøknadForBrukerTjeneste,
            @KonfigVerdi(value = "ENABLE_NY_UTLEDNING_AV_UTENLANDSOPPHOLD", defaultVerdi = "false") boolean nyUtledningAvUtenlandsopphold) {
        this.behandlingRepository = behandlingRepository;
        this.søknadsfristTjeneste = søknadsfristTjeneste;
        this.periodeFraSøknadForBrukerTjeneste = periodeFraSøknadForBrukerTjeneste;
        this.nyUtledningAvUtenlandsopphold = nyUtledningAvUtenlandsopphold;
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
                    schema = @Schema(implementation = UtenlandsoppholdDto.class)))
    })
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    public UtenlandsoppholdDto getUtenlandsopphold(
        @NotNull @QueryParam(BehandlingUuidDto.NAME)
        @Parameter(description = BehandlingUuidDto.DESC) @Valid
        @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class)
            BehandlingUuidDto behandlingIdDto) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingIdDto.getBehandlingUuid());
        var behandlingReferanse = BehandlingReferanse.fra(behandling);
        var vurderteSøknadsperioder = søknadsfristTjeneste.vurderSøknadsfrist(behandlingReferanse);
        var perioderFraSøknad = periodeFraSøknadForBrukerTjeneste.hentPerioderFraSøknad(behandlingReferanse);

        LocalDateTimeline<UtledetUtenlandsopphold> utenlandsoppholdTidslinje = UtenlandsoppholdTidslinjeTjeneste.byggTidslinje(vurderteSøknadsperioder, perioderFraSøknad, nyUtledningAvUtenlandsopphold);
        UtenlandsoppholdDto dto = new UtenlandsoppholdDto();

        for (LocalDateSegment<UtledetUtenlandsopphold> s : utenlandsoppholdTidslinje.toSegments()) {
            dto.leggTil(
                s.getFom(),
                s.getTom(),
                s.getValue().getLandkode(),
                s.getValue().getÅrsak());
        }

        return dto;
    }

}
