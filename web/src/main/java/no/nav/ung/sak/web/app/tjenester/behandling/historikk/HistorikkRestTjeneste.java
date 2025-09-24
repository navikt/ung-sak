package no.nav.ung.sak.web.app.tjenester.behandling.historikk;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.EntityTag;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessurs;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursResourceType;
import no.nav.k9.felles.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.ung.sak.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.ung.sak.historikk.HistorikkTjenesteAdapter;
import no.nav.ung.sak.kontrakt.behandling.SaksnummerDto;
import no.nav.ung.sak.kontrakt.historikk.HistorikkinnslagDto;
import no.nav.ung.sak.web.server.abac.AbacAttributtSupplier;
import no.nav.ung.sak.web.server.typedresponse.EntityResponse;
import no.nav.ung.sak.web.server.typedresponse.SpecialEmptyResponse;
import no.nav.ung.sak.web.server.typedresponse.TypedResponse;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionType.READ;

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
    @BeskyttetRessurs(action = READ, resource = BeskyttetRessursResourceType.FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public TypedResponse<List<HistorikkinnslagDto>> hentAlleInnslag(@Context HttpServletRequest request,
                                                                    @NotNull @QueryParam("saksnummer") @Parameter(description = "Saksnummer må være et eksisterende saksnummer") @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) SaksnummerDto saksnummerDto,
                                                                    @Context Request req) {

        // FIXME XSS valider requestURL eller bruk relativ URL
        String requestURL = getRequestPath(request);
        String url = requestURL + "/dokument/hent-dokument";

        var saksnummer = saksnummerDto.getVerdi();
        var historikkinnslag = historikkTjeneste.finnHistorikkInnslag(saksnummer);
        final Optional<EntityTag> etag = historikkinnslag.stream()
            .max( Comparator.comparing(Historikkinnslag::getOpprettetTidspunkt))
            .map(h -> new EntityTag(h.getUuid().toString()));

        if(etag.isPresent()) {
            final Response.ResponseBuilder rb = req.evaluatePreconditions(etag.get());
            if(rb != null) {
                return new SpecialEmptyResponse<>(rb.build());
            }
        }

        // utsetter til vi har funnet etag her slik at vi unngår Saf kall. Men her er utdatert, så må gjøres.
        var dtoer = historikkTjeneste.mapTilDto(historikkinnslag, saksnummer);
        for (var dto : dtoer) {
            for (var linkDto : dto.dokumenter()) {
                String journalpostId = linkDto.getJournalpostId();
                String dokumentId = linkDto.getDokumentId();
                UriBuilder uriBuilder = UriBuilder.fromPath(url);
                uriBuilder.queryParam("journalpostId", journalpostId);
                uriBuilder.queryParam("dokumentId", dokumentId);
                linkDto.setUrl(uriBuilder.build());
            }
        }
        return new EntityResponse<>(dtoer, etag);
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
