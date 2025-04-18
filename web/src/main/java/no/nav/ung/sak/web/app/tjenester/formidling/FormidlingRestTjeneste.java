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
import no.nav.ung.sak.formidling.BrevGenerererTjeneste;
import no.nav.ung.sak.formidling.GenerertBrev;
import no.nav.ung.sak.formidling.VedtaksbrevRegelResulat;
import no.nav.ung.sak.formidling.VedtaksbrevRegler;
import no.nav.ung.sak.kontrakt.behandling.BehandlingIdDto;
import no.nav.ung.sak.kontrakt.formidling.vedtaksbrev.VedtaksbrevForhåndsvisDto;
import no.nav.ung.sak.kontrakt.formidling.vedtaksbrev.VedtaksbrevOperasjonerDto;
import no.nav.ung.sak.web.server.abac.AbacAttributtSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;
import static no.nav.ung.abac.BeskyttetRessursKoder.FAGSAK;

@Path("")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Transactional
public class FormidlingRestTjeneste {

    private BrevGenerererTjeneste brevGenerererTjeneste;
    private VedtaksbrevRegler vedtaksbrevRegler;

    private static final Logger LOG = LoggerFactory.getLogger(FormidlingRestTjeneste.class);
    private static final String PDF_MEDIA_STRING = "application/pdf";
    private static final MediaType PDF_MEDIA_TYPE = MediaType.valueOf(PDF_MEDIA_STRING);

    @Inject
    public FormidlingRestTjeneste(
        BrevGenerererTjeneste brevGenerererTjeneste,
        VedtaksbrevRegler vedtaksbrevRegler) {
        this.brevGenerererTjeneste = brevGenerererTjeneste;
        this.vedtaksbrevRegler = vedtaksbrevRegler;
    }

    FormidlingRestTjeneste() {
    }


    @GET
    @Path("/formidling/vedtaksbrev")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Operasjoner som er mulig for vedtaksbrev", tags = "formidling")
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    public VedtaksbrevOperasjonerDto tilgjengeligeVedtaksbrev(
        @NotNull @QueryParam("behandlingId") @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingIdDto dto) {

        VedtaksbrevRegelResulat resultat = vedtaksbrevRegler.kjør(Long.valueOf(dto.getId()));
        LOG.info("VedtaksbrevRegelResultat: {}", resultat.safePrint());
        return resultat.vedtaksbrevOperasjoner();
    }


    /**
     * MediaType.APPLICATION_JSON is added to Produces because currently the generated client always adds accept: application/json to requests.
     */
    @POST
    @Path("/formidling/vedtaksbrev/forhaandsvis")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_OCTET_STREAM, PDF_MEDIA_STRING, MediaType.TEXT_HTML, MediaType.APPLICATION_JSON})
    @Operation(description = "Forhåndsvise vedtaksbrev for en behandling. Bruk application/octet-stream fra swagger for å laste ned pdf ", tags = "formidling",
        responses = @ApiResponse(
            responseCode = "200",
            description = "pdf",
            content = {
                @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM, schema = @Schema(type = "string", format = "binary")),
                @Content(mediaType = PDF_MEDIA_STRING, schema = @Schema(type = "string", format = "binary")),
                @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(type = "string", format = "binary")),
                @Content(mediaType = MediaType.TEXT_HTML, schema = @Schema(type = "string"))
            }
        )
    )
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    public Response forhåndsvisVedtaksbrev(
        @NotNull @Parameter(description = "") @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) VedtaksbrevForhåndsvisDto dto,
        @Context HttpServletRequest request
    ) {

        GenerertBrev generertBrev = brevGenerererTjeneste.genererVedtaksbrev(dto.behandlingId());
        if (generertBrev == null) {
            return Response.status(Response.Status.NOT_FOUND.getStatusCode()).build();
        }

        var mediaTypeReq = Objects.requireNonNullElse(request.getHeader(HttpHeaders.ACCEPT), MediaType.APPLICATION_OCTET_STREAM);

        return switch (mediaTypeReq) {
            case PDF_MEDIA_STRING, MediaType.APPLICATION_JSON -> Response.ok(generertBrev.dokument().pdf()).build();
            case MediaType.TEXT_HTML -> Response.ok(generertBrev.dokument().html()).build();
            default -> Response.ok(generertBrev.dokument().pdf()) //Kun for å få swagger til å laste ned pdf
                .header("Content-Disposition", String.format("attachment; filename=\"%s-%s.pdf\"", dto.behandlingId(), generertBrev.malType().getKode()))
                .build();

        };
    }

}

