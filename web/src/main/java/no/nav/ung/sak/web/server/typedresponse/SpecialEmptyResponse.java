package no.nav.ung.sak.web.server.typedresponse;

import jakarta.ws.rs.core.Response;

import java.util.Objects;

/**
  * Brukes når ein metode som normalt returnerer ein {@link TypedResponse} i staden må returnere ein tom respons med spesielle headers/status.
  * Returner då en instans av denne i stedet, som deretter vil bli fanga av {@link TypedResponseFilter} og "overført" til å bli gjeldande respons.
 * <p>
 *     Brukast for eksempel viss ein skal returnere 304 Not Modified som svar på request med matchande etag.
 * </p>
  */
public final class SpecialEmptyResponse<T> implements TypedResponse<T> {
    private final Response specialResponse;

    public SpecialEmptyResponse(final Response specialResponse) {
        this.specialResponse = Objects.requireNonNull(specialResponse);
        // The specialResponse is not allowed to have an entity.
        if (specialResponse.hasEntity()) {
            throw new IllegalArgumentException("SpecialEmptyResponse must not have an entity");
        }
        if(specialResponse.getStatus() == 200) {
            throw new IllegalArgumentException("SpecialEmptyResponse must not have status 200. Would violate openapi contract of TypedResponse.");
        }
    }

    public Response getSpecialResponse() {
        return this.specialResponse;
    }

    @Override
    public T getEntity() {
        throw new IllegalCallerException("SpecialEmptyResponse does not have entity. Implements TypedResponse to be compatible, but it is expected that the response is filtered and replaced before this is called.");
    }
}
