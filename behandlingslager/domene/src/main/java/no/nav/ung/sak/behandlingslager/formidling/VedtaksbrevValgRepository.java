package no.nav.ung.sak.behandlingslager.formidling;


import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import no.nav.k9.felles.jpa.HibernateVerktøy;

import java.util.Optional;

@Dependent
public class VedtaksbrevValgRepository {

    private final EntityManager entityManager;

    @Inject
    public VedtaksbrevValgRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public VedtaksbrevValgEntitet lagre(VedtaksbrevValgEntitet vedtaksbrevValgEntitet) {
        entityManager.persist(vedtaksbrevValgEntitet);
        entityManager.flush();
        return vedtaksbrevValgEntitet;
    }

    public Optional<VedtaksbrevValgEntitet> finnVedtakbrevValg(Long behandlingId) {
        TypedQuery<VedtaksbrevValgEntitet> query = entityManager.createQuery(
                "SELECT v FROM VedtaksbrevValgEntitet v WHERE v.behandlingId = :behandlingId and aktiv = true", VedtaksbrevValgEntitet.class)
            .setParameter("behandlingId", behandlingId);
        return HibernateVerktøy.hentUniktResultat(query);
    }
}
