package no.nav.k9.sak.behandlingslager.behandling.søknadsfrist;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import no.nav.k9.felles.jpa.HibernateVerktøy;
import no.nav.k9.sak.trigger.ProsessTriggere;

@Dependent
public class AvklartSøknadsfristRepository {

    private EntityManager entityManager;

    @Inject
    public AvklartSøknadsfristRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public void lagreAvklaring(Long behandlingId, Set<AvklartKravDokument> avklartKravDokumenter) {

    }

    public void lagreOverstyring(Long behandlingId, Set<AvklartKravDokument> overstyrtKravDokumenter) {
        var avklartSøknadsfristResultat = hentEksisterendeGrunnlag(behandlingId);

        avklartSøknadsfristResultat.map(AvklartSøknadsfristResultat::getOverstyrtHolder)
            .map(KravDokumentHolder::getDokumenter)
            .orElse(null);

    }

    public Optional<AvklartSøknadsfristResultat> hentGrunnlag(Long behandlingId) {
        return hentEksisterendeGrunnlag(behandlingId);
    }

    private Optional<AvklartSøknadsfristResultat> hentEksisterendeGrunnlag(Long behandlingId) {
        var query = entityManager.createQuery(
            "SELECT s " +
                "FROM AvklartSøknadsfristResultat s " +
                "WHERE s.behandlingId = :behandlingId " +
                "AND s.aktiv = true", AvklartSøknadsfristResultat.class);

        query.setParameter("behandlingId", behandlingId);

        return HibernateVerktøy.hentUniktResultat(query);
    }
}
