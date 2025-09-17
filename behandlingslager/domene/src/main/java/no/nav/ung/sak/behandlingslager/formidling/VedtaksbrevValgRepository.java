package no.nav.ung.sak.behandlingslager.formidling;


import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import no.nav.k9.felles.jpa.HibernateVerktøy;
import no.nav.ung.kodeverk.dokument.DokumentMalType;

import java.util.List;
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

    public List<VedtaksbrevValgEntitet> finnVedtakbrevValg(Long behandlingId) {
        TypedQuery<VedtaksbrevValgEntitet> query = entityManager.createQuery(
                "SELECT v FROM VedtaksbrevValgEntitet v WHERE v.behandlingId = :behandlingId", VedtaksbrevValgEntitet.class)
            .setParameter("behandlingId", behandlingId);
        return query.getResultList();
    }

    public Optional<VedtaksbrevValgEntitet> finnVedtakbrevValg(Long behandlingId, DokumentMalType dokumentMalType) {
        TypedQuery<VedtaksbrevValgEntitet> query = entityManager.createQuery(
                "SELECT v FROM VedtaksbrevValgEntitet v WHERE v.behandlingId = :behandlingId and v.dokumentMalType = :dokumentMalType", VedtaksbrevValgEntitet.class)
            .setParameter("behandlingId", behandlingId)
            .setParameter("dokumentMalType", dokumentMalType);
        return HibernateVerktøy.hentUniktResultat(query);
    }
}
