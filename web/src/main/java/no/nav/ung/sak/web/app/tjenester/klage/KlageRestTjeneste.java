package no.nav.ung.sak.web.app.tjenester.klage;

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
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.CacheControl;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import no.nav.k9.felles.sikkerhet.abac.*;
import no.nav.ung.kodeverk.hjemmel.Hjemmel;
import no.nav.ung.kodeverk.klage.KlageMedholdÅrsak;
import no.nav.ung.kodeverk.klage.KlageVurderingOmgjør;
import no.nav.ung.kodeverk.klage.KlageVurderingType;
import no.nav.ung.kodeverk.klage.KlageVurdertAv;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.klage.KlageRepository;
import no.nav.ung.sak.behandlingslager.behandling.klage.KlageVurderingAdapter;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.fritekst.FritekstRepository;
import no.nav.ung.sak.klage.domenetjenester.KlageVurderingTjeneste;
import no.nav.ung.sak.kontrakt.behandling.BehandlingUuidDto;
import no.nav.ung.sak.kontrakt.klage.*;
import no.nav.ung.sak.web.app.rest.Redirect;
import no.nav.ung.sak.web.server.abac.AbacAttributtSupplier;

import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;

import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionType.READ;
import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionType.UPDATE;
import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursResourceType.FAGSAK;

@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
@Path("")
@Transactional
public class KlageRestTjeneste {
    public static final String KLAGE_V2_PATH = "/klage-v2";
    private static final String MELLOMLAGRE_PART_PATH = "/klage-v2/mellomlagre-klage";
    private static final String MELLOMLAGRE_GJENAPNE_KLAGE_PART_PATH = "/klage-v2/mellomlagre-gjennapne-klage";
    public static final String HJEMLER = KLAGE_V2_PATH+"/hjemler";

    private BehandlingRepository behandlingRepository;
    private KlageRepository klageRepository;
    private KlageVurderingTjeneste klageVurderingTjeneste;
    private FritekstRepository fritekstRepository;

    public KlageRestTjeneste() {
        // for CDI proxy
    }

    @Inject
    public KlageRestTjeneste(BehandlingRepository behandlingRepository,
                             KlageRepository klageRepository,
                             KlageVurderingTjeneste klageVurderingTjeneste,
                             FritekstRepository fritekstRepository) {
        this.behandlingRepository = behandlingRepository;
        this.klageRepository = klageRepository;
        this.klageVurderingTjeneste = klageVurderingTjeneste;
        this.fritekstRepository = fritekstRepository;
    }

