package no.nav.k9.sak.web.app.tjenester.behandling.tilsyn;

import static no.nav.k9.abac.BeskyttetRessursKoder.FAGSAK;
import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;

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
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessurs;
import no.nav.k9.felles.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.k9.kodeverk.behandling.BehandlingStatus;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.kontrakt.behandling.BehandlingUuidDto;
import no.nav.k9.sak.kontrakt.tilsyn.EtablertTilsynNattevåkOgBeredskapDto;
import no.nav.k9.sak.web.server.abac.AbacAttributtSupplier;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.unntaketablerttilsyn.UnntakEtablertTilsynForPleietrengende;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.unntaketablerttilsyn.UnntakEtablertTilsynGrunnlagRepository;

@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
@Path("")
@Transactional
public class VurderTilsynRestTjeneste {

    public static final String BASEPATH = "/behandling/tilsyn";

    private UnntakEtablertTilsynGrunnlagRepository unntakEtablertTilsynGrunnlagRepository;
    private BehandlingRepository behandlingRepository;
    private EtablertTilsynNattevåkOgBeredskapMapper etablertTilsynNattevåkOgBeredskapMapper;

    VurderTilsynRestTjeneste() {
        // for CDI proxy
    }

    @Inject
    public VurderTilsynRestTjeneste(UnntakEtablertTilsynGrunnlagRepository unntakEtablertTilsynGrunnlagRepository,
                                    BehandlingRepository behandlingRepository,
                                    EtablertTilsynNattevåkOgBeredskapMapper etablertTilsynNattevåkOgBeredskapMapper) {
       this.unntakEtablertTilsynGrunnlagRepository = unntakEtablertTilsynGrunnlagRepository;
       this.behandlingRepository = behandlingRepository;
       this.etablertTilsynNattevåkOgBeredskapMapper = etablertTilsynNattevåkOgBeredskapMapper;
    }

    @GET
    @Operation(description = "Hent etablert tilsyn perioder",
        summary = "Returnerer alle perioder med etablert tilsyn",
        tags="tilsyn",
        responses = {
            @ApiResponse(responseCode = "200",
                description = "perioder med etablert tilsyn, nattevåk og beredskap",
                content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = EtablertTilsynNattevåkOgBeredskapDto.class)))
        })
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    @Path((BASEPATH))
    public EtablertTilsynNattevåkOgBeredskapDto hentEtablertTilsyn(@NotNull @QueryParam(BehandlingUuidDto.NAME)
                                                @Parameter(description = BehandlingUuidDto.DESC)
                                                @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class)
                                                    BehandlingUuidDto behandlingUuidDto) {
        var behandling = behandlingRepository.hentBehandling(behandlingUuidDto.getBehandlingUuid());
        
        final UnntakEtablertTilsynForPleietrengende unntakEtablertTilsynForPleietrengende;
        if (behandling.getStatus().erFerdigbehandletStatus() || behandling.getStatus().equals(BehandlingStatus.FATTER_VEDTAK)) {
            unntakEtablertTilsynForPleietrengende = unntakEtablertTilsynGrunnlagRepository.hentHvisEksisterer(behandling.getId()).map(g -> g.getUnntakEtablertTilsynForPleietrengende()).orElseThrow();
        } else {
            unntakEtablertTilsynForPleietrengende = unntakEtablertTilsynGrunnlagRepository.hentHvisEksistererUnntakPleietrengende(behandling.getFagsak().getPleietrengendeAktørId()).orElseThrow();
        }

        var behandlingRef = BehandlingReferanse.fra(behandling);
        return etablertTilsynNattevåkOgBeredskapMapper.tilDto(behandlingRef, unntakEtablertTilsynForPleietrengende);
    }

}
