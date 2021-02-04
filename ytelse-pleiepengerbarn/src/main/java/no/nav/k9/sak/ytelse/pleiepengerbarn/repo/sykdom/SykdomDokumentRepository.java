package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.Query;
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
                + "where p.aktørId = :aktørId", SykdomDokument.class);

        q.setParameter("aktørId", pleietrengende);

        return q.getResultList();
    }

    public Optional<SykdomDokument> hentDokument(Long dokumentId, AktørId pleietrengende) {
        final TypedQuery<SykdomDokument> q = entityManager.createQuery(
                "SELECT d From SykdomDokument as d "
                + "inner join d.sykdomVurderinger as sv "
                + "inner join sv.person as p "
                + "where p.aktørId = :aktørId"
                + "  and d.id = :dokumentId", SykdomDokument.class);

        q.setParameter("dokumentId", dokumentId);
        q.setParameter("aktørId", pleietrengende);

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

    public SykdomInnleggelser hentInnleggelse(AktørId pleietrengende) {
        final TypedQuery<SykdomInnleggelser> q = entityManager.createQuery(
                "SELECT si " +
                    "FROM SykdomInnleggelser as si " +
                    "where si.versjon = " +
                    "(select max(si2.versjon) " +
                    "from SykdomInnleggelser as si2 " +
                        "inner join si2.vurderinger as sv2 " +
                        "inner join sv2.person as p " +
                    "where p.aktørId = :aktørId )", SykdomInnleggelser.class);
        q.setParameter("aktørId", pleietrengende);

        return q.getSingleResult();
    }

    public SykdomInnleggelser hentInnleggelse(UUID behandlingUUID) {
        final TypedQuery<SykdomInnleggelser> q = entityManager.createQuery(
                "SELECT gi " +
                "FROM SykdomGrunnlagBehandling as sgb " +
                    "inner join sgb.grunnlag as sg " +
                    "inner join sg.innleggelser as gi " +
                "where sgb.behandlingUuid = :behandlingUuid " +
                    "and sgb.versjon = " +
                    "( select max(sgb2.versjon) " +
                    "From SykdomGrunnlagBehandling as sgb2 " +
                    "where sgb2.behandlingUuid = sgb.behandlingUuid )"
            , SykdomInnleggelser.class);
        q.setParameter("behandlingUuid", behandlingUUID);

        return q.getSingleResult();
    }

    public void opprettEllerOppdaterInnleggelser(SykdomInnleggelser innleggelser, AktørId pleietrengende) {
        SykdomVurderinger vurderinger = sykdomVurderingRepository.hentEllerLagreSykdomVurderinger(pleietrengende, innleggelser.getOpprettetAv(), innleggelser.getOpprettetTidspunkt());
        innleggelser.setVurderinger(vurderinger);
        boolean lagNy = innleggelser.getVersjon() == null;
        if (lagNy) {
            opprettInnleggelser(innleggelser, pleietrengende);
        } else {
            oppdaterInnleggelser(innleggelser, pleietrengende);
        }
    }

    private void opprettInnleggelser(SykdomInnleggelser innleggelser, AktørId pleietrengende) {
        innleggelser.setVersjon(0L);
        entityManager.persist(innleggelser);
        entityManager.flush();
    }

    void oppdaterInnleggelser(SykdomInnleggelser innleggelser, AktørId pleietrengende) {
        final TypedQuery<SykdomInnleggelser> q = entityManager.createQuery(
            "Select si " +
                "FROM SykdomInnleggelser as si" +
                "   inner join si.vurderinger as sv " +
                "   inner join sv.person as sp " +
                "where si.versjon = :versjon " +
                "and sp.aktørId = :aktørId "
            , SykdomInnleggelser.class);
        q.setParameter("versjon", innleggelser.getVersjon());
        q.setParameter("aktørId", pleietrengende);

        try {
            SykdomInnleggelser eksisterende = q.getSingleResult();
        } catch (NoResultException | NonUniqueResultException e) {
            throw new IllegalStateException("Fant ikke unik SykdomInnleggelser å erstatte", e);
        }

        innleggelser.setVersjon(innleggelser.getVersjon()+1);

        entityManager.persist(innleggelser);
        entityManager.flush();
    }
}
