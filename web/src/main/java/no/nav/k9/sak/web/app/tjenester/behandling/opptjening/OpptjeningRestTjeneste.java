package no.nav.k9.sak.web.app.tjenester.behandling.opptjening;

import static no.nav.k9.abac.BeskyttetRessursKoder.FAGSAK;
import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.kontrakt.behandling.BehandlingUuidDto;
import no.nav.k9.sak.kontrakt.opptjening.InntekterDto;
import no.nav.k9.sak.kontrakt.opptjening.OpptjeningDto;
import no.nav.k9.sak.kontrakt.opptjening.OpptjeningerDto;
import no.nav.k9.sak.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.k9.sak.web.server.abac.AbacAttributtSupplier;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.TilpassetAbacAttributt;

@ApplicationScoped
@Path("")
@Produces(MediaType.APPLICATION_JSON)
@Transactional
public class OpptjeningRestTjeneste {

    public static final String PATH_V2 = "/behandling/opptjening-v2";
    public static final String INNTEKT_PATH = "/behandling/opptjening/inntekt";

    private BehandlingRepository behandlingRepository;
    private MapOpptjening dtoMapper;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private MapInntekter mapInntekter;

    public OpptjeningRestTjeneste() {
        // for CDI proxy
    }

    @Inject
    public OpptjeningRestTjeneste(BehandlingRepository behandlingRepository,
                                  SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                                  MapInntekter mapInntekter,
                                  MapOpptjening dtoMapper) {
        this.behandlingRepository = behandlingRepository;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.mapInntekter = mapInntekter;
        this.dtoMapper = dtoMapper;
    }

    @GET
    @Path(INNTEKT_PATH)
    @Operation(description = "Hent informasjon om inntekt", tags = "opptjening", responses = {
            @ApiResponse(responseCode = "200", description = "Returnerer inntekter, null hvis ikke eksisterer (GUI støtter ikke NOT_FOUND p.t.)", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = OpptjeningDto.class)))
    })
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public InntekterDto getInntekt(@NotNull @QueryParam(BehandlingUuidDto.NAME) @Parameter(description = BehandlingUuidDto.DESC) @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingUuidDto behandlingUuid) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingUuid.getBehandlingUuid());
        Long behandlingId = behandling.getId();
        var ref = BehandlingReferanse.fra(behandling, skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandlingId));
        return mapInntekter.hentPgiInntekterFørStp(ref);
    }

    @GET
    @Path(PATH_V2)
    @Operation(description = "Hent informasjon om opptjening", tags = "opptjening", responses = {
            @ApiResponse(responseCode = "200", description = "Returnerer Opptjening, null hvis ikke eksisterer (GUI støtter ikke NOT_FOUND p.t.)", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = OpptjeningerDto.class)))
    })
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public OpptjeningerDto getOpptjeninger(@NotNull @QueryParam(BehandlingUuidDto.NAME) @Parameter(description = BehandlingUuidDto.DESC) @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingUuidDto behandlingUuid) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingUuid.getBehandlingUuid());
        return getOpptjeningerFraBehandling(behandling);
    }

    private OpptjeningerDto getOpptjeningerFraBehandling(Behandling behandling) {
        var skjæringstidspunkt = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandling.getId());
        var ref = BehandlingReferanse.fra(behandling, skjæringstidspunkt);
        return dtoMapper.mapTilOpptjeninger(ref);
    }
}
