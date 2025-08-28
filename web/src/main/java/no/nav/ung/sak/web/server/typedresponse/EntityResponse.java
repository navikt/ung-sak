package no.nav.ung.sak.web.server.typedresponse;

import jakarta.ws.rs.core.EntityTag;

import java.util.Objects;
import java.util.Optional;

/**
 * Standard implementasjon av {@link TypedResponse}. Brukast i vanleg tilfelle når endepunkt skal returnere deklarert respons type.
 * Gir også mulighet for å sette ETag på responsen.
 */
public class EntityResponse<E> implements TypedResponse<E>, ETaggableResponse {
    private final E entity;
    private final Optional<EntityTag> etag;

    public EntityResponse(final E entity, final Optional<EntityTag> etag) {
        this.entity = Objects.requireNonNull(entity);
        this.etag = Objects.requireNonNull(etag);
    }
    public EntityResponse(final E entity) {
        this(entity, Optional.empty());
    }

    public EntityResponse<E> withEtag(final Optional<EntityTag> etag) {
        return new EntityResponse<E>(this.entity, etag);
    }

    @Override
    public E getEntity() {
        return this.entity;
    }

    @Override
    public Optional<EntityTag> etag() {
        return this.etag;
    }
}
