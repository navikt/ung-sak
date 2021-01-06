package no.nav.k9.sak.web.app.tjenester.behandling.sykdom;

import static no.nav.k9.abac.BeskyttetRessursKoder.FAGSAK;
import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;

import java.util.Collection;
import java.util.function.Function;

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
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.kontrakt.behandling.BehandlingUuidDto;
import no.nav.k9.sak.web.server.abac.AbacAttributtSupplier;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomVurderingType;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomVurderingVersjon;
import no.nav.vedtak.sikkerhet.abac.AbacDataAttributter;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomVurderingRepository;

@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
@Path(SykdomVurderingRestTjeneste.BASE_PATH)
@Transactional
public class SykdomVurderingRestTjeneste {

    public static final String BASE_PATH = "/behandling/sykdom/vurdering";
    public static final String VURDERING = "/";
    public static final String VURDERING_PATH = BASE_PATH + VURDERING;
    private static final String VURDERING_OVERSIKT_KTP = "/oversikt/KONTINUERLIG_TILSYN_OG_PLEIE";
    private static final String VURDERING_OVERSIKT_TOO = "/oversikt/KONTINUERLIG_TILSYN_OG_PLEIE";
    public static final String VURDERING_OVERSIKT_KTP_PATH = BASE_PATH + VURDERING_OVERSIKT_KTP;
    public static final String VURDERING_OVERSIKT_TOO_PATH = BASE_PATH + VURDERING_OVERSIKT_TOO;

    private BehandlingRepository behandlingRepository;
    private SykdomVurderingOversiktMapper sykdomVurderingOversiktMapper;
    private SykdomVurderingMapper sykdomVurderingMapper;
    private SykdomVurderingRepository sykdomVurderingRepository;
    

    public SykdomVurderingRestTjeneste() {
    }
    
    /*
     * private Instance<VilkårsPerioderTilVurderingTjeneste> vilkårsPerioderTilVurderingTjenester;
    @Any Instance<VilkårsPerioderTilVurderingTjeneste> vilkårsPerioderTilVurderingTjenester) {
        */

    @Inject
    public SykdomVurderingRestTjeneste(BehandlingRepository behandlingRepository, SykdomVurderingOversiktMapper sykdomVurderingOversiktMapper, SykdomVurderingMapper sykdomVurderingMapper, SykdomVurderingRepository sykdomVurderingRepository) {
        this.behandlingRepository = behandlingRepository;
        this.sykdomVurderingOversiktMapper = sykdomVurderingOversiktMapper;
        this.sykdomVurderingMapper = sykdomVurderingMapper;
        this.sykdomVurderingRepository = sykdomVurderingRepository;
    }

    @GET
    @Path(VURDERING_OVERSIKT_KTP)
    @Operation(description = "En oversikt over sykdomsvurderinger for kontinuerlig tilsyn og pleie",
        summary = "En oversikt over sykdomsvurderinger for kontinuerlig tilsyn og pleie",
        tags = "sykdom",
        responses = {
            @ApiResponse(responseCode = "200",
                description = "",
                content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = SykdomVurderingOversikt.class)))
        })
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    public SykdomVurderingOversikt hentSykdomsoversiktForKtp(
            @NotNull @QueryParam(BehandlingUuidDto.NAME)
            @Parameter(description = BehandlingUuidDto.DESC)
            @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class)
            BehandlingUuidDto behandlingUuid) {
        
        return hentSykdomsoversikt(behandlingUuid, SykdomVurderingType.KONTINUERLIG_TILSYN_OG_PLEIE);
    }
    
    @GET
    @Path(VURDERING_OVERSIKT_TOO)
    @Operation(description = "En oversikt over sykdomsvurderinger for to omsorgspersoner",
        summary = "En oversikt over sykdomsvurderinger for to omsorgspersoner",
        tags = "sykdom",
        responses = {
            @ApiResponse(responseCode = "200",
                description = "",
                content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = SykdomVurderingOversikt.class)))
        })
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    public SykdomVurderingOversikt hentSykdomsoversiktForToOmsorgspersoner(
            @NotNull @QueryParam(BehandlingUuidDto.NAME)
            @Parameter(description = BehandlingUuidDto.DESC)
            @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class)
            BehandlingUuidDto behandlingUuid) {
        
        return hentSykdomsoversikt(behandlingUuid, SykdomVurderingType.TO_OMSORGSPERSONER);
    }
    
    private SykdomVurderingOversikt hentSykdomsoversikt(BehandlingUuidDto behandlingUuid, SykdomVurderingType sykdomVurderingType) {
        final var behandling = behandlingRepository.hentBehandlingHvisFinnes(behandlingUuid.getBehandlingUuid()).orElseThrow();

        final Collection<SykdomVurderingVersjon> vurderinger = hentVurderinger(sykdomVurderingType, behandling);
        return sykdomVurderingOversiktMapper.map(behandling.getUuid().toString(), vurderinger);
    }

    private Collection<SykdomVurderingVersjon> hentVurderinger(SykdomVurderingType sykdomVurderingType, final Behandling behandling) {
        final Collection<SykdomVurderingVersjon> vurderinger;
        if (behandling.getStatus().erFerdigbehandletStatus()) {
            vurderinger = sykdomVurderingRepository.hentVurderingerFor(sykdomVurderingType, behandling.getUuid(), behandling.getFagsak().getPleietrengendeAktørId());
        } else {
            vurderinger = sykdomVurderingRepository.hentVurderingerFor(sykdomVurderingType, null, behandling.getFagsak().getPleietrengendeAktørId());
        }
        return vurderinger;
    }
    
    @GET
    @Path(VURDERING)
    @Operation(description = "Henter informasjon om én gitt vurdering.",
        summary = "En gitt vurdering angitt med ID.",
        tags = "sykdom",
        responses = {
            @ApiResponse(responseCode = "200",
                description = "",
                content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = SykdomVurderingDto.class)))
        })
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    public SykdomVurderingDto hentSykdomsInformasjonFor(
            @QueryParam(BehandlingUuidDto.NAME)
            @Parameter(description = BehandlingUuidDto.DESC)
            @NotNull
            @Valid
            @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class)
            BehandlingUuidDto behandlingUuid,

            @QueryParam(SykdomVurderingIdDto.NAME)
            @Parameter(description = SykdomVurderingIdDto.DESC)
            @NotNull
            @Valid 
            @TilpassetAbacAttributt(supplierClass = AbacDataSupplier.class)
            SykdomVurderingIdDto vurderingId) {
        final var behandling = behandlingRepository.hentBehandlingHvisFinnes(behandlingUuid.getBehandlingUuid()).orElseThrow();
        
        return sykdomVurderingMapper.map(behandling, vurderingId.getSykdomVurderingId());
    }
    
    /*
    private VilkårsPerioderTilVurderingTjeneste getPerioderTilVurderingTjeneste(Behandling behandling) {
        return BehandlingTypeRef.Lookup.find(VilkårsPerioderTilVurderingTjeneste.class, vilkårsPerioderTilVurderingTjenester, behandling.getFagsakYtelseType(), behandling.getType())
            .orElseThrow(() -> new UnsupportedOperationException("VilkårsPerioderTilVurderingTjeneste ikke implementert for ytelse [" + behandling.getFagsakYtelseType() + "], behandlingtype [" + behandling.getType() + "]"));
    }
    */
    
    public static class AbacDataSupplier implements Function<Object, AbacDataAttributter> {
        @Override
        public AbacDataAttributter apply(Object obj) {
            return AbacDataAttributter.opprett();
        }
    }
}
