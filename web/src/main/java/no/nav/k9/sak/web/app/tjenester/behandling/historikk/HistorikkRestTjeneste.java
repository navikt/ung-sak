package no.nav.k9.sak.web.app.tjenester.behandling.historikk;

import static no.nav.k9.abac.BeskyttetRessursKoder.FAGSAK;
import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.transaction.Transactional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import no.nav.k9.sak.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.k9.sak.historikk.HistorikkTjenesteAdapter;
import no.nav.k9.sak.kontrakt.behandling.SaksnummerDto;
import no.nav.k9.sak.web.server.abac.AbacAttributtSupplier;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.TilpassetAbacAttributt;

@Path(HistorikkRestTjeneste.PATH)
@ApplicationScoped
@Transactional
public class HistorikkRestTjeneste {

    public static final String PATH = "/historikk";

    private HistorikkTjenesteAdapter historikkTjeneste;

    public HistorikkRestTjeneste() {
        // Rest CDI
    }

    @Inject
    public HistorikkRestTjeneste(HistorikkTjenesteAdapter historikkTjeneste) {
        this.historikkTjeneste = historikkTjeneste;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Operation(description = "Henter alle historikkinnslag for en gitt sak.", tags = "historikk")
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response hentAlleInnslag(@Context HttpServletRequest request,
                                    @NotNull @QueryParam("saksnummer") @Parameter(description = "Saksnummer må være et eksisterende saksnummer") @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) SaksnummerDto saksnummerDto,
                                    @Context Request req) {

        CacheControl cc = new CacheControl();
        cc.setMaxAge((int) TimeUnit.HOURS.toSeconds(1));

        // FIXME XSS valider requestURL eller bruk relativ URL
        String requestURL = getRequestPath(request);
        String url = requestURL + "/dokument/hent-dokument";

        var saksnummer = saksnummerDto.getVerdi();
        var historikkinnslag = historikkTjeneste.finnHistorikkInnslag(saksnummer);

        if (!historikkinnslag.isEmpty()) {

            EntityTag etag = historikkinnslag.stream()
                .max(Historikkinnslag.COMP_REKKEFØLGE)
                .map(h -> new EntityTag(h.getUuid().toString()))
                .get();

            var rb = req.evaluatePreconditions(etag);

            if (rb == null) {
                // utsetter til vi har funnet etag her slik at vi unngår Saf kall. Men her er utdatert, så må gjøres.
                var dtoer = historikkTjeneste.mapTilDto(historikkinnslag, saksnummer);
                for (var dto : dtoer) {
                    for (var linkDto : dto.getDokumentLinks()) {
                        String journalpostId = linkDto.getJournalpostId();
                        String dokumentId = linkDto.getDokumentId();
                        UriBuilder uriBuilder = UriBuilder.fromPath(url);
                        uriBuilder.queryParam("journalpostId", journalpostId);
                        uriBuilder.queryParam("dokumentId", dokumentId);
                        linkDto.setUrl(uriBuilder.build());
                    }
                }
                return Response.ok(dtoer).cacheControl(cc).tag(etag).build();
            } else {
                return rb.cacheControl(cc).tag(etag).build();
            }
        } else {
            return Response.ok().entity(Collections.emptyList()).build();
        }
    }

    String getRequestPath(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append(request.getScheme())
            .append("://")
            .append(request.getLocalName())
            .append(":") // NOSONAR
            .append(request.getLocalPort());

        stringBuilder.append(request.getContextPath())
            .append(request.getServletPath());
        return stringBuilder.toString();
    }
}
