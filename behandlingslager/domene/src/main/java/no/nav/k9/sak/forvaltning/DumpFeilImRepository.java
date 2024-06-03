package no.nav.k9.sak.forvaltning;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.jpa.HibernateVerktøy;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

@Dependent
public class DumpFeilImRepository {

    private EntityManager entityManager;

    @Inject
    public DumpFeilImRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public void lagre(Long behandlingId,
                      Set<DatoIntervallEntitet> vilkårperioder,
                      Set<DatoIntervallEntitet> fordelperioder) {
        var eksisterende = hentEksisterendeGrunnlag(behandlingId);
        eksisterende.ifPresent(this::deaktiver);
        eksisterende.ifPresent(entityManager::persist);

        var vpEntiteter = vilkårperioder.stream().map(DumpFeilIMVilkårperiode::new).collect(Collectors.toSet());
        vpEntiteter.forEach(entityManager::persist);
        var fpEntiteter = fordelperioder.stream().map(DumpFeilIMFordelperiode::new).collect(Collectors.toSet());
        fpEntiteter.forEach(entityManager::persist);
        var ny = new DumpFeilIM(behandlingId,
            vpEntiteter,
            fpEntiteter);
        entityManager.persist(ny);
        entityManager.flush();
    }

    private Optional<DumpFeilIM> hentEksisterendeGrunnlag(Long behandlingId) {
        var query = entityManager.createQuery(
            "SELECT s " +
                "FROM DumpFeilIm s " +
                "WHERE s.behandlingId = :behandlingId " +
                "AND s.aktiv = true", DumpFeilIM.class);

        query.setParameter("behandlingId", behandlingId);

        return HibernateVerktøy.hentUniktResultat(query);
    }

    public List<DumpFeilIM> hentAlle() {
        var query = entityManager.createQuery(
            "SELECT s " +
                "FROM DumpFeilIm s " +
                "WHERE s.aktiv = true", DumpFeilIM.class);
        return query.getResultList();
    }



    private void deaktiver(DumpFeilIM it) {
        it.deaktiver();
        entityManager.persist(it);
        entityManager.flush();
    }


}
