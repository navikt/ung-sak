package no.nav.k9.sak.behandlingslager.behandling.søknadsfrist;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import no.nav.k9.felles.jpa.HibernateVerktøy;

@Dependent
public class AvklartSøknadsfristRepository {

    private EntityManager entityManager;

    @Inject
    public AvklartSøknadsfristRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public void lagreAvklaring(Long behandlingId, Set<AvklartKravDokument> avklartKravDokumenter) {
        var avklartSøknadsfristResultat = hentEksisterendeResultat(behandlingId);
        var overstyrtHolder = avklartSøknadsfristResultat.flatMap(AvklartSøknadsfristResultat::getOverstyrtHolder)
            .orElse(null);

        var nyttResultat = new AvklartSøknadsfristResultat(overstyrtHolder, new KravDokumentHolder(avklartKravDokumenter));

        lagreResultat(behandlingId, nyttResultat);
    }

    public Optional<AvklartSøknadsfristResultat> hentHvisEksisterer(Long behandlingId) {
        Objects.requireNonNull(behandlingId);
        return hentEksisterendeResultat(behandlingId);
    }

    public void lagreOverstyring(Long behandlingId, Set<AvklartKravDokument> overstyrtKravDokumenter) {
        var avklartSøknadsfristResultat = hentEksisterendeResultat(behandlingId);

        var avklartKravDokuments = avklartSøknadsfristResultat.flatMap(AvklartSøknadsfristResultat::getAvklartHolder)
            .orElse(null);

        var nyttResultat = new AvklartSøknadsfristResultat(new KravDokumentHolder(overstyrtKravDokumenter), avklartKravDokuments);

        lagreResultat(behandlingId, nyttResultat);
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
        grunnlag.ifPresent(entitet -> lagre(nyBehandlingId, new AvklartSøknadsfristResultat(entitet.getOverstyrtHolder().orElse(null), entitet.getAvklartHolder().orElse(null))));
    }

    private void lagre(Long nyBehandlingId, AvklartSøknadsfristResultat avklartSøknadsfristResultat) {
        avklartSøknadsfristResultat.setBehandlingId(nyBehandlingId);
        if (avklartSøknadsfristResultat.getAvklartHolder().isPresent()) {
            entityManager.persist(avklartSøknadsfristResultat.getAvklartHolder().get());
        }
        if (avklartSøknadsfristResultat.getOverstyrtHolder().isPresent()) {
            entityManager.persist(avklartSøknadsfristResultat.getOverstyrtHolder().get());
        }
        entityManager.persist(avklartSøknadsfristResultat);
        entityManager.flush();
    }
}
