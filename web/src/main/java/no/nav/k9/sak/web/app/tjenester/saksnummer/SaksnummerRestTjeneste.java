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
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessurs;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt;
import no.nav.k9.felles.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.k9.sak.behandlingslager.saksnummer.SaksnummerAktørKoblingRepository;
import no.nav.k9.sak.behandlingslager.saksnummer.SaksnummerAktørKoblingEntitet;
import no.nav.k9.sak.behandlingslager.saksnummer.SaksnummerRepository;
import no.nav.k9.sak.kontrakt.behandling.SaksnummerDto;
import no.nav.k9.sak.kontrakt.person.AktørIdDto;
import no.nav.k9.sak.kontrakt.saksnummer.SaksnummerAktørKoblingDto;
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
    private SaksnummerAktørKoblingRepository saksnummerAktørKoblingRepository;

    public SaksnummerRestTjeneste() {// For Rest-CDI
    }

    @Inject
    public SaksnummerRestTjeneste(SaksnummerRepository saksnummerRepository, SaksnummerAktørKoblingRepository saksnummerAktørKoblingRepository) {
        this.saksnummerRepository = saksnummerRepository;
        this.saksnummerAktørKoblingRepository = saksnummerAktørKoblingRepository;
    }

    @POST
    @Path("/reserver")
    @Produces(JSON_UTF8)
    @Operation(description = "Reserver saksnummer.", summary = ("Reserver saksnummer"), tags = "saksnummer")
    @BeskyttetRessurs(action = BeskyttetRessursActionAttributt.CREATE, resource = FAGSAK)
    public SaksnummerDto reserverSaksnummer() {
        return new SaksnummerDto(saksnummerRepository.genererNyttSaksnummer());
    }

    @POST
    @Path("/aktor")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Kobler aktør til saksnummer.", summary = ("Kobler aktør til saksnummer"), tags = "saksnummer")
    @BeskyttetRessurs(action = BeskyttetRessursActionAttributt.UPDATE, resource = FAGSAK)
    public Response kobleAktørPåSaksnummer(@NotNull @QueryParam("saksnummer") @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) Saksnummer saksnummer,
                                           @NotNull @QueryParam("aktørId") @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) AktørIdDto aktørId,
                                           @NotNull @QueryParam("journalpostId") @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) JournalpostId journalpostId) {
        saksnummerAktørKoblingRepository.lagre(saksnummer.getVerdi(), aktørId.getAktorId(), journalpostId.getVerdi());
        return Response.ok().build();
    }

    @GET
    @Path("/aktor")
    @Produces(JSON_UTF8)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Henter koblinger mellom aktør og saksnummer.", summary = ("Henter koblinger mellom aktør og saksnummer."), tags = "saksnummer")
    @BeskyttetRessurs(action = BeskyttetRessursActionAttributt.UPDATE, resource = FAGSAK)
    public Response hentSaksnummerKobling(@NotNull @QueryParam("saksnummer") @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) Saksnummer saksnummer,
                                          @NotNull @QueryParam("aktørId") @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) AktørIdDto aktørId,
                                          @NotNull @QueryParam("journalpostId") @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) JournalpostId journalpostId) {
        SaksnummerAktørKoblingEntitet kobling = saksnummerAktørKoblingRepository.hent(saksnummer.getVerdi(), aktørId.getAktorId(), journalpostId.getVerdi());
        if (kobling == null) {
            return Response.ok().build();
        }
        SaksnummerAktørKoblingDto dto = new SaksnummerAktørKoblingDto(kobling.getSaksnummer(), kobling.getAktørId(), kobling.getJournalpostId());
        return Response.ok(dto).build();
    }

    @POST
    @Path("/aktor/slett")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Sletter kobling mellom aktør og saksnummer.", summary = ("Sletter kobling mellom aktør og saksnummer."), tags = "saksnummer")
    @BeskyttetRessurs(action = BeskyttetRessursActionAttributt.UPDATE, resource = FAGSAK)
    public Response slettSaksnummerKobling(@NotNull @QueryParam("saksnummer") @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) Saksnummer saksnummer,
                                           @NotNull @QueryParam("aktørId") @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) AktørIdDto aktørId,
                                           @NotNull @QueryParam("journalpostId") @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) JournalpostId journalpostId) {
        saksnummerAktørKoblingRepository.slett(saksnummer.getVerdi(), aktørId.getAktorId(), journalpostId.getVerdi());
        return Response.ok().build();
    }
}