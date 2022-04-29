package no.nav.k9.sak.web.app.tjenester.behandling.aksjonspunkt;

import static no.nav.k9.abac.BeskyttetRessursKoder.FAGSAK;
import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;
import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt.UPDATE;

import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Set;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.CacheControl;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessurs;
import no.nav.k9.felles.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.k9.kodeverk.behandling.BehandlingStatus;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.kontrakt.aksjonspunkt.AksjonspunktDto;
import no.nav.k9.sak.kontrakt.aksjonspunkt.AksjonspunktKode;
import no.nav.k9.sak.kontrakt.aksjonspunkt.BekreftedeAksjonspunkterDto;
import no.nav.k9.sak.kontrakt.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.k9.sak.kontrakt.aksjonspunkt.BekreftetOgOverstyrteAksjonspunkterDto;
import no.nav.k9.sak.kontrakt.behandling.BehandlingIdDto;
import no.nav.k9.sak.kontrakt.behandling.BehandlingUuidDto;
import no.nav.k9.sak.produksjonsstyring.totrinn.TotrinnTjeneste;
import no.nav.k9.sak.produksjonsstyring.totrinn.Totrinnsvurdering;
import no.nav.k9.sak.web.app.rest.Redirect;
import no.nav.k9.sak.web.server.abac.AbacAttributtSupplier;

@ApplicationScoped
@Transactional
@Path("")
@Produces(MediaType.APPLICATION_JSON)
public class AksjonspunktRestTjeneste {

    public static final String AKSJONSPUNKT_OVERSTYR_PATH = "/behandling/aksjonspunkt/overstyr";
    public static final String AKSJONSPUNKT_PATH = "/behandling/aksjonspunkt";
    public static final String AKSJONSPUNKT_V2_PATH = "/behandling/aksjonspunkt-v2";
    public static final String AKSJONSPUNKT_RISIKO_PATH = "/behandling/aksjonspunkt/risiko";
    public static final String AKSJONSPUNKT_KONTROLLER_REVURDERING_PATH = "/behandling/aksjonspunkt/kontroller-revurdering";
    private AksjonspunktApplikasjonTjeneste applikasjonstjeneste;
    private BehandlingRepository behandlingRepository;
    private BehandlingsutredningApplikasjonTjeneste behandlingutredningTjeneste;
    private TotrinnTjeneste totrinnTjeneste;

    public AksjonspunktRestTjeneste() {
        // Bare for RESTeasy
    }

    @Inject
    public AksjonspunktRestTjeneste(AksjonspunktApplikasjonTjeneste aksjonpunktApplikasjonTjeneste,
                                    BehandlingRepository behandlingRepository,
                                    BehandlingsutredningApplikasjonTjeneste behandlingutredningTjeneste,
                                    TotrinnTjeneste totrinnTjeneste) {

        this.applikasjonstjeneste = aksjonpunktApplikasjonTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.behandlingutredningTjeneste = behandlingutredningTjeneste;
        this.totrinnTjeneste = totrinnTjeneste;
    }

    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Path(AKSJONSPUNKT_PATH)
    @Operation(description = "Hent aksjonspunter for en behandling", tags = "aksjonspunkt", responses = {
        @ApiResponse(responseCode = "200", content = @Content(array = @ArraySchema(uniqueItems = true, arraySchema = @Schema(implementation = Set.class), schema = @Schema(implementation = AksjonspunktDto.class)), mediaType = MediaType.APPLICATION_JSON))
    })
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response getAksjonspunkter(@NotNull @QueryParam("behandlingId") @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingIdDto behandlingIdDto) { // NOSONAR
        Long behandlingId = behandlingIdDto.getBehandlingId();
        Behandling behandling = behandlingId != null
            ? behandlingRepository.hentBehandling(behandlingId)
            : behandlingRepository.hentBehandling(behandlingIdDto.getBehandlingUuid());
        return getAksjonspunkter(behandling);
    }

    private Response getAksjonspunkter(Behandling behandling) {
        Collection<Totrinnsvurdering> ttVurderinger = totrinnTjeneste.hentTotrinnaksjonspunktvurderinger(behandling);
        Set<AksjonspunktDto> dto = AksjonspunktDtoMapper.lagAksjonspunktDto(behandling, ttVurderinger);
        CacheControl cc = new CacheControl();
        cc.setNoCache(true);
        cc.setNoStore(true);
        cc.setMaxAge(0);
        return Response.ok(dto).cacheControl(cc).build();
    }

