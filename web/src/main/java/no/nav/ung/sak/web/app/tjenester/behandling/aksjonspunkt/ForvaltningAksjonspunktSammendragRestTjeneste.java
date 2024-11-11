package no.nav.ung.sak.web.app.tjenester.behandling.aksjonspunkt;

import static no.nav.k9.abac.BeskyttetRessursKoder.DRIFT;
import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.CacheControl;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Variant;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessurs;
import no.nav.k9.felles.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.Venteårsak;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.ung.sak.behandlingslager.behandling.aksjonspunkt.AksjonspunktRepository;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.kontrakt.aksjonspunkt.BehandlingAksjonspunktDto;
import no.nav.ung.sak.kontrakt.behandling.SaksnummerDto;
import no.nav.ung.sak.typer.Periode;
import no.nav.ung.sak.web.server.abac.AbacAttributtEmptySupplier;

@ApplicationScoped
@Transactional
@Path("/aksjonspunkt")
@Produces(MediaType.APPLICATION_JSON)
public class ForvaltningAksjonspunktSammendragRestTjeneste {

    private static final MediaType TEXT = MediaType.TEXT_PLAIN_TYPE;
    private static final MediaType JSON = MediaType.APPLICATION_JSON_TYPE;

    private final List<Variant> reqVariants = Variant.mediaTypes(TEXT, JSON).build();
    private AksjonspunktRepository aksjonspunktRepository;

    public ForvaltningAksjonspunktSammendragRestTjeneste() {
    }

