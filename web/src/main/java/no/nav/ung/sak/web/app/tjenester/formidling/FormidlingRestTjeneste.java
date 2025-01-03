package no.nav.ung.sak.web.app.tjenester.formidling;

import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;
import static no.nav.ung.abac.BeskyttetRessursKoder.FAGSAK;

import java.util.Objects;

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
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessurs;
import no.nav.k9.felles.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.sak.formidling.BrevGenerererTjeneste;
import no.nav.ung.sak.formidling.domene.GenerertBrev;
import no.nav.ung.sak.formidling.dto.Brevbestilling;
import no.nav.ung.sak.kontrakt.behandling.BehandlingIdDto;
import no.nav.ung.sak.kontrakt.formidling.vedtaksbrev.VedtaksbrevForhåndsvisDto;
import no.nav.ung.sak.kontrakt.formidling.vedtaksbrev.VedtaksbrevOperasjonerDto;
import no.nav.ung.sak.web.server.abac.AbacAttributtSupplier;

@Path("")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Transactional
public class FormidlingRestTjeneste {

    private BrevGenerererTjeneste brevGenerererTjeneste;

    private static final String PDF_MEDIA_STRING = "application/pdf";
    private static final MediaType PDF_MEDIA_TYPE = MediaType.valueOf(PDF_MEDIA_STRING);


    @Inject
    public FormidlingRestTjeneste(BrevGenerererTjeneste brevGenerererTjeneste) {
        this.brevGenerererTjeneste = brevGenerererTjeneste;
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
        @NotNull @Parameter(description = "behandlingId") @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingIdDto dto) {

        return new VedtaksbrevOperasjonerDto(true,
            new VedtaksbrevOperasjonerDto.AutomatiskBrevOperasjoner(false, false),
            false,
            false,
            false);
    }


    @POST
    @Path("/formidling/vedtaksbrev/forhaandsvis")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_OCTET_STREAM, PDF_MEDIA_STRING, MediaType.TEXT_HTML})
    @Operation(description = "Forhåndsvise vedtaksbrev for en behandling", tags = "formidling",
        responses = @ApiResponse(
            responseCode = "200",
            description = "pdf",
            content = {
                @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM, schema = @Schema(type = "string", format = "binary")),
                @Content(mediaType = PDF_MEDIA_STRING, schema = @Schema(type = "string", format = "binary")),
                @Content(mediaType = MediaType.TEXT_HTML, schema = @Schema(type = "string"))
                }
            )
        )
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    public Response forhåndsvisVedtaksbrev(
        @NotNull @Parameter(description = "") @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) VedtaksbrevForhåndsvisDto dto,
        @Context HttpServletRequest request
    ) {
        GenerertBrev generertBrev = brevGenerererTjeneste.generer(new Brevbestilling(
            dto.behandlingId(),
            DokumentMalType.INNVILGELSE_DOK,
            null,
            null,
            dto.dokumentdata()
        ));

        var mediaTypeReq = Objects.requireNonNullElse(request.getHeader(HttpHeaders.ACCEPT), MediaType.APPLICATION_OCTET_STREAM);

        return switch (mediaTypeReq) {
            case PDF_MEDIA_STRING -> Response.ok(generertBrev.dokument().pdf()).build();
            case MediaType.TEXT_HTML -> Response.ok(generertBrev.dokument().html()).build();
            default -> Response.ok(generertBrev.dokument().pdf()) //Kun for å få swagger til å laste ned pdf
                .header("Content-Disposition", String.format("attachment; filename=\"%s-%s.pdf\"", dto.behandlingId(), generertBrev.malType().getKode()))
                .build();

        };
    }


}