    @GET
    @Path(AKSJONSPUNKT_V2_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Hent aksjonspunter for en behandling", tags = "aksjonspunkt", responses = {
        @ApiResponse(responseCode = "200", content = @Content(array = @ArraySchema(uniqueItems = true, arraySchema = @Schema(implementation = Set.class), schema = @Schema(implementation = AksjonspunktDto.class)), mediaType = MediaType.APPLICATION_JSON))
    })
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response getAksjonspunkter(@NotNull @QueryParam(BehandlingUuidDto.NAME) @Parameter(description = BehandlingUuidDto.DESC) @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingUuidDto behandlingUuid) {
        var behandling = behandlingRepository.hentBehandling(behandlingUuid.getBehandlingUuid());
        return getAksjonspunkter(behandling);
    }

    /**
     * @deprecated pt. ikke i bruk i K9
     */
    @Deprecated(forRemoval = true)
    @GET
    @Path(AKSJONSPUNKT_RISIKO_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Hent risikoaksjonspunkt for en behandling", tags = "aksjonspunkt", responses = {
        @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = AksjonspunktDto.class), mediaType = MediaType.APPLICATION_JSON))
    })
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response getRisikoAksjonspunkt(@NotNull @QueryParam(BehandlingUuidDto.NAME) @Parameter(description = BehandlingUuidDto.DESC) @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingUuidDto behandlingUuid) {
        CacheControl cc = new CacheControl();
        cc.setNoCache(true);
        cc.setNoStore(true);
        cc.setMaxAge(0);
        return Response.ok().cacheControl(cc).build();
    }

    @GET
    @Path(AKSJONSPUNKT_KONTROLLER_REVURDERING_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Har behandling åpent kontroller revurdering aksjonspunkt", tags = "aksjonspunkt", responses = {
        @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = Boolean.class), mediaType = MediaType.APPLICATION_JSON))
    })
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response erKontrollerRevurderingAksjonspunktÅpent(@NotNull @QueryParam(BehandlingUuidDto.NAME) @Parameter(description = BehandlingUuidDto.DESC) @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingUuidDto behandlingUuid) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingUuid.getBehandlingUuid());
        boolean harÅpentAksjonspunkt = behandling.harÅpentAksjonspunktMedType(AksjonspunktDefinisjon.KONTROLLER_REVURDERINGSBEHANDLING_VARSEL_VED_UGUNST);
        CacheControl cc = new CacheControl();
        cc.setNoCache(true);
        cc.setNoStore(true);
        cc.setMaxAge(0);
        return Response.ok(harÅpentAksjonspunkt).cacheControl(cc).build();
    }

    /**
     * Håndterer prosessering av aksjonspunkt og videre behandling.
     * <p>
     * MERK: Det skal ikke ligge spesifikke sjekker som avhenger av status på behanlding, steg eller knytning til
     * spesifikke aksjonspunkter idenne tjenesten.
     *
     * @throws URISyntaxException
     */
    @POST
    @Path(AKSJONSPUNKT_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Lagre endringer gitt av aksjonspunktene og rekjør behandling fra gjeldende steg", tags = "aksjonspunkt")
    @BeskyttetRessurs(action = UPDATE, resource = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response bekreft(@Context HttpServletRequest request,
                            @Parameter(description = "Liste over aksjonspunkt som skal bekreftes, inklusiv data som trengs for å løse de.") @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BekreftedeAksjonspunkterDto apDto)
        throws URISyntaxException { // NOSONAR

        Long behandlingId = apDto.getBehandlingId();
        Collection<BekreftetAksjonspunktDto> bekreftedeAksjonspunktDtoer = apDto.getBekreftedeAksjonspunktDtoer();

        Behandling behandling = behandlingId != null
            ? behandlingRepository.hentBehandling(behandlingId)
            : behandlingRepository.hentBehandling(apDto.getBehandlingUuid());

        behandlingutredningTjeneste.kanEndreBehandling(behandling.getId(), apDto.getBehandlingVersjon());

        validerBetingelserForAksjonspunkt(behandling, apDto.getBekreftedeAksjonspunktDtoer());

        applikasjonstjeneste.bekreftAksjonspunkter(bekreftedeAksjonspunktDtoer, behandling.getId());

        return Redirect.tilBehandlingPollStatus(request, behandling.getUuid());
    }

    /**
     * Oppretting og prosessering av aksjonspunkt som har type overstyringspunkt og eventuelt andre aksjonspunkter.
     * <p>
     * MERK: Det skal ikke ligge spesifikke sjekker som avhenger av status på behanlding, steg eller knytning til
     * spesifikke aksjonspunkter idenne tjenesten.
     */
    @POST
    @Path(AKSJONSPUNKT_OVERSTYR_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Overstyrer stegene", tags = "aksjonspunkt")
    @BeskyttetRessurs(action = UPDATE, resource = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response overstyr(@Context HttpServletRequest request,
                             @Parameter(description = "Liste over aksjonspunkter.") @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BekreftetOgOverstyrteAksjonspunkterDto apDto)
        throws URISyntaxException { // NOSONAR

        Long behandlingId = apDto.getBehandlingId();
        Behandling behandling = behandlingId != null
            ? behandlingRepository.hentBehandling(behandlingId)
            : behandlingRepository.hentBehandling(apDto.getBehandlingUuid());

        behandlingutredningTjeneste.kanEndreBehandling(behandling.getId(), apDto.getBehandlingVersjon());

        if (!apDto.getBekreftedeAksjonspunktDtoer().isEmpty()) {
            validerBetingelserForAksjonspunkt(behandling, apDto.getBekreftedeAksjonspunktDtoer());
            applikasjonstjeneste.bekreftAksjonspunkter(apDto.getBekreftedeAksjonspunktDtoer(), behandling.getId());
        }

        if (!apDto.getOverstyrteAksjonspunktDtoer().isEmpty()) {
            validerBetingelserForAksjonspunkt(behandling, apDto.getOverstyrteAksjonspunktDtoer());
            applikasjonstjeneste.overstyrAksjonspunkter(apDto.getOverstyrteAksjonspunktDtoer(), behandling.getId());
        }

        return Redirect.tilBehandlingPollStatus(request, behandling.getUuid());
    }


    private void validerBetingelserForAksjonspunkt(Behandling behandling, Collection<? extends AksjonspunktKode> aksjonspunktDtoer) {
        if (behandling.getStatus().equals(BehandlingStatus.FATTER_VEDTAK) && !erFatteVedtakAkpt(aksjonspunktDtoer)) {
            throw AksjonspunktRestTjenesteFeil.FACTORY.totrinnsbehandlingErStartet(String.valueOf(behandling.getId())).toException();
        }
    }

    private boolean erFatteVedtakAkpt(Collection<? extends AksjonspunktKode> aksjonspunktDtoer) {
        return aksjonspunktDtoer.size() == 1 &&
            aksjonspunktDtoer.iterator().next().getKode().equals(AksjonspunktDefinisjon.FATTER_VEDTAK.getKode());
    }
}
