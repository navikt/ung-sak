package no.nav.ung.sak.web.server.typedresponse;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.Response;

import java.io.IOException;

/**
 * Prosesserer alle responser fra endepunkt som deklarerer at dei returnerer {@link TypedResponse}. Viss respons berre
 * implementerer {@link TypedResponse} skjer ingen prosessering. Viss den i tillegg har visse spesielle interface eller
 * er instanser av spesielle klasser vil responsen bli prosessert:
 * <p>
 * {@link SpecialEmptyResponse}: Respons objekt returnert i entitet blir "overført" til å bli gjeldande respons.
 * <p>
 * {@link ETaggableResponse}: ETag fra respons blir satt som ETag header i gjeldande respons.
 */
public class TypedResponseFilter implements ContainerResponseFilter {

    public TypedResponseFilter() {}

    /**
     * Overfører respons fra {@link SpecialEmptyResponse} til å bli gjeldande respons. Beheld evt. headers fra original
     * respons som ikkje er satt i {@link SpecialEmptyResponse}.
     * @param res original respons
     * @param specialResponse spesiell respons returnert frå endepunktet.
     */
    private void transferResponse(final ContainerResponseContext res, final Response specialResponse) {
        res.setEntity(null);
        res.setStatus(specialResponse.getStatus());
        final var resHeaders = res.getHeaders();
        final var specialHeaders = specialResponse.getHeaders();
        resHeaders.putAll(specialHeaders);
    }

    @Override
    public void filter(ContainerRequestContext req, ContainerResponseContext res) throws IOException {
        if(res.hasEntity()) {
            final var entity = res.getEntity();
            if(entity instanceof SpecialEmptyResponse<?> special) {
                final Response specialResponse = special.getSpecialResponse();
                transferResponse(res, specialResponse);
            }
            if(entity instanceof ETaggableResponse etaggable) {
                etaggable.etag().ifPresent(etag -> {
                    res.getHeaders().putSingle("ETag", etag);
                });
            }
            // Fleire spesialprosesseringar kan leggast til her ved behov.
        }
    }
}
