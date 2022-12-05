package no.nav.k9.sak.web.app;

import static jakarta.ws.rs.core.NewCookie.DEFAULT_MAX_AGE;
import static no.nav.k9.felles.sikkerhet.Constants.ID_TOKEN_COOKIE_NAME;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.felles.oidc.config.ServerInfo;
import no.nav.k9.felles.oidc.ressurs.ExtractRequestDataHelp;
import no.nav.k9.felles.sikkerhet.ContextPathHolder;
import no.nav.k9.felles.util.Tuple;
import no.nav.k9.sikkerhet.oidc.config.OpenIDProvider;
import no.nav.k9.sikkerhet.oidc.token.bruker.BrukerTokenProvider;

@Path("/login")
@ApplicationScoped
public class FrontendLoginResource {

    private final ExtractRequestDataHelp requestDataHelp = new ExtractRequestDataHelp();
    private List<Tuple<String, String>> optionalScopes;
    private BrukerTokenProvider brukerTokenProvider;

    public FrontendLoginResource() {
    }

    @Inject
    public FrontendLoginResource(@KonfigVerdi(value = "app.auth.schema.azuread.optional.paths", required = false, defaultVerdi = "") String optionalScopes, BrukerTokenProvider brukerTokenProvider) {
        this.optionalScopes = hentUtPathMedScope(optionalScopes, ContextPathHolder.instance().getContextPath());
        this.brukerTokenProvider = brukerTokenProvider;
    }

    @GET
    public Response login(@QueryParam("redirectTo") @DefaultValue("/k9/web/") String redirectTo, @QueryParam("original") @DefaultValue("/k9/sak") String originalUri, @Context HttpServletRequest httpServletRequest) {
        var uri = URI.create(redirectTo);
        var relativePath = "";
        if (uri.getPath() != null) {
            relativePath += uri.getPath();
        }
        if (uri.getQuery() != null) {
            relativePath += '?' + uri.getQuery();
        }
        if (uri.getFragment() != null) {
            relativePath += '#' + uri.getFragment();
        }
        if (!relativePath.startsWith("/")) {
            relativePath = "/" + relativePath;
        }
        var responseBuilder = Response.status(307);
        if (originalUri != null && !originalUri.startsWith("/k9/sak") && brukerTokenProvider.getToken().getIssuer() == OpenIDProvider.AZUREAD) {
            var tuple = findScopeForUrl(originalUri);
            if (tuple != null) {
                var requestedDomain = requestDataHelp.requestedHostWithScheme(httpServletRequest);
                var token = brukerTokenProvider.getToken(tuple.getElement2());
                String cookieDomain = ServerInfo.instance().getValidCookieDomain(requestedDomain);
                responseBuilder.cookie(new NewCookie(ID_TOKEN_COOKIE_NAME, token.getToken(), tuple.getElement1(), cookieDomain, "", DEFAULT_MAX_AGE, true, true));
            }
        }
        //  når vi har kommet hit, er brukeren innlogget og har fått ID-token. Kan da gjøre redirect til hovedsiden for VL
        return responseBuilder.header(HttpHeaders.LOCATION, relativePath).build();
    }

    private Tuple<String, String> findScopeForUrl(String originalUri) {
        var optionalScope = optionalScopes.stream().filter(it -> originalUri.startsWith(it.getElement1())).findFirst();
        return optionalScope.orElse(null);
    }

    List<Tuple<String, String>> hentUtPathMedScope(String configString, String cookiePath) {
        if (configString == null) {
            return List.of();
        }
        return Arrays.stream(configString.split(";"))
            .map(it -> trekkUtTilTuple(it, cookiePath))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    private Tuple<String, String> trekkUtTilTuple(String it, String cookiePath) {
        if (it == null || it.isEmpty()) {
            return null;
        }
        var split = it.split(",");
        if (split.length == 2) {
            var path = split[0].trim();
            var scope = split[1].trim();

            if (!path.startsWith("/k9/") || Objects.equals("/k9/", path) || Objects.equals("/k9", path) || Objects.equals(cookiePath, path)) {
                throw new IllegalStateException("Ugyldig path");
            }

            return new Tuple<>(path, scope);
        }
        if (split.length == 0) {
            return null;
        }
        throw new IllegalStateException();
    }
}
