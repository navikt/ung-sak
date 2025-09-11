package no.nav.ung.sak.behandlingslager.etterlysning;

import jakarta.persistence.EntityManager;

import java.util.Objects;

public class ForhåndsvarslerRepository {

    private final EntityManager entityManager;

    public ForhåndsvarslerRepository(EntityManager entityManager) {
        Objects.requireNonNull(entityManager, "entityManager must not be null");
        this.entityManager = entityManager;
    }

    public void lagre() {}

    public void kopier() {}


}
