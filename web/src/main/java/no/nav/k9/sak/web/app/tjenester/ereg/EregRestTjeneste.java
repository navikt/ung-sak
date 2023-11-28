package no.nav.k9.sak.web.app.tjenester.ereg;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import no.nav.k9.felles.integrasjon.organisasjon.OrganisasjonRestKlient;
import no.nav.k9.felles.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.k9.sak.web.server.abac.AbacAttributtSupplier;

@Path("")
@ApplicationScoped
public class EregRestTjeneste {

    private OrganisasjonRestKlient eregRestKlient;

    public EregRestTjeneste() {
    }

    @Inject
    public EregRestTjeneste(OrganisasjonRestKlient eregRestKlient) {
        this.eregRestKlient = eregRestKlient;
    }

    @GET
    @Path("/ereg/organisasjon/{organisasjonsnr}/brevmottaker-info")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public Response getOrganisasjon(@NotNull @PathParam(OrganisasjonsnrDto.NAME) @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) OrganisasjonsnrDto organisasjonsnrDto) {
        var orgnr = organisasjonsnrDto.getOrgnr();
        if (orgnr.length() != 9 || !orgnr.matches("\\d{9}")) {
            return Response.status(400, "organisasjonsnrDto must be 9 digits").build();
        }
        try {
            var org = eregRestKlient.hentOrganisasjon(orgnr);
            final var response = new EregOrganisasjonBrevmottakerInfoResponseDto(org.getNavn());
            return Response.ok(response).build();
        } catch (IllegalArgumentException e) {
            // If org number is not found, it seems like eregRestKlient throws IllegalArgumentException
            // with the message matched below. At least this happens in dev.
            // For this endpoint I want to return empty json in the case when given organization
            // number is not found, so returns that below then.
            if (e.getMessage().equalsIgnoreCase("argument \"content\" is null")) {
                final var emptyObject = new Object();
                return Response.ok(emptyObject).build();
            }
            // If some other IllegalArgumentException happens, rethrow it.
            throw e;
        }
    }
}
