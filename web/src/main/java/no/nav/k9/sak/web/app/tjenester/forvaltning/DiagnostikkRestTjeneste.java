package no.nav.k9.sak.web.app.tjenester.forvaltning;

import static no.nav.k9.abac.BeskyttetRessursKoder.FAGSAK;

import java.util.Objects;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
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
import no.nav.k9.sak.web.server.abac.AbacAttributtSupplier;

@Path("/diagnostikk")
@ApplicationScoped
@Transactional
public class DiagnostikkRestTjeneste {

    private FagsakRepository fagsakRepository;
    private DebugDumpsters dumpsters;

    DiagnostikkRestTjeneste() {
        // for proxy
    }

    @Inject
    DiagnostikkRestTjeneste(FagsakRepository fagsakRepository, DebugDumpsters dumpsters) {
        this.fagsakRepository = fagsakRepository;
        this.dumpsters = dumpsters;
    }

    @POST
    @Path("/sak")
    @Consumes(MediaType.TEXT_PLAIN)
    @Operation(description = "Henter en dump av info for debugging og analyse av en sak. Logger hvem som har hatt innsyn i sak", summary = ("Henter en dump av info for debugging og analyse av en sak"), tags = "forvaltning")
    @BeskyttetRessurs(action = BeskyttetRessursActionAttributt.READ, resource = FAGSAK)
    public Response dumpSak(@NotNull @QueryParam("saksnummer") @Parameter(description = "saksnummer") @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) SaksnummerDto saksnummerDto) {

        var saksnummer = Objects.requireNonNull(saksnummerDto.getVerdi());
        var fagsak = fagsakRepository.hentSakGittSaksnummer(saksnummer).orElseThrow(() -> new IllegalArgumentException("Fant ikke fagsak for saksnummer=" + saksnummer));

        var streamingOutput = dumpsters.dumper(fagsak);

        return Response.ok(streamingOutput)
            .type(MediaType.TEXT_PLAIN)
            .header("Content-Disposition", String.format("attachment; filename=\"%s_v%s.zip\"", saksnummer.getVerdi(), fagsak.getVersjon()))
            .build();

    }

}