    @GET
    @Path(KLAGE_V2_PATH)
    @Operation(description = "Hent informasjon om klagevurdering for en klagebehandling",
        tags = "no/nav/k9/klage",
        responses = {
            @ApiResponse(responseCode = "200",
                description = "Returnerer vurdering av en klage fra ulike instanser",
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = KlagebehandlingDto.class)
                )
            )
        })
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response getKlageVurdering(@NotNull @QueryParam(BehandlingUuidDto.NAME) @Parameter(description = BehandlingUuidDto.DESC) @Valid  @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingUuidDto uuidDto) {
        Behandling behandling = behandlingRepository.hentBehandling(uuidDto.getBehandlingUuid());

        KlagebehandlingDto dto = mapFra(behandling);
        CacheControl cc = new CacheControl();
        cc.setNoCache(true);
        cc.setNoStore(true);
        cc.setMaxAge(0);
        return Response.ok(dto).cacheControl(cc).build();
    }

    @GET
    @Path(HJEMLER)
    @Operation(description = "Henter hjemler aktuelle for bruk i klagevurdering",
        tags = "no/nav/k9/klage",
        responses = {
            @ApiResponse(responseCode = "200",
                description = "Returnerer alle valgbare hjemler for klagevurdering",
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    array = @ArraySchema(arraySchema = @Schema(implementation = Set.class),
                        schema = @Schema(implementation = KlageHjemmelDto.class)
                    )
                )
            )
        })
    @BeskyttetRessurs(action = READ, resource = BeskyttetRessursResourceType.APPLIKASJON, auditlogg = false)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response hentValgbareKlagehjemler() {
        var klagehjemler = Arrays.stream(Hjemmel.values()).map(hjemmel ->
            new KlageHjemmelDto(hjemmel.getKode(), hjemmel.getNavn())
        );

        return Response.ok(klagehjemler).build();
    }

    @POST
    @Path(MELLOMLAGRE_PART_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Mellomlagring av vurderingstekst for klagebehandling", tags = "no/nav/k9/klage")
    @BeskyttetRessurs(action = UPDATE, resource = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response mellomlagreKlage(@Parameter(description = "KlageVurderingAdapter tilpasset til mellomlagring.") @Valid AbacKlageVurderingResultatAksjonspunktMellomlagringDto apDto)
        throws URISyntaxException { // NOSONAR

        Behandling behandling = behandlingRepository.hentBehandling(apDto.getBehandlingId());
        KlageVurderingAdapter klageVurderingAdapter = mapDto(apDto);
        klageVurderingTjeneste.mellomlagreVurderingResultat(behandling, klageVurderingAdapter);
        return Response.ok().build();
    }

    @POST
    @Path(MELLOMLAGRE_GJENAPNE_KLAGE_PART_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Mellomlagring av vurderingstekst for klagebehandling", tags = "no/nav/k9/klage")
    @BeskyttetRessurs(action = UPDATE, resource = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response mellomlagreKlageOgGjenåpneAp(@Context HttpServletRequest request,
                                                 @Parameter(description = "KlageVurderingAdapter tilpasset til mellomlagring.") @Valid AbacKlageVurderingResultatAksjonspunktMellomlagringDto apDto)
        throws URISyntaxException { // NOSONAR

        Behandling behandling = behandlingRepository.hentBehandling(apDto.getBehandlingId());
        KlageVurderingAdapter klageVurderingAdapter = mapDto(apDto);
        klageVurderingTjeneste.mellomlagreVurderingResultatOgÅpneAksjonspunkt(behandling, klageVurderingAdapter);
        return Redirect.tilBehandlingPollStatus(request, behandling.getUuid());
    }


    private KlagebehandlingDto mapFra(Behandling behandling) {
        KlagebehandlingDto dto = new KlagebehandlingDto();
        Optional<KlageFormkravResultatDto> nfpFormkrav = KlageFormkravResultatDtoMapper.mapNFPKlageFormkravResultatDto(behandling, klageRepository);
        Optional<KlageVurderingResultatDto> nfpVurdering = KlageVurderingResultatDtoMapper.mapFørsteinstansKlageVurderingResultatDto(behandling, klageRepository, fritekstRepository);
        Optional<KlageVurderingResultatDto> nkVurdering = KlageVurderingResultatDtoMapper.mapAndreinstansKlageVurderingResultatDto(behandling, klageRepository, fritekstRepository);

        if (nfpVurdering.isPresent() || nkVurdering.isPresent() || nfpFormkrav.isPresent()) {
            nfpVurdering.ifPresent(dto::setKlageVurderingResultatNFP);
            nkVurdering.ifPresent(dto::setKlageVurderingResultatNK);
            nfpFormkrav.ifPresent(dto::setKlageFormkravResultatNFP);
            return dto;
        } else {
            return null;
        }
    }

    private KlageVurderingAdapter mapDto(KlageVurderingResultatAksjonspunktMellomlagringDto apDto) {
        Hjemmel hjemmel = Hjemmel.fraKode(apDto.getHjemmel());

        return new KlageVurderingAdapter(apDto.getKlageVurdering(), apDto.getKlageMedholdArsak(), apDto.getKlageVurderingOmgjoer(),
            apDto.getBegrunnelse(), apDto.getFritekstTilBrev(), hjemmel, null, KlageVurdertAv.VEDTAKSINSTANS);
    }

    public static class AbacKlageVurderingResultatAksjonspunktMellomlagringDto extends KlageVurderingResultatAksjonspunktMellomlagringDto implements AbacDto {

        public AbacKlageVurderingResultatAksjonspunktMellomlagringDto() {
            super();
        }

        public AbacKlageVurderingResultatAksjonspunktMellomlagringDto(String kode, Long behandlingId, String begrunnelse, KlageVurderingType klageVurderingType, KlageMedholdÅrsak klageMedholdArsak,
                                                                      String fritekstTilBrev, String klageHjemmel, KlageVurderingOmgjør klageVurderingOmgjoer) {
            super(kode, behandlingId, begrunnelse, klageVurderingType, klageMedholdArsak, fritekstTilBrev, klageHjemmel, klageVurderingOmgjoer);
        }

        @Override
        public AbacDataAttributter abacAttributter() {
            return AbacDataAttributter.opprett()
                .leggTil(StandardAbacAttributtType.BEHANDLING_ID, getBehandlingId());
        }

    }
}
