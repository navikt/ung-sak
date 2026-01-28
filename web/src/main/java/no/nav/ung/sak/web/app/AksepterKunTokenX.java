package no.nav.ung.sak.web.app;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import no.nav.k9.felles.sikkerhet.jaspic.OidcTokenHolder;
import no.nav.k9.felles.sikkerhet.oidc.OidcTokenValidator;
import no.nav.k9.felles.sikkerhet.oidc.OidcTokenValidatorProvider;
import no.nav.k9.sikkerhet.oidc.config.OpenIDProvider;
import no.nav.k9.sikkerhet.oidc.token.internal.JwtUtil;

import java.util.Optional;

@Provider
@Priority(Priorities.AUTHENTICATION)
public class AksepterKunTokenX implements ContainerRequestFilter {

    @Context
    private ResourceInfo resourceinfo;

    public AksepterKunTokenX() {
        // Ingenting
    }

    @Override
    public void filter(ContainerRequestContext req) {
        Optional<OidcTokenHolder> token = getTokenFromHeader(req);
        if (token.isEmpty()) {
            throw new WebApplicationException("Token not found", Response.Status.UNAUTHORIZED);
        }
        String issuer = JwtUtil.getIssuer(token.get().getAccessToken());
        OidcTokenValidator tokenValidator = OidcTokenValidatorProvider.instance().getValidator(issuer);
        boolean erTokenX = tokenValidator.getProvider().equals(OpenIDProvider.TOKENX);
        if (!erTokenX) {
            throw new WebApplicationException("Må kalle med TokenX", Response.Status.FORBIDDEN);
        }
    }


    private Optional<OidcTokenHolder> getTokenFromHeader(ContainerRequestContext request) {
        String headerValue = request.getHeaderString(org.eclipse.jetty.http.HttpHeader.AUTHORIZATION.asString());
        return headerValue != null && headerValue.startsWith("Bearer ")
            ? Optional.of(new OidcTokenHolder(headerValue.substring("Bearer ".length()), false))
            : Optional.empty();
    }

}
