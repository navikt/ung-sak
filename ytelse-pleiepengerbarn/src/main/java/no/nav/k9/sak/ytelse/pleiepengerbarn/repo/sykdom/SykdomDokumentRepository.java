package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

import no.nav.k9.sak.kontrakt.sykdom.dokument.SykdomDokumentType;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.JournalpostId;

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

        List<SykdomDokument> dokuments = q.getResultList();

        return dokuments;
    }

    public boolean isDokumentBruktIVurdering(Long dokumentId) {
        final TypedQuery<SykdomVurderingVersjon> q = entityManager.createQuery(
            "SELECT vv From SykdomVurderingVersjon as vv "
                + "inner join vv.dokumenter as d "
                + "where d.id = :dokumentId", SykdomVurderingVersjon.class);
        q.setParameter("dokumentId", dokumentId);

        return q.getResultList().size() > 0;
    }

    public List<SykdomDokument> hentNyeDokumenterFor(UUID behandlingUUID) {
        final TypedQuery<SykdomDokument> q = entityManager.createQuery(
            "SELECT d From SykdomDokument as d "
                + "where d.behandlingUuid = :behandlingUUID", SykdomDokument.class);

        q.setParameter("behandlingUUID", behandlingUUID);

        List<SykdomDokument> dokuments = q.getResultList();

        return dokuments;
    }

    public List<SykdomDokument> hentDokumenterSomErRelevanteForSykdom(AktørId pleietrengende) {
        return hentAlleDokumenterFor(pleietrengende)
            .stream()
            .filter(d -> d.getType().isRelevantForSykdom())
            .collect(Collectors.toList());
    }

    public List<SykdomDokument> hentDuplikaterAv(Long dokumentId) {
        final TypedQuery<SykdomDokument> q = entityManager.createQuery(
            "SELECT d From SykdomDokument as d "
                + "inner join d.informasjoner as i "
                + "inner join i.duplikatAvDokument as dd "
                + "where dd.id = :dokumentId"
                + "  and i.versjon = ("
                + "    select max(i2.versjon) "
                + "    From SykdomDokumentInformasjon as i2 "
                + "    where i2.dokument = i.dokument"
                + "  )", SykdomDokument.class);

        q.setParameter("dokumentId", dokumentId);
        return q.getResultList();
    }

    public List<SykdomDokument> hentDokumentSomIkkeHarOppdatertEksisterendeVurderinger(AktørId pleietrengende) {
        return hentDokumenterSomErRelevanteForSykdom(pleietrengende)
            .stream()
            .filter(d -> !harKvittertDokumentForEksisterendeVurderinger(d))
            .collect(Collectors.toList());
    }

    public List<SykdomDokument> hentGodkjenteLegeerklæringer(AktørId pleietrengende) {
        return hentAlleDokumenterFor(pleietrengende).stream().filter(d -> d.getType() == SykdomDokumentType.LEGEERKLÆRING_SYKEHUS && d.getDuplikatAvDokument() == null).collect(Collectors.toList());
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

        Optional<SykdomDokument> dokument = q.getResultList().stream().findFirst();

        return dokument;
    }
    
    public boolean finnesSykdomDokument(JournalpostId journalpostId, String dokumentInfoId) {
        Objects.requireNonNull(journalpostId, "journalpostId");
        
        final String dokumentInfoSjekk = (dokumentInfoId == null) ? "d.dokumentInfoId IS NULL" : "d.dokumentInfoId = :dokumentInfoId";
        
        final TypedQuery<SykdomDokument> q = entityManager.createQuery(
                "SELECT d From SykdomDokument as d "
                    + "where d.journalpostId = :journalpostId"
                    + "  and " + dokumentInfoSjekk, SykdomDokument.class);

        q.setParameter("journalpostId", journalpostId);
        if (dokumentInfoId != null) {
            q.setParameter("dokumentInfoId", dokumentInfoId);
        }

        Optional<SykdomDokument> dokument = q.getResultList().stream().findFirst();

        return dokument.isPresent();
    }

    public void lagre(SykdomDokument dokument, AktørId pleietrengende) {
        if (dokument.getId() != null) {
            throw new IllegalStateException("Dokumentet har allerede blitt lagret.");
        }
        final SykdomVurderinger sykdomVurderinger = sykdomVurderingRepository.hentEllerLagreSykdomVurderinger(pleietrengende, dokument.getEndretAv(), dokument.getEndretTidspunkt());
        dokument.setSykdomVurderinger(sykdomVurderinger);

        entityManager.persist(dokument);
        entityManager.flush();
    }

    public void oppdater(SykdomDokumentInformasjon dokumentInformasjon) {
        if (dokumentInformasjon.getDokument().getId() == null) {
            throw new IllegalStateException("Kan ikke oppdatere dokument som ikke har vært lagret før.");
        }

        entityManager.persist(dokumentInformasjon);
        entityManager.flush();
    }

    public boolean harKvittertDokumentForEksisterendeVurderinger(SykdomDokument dokument) {
        TypedQuery<SykdomDokumentHarOppdatertEksisterendeVurderinger> q =
            entityManager.createQuery(
                "SELECT k " +
                    "FROM SykdomDokumentHarOppdatertEksisterendeVurderinger as k " +
                    "WHERE k.id = :id", SykdomDokumentHarOppdatertEksisterendeVurderinger.class);

        q.setParameter("id", dokument.getId());

        return q.getResultList().stream().findFirst().isPresent();
    }

    public void kvitterDokumenterMedOppdatertEksisterendeVurderinger(SykdomDokumentHarOppdatertEksisterendeVurderinger utkvittering) {

        String sql = "INSERT INTO sykdom_dokument_har_oppdatert_eksisterende_vurderinger " +
            "(sykdom_dokument_id, opprettet_av, opprettet_tid) " +
            "VALUES(:id, :opprettetAv, :opprettetTidspunkt) " +
            "ON CONFLICT DO NOTHING";

        Query query = entityManager.createNativeQuery(sql)
            .setParameter("id", utkvittering.getId())
            .setParameter("opprettetAv", utkvittering.getOpprettetAv())
            .setParameter("opprettetTidspunkt", utkvittering.getOpprettetTidspunkt());

        query.executeUpdate();
        entityManager.flush();
    }

    public SykdomInnleggelser hentInnleggelse(AktørId pleietrengende) {
        var sykdomInnleggelser = hentInnleggelseOrNull(pleietrengende);
        if (sykdomInnleggelser != null) {
            return sykdomInnleggelser;
        }
        return new SykdomInnleggelser(null, null, Collections.emptyList(), null, null);
    }

    SykdomInnleggelser hentInnleggelseOrNull(AktørId pleietrengende) {
        final TypedQuery<SykdomInnleggelser> q = entityManager.createQuery(
            "SELECT si "
                + "FROM SykdomInnleggelser as si "
                + "  inner join si.vurderinger as sv "
                + "  inner join sv.person as p "
                + "where si.versjon = ( "
                + "  select max(si2.versjon) "
                + "  from SykdomInnleggelser as si2 "
                + "  where si2.vurderinger = si.vurderinger "
                + ") "
                + "and p.aktørId = :aktørId", SykdomInnleggelser.class);
        q.setParameter("aktørId", pleietrengende);

        final List<SykdomInnleggelser> result = q.getResultList();
        if (result.isEmpty()) {
            return null;
        }
        if (result.size() != 1) {
            throw new IllegalStateException("Forventer maksimalt én rad som svar.");
        }
        return result.get(0);
    }

    public SykdomInnleggelser hentInnleggelse(UUID behandlingUUID) {
        var sykdomInnleggelser = hentInnleggelseOrNull(behandlingUUID);
        if (sykdomInnleggelser != null) {
            return sykdomInnleggelser;
        }
        return new SykdomInnleggelser(null, null, Collections.emptyList(), null, null);
    }

    public SykdomInnleggelser hentInnleggelseOrNull(UUID behandlingUUID) {
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

        final List<SykdomInnleggelser> result = q.getResultList();
        if (result.isEmpty()) {
            return null;
        }
        if (result.size() != 1) {
            throw new IllegalStateException("Forventer maksimalt én rad som svar.");
        }
        return result.get(0);
    }

    public void opprettEllerOppdaterInnleggelser(SykdomInnleggelser innleggelser, AktørId pleietrengende) {
        if (innleggelser.getId() != null) {
            throw new IllegalStateException("Innleggelser skal aldri oppdateres. Man skal alltid inserte ny");
        }
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

    private void oppdaterInnleggelser(SykdomInnleggelser innleggelser, AktørId pleietrengende) {
        final TypedQuery<SykdomInnleggelser> q = entityManager.createQuery(
            "Select si " +
                "FROM SykdomInnleggelser as si " +
                "   inner join si.vurderinger as sv " +
                "   inner join sv.person as sp " +
                "where si.versjon = :versjon " +
                "and sp.aktørId = :aktørId "
            , SykdomInnleggelser.class);
        q.setParameter("versjon", innleggelser.getVersjon());
        q.setParameter("aktørId", pleietrengende);

        try {
            q.getSingleResult();
        } catch (NoResultException | NonUniqueResultException e) {
            throw new IllegalStateException("Fant ikke unik SykdomInnleggelser å erstatte", e);
        }

        innleggelser.setVersjon(innleggelser.getVersjon() + 1);

        entityManager.persist(innleggelser);
        entityManager.flush();
    }

    public SykdomDiagnosekoder hentDiagnosekoder(AktørId pleietrengende) {
        var diagnosekoder = hentDiagnosekoderOrNull(pleietrengende);
        if (diagnosekoder != null) {
            return diagnosekoder;
        }
        return new SykdomDiagnosekoder(null, null, new ArrayList<>(), null, null);
    }

    SykdomDiagnosekoder hentDiagnosekoderOrNull(AktørId pleietrengende) {
        final TypedQuery<SykdomDiagnosekoder> q = entityManager.createQuery(
            "SELECT sd " +
                "FROM SykdomDiagnosekoder as sd " +
                "   inner join sd.vurderinger as sv " +
                "   inner join sv.person as p " +
                "where sd.versjon = ( " +
                "   select max(sd2.versjon) " +
                "   from SykdomDiagnosekoder as sd2 " +
                "   where sd2.vurderinger = sd.vurderinger " +
                ") " +
                "and p.aktørId = :aktørId", SykdomDiagnosekoder.class);
        q.setParameter("aktørId", pleietrengende);

        final List<SykdomDiagnosekoder> result = q.getResultList();
        if (result.isEmpty()) {
            return null;
        }
        if (result.size() != 1) {
            throw new IllegalStateException("Forventer maksimalt én rad som svar.");
        }
        return result.get(0);
    }

    public void opprettEllerOppdaterDiagnosekoder(SykdomDiagnosekoder diagnosekoder, AktørId pleietrengende) {
        if (diagnosekoder.getId() != null) {
            throw new IllegalStateException("Diagnosekoder skal aldri oppdateres. Man skal alltid inserte ny");
        }
        SykdomVurderinger vurderinger = sykdomVurderingRepository.hentEllerLagreSykdomVurderinger(pleietrengende, diagnosekoder.getOpprettetAv(), diagnosekoder.getOpprettetTidspunkt());
        diagnosekoder.setVurderinger(vurderinger);
        boolean lagNy = diagnosekoder.getVersjon() == null;
        if (lagNy) {
            opprettDiagnosekoder(diagnosekoder);
        } else {
            oppdaterDiagnosekoder(diagnosekoder, pleietrengende);
        }
    }

    private void opprettDiagnosekoder(SykdomDiagnosekoder diagnosekoder) {
        diagnosekoder.setVersjon(0L);
        entityManager.persist(diagnosekoder);
        entityManager.flush();
    }

    private void oppdaterDiagnosekoder(SykdomDiagnosekoder diagnosekoder, AktørId pleietrengende) {
        final TypedQuery<SykdomDiagnosekoder> q = entityManager.createQuery(
            "Select sd " +
                "FROM SykdomDiagnosekoder as sd " +
                "   inner join sd.vurderinger as sv " +
                "   inner join sv.person as sp " +
                "where sd.versjon = :versjon " +
                "and sp.aktørId = :aktørId "
            , SykdomDiagnosekoder.class);
        q.setParameter("versjon", diagnosekoder.getVersjon());
        q.setParameter("aktørId", pleietrengende);

        try {
            q.getSingleResult();
        } catch (NoResultException | NonUniqueResultException e) {
            throw new IllegalStateException("Fant ikke unik SykdomDiagnosekoder å erstatte", e);
        }

        diagnosekoder.setVersjon(diagnosekoder.getVersjon() + 1);

        entityManager.persist(diagnosekoder);
        entityManager.flush();
    }
}
