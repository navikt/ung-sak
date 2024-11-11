package no.nav.ung.sak.web.app.tjenester.brukerdialog;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessurs;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt;

import static no.nav.k9.felles.sikkerhet.abac.PepImpl.TOKENX_RESOURCE;

@Path(BrukerdialogRestTjeneste.BASE_PATH)
@ApplicationScoped
@Transactional
public class BrukerdialogRestTjeneste {
    static final String BASE_PATH = "/brukerdialog";

    private static final String JSON_UTF8 = "application/json; charset=UTF-8";

    private BrukerdialogTjeneste brukerdialogTjeneste;

    public BrukerdialogRestTjeneste() {// For Rest-CDI
    }

    @Inject
    public BrukerdialogRestTjeneste(
        BrukerdialogTjeneste brukerdialogTjeneste

    ) {
        this.brukerdialogTjeneste = brukerdialogTjeneste;
    }

    @POST
    @Path("/omsorgsdager-kronisk-sykt-barn/har-gyldig-vedtak")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Returnerer nyeste gyldige vedtak for en gitt aktørId", summary = "Returnerer nyeste gyldige vedtak for en gitt aktørId", tags = "brukerdialog")
    @BeskyttetRessurs(action = BeskyttetRessursActionAttributt.READ, resource = TOKENX_RESOURCE)
    // Dette endepunktet brukes av k9-sak-innsyn-api. I k9-sak-innsyn-api hentes aktørId fra token.
    public HarGyldigOmsorgsdagerVedtakDto hentSisteGyldigeVedtakForAktorId(
        @Valid HentSisteGyldigeVedtakForAktørInputDto inputDto
    ) {
        return brukerdialogTjeneste.harGyldigOmsorgsdagerVedtak(inputDto.pleietrengendeAktørId());
    }
}

