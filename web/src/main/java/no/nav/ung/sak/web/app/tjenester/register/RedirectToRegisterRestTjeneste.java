package no.nav.ung.sak.web.app.tjenester.register;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import no.nav.k9.felles.integrasjon.rest.NoAuthRestClient;
import no.nav.k9.felles.integrasjon.rest.OidcRestClientResponseHandler;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessurs;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursResourceType;
import no.nav.k9.felles.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.ung.sak.domene.person.tps.TpsTjeneste;
import no.nav.ung.sak.kontrakt.behandling.SaksnummerDto;
import no.nav.ung.sak.web.server.abac.AbacAttributtSupplier;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.message.BasicHeader;

import java.io.IOException;
import java.net.URI;
import java.util.Optional;

import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionType.READ;

@ApplicationScoped
@Transactional
@Path(RedirectToRegisterRestTjeneste.BASE_PATH)
public class RedirectToRegisterRestTjeneste {
    public static final String BASE_PATH = "/register/redirect-to";

    private static final String AA_REG_POSTFIX = "/aa-reg";
    private static final String AINNTEKT_REG_POSTFIX = "/a-inntekt";
    public static final String AA_REG_PATH = BASE_PATH + AA_REG_POSTFIX;
    public static final String AINNTEKT_REG_PATH = BASE_PATH + AINNTEKT_REG_POSTFIX;

    private TpsTjeneste tpsTjeneste;
    private FagsakRepository fagsakRepository;
    private BehandlingRepository behandlingRepository;
    private NoAuthRestClient restClient;
    private String arbeidOgInntektBaseURL;

    RedirectToRegisterRestTjeneste() {
        // CDI
    }

    @Inject
    public RedirectToRegisterRestTjeneste(
        TpsTjeneste tpsTjeneste,
        FagsakRepository fagsakRepository, BehandlingRepository behandlingRepository,
        NoAuthRestClient restClient,
        @KonfigVerdi(value = "arbeid.og.inntekt.base.url", required = false, defaultVerdi = "https://arbeid-og-inntekt.nais.adeo.no") String arbeidOgInntektBaseURL) {
        this.tpsTjeneste = tpsTjeneste;
        this.fagsakRepository = fagsakRepository;
        this.behandlingRepository = behandlingRepository;
        this.restClient = restClient;
        this.arbeidOgInntektBaseURL = arbeidOgInntektBaseURL;
    }

    @GET
    @Operation(description = "Redirecter til aa-reg for arbeidstakeren", tags = "aktoer", responses = {
        @ApiResponse(responseCode = "307", description = "Redirecter til aa-reg for arbeidstakeren")
    })
    @BeskyttetRessurs(action = READ, resource = BeskyttetRessursResourceType.FAGSAK)
    @Path(AA_REG_POSTFIX)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response getAktoerInfo(
        @NotNull
        @QueryParam(SaksnummerDto.NAME)
        @Valid
        @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class)
            SaksnummerDto saksnummerDto) {
        Fagsak fagsak = fagsakRepository.hentSakGittSaksnummer(saksnummerDto.getVerdi()).get();
        var personIdent = tpsTjeneste.hentFnrForAktør(fagsak.getAktørId());

        var uri = URI.create(arbeidOgInntektBaseURL + "/api/v2/redirect/sok/arbeidstaker");

        HttpUriRequest request = new HttpGet(uri);
        request.addHeader(new BasicHeader("Nav-Personident", personIdent.getIdent()));
        try {
            var respons = restClient.execute(request, new OidcRestClientResponseHandler.StringResponseHandler(uri));
            var redirectUri = URI.create(respons);

            return Response.temporaryRedirect(redirectUri).build();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @GET
    @Operation(description = "Redirecter til a-inntekt for arbeidstakeren", tags = "aktoer", responses = {
        @ApiResponse(responseCode = "307", description = "Redirecter til a-inntekt for arbeidstakeren")
    })
    @BeskyttetRessurs(action = READ, resource = BeskyttetRessursResourceType.FAGSAK)
    @Path(AINNTEKT_REG_POSTFIX)
    public Response getAInntektUrl(
        @NotNull
        @QueryParam(SaksnummerDto.NAME)
        @Parameter(description = SaksnummerDto.DESC)
        @Valid
        @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class)
            SaksnummerDto saksnummerDto) {
        Fagsak fagsak = fagsakRepository.hentSakGittSaksnummer(saksnummerDto.getVerdi()).get();
        Optional<Behandling> sisteBehandling = behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(fagsak.getId());
        var personIdent = tpsTjeneste.hentFnrForAktør(fagsak.getAktørId());

        var uri = URI.create(arbeidOgInntektBaseURL + "/api/v2/redirect/sok/a-inntekt");
        HttpUriRequest request = new HttpGet(uri);
        request.addHeader(new BasicHeader("Nav-Personident", personIdent.getIdent()));
        request.addHeader(new BasicHeader("Nav-A-inntekt-Filter", "Ung"));
        sisteBehandling.ifPresent(b -> request.addHeader(new BasicHeader("Nav-Enhet",  b.getBehandlendeEnhet())));
        try {
            var respons = restClient.execute(request, new OidcRestClientResponseHandler.StringResponseHandler(uri));
            var redirectUri = URI.create(respons);

            return Response.temporaryRedirect(redirectUri).build();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
