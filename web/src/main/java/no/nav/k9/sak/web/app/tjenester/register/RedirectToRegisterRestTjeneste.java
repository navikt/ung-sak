package no.nav.k9.sak.web.app.tjenester.register;

import static no.nav.k9.abac.BeskyttetRessursKoder.FAGSAK;
import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;

import java.net.URI;
import java.util.Set;

import org.apache.http.message.BasicHeader;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import no.nav.k9.felles.integrasjon.rest.OidcRestClient;
import no.nav.k9.felles.integrasjon.saf.Sak;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessurs;
import no.nav.k9.felles.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.domene.person.tps.TpsTjeneste;
import no.nav.k9.sak.kontrakt.behandling.BehandlingUuidDto;
import no.nav.k9.sak.kontrakt.behandling.SaksnummerDto;
import no.nav.k9.sak.kontrakt.person.AktørIdDto;
import no.nav.k9.sak.kontrakt.person.AktørInfoDto;
import no.nav.k9.sak.web.server.abac.AbacAttributtSupplier;

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
    private OidcRestClient restClient;
    private String arbeidOgInntektBaseURL;

    RedirectToRegisterRestTjeneste() {
        // CDI
    }

    @Inject
    public RedirectToRegisterRestTjeneste(
        TpsTjeneste tpsTjeneste,
        BehandlingRepository behandlingRepository,
        FagsakRepository fagsakRepository, OidcRestClient restClient,
        @KonfigVerdi(value = "aareg.base.url", required = false, defaultVerdi = "https://arbeid-og-inntekt.nais.adeo.no") String arbeidOgInntektBaseURL) {
        this.tpsTjeneste = tpsTjeneste;
        this.fagsakRepository = fagsakRepository;
        this.restClient = restClient;
        this.arbeidOgInntektBaseURL = arbeidOgInntektBaseURL;
    }

    @GET
    @Operation(description = "Redirecter til aa-reg for arbeidstakeren", tags = "aktoer", responses = {
        @ApiResponse(responseCode = "200", description = "Redirecter til aa-reg for arbeidstakeren", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = SaksnummerDto.class)))
    })
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
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

        var redirectUrl = restClient.get(uri, Set.of(new BasicHeader("Nav-Personident", personIdent.getIdent())), String.class);

        var redirectUri = URI.create(redirectUrl);
        return Response.temporaryRedirect(redirectUri).build();
    }

    @GET
    @Operation(description = "Redirecter til a-inntekt for arbeidstakeren", tags = "aktoer", responses = {
        @ApiResponse(responseCode = "200", description = "Redirecter til a-inntekt for arbeidstakeren", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = SaksnummerDto.class)))
    })
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    @Path(AINNTEKT_REG_POSTFIX)
    public Response getAInntektUrl(
            @NotNull
            @QueryParam(SaksnummerDto.NAME)
            @Parameter(description = SaksnummerDto.DESC)
            @Valid
            @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class)
            SaksnummerDto saksnummerDto) {
        Fagsak fagsak = fagsakRepository.hentSakGittSaksnummer(saksnummerDto.getVerdi()).get();
        var personIdent = tpsTjeneste.hentFnrForAktør(fagsak.getAktørId());

        var uri = URI.create(arbeidOgInntektBaseURL + "/api/v2/redirect/sok/a-inntekt");
        var redirectUrl = restClient.get(uri, Set.of(new BasicHeader("Nav-Personident", personIdent.getIdent())), String.class);

        var redirectUri = URI.create(redirectUrl);
        return Response.temporaryRedirect(redirectUri).build();
    }
}
