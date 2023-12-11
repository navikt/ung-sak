package no.nav.k9.sak.web.app.tjenester.saksnummer;

import static no.nav.k9.abac.BeskyttetRessursKoder.FAGSAK;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessurs;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt;
import no.nav.k9.felles.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.k9.sak.behandlingslager.saksnummer.SaksnummerAktorRepository;
import no.nav.k9.sak.behandlingslager.saksnummer.SaksnummerRepository;
import no.nav.k9.sak.kontrakt.behandling.SaksnummerDto;
import no.nav.k9.sak.kontrakt.person.AktørIdDto;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.k9.sak.web.server.abac.AbacAttributtSupplier;

@Path(SaksnummerRestTjeneste.BASE_PATH)
@ApplicationScoped
@Transactional
public class SaksnummerRestTjeneste {
    public static final String BASE_PATH = "/saksnummer";
    private static final String JSON_UTF8 = "application/json; charset=UTF-8";
    private static final Logger log = LoggerFactory.getLogger(SaksnummerRestTjeneste.class);

    private SaksnummerRepository saksnummerRepository;
    private SaksnummerAktorRepository saksnummerAktorRepository;

    public SaksnummerRestTjeneste() {// For Rest-CDI
    }

    @Inject
    public SaksnummerRestTjeneste(SaksnummerRepository saksnummerRepository, SaksnummerAktorRepository saksnummerAktorRepository) {
        this.saksnummerRepository = saksnummerRepository;
        this.saksnummerAktorRepository = saksnummerAktorRepository;
    }

    @POST
    @Path("/saksnummer/reserver")
    @Produces(JSON_UTF8)
    @Operation(description = "Reserver saksnummer.", summary = ("Reserver saksnummer"), tags = "fordel")
    @BeskyttetRessurs(action = BeskyttetRessursActionAttributt.CREATE, resource = FAGSAK)
    public SaksnummerDto reserverSaksnummer() {
        return new SaksnummerDto(saksnummerRepository.genererNyttSaksnummer());
    }

    @POST
    @Path("/saksnummer/aktor")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Kobler aktør til saksnummer.", summary = ("Kobler aktør til saksnummer"), tags = "fordel")
    @BeskyttetRessurs(action = BeskyttetRessursActionAttributt.CREATE, resource = FAGSAK)
    public Response kobleAktørPåSaksnummer(@NotNull @QueryParam("saksnummer") @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) Saksnummer saksnummer,
                                           @NotNull @QueryParam("aktørId") @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) AktørIdDto aktørId,
                                           @NotNull @QueryParam("journalpostId") @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) JournalpostId journalpostId) {
        saksnummerAktorRepository.lagre(saksnummer.getVerdi(), aktørId.getAktorId(), journalpostId.getVerdi());
        return Response.noContent().build();
    }
}
