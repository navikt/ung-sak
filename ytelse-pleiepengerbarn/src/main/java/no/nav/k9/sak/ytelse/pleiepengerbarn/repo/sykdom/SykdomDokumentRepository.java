package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import no.nav.k9.sak.typer.AktørId;

@Dependent
public class SykdomDokumentRepository {

    private EntityManager entityManager;
    private SykdomVurderingRepository sykdomVurderingRepository;

    
    @Inject
    public SykdomDokumentRepository(EntityManager entityManager, SykdomVurderingRepository sykdomVurderingRepository) {
        this.entityManager = Objects.requireNonNull(entityManager, "entityManager");
        this.sykdomVurderingRepository = Objects.requireNonNull(sykdomVurderingRepository, "sykdomVurderingRepository");
    }


    public List<SykdomDokument> hentAlleDokumenterFor(AktørId pleietrengende) {
        final TypedQuery<SykdomDokument> q = entityManager.createQuery(
                "SELECT d From SykdomDokument as d "
                + "inner join d.sykdomVurderinger as sv "
                + "inner join sv.person as p "
                + "where p.aktoerId = :aktoerId", SykdomDokument.class);
        
        q.setParameter("aktoerId", pleietrengende);
        
        return q.getResultList();
    }
    
    public Optional<SykdomDokument> hentDokument(Long dokumentId, AktørId pleietrengende) {
        final TypedQuery<SykdomDokument> q = entityManager.createQuery(
                "SELECT d From SykdomDokument as d "
                + "inner join d.sykdomVurderinger as sv "
                + "inner join sv.person as p "
                + "where p.aktoerId = :aktoerId"
                + "  and d.id = :dokumentId", SykdomDokument.class);
        
        q.setParameter("dokumentId", dokumentId);
        q.setParameter("aktoerId", pleietrengende);
        
        return q.getResultList().stream().findFirst();
    }
    
    public void lagre(SykdomDokument dokument, AktørId pleietrengende) {
        final SykdomVurderinger sykdomVurderinger = sykdomVurderingRepository.hentEllerLagreSykdomVurderinger(pleietrengende, dokument.getEndretAv(), dokument.getEndretTidspunkt());
        dokument.setSykdomVurderinger(sykdomVurderinger);
        
        entityManager.persist(dokument);
        entityManager.flush();
    }
    
    public void oppdater(SykdomDokument dokument) {
        if (dokument.getId() == null) {
            throw new IllegalStateException("Kan ikke oppdatere dokument som ikke har vært lagret før.");
        }
        entityManager.persist(dokument);
        entityManager.flush();
    }
}
