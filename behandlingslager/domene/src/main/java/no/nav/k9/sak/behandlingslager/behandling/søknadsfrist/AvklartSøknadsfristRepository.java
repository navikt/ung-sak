package no.nav.k9.sak.behandlingslager.behandling.søknadsfrist;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import no.nav.k9.felles.jpa.HibernateVerktøy;

@Dependent
public class AvklartSøknadsfristRepository {

    private EntityManager entityManager;

    @Inject
    public AvklartSøknadsfristRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public void lagreAvklaring(Long behandlingId, Set<AvklartKravDokument> avklartKravDokumenter) {

    }

    public Optional<AvklartSøknadsfristResultat> hentHvisEksisterer(Long behandlingId) {
        Objects.requireNonNull(behandlingId);
        return hentEksisterendeResultat(behandlingId);
    }

    public void lagreOverstyring(Long behandlingId, Set<AvklartKravDokument> overstyrtKravDokumenter) {
        var avklartSøknadsfristResultat = hentEksisterendeResultat(behandlingId);

        avklartSøknadsfristResultat.map(AvklartSøknadsfristResultat::getOverstyrtHolder)
            .map(KravDokumentHolder::getDokumenter)
            .orElse(null);

    }

    private AvklartSøknadsfristResultat lagreResultat(Long behandlingId, AvklartSøknadsfristResultat nyttResultat) {
        var eksisterendeGrunnlag = hentEksisterendeResultat(behandlingId);

        eksisterendeGrunnlag.ifPresent(this::deaktiver);
        lagre(behandlingId, nyttResultat);
        return nyttResultat;
    }

    private void deaktiver(AvklartSøknadsfristResultat resultat) {
        resultat.deaktiver();
        entityManager.persist(resultat);
        entityManager.flush();
    }

    public Optional<AvklartSøknadsfristResultat> hentGrunnlag(Long behandlingId) {
        return hentEksisterendeResultat(behandlingId);
    }

    private Optional<AvklartSøknadsfristResultat> hentEksisterendeResultat(Long behandlingId) {
        var query = entityManager.createQuery(
            "SELECT s " +
                "FROM AvklartSøknadsfristResultat s " +
                "WHERE s.behandlingId = :behandlingId " +
                "AND s.aktiv = true", AvklartSøknadsfristResultat.class);

        query.setParameter("behandlingId", behandlingId);

        return HibernateVerktøy.hentUniktResultat(query);
    }

    public void kopierGrunnlagFraEksisterendeBehandling(Long originalBehandlingId, Long nyBehandlingId) {
        var grunnlag = hentEksisterendeResultat(originalBehandlingId);
        grunnlag.ifPresent(entitet -> lagre(nyBehandlingId, new AvklartSøknadsfristResultat(entitet.getOverstyrtHolder(), entitet.getAvklartHolder())));
    }

    private void lagre(Long nyBehandlingId, AvklartSøknadsfristResultat avklartSøknadsfristResultat) {
        avklartSøknadsfristResultat.setBehandlingId(nyBehandlingId);
        if (avklartSøknadsfristResultat.getAvklartHolder() != null) {
            entityManager.persist(avklartSøknadsfristResultat.getAvklartHolder());
        }
        if (avklartSøknadsfristResultat.getOverstyrtHolder() != null) {
            entityManager.persist(avklartSøknadsfristResultat.getOverstyrtHolder());
        }
        entityManager.persist(avklartSøknadsfristResultat);
        entityManager.flush();
    }
}
