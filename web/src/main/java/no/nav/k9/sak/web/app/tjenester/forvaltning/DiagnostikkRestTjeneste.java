package no.nav.k9.sak.web.app.tjenester.forvaltning;

import static no.nav.k9.abac.BeskyttetRessursKoder.DRIFT;

import java.util.Objects;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.jetbrains.annotations.NotNull;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessurs;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt;
import no.nav.k9.felles.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.kontrakt.behandling.SaksnummerDto;
import no.nav.k9.sak.web.app.tjenester.forvaltning.dump.DebugDumpsters;
import no.nav.k9.sak.web.app.tjenester.forvaltning.dump.logg.DiagnostikkFagsakLogg;
import no.nav.k9.sak.web.server.abac.AbacAttributtSupplier;

@Path("/diagnostikk")
@ApplicationScoped
@Transactional
public class DiagnostikkRestTjeneste {

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
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Operation(description = "Henter en dump av info for debugging og analyse av en sak. Logger hvem som har hatt innsyn i sak", summary = ("Henter en dump av info for debugging og analyse av en sak"), tags = "forvaltning")
    @BeskyttetRessurs(action = BeskyttetRessursActionAttributt.READ, resource = DRIFT)
    public Response dumpSak(@NotNull @QueryParam("saksnummer") @Parameter(description = "saksnummer") @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) SaksnummerDto saksnummerDto) {

        var saksnummer = Objects.requireNonNull(saksnummerDto.getVerdi());
        var fagsak = fagsakRepository.hentSakGittSaksnummer(saksnummer).orElseThrow(() -> new IllegalArgumentException("Fant ikke fagsak for saksnummer=" + saksnummer));

        /*
         * logg tilgang til tabell - må gjøres før dumps (siden StreamingOutput ikke kjører i scope av denne metoden på stacken,
         * og derfor ikke har nytte av @Transactional.
         */
        entityManager.persist(new DiagnostikkFagsakLogg(fagsak.getId()));
        entityManager.flush();

        var streamingOutput = dumpsters.dumper(fagsak);

        return Response.ok(streamingOutput)
            .type(MediaType.APPLICATION_OCTET_STREAM)
            .header("Content-Disposition", String.format("attachment; filename=\"%s-%s-v%s.zip\"", fagsak.getYtelseType(), saksnummer.getVerdi(), fagsak.getVersjon()))
            .build();

    }

}
