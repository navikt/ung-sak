package no.nav.k9.sak.web.app.tjenester.behandling.opplæringspenger.institusjon;

import static no.nav.k9.abac.BeskyttetRessursKoder.FAGSAK;
import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;

import java.util.Set;

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
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessurs;
import no.nav.k9.felles.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.kontrakt.behandling.BehandlingUuidDto;
import no.nav.k9.sak.web.server.abac.AbacAttributtSupplier;
import no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.institusjon.GodkjentOpplæringsinstitusjonTjeneste;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertOpplæringRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.UttakPerioderGrunnlagRepository;

@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
@Path("")
@Transactional
public class VurderInstitusjonRestTjeneste {

    public static final String BASEPATH = "/behandling/institusjon";

    private BehandlingRepository behandlingRepository;
    private VurdertOpplæringRepository vurdertOpplæringRepository;
    private VurderInstitusjonMapper mapper;
    private UttakPerioderGrunnlagRepository uttakPerioderGrunnlagRepository;

    VurderInstitusjonRestTjeneste() {
    }

    @Inject
    public VurderInstitusjonRestTjeneste(BehandlingRepository behandlingRepository,
                                         VurdertOpplæringRepository vurdertOpplæringRepository,
                                         GodkjentOpplæringsinstitusjonTjeneste godkjentOpplæringsinstitusjonTjeneste,
                                         UttakPerioderGrunnlagRepository uttakPerioderGrunnlagRepository) {
        this.behandlingRepository = behandlingRepository;
        this.vurdertOpplæringRepository = vurdertOpplæringRepository;
        this.mapper = new VurderInstitusjonMapper(godkjentOpplæringsinstitusjonTjeneste);
        this.uttakPerioderGrunnlagRepository = uttakPerioderGrunnlagRepository;
    }

    @GET
    @Operation(description = "Hent institusjonsvurderinger",
        summary = "Returnerer alle perioder og tilhørende institusjonsvurderinger",
        tags = "opplæringspenger",
        responses = {
            @ApiResponse(responseCode = "200",
                description = "perioder fra søknad og vurderte institusjoner",
                content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = InstitusjonerDto.class)))
        })
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    @Path((BASEPATH))
    public Response hentVurdertInstitusjon(@NotNull @QueryParam(BehandlingUuidDto.NAME)
                                           @Parameter(description = BehandlingUuidDto.DESC)
                                           @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class)
                                           BehandlingUuidDto behandlingUuidDto) {
        var behandling = behandlingRepository.hentBehandling(behandlingUuidDto.getBehandlingUuid());
        var referanse = BehandlingReferanse.fra(behandling);

        var grunnlag = vurdertOpplæringRepository.hentAktivtGrunnlagForBehandling(referanse.getBehandlingId());

        var perioderFraSøknad = uttakPerioderGrunnlagRepository.hentGrunnlag(referanse.getBehandlingId())
            .map(uttaksPerioderGrunnlag -> uttaksPerioderGrunnlag.getRelevantSøknadsperioder().getPerioderFraSøknadene())
            .orElse(Set.of());

        return Response.ok()
            .entity(mapper.mapTilDto(grunnlag.orElse(null), perioderFraSøknad))
            .build();
    }
}
