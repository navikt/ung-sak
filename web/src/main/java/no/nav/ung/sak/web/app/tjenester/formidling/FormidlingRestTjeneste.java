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
import no.nav.ung.sak.formidling.vedtaksbrevvalg.VedtaksbrevValgEntitet;
import no.nav.ung.sak.formidling.vedtaksbrevvalg.VedtaksbrevValgRepository;
import no.nav.ung.sak.kontrakt.behandling.BehandlingIdDto;
import no.nav.ung.sak.kontrakt.formidling.vedtaksbrev.VedtaksbrevForhåndsvisDto;
import no.nav.ung.sak.kontrakt.formidling.vedtaksbrev.VedtaksbrevOperasjonerDto;
import no.nav.ung.sak.kontrakt.formidling.vedtaksbrev.VedtaksbrevOperasjonerRequestDto;
import no.nav.ung.sak.web.server.abac.AbacAttributtSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Optional;

import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;
import static no.nav.ung.abac.BeskyttetRessursKoder.FAGSAK;

@Path("")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Transactional
public class FormidlingRestTjeneste {

    private BrevGenerererTjeneste brevGenerererTjeneste;
    private VedtaksbrevRegler vedtaksbrevRegler;
    private VedtaksbrevValgRepository vedtaksbrevValgRepository;

    private static final Logger LOG = LoggerFactory.getLogger(FormidlingRestTjeneste.class);
    private static final String PDF_MEDIA_STRING = "application/pdf";

    @Inject
    public FormidlingRestTjeneste(
        BrevGenerererTjeneste brevGenerererTjeneste,
        VedtaksbrevRegler vedtaksbrevRegler,
        VedtaksbrevValgRepository vedtaksbrevValgRepository) {
        this.brevGenerererTjeneste = brevGenerererTjeneste;
        this.vedtaksbrevRegler = vedtaksbrevRegler;
        this.vedtaksbrevValgRepository = vedtaksbrevValgRepository;
    }

    FormidlingRestTjeneste() {
    }


    @GET
    @Path("/formidling/vedtaksbrev")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Operasjoner som er mulig for vedtaksbrev", tags = "formidling")
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    public VedtaksbrevOperasjonerDto vedtaksbrevOperasjoner(
        @NotNull @QueryParam("behandlingId") @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingIdDto dto) {

        VedtaksbrevRegelResulat resultat = vedtaksbrevRegler.kjør(Long.valueOf(dto.getId()));
        LOG.info("VedtaksbrevRegelResultat: {}", resultat.safePrint());
        return resultat.vedtaksbrevOperasjoner();
    }

    @POST
    @Path("/formidling/vedtaksbrev")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Lagring av brevvalg eks redigert eller hindretbrev  ", tags = "formidling")
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    public Response lagreVedtaksbrev(
        @NotNull @Parameter(description = "") @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) VedtaksbrevOperasjonerRequestDto dto) {

        VedtaksbrevRegelResulat resultat = vedtaksbrevRegler.kjør(dto.behandlingId());

        var vedtaksbrevValgEntitet = Optional.ofNullable(vedtaksbrevValgRepository.finnVedtakbrevValg(dto.behandlingId()))
            .orElse(VedtaksbrevValgEntitet.ny(dto.behandlingId()));

        if (!resultat.vedtaksbrevOperasjoner().enableRediger() && dto.redigert() != null) {
            return Response.status(Response.Status.BAD_REQUEST.getStatusCode(),
                    "Brevet kan ikke redigeres. ")
                .build();
        }

        if (Boolean.TRUE.equals(dto.redigert()) && (dto.redigertHtml() == null || dto.redigertHtml().isBlank())) {
            return Response.status(Response.Status.BAD_REQUEST.getStatusCode(),
                    "Redigert tekst kan ikke være tom")
                .build();
        }

        if ((dto.redigert() == null || !dto.redigert()) && dto.redigertHtml() != null) {
            return Response.status(Response.Status.BAD_REQUEST.getStatusCode(),
                    "Kan ikke ha redigert tekst samtidig som redigert er true")
                .build();
        }

        if (!resultat.vedtaksbrevOperasjoner().enableHindre() && dto.hindret() != null) {
            return Response.status(Response.Status.BAD_REQUEST.getStatusCode(),
                    "Brevet kan ikke hindres. ")
                .build();
        }


        vedtaksbrevValgEntitet.setHindret(Boolean.TRUE.equals(dto.hindret()));
        vedtaksbrevValgEntitet.setRedigert(Boolean.TRUE.equals(dto.redigert()));
        vedtaksbrevValgEntitet.setRedigertBrevHtml(dto.redigertHtml());

        vedtaksbrevValgRepository.lagre(vedtaksbrevValgEntitet);

        return Response.ok().build();

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
        var mediaTypeReq = Objects.requireNonNullElse(request.getHeader(HttpHeaders.ACCEPT), MediaType.APPLICATION_OCTET_STREAM);

        GenerertBrev generertBrev = mediaTypeReq.equals(MediaType.TEXT_HTML) ?
            brevGenerererTjeneste.genererVedtaksbrevKunHtml(dto.behandlingId()) :
            brevGenerererTjeneste.genererVedtaksbrev(dto.behandlingId());

        if (generertBrev == null) {
            return Response.status(Response.Status.NOT_FOUND.getStatusCode()).build();
        }

        return switch (mediaTypeReq) {
            case PDF_MEDIA_STRING, MediaType.APPLICATION_JSON -> Response.ok(generertBrev.dokument().pdf()).build();
            case MediaType.TEXT_HTML -> Response.ok(generertBrev.dokument().html()).build();
            default -> Response.ok(generertBrev.dokument().pdf()) //Kun for å få swagger til å laste ned pdf
                .header("Content-Disposition", String.format("attachment; filename=\"%s-%s.pdf\"", dto.behandlingId(), generertBrev.malType().getKode()))
                .build();

        };
    }

}

