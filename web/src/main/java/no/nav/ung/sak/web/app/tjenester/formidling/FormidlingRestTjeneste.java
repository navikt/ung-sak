package no.nav.ung.sak.web.app.tjenester.formidling;

import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;
import static no.nav.ung.abac.BeskyttetRessursKoder.FAGSAK;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessurs;
import no.nav.k9.felles.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.ung.kodeverk.formidling.RolleType;
import no.nav.ung.sak.formidling.BrevGenerererTjeneste;
import no.nav.ung.sak.formidling.domene.GenerertBrev;
import no.nav.ung.sak.formidling.dto.BrevbestillingDto;
import no.nav.ung.sak.formidling.dto.PartRequestDto;
import no.nav.ung.sak.kontrakt.formidling.ForhåndsvisDto;
import no.nav.ung.sak.web.server.abac.AbacAttributtSupplier;

@Path("")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Transactional
public class FormidlingRestTjeneste {

    private BrevGenerererTjeneste brevGenerererTjeneste;

    @Inject
    public FormidlingRestTjeneste(BrevGenerererTjeneste brevGenerererTjeneste) {
        this.brevGenerererTjeneste = brevGenerererTjeneste;
    }

    FormidlingRestTjeneste() {
    }


    @POST
    @Path("/formidling/forhaandsvis")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces("application/pdf")
    @Operation(description = "Forhåndsvise brev", tags = "formidling")
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    public Response forhåndsvis(
        @NotNull @Parameter(description = "") @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) ForhåndsvisDto forhåndsvisDto
    ) {
        GenerertBrev generertBrev = brevGenerererTjeneste.generer(new BrevbestillingDto(
            forhåndsvisDto.behandlingId(),
            forhåndsvisDto.dokumentMal(),
            null,
            forhåndsvisDto.mottaker() != null ? new PartRequestDto(forhåndsvisDto.mottaker().id(), forhåndsvisDto.mottaker().type(), RolleType.BRUKER) : null,
            forhåndsvisDto.dokumentdata()
        ));

        return Response.ok(generertBrev.dokument().pdf())
            .type(MediaType.APPLICATION_OCTET_STREAM)
            .header("Content-Disposition", String.format("attachment; filename=\"%s-%s.pdf\"", forhåndsvisDto.behandlingId(), generertBrev.malType().getKode()))
            .build();
    }

}