    @Inject
    public ForvaltningAksjonspunktSammendragRestTjeneste(AksjonspunktRepository aksjonspunktRepository) {
        this.aksjonspunktRepository = aksjonspunktRepository;
    }

    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN })
    @Operation(description = "Hent aksjonspunter for saker", tags = "aksjonspunkt", responses = {
            @ApiResponse(responseCode = "200", description = "Returnerer behandlinger med aksjonspunkt på JSON format", content = @Content(array = @ArraySchema(uniqueItems = true, arraySchema = @Schema(implementation = List.class), schema = @Schema(implementation = BehandlingAksjonspunktDto.class)), mediaType = MediaType.APPLICATION_JSON)),
            @ApiResponse(responseCode = "200", description = "Returnerer behandlinger med aksjonspunkt på CSV format", content = @Content(mediaType = MediaType.TEXT_PLAIN))
    })
    @BeskyttetRessurs(action = READ, resource = DRIFT)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response getAksjonspunkter(@QueryParam("saksnummer") @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtEmptySupplier.class) SaksnummerDto saksnummerDto,
                                      @Context Request request) { // NOSONAR

        Map<Behandling, List<Aksjonspunkt>> map;
        if (saksnummerDto != null) {
            var saksnummer = saksnummerDto.getVerdi();
            map = aksjonspunktRepository.hentAksjonspunkter(saksnummer, AksjonspunktStatus.OPPRETTET);
        } else {
            map = aksjonspunktRepository.hentAksjonspunkter(AksjonspunktStatus.OPPRETTET);
        }

        CacheControl cc = new CacheControl();
        cc.setNoCache(true);
        cc.setNoStore(true);
        cc.setMaxAge(0);
        cc.setPrivate(true);

        List<BehandlingAksjonspunktDto> dtos = mapFra(map);

        Variant v = request.selectVariant(reqVariants);
        if (v.getMediaType() == JSON) {
            return Response.ok(dtos, v).cacheControl(cc).build();
        } else if (v.getMediaType() == TEXT) {
            String csv = mapToCsv(dtos);
            return Response.ok(csv, v).cacheControl(cc).build();
        } else {
            return Response.notAcceptable(reqVariants).build();
        }
    }

    @GET
    @Path("/sammendrag_periode")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Operation(description = "Hent aksjonspunter for saker", tags = "aksjonspunkt", responses = {
            @ApiResponse(responseCode = "200", description = "Returnerer behandlinger med aksjonspunkt opprettet innenfor angitt periode på CSV format", content = @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM))
    })
    @BeskyttetRessurs(action = READ, resource = DRIFT)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response getAksjonspunkterSammendrag(@QueryParam("opprettetPeriode") @Parameter(description = "Tidsrom for opprettelse. Format YYYY-MM-DD/YYYY-MM-DD.") @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtEmptySupplier.class) @NotNull Periode opprettetPeriode,
                                                @QueryParam("medUtforte") @Parameter(description = "Valgfritt å inkludere utførte aksjonspunkt. Default er false.") @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtEmptySupplier.class) boolean medUtforte,
                                                @SuppressWarnings("unused") @Context Request request) { // NOSONAR

        var aksjonspunktStatuser = medUtforte
            ? new AksjonspunktStatus[] { AksjonspunktStatus.OPPRETTET, AksjonspunktStatus.UTFØRT }
            : new AksjonspunktStatus[] { AksjonspunktStatus.OPPRETTET };

        var map = aksjonspunktRepository.hentAksjonspunkter(
            opprettetPeriode.getFom(), opprettetPeriode.getTom().plusDays(1), aksjonspunktStatuser);

        CacheControl cc = new CacheControl();
        cc.setNoCache(true);
        cc.setNoStore(true);
        cc.setMaxAge(0);
        cc.setPrivate(true);

        String csv = mapToCsv(map);
        return Response.ok(csv, MediaType.APPLICATION_OCTET_STREAM).cacheControl(cc).build();
    }

    private String mapToCsv(Map<Behandling, List<Aksjonspunkt>> map) {
        var sb = new StringBuilder(2048);

        // quick and dirt til csv. Kan godt forbedres. Ingen av feltene trenger escaping

        // headere, pass på rekkefølge her!
        sb.append("ytelseType,saksnummer,fagsakStatus,behandlingUuid,behandlingId,behandlingType,behandlingStatus,"
            + "behandlingOpprettet,behandlingAvsluttet,aksjonspunktDef,aksjonspunktType,vilkårType,aksjonspunktStatus,"
            + "venteårsak,fristTid,kanLøses,totrinnsbehandling,opprettetTid,endretTid\n");
        for (var entry : map.entrySet()) {
            var b = entry.getKey();
            var f = b.getFagsak();
            String yt = f.getYtelseType().getKode();
            String sn = f.getSaksnummer().getVerdi();
            String fs = f.getStatus().getKode();
            String uuid = b.getUuid().toString();
            String bid = Long.toString(b.getId());
            String bt = b.getType().getKode();
            String bs = b.getStatus().getKode();
            LocalDateTime bo = b.getOpprettetDato();
            LocalDateTime ba = b.getAvsluttetDato();

            for (var a : b.getAksjonspunkter()) {
                var ad = a.getAksjonspunktDefinisjon().getKode();
                var at = a.getAksjonspunktDefinisjon().getAksjonspunktType().getKode();
                var vt = a.getAksjonspunktDefinisjon().getVilkårType() == null || a.getAksjonspunktDefinisjon().getVilkårType() == VilkårType.UDEFINERT ? ""
                    : a.getAksjonspunktDefinisjon().getVilkårType(); // NOSONAR
                var as = a.getStatus() == null ? "" : a.getStatus().getKode();
                var vå = a.getVenteårsak() == null || a.getVenteårsak() == Venteårsak.UDEFINERT ? "" : a.getVenteårsak().getKode(); // NOSONAR
                var ft = a.getFristTid();
                var kl = AksjonspunktDtoMapper.kanLøses(a.getAksjonspunktDefinisjon(), b) == null ? "" : AksjonspunktDtoMapper.kanLøses(a.getAksjonspunktDefinisjon(), b);
                var tt = a.isToTrinnsBehandling() || a.getAksjonspunktDefinisjon().getDefaultTotrinnBehandling();
                var ot = a.getOpprettetTidspunkt();
                var et = a.getEndretTidspunkt();

                Object[] args = new Object[] { yt, sn, fs, uuid, bid, bt, bs, bo, ba, ad, at, vt, as, vå, ft, kl, tt, ot, et };
                String fmt = "%s,".repeat(args.length);
                var s = String.format(fmt.substring(0, fmt.length() - 1), args);
                sb.append(s).append('\n');
            }
        }

        return sb.toString();
    }

    private String mapToCsv(List<BehandlingAksjonspunktDto> dtos) {
        var sb = new StringBuilder(2048);

        // quick and dirt til csv. Kan godt forbedres. Ingen av feltene trenger escaping

        // headere, pass på rekkefølge her!
        sb.append("ytelseType,saksnummer,fagsakStatus,behandlingUuid,behandlingType,behandlingStatus,"
            + "aksjonspunktDef,aksjonspunktType,vilkårType,aksjonspunktStatus,venteårsak,fristTid,"
            + "kanLøses,totrinnsbehandling,totrinnsbehandlingGodkjent\n");

        for (var d : dtos) {
            // ingen av feltene trenger escaping så langt - kun id og kodeverdier
            var yt = d.getYtelseType().getKode();
            var sn = d.getSaksnummer().getVerdi();
            var fs = d.getFagsakStatus().getKode();
            var uuid = d.getBehandlingUuid().toString();
            var bt = d.getBehandlingType().getKode();
            var bs = d.getBehandlingStatus().getKode();

            for (var a : d.getAksjonspunkter()) {
                var ad = a.getDefinisjon().getKode();
                var at = a.getAksjonspunktType().getKode();
                var vt = a.getVilkarType() == null || a.getVilkarType() == VilkårType.UDEFINERT ? "" : a.getVilkarType(); // NOSONAR
                var as = a.getStatus() == null ? "" : a.getStatus().getKode();
                var vå = a.getVenteårsak() == null || a.getVenteårsak() == Venteårsak.UDEFINERT ? "" : a.getVenteårsak().getKode(); // NOSONAR
                var ft = a.getFristTid();
                var kl = a.getKanLoses() == null ? "" : a.getKanLoses();
                var tt = a.getToTrinnsBehandling() == null ? "" : a.getToTrinnsBehandling();
                var ttg = a.getToTrinnsBehandlingGodkjent() == null ? "" : a.getToTrinnsBehandlingGodkjent();

                Object[] args = new Object[] { yt, sn, fs, uuid, bt, bs, ad, at, vt, as, vå, ft, kl, tt, ttg };
                String fmt = "%s,".repeat(args.length);
                var s = String.format(fmt.substring(0, fmt.length() - 1), args);
                sb.append(s).append('\n');
            }
        }

        return sb.toString();
    }

    private List<BehandlingAksjonspunktDto> mapFra(Map<Behandling, List<Aksjonspunkt>> map) {
        List<BehandlingAksjonspunktDto> list = new ArrayList<>();

        for (var entry : map.entrySet()) {
            Behandling behandling = entry.getKey();
            Fagsak fagsak = behandling.getFagsak();
            var aksjonspunkter = AksjonspunktDtoMapper.mapFra(behandling, entry.getValue());
            var ytelseType = fagsak.getYtelseType();
            var fagsakStatus = fagsak.getStatus();
            var saksnummer = fagsak.getSaksnummer();
            var behandlingUuid = behandling.getUuid();
            var behandlingType = behandling.getType();
            var behandlingStatus = behandling.getStatus();
            var dto = new BehandlingAksjonspunktDto(saksnummer, ytelseType, fagsakStatus, behandlingUuid, behandlingType, behandlingStatus, aksjonspunkter);
            list.add(dto);
        }
        return list;
    }

}
