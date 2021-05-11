package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.unntaketablerttilsyn;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import java.util.Objects;

@Dependent
public class UnntakEtablertTilsynRepository {


    private EntityManager entityManager;

    @Inject
    public UnntakEtablertTilsynRepository(EntityManager entityManager) {
        Objects.requireNonNull(entityManager, "entityManager"); //$NON-NLS-1$
        this.entityManager = entityManager;
    }


    UnntakEtablertTilsyn hent(Long id) {
        return entityManager.find(UnntakEtablertTilsyn.class, id);
    }

    public Long lagre(UnntakEtablertTilsyn unntakEtablertTilsyn) {
        entityManager.persist(unntakEtablertTilsyn);
        entityManager.flush();
        return unntakEtablertTilsyn.getId();
    }


}
