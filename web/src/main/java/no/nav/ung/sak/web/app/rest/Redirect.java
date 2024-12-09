package no.nav.ung.sak.web.app.rest;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.ung.sak.kontrakt.AsyncPollingStatus;
import no.nav.ung.sak.kontrakt.behandling.BehandlingUuidDto;
import no.nav.ung.sak.typer.Saksnummer;
import no.nav.ung.sak.web.app.tjenester.behandling.BehandlingRestTjeneste;
import no.nav.ung.sak.web.app.tjenester.fagsak.FagsakRestTjeneste;

public final class Redirect {
    private static final Logger log = LoggerFactory.getLogger(Redirect.class);

    private Redirect() {
        // no ctor
    }

    public static Response tilBehandlingPollStatus(HttpServletRequest request, UUID behandlingUuid, Optional<String> gruppeOpt)
        throws URISyntaxException {
        UriBuilder uriBuilder = getUriBuilder(request);
        uriBuilder.path(BehandlingRestTjeneste.STATUS_PATH);
        uriBuilder.queryParam(BehandlingUuidDto.NAME, behandlingUuid);
        gruppeOpt.ifPresent(s -> uriBuilder.queryParam("gruppe", s));
        return Response.accepted().location(honorXForwardedProto(request, uriBuilder.build())).build();
    }

    public static Response tilBehandlingPollStatus(HttpServletRequest request, UUID behandlingUuid) throws URISyntaxException {
        return tilBehandlingPollStatus(request, behandlingUuid, Optional.empty());
    }

    public static Response tilBehandlingEllerPollStatus(HttpServletRequest request, UUID behandlingUuid, AsyncPollingStatus status) throws URISyntaxException {
        UriBuilder uriBuilder = getUriBuilder(request);
        uriBuilder.path(BehandlingRestTjeneste.BEHANDLING_PATH);
        uriBuilder.queryParam(BehandlingUuidDto.NAME, behandlingUuid);
        return buildResponse(request, status, uriBuilder.build());
    }

    public static Response tilFagsakEllerPollStatus(HttpServletRequest request, Saksnummer saksnummer, AsyncPollingStatus status) throws URISyntaxException {
        UriBuilder uriBuilder = getUriBuilder(request);
        uriBuilder.path(FagsakRestTjeneste.PATH);
        uriBuilder.queryParam("saksnummer", saksnummer.getVerdi());
        return buildResponse(request, status, uriBuilder.build());
    }

    private static UriBuilder getUriBuilder(HttpServletRequest request) {
        UriBuilder uriBuilder = request == null || request.getContextPath() == null ? UriBuilder.fromUri("") : UriBuilder.fromUri(URI.create(request.getContextPath()));
        Optional.ofNullable(request.getServletPath()).ifPresent(c -> uriBuilder.path(c));
        return uriBuilder;
    }

    private static Response buildResponse(HttpServletRequest request, AsyncPollingStatus status, URI resultatUri) throws URISyntaxException {
        URI uri = honorXForwardedProto(request, resultatUri);
        if (status != null) {
            // sett alltid resultat-location i tilfelle timeout på klient
            status.setLocation(uri);
            return Response.status(status.getStatus().getHttpStatus()).entity(status).build();
        } else {
            return Response.seeOther(uri).build();
        }
    }

    /**
     * @see URI#create(String)
     */
    private static URI honorXForwardedProto(HttpServletRequest request, URI location) throws URISyntaxException {
        URI newLocation = null;
        if (relativLocationAndRequestAvailable(location)) {
            String xForwardedProto = getXForwardedProtoHeader(request);

            if (mismatchedScheme(xForwardedProto, request)) {
                String path = location.toString();
                if (path.startsWith("/")) { // NOSONAR
                    path = path.substring(1); // NOSONAR
                }
                URI baseUri = new URI(request.getRequestURI());
                try {
                    URI rewritten = new URI(xForwardedProto, baseUri.getSchemeSpecificPart(), baseUri.getFragment())
                        .resolve(path);
                    log.debug("Rewrote URI from '{}' to '{}'", location, rewritten);
                    newLocation = rewritten;
                } catch (URISyntaxException e) {
                    throw new IllegalArgumentException(e.getMessage(), e);
                }
            }
        }
        return newLocation != null ? newLocation : leggTilBaseUri(location);
    }

    private static boolean relativLocationAndRequestAvailable(URI location) {
        return location != null && !location.isAbsolute();
    }

    /**
     * @return http, https or null
     */
    private static String getXForwardedProtoHeader(HttpServletRequest httpRequest) {
        String xForwardedProto = httpRequest.getHeader("X-Forwarded-Proto");
        if ("https".equalsIgnoreCase(xForwardedProto) ||
            "http".equalsIgnoreCase(xForwardedProto)) {
            return xForwardedProto;
        }
        return null;
    }

    private static boolean mismatchedScheme(String xForwardedProto, HttpServletRequest httpRequest) {
        return xForwardedProto != null &&
            !xForwardedProto.equalsIgnoreCase(httpRequest.getScheme());
    }

    @SuppressWarnings("resource")
    private static URI leggTilBaseUri(URI resultatUri) {
        // tvinger resultatUri til å være en absolutt URI (passer med Location Header og Location felt når kommer i payload)
        Response response = Response.noContent().location(resultatUri).build();
        return response.getLocation();
    }
}
