package no.nav.k9.sak.web.app.tjenester.forvaltning;

import static no.nav.k9.abac.BeskyttetRessursKoder.DRIFT;

import java.util.Objects;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessurs;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt;
import no.nav.k9.felles.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.k9.prosesstask.rest.AbacEmptySupplier;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.kontrakt.KortTekst;
import no.nav.k9.sak.kontrakt.behandling.SaksnummerDto;
import no.nav.k9.sak.web.app.tjenester.forvaltning.dump.DebugDumpsters;
import no.nav.k9.sak.web.app.tjenester.forvaltning.dump.logg.DiagnostikkFagsakLogg;
import no.nav.k9.sak.web.server.abac.AbacAttributtSupplier;

@Path(DiagnostikkRestTjeneste.BASE_PATH)
@ApplicationScoped
@Transactional
public class DiagnostikkRestTjeneste {

    static final String BASE_PATH = "/diagnostikk";
    private FagsakRepository fagsakRepository;
    private DebugDumpsters dumpsters;
    private EntityManager entityManager;

    DiagnostikkRestTjeneste() {
        // for proxy
    }

    @Inject
    DiagnostikkRestTjeneste(FagsakRepository fagsakRepository, EntityManager entityManager, DebugDumpsters dumpsters) {
        this.fagsakRepository = fagsakRepository;
        this.entityManager = entityManager;
        this.dumpsters = dumpsters;
    }

    @POST
    @Path("/sak")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Operation(description = "Henter en dump av info for debugging og analyse av en sak. Logger hvem som har hatt innsyn i sak", summary = ("Henter en dump av info for debugging og analyse av en sak"), tags = "forvaltning")
    @BeskyttetRessurs(action = BeskyttetRessursActionAttributt.READ, resource = DRIFT)
    public Response dumpSak(@NotNull @FormParam("saksnummer") @Parameter(description = "saksnummer", allowEmptyValue = false, required = true, schema = @Schema(type = "string", maximum = "10")) @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) SaksnummerDto saksnummerDto,
                            @NotNull @FormParam("begrunnelse") @Parameter(description = "begrunnelse", allowEmptyValue = false, required = true, schema = @Schema(type = "string", maximum = "2000")) @Valid @TilpassetAbacAttributt(supplierClass = AbacEmptySupplier.class) KortTekst begrunnelse) {

        var saksnummer = Objects.requireNonNull(saksnummerDto.getVerdi());
        var fagsakOpt = fagsakRepository.hentSakGittSaksnummer(saksnummer);
        if (fagsakOpt.isEmpty()) {
            return Response.status(Status.BAD_REQUEST.getStatusCode(), "Fant ikke fagsak for angitt saksnummer").build();
        }
        var fagsak = fagsakOpt.get();
        /*
         * logg tilgang til tabell - må gjøres før dumps (siden StreamingOutput ikke kjører i scope av denne metoden på stacken,
         * og derfor ikke har nytte av @Transactional.
         */
        entityManager.persist(new DiagnostikkFagsakLogg(fagsak.getId(), BASE_PATH + "/sak", begrunnelse.getTekst()));
        entityManager.flush();

        var streamingOutput = dumpsters.dumper(fagsak);

        return Response.ok(streamingOutput)
            .type(MediaType.APPLICATION_OCTET_STREAM)
            .header("Content-Disposition", String.format("attachment; filename=\"%s-%s-v%s.zip\"", fagsak.getYtelseType(), saksnummer.getVerdi(), fagsak.getVersjon()))
            .build();

    }

}
