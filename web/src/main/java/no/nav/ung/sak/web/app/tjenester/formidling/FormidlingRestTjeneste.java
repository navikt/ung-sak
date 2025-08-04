package no.nav.ung.sak.web.app.tjenester.formidling;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessurs;
import no.nav.k9.felles.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.ung.sak.formidling.GenerertBrev;
import no.nav.ung.sak.formidling.bestilling.BrevbestillingResultat;
import no.nav.ung.sak.formidling.informasjonsbrev.InformasjonsbrevTjeneste;
import no.nav.ung.sak.formidling.vedtak.VedtaksbrevTjeneste;
import no.nav.ung.sak.kontrakt.behandling.BehandlingIdDto;
import no.nav.ung.sak.kontrakt.formidling.informasjonsbrev.InformasjonsbrevBestillingRequest;
import no.nav.ung.sak.kontrakt.formidling.informasjonsbrev.InformasjonsbrevValgResponseDto;
import no.nav.ung.sak.kontrakt.formidling.vedtaksbrev.VedtaksbrevForhåndsvisRequest;
import no.nav.ung.sak.kontrakt.formidling.vedtaksbrev.VedtaksbrevValgRequest;
import no.nav.ung.sak.kontrakt.formidling.vedtaksbrev.VedtaksbrevValgResponse;
import no.nav.ung.sak.web.server.abac.AbacAttributtSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

import static jakarta.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM;
import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;
import static no.nav.ung.abac.BeskyttetRessursKoder.FAGSAK;

@Path("")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Transactional
public class FormidlingRestTjeneste {

    private VedtaksbrevTjeneste vedtaksbrevTjeneste;
    private InformasjonsbrevTjeneste informasjonsbrevTjeneste;

    private static final Logger LOG = LoggerFactory.getLogger(FormidlingRestTjeneste.class);
    private static final String PDF_MEDIA_STRING = "application/pdf";

    @Inject
    public FormidlingRestTjeneste(VedtaksbrevTjeneste vedtaksbrevTjeneste, InformasjonsbrevTjeneste informasjonsbrevTjeneste) {
        this.vedtaksbrevTjeneste = vedtaksbrevTjeneste;
        this.informasjonsbrevTjeneste = informasjonsbrevTjeneste;
    }

    FormidlingRestTjeneste() {
    }


    @GET
    @Path("/formidling/vedtaksbrev")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Operasjoner som er mulig for vedtaksbrev", tags = "formidling")
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    public VedtaksbrevValgResponse vedtaksbrevValg(
        @NotNull @QueryParam("behandlingId") @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingIdDto dto) {
        return vedtaksbrevTjeneste.vedtaksbrevValg(dto.getBehandlingId());
    }

    @POST
    @Path("/formidling/vedtaksbrev")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Lagring av brevvalg eks redigert eller hindretbrev  ", tags = "formidling",
        responses = @ApiResponse(responseCode = "200", description = "lagret ok")
    )
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    public Response lagreVedtaksbrevValg(
        @NotNull @Parameter(description = "") @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) VedtaksbrevValgRequest dto) {
        vedtaksbrevTjeneste.lagreVedtaksbrev(dto);
        return Response.ok().build();
    }

    /**
     * MediaType.APPLICATION_JSON is added to Produces because currently the generated client always adds accept: application/json to requests.
     */
    @POST
    @Path("/formidling/vedtaksbrev/forhaandsvis")
    @Consumes(MediaType.APPLICATION_JSON)
    //Json er med fordi frontend klienten alltid setter Accept = json, men denne produserer ikke json
    @Produces({APPLICATION_OCTET_STREAM, PDF_MEDIA_STRING, MediaType.TEXT_HTML, MediaType.APPLICATION_JSON})
    @Operation(description = "Forhåndsvise vedtaksbrev for en behandling. Bruk application/octet-stream fra swagger for å laste ned pdf ", tags = "formidling",
        responses = @ApiResponse(
            responseCode = "200",
            description = "pdf",
            content = {
                @Content(mediaType = APPLICATION_OCTET_STREAM, schema = @Schema(type = "string", format = "binary")),
                @Content(mediaType = PDF_MEDIA_STRING, schema = @Schema(type = "string", format = "binary")),
                @Content(mediaType = MediaType.TEXT_HTML, schema = @Schema(type = "string"))
            }
        )
    )
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    public Response forhåndsvisVedtaksbrev(
        @NotNull @Parameter(description = "") @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) VedtaksbrevForhåndsvisRequest dto,
        @Context HttpServletRequest request
    ) {
        var generertBrev = vedtaksbrevTjeneste.forhåndsvis(dto);

        return lagForhåndsvisResponse(dto.behandlingId(), request, generertBrev);

    }

    @GET
    @Path("/formidling/informasjonsbrev")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Maler for informasjon og støttebrev", tags = "formidling")
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    public InformasjonsbrevValgResponseDto informasjonsbrevValg(
        @NotNull @QueryParam("behandlingId") @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingIdDto dto) {
        return new InformasjonsbrevValgResponseDto(informasjonsbrevTjeneste.informasjonsbrevValg(dto.getBehandlingId()));
    }


    /**
     * MediaType.APPLICATION_JSON is added to Produces because currently the generated client always adds accept: application/json to requests.
     */
    @POST
    @Path("/formidling/informasjonsbrev/forhaandsvis")
    @Consumes(MediaType.APPLICATION_JSON)
    //Json er med fordi frontend klienten alltid setter Accept = json, men denne produserer ikke json
    @Produces({APPLICATION_OCTET_STREAM, PDF_MEDIA_STRING, MediaType.TEXT_HTML, MediaType.APPLICATION_JSON})
    @Operation(description = "Forhåndsvise inforasjonsbrev for en behandling. Bruk application/octet-stream fra swagger for å laste ned pdf ", tags = "formidling",
        responses = @ApiResponse(
            responseCode = "200",
            description = "pdf",
            content = {
                @Content(mediaType = APPLICATION_OCTET_STREAM, schema = @Schema(type = "string", format = "binary")),
                @Content(mediaType = PDF_MEDIA_STRING, schema = @Schema(type = "string", format = "binary")),
                @Content(mediaType = MediaType.TEXT_HTML, schema = @Schema(type = "string"))
            }
        )
    )
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    public Response forhåndsvisInformasjonsbrev(
        @NotNull @Parameter(description = "") @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) InformasjonsbrevBestillingRequest dto,
        @Valid @QueryParam("kunHtml") Boolean kunHtml,
        @Context HttpServletRequest request
    ) {
        var generertBrev = informasjonsbrevTjeneste.forhåndsvis(dto, kunHtml);

        return lagForhåndsvisResponse(dto.behandlingId(), request, generertBrev);

    }

    private static Response lagForhåndsvisResponse(Long behandlingId, HttpServletRequest request, GenerertBrev generertBrev) {
        if (generertBrev == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        boolean kunhtml = generertBrev.dokument().pdf() == null;
        var responseMediaType = kunhtml ? MediaType.TEXT_HTML : "application/pdf";
        var resultat = kunhtml ? generertBrev.dokument().html() : generertBrev.dokument().pdf();

        String mediaTypeReq = Objects.requireNonNullElse(request.getHeader(HttpHeaders.ACCEPT), APPLICATION_OCTET_STREAM);
        if (Objects.equals(mediaTypeReq, APPLICATION_OCTET_STREAM)) {
            var extension = kunhtml ? ".html" : ".pdf";
            return Response.ok(resultat) //Kun for å få swagger til å laste ned pdf
                .header("Content-Disposition", String.format("attachment; filename=\"%s-%s.%s\"", behandlingId, generertBrev.malType().getKode(), extension))
                .build();
        }

        return Response.ok(resultat).type(responseMediaType).build();
    }

    @POST
    @Path("/formidling/informasjonsbrev/bestill")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Bestill informasjonsbrev for en behandling. ", tags = "formidling")
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    public Response bestillInformasjonsbrev(
        @NotNull @Parameter(description = "") @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) InformasjonsbrevBestillingRequest dto
    ) {
        BrevbestillingResultat resultat = informasjonsbrevTjeneste.bestill(dto);

        return Response.ok(resultat.journalpostId()).build();

    }

}

