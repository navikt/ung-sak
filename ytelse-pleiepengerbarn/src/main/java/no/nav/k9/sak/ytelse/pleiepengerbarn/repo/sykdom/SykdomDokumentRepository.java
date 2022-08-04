package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.NonUniqueResultException;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;

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


    public List<PleietrengendeSykdomDokument> hentAlleDokumenterFor(AktørId pleietrengende) {
        final TypedQuery<PleietrengendeSykdomDokument> q = entityManager.createQuery(
            "SELECT d From PleietrengendeSykdomDokument as d "
                + "inner join d.pleietrengendeSykdom as sv "
                + "inner join sv.person as p "
                + "where p.aktørId = :aktørId", PleietrengendeSykdomDokument.class);

        q.setParameter("aktørId", pleietrengende);

        List<PleietrengendeSykdomDokument> dokuments = q.getResultList();

        return dokuments;
    }

    public boolean isDokumentBruktIVurdering(Long dokumentId) {
        final TypedQuery<PleietrengendeSykdomVurderingVersjon> q = entityManager.createQuery(
            "SELECT vv From PleietrengendeSykdomVurderingVersjon as vv "
                + "inner join vv.dokumenter as d "
                + "where d.id = :dokumentId", PleietrengendeSykdomVurderingVersjon.class);
        q.setParameter("dokumentId", dokumentId);

        return q.getResultList().size() > 0;
    }

    public List<PleietrengendeSykdomDokument> hentNyeDokumenterFor(UUID behandlingUUID) {
        final TypedQuery<PleietrengendeSykdomDokument> q = entityManager.createQuery(
            "SELECT d From PleietrengendeSykdomDokument as d "
                + "where d.søkersBehandlingUuid = :behandlingUUID", PleietrengendeSykdomDokument.class);

        q.setParameter("behandlingUUID", behandlingUUID);

        List<PleietrengendeSykdomDokument> dokuments = q.getResultList();

        return dokuments;
    }

    public List<PleietrengendeSykdomDokument> hentDokumenterSomErRelevanteForSykdom(AktørId pleietrengende) {
        return hentAlleDokumenterFor(pleietrengende)
            .stream()
            .filter(d -> d.getType().isRelevantForSykdom())
            .collect(Collectors.toList());
    }

    public List<PleietrengendeSykdomDokument> hentDuplikaterAv(Long dokumentId) {
        final TypedQuery<PleietrengendeSykdomDokument> q = entityManager.createQuery(
            "SELECT d From PleietrengendeSykdomDokument as d "
                + "inner join d.informasjoner as i "
                + "inner join i.duplikatAvDokument as dd "
                + "where dd.id = :dokumentId"
                + "  and i.versjon = ("
                + "    select max(i2.versjon) "
                + "    From PleietrengendeSykdomDokumentInformasjon as i2 "
                + "    where i2.dokument = i.dokument"
                + "  )", PleietrengendeSykdomDokument.class);

        q.setParameter("dokumentId", dokumentId);
        return q.getResultList();
    }

    public List<PleietrengendeSykdomDokument> hentDokumentSomIkkeHarOppdatertEksisterendeVurderinger(AktørId pleietrengende) {
        return hentDokumenterSomErRelevanteForSykdom(pleietrengende)
            .stream()
            .filter(d -> !harKvittertDokumentForEksisterendeVurderinger(d) && !d.isDuplikat())
            .collect(Collectors.toList());
    }

    public List<PleietrengendeSykdomDokument> hentGodkjenteLegeerklæringer(AktørId pleietrengende) {
        return hentAlleDokumenterFor(pleietrengende).stream().filter(d -> d.getType() == SykdomDokumentType.LEGEERKLÆRING_SYKEHUS && d.getDuplikatAvDokument() == null).collect(Collectors.toList());
    }

    public Optional<PleietrengendeSykdomDokument> hentDokument(Long dokumentId, AktørId pleietrengende) {
        final TypedQuery<PleietrengendeSykdomDokument> q = entityManager.createQuery(
            "SELECT d From PleietrengendeSykdomDokument as d "
                + "inner join d.pleietrengendeSykdom as sv "
                + "inner join sv.person as p "
                + "where p.aktørId = :aktørId"
                + "  and d.id = :dokumentId", PleietrengendeSykdomDokument.class);

        q.setParameter("dokumentId", dokumentId);
        q.setParameter("aktørId", pleietrengende);

        Optional<PleietrengendeSykdomDokument> dokument = q.getResultList().stream().findFirst();

        return dokument;
    }

    public boolean finnesSykdomDokument(JournalpostId journalpostId, String dokumentInfoId) {
        Objects.requireNonNull(journalpostId, "journalpostId");

        final String dokumentInfoSjekk = (dokumentInfoId == null) ? "d.dokumentInfoId IS NULL" : "d.dokumentInfoId = :dokumentInfoId";

        final TypedQuery<PleietrengendeSykdomDokument> q = entityManager.createQuery(
                "SELECT d From PleietrengendeSykdomDokument as d "
                    + "where d.journalpostId = :journalpostId"
                    + "  and " + dokumentInfoSjekk, PleietrengendeSykdomDokument.class);

        q.setParameter("journalpostId", journalpostId);
        if (dokumentInfoId != null) {
            q.setParameter("dokumentInfoId", dokumentInfoId);
        }

        Optional<PleietrengendeSykdomDokument> dokument = q.getResultList().stream().findFirst();

        return dokument.isPresent();
    }

    public void lagre(PleietrengendeSykdomDokument dokument, AktørId pleietrengende) {
        if (dokument.getId() != null) {
            throw new IllegalStateException("Dokumentet har allerede blitt lagret.");
        }
        final PleietrengendeSykdom pleietrengendeSykdom = sykdomVurderingRepository.hentEllerLagreSykdomVurderinger(pleietrengende, dokument.getEndretAv(), dokument.getEndretTidspunkt());
        dokument.setSykdomVurderinger(pleietrengendeSykdom);

        entityManager.persist(dokument);
        entityManager.flush();
    }

    public void oppdater(PleietrengendeSykdomDokumentInformasjon dokumentInformasjon) {
        if (dokumentInformasjon.getDokument().getId() == null) {
            throw new IllegalStateException("Kan ikke oppdatere dokument som ikke har vært lagret før.");
        }

        entityManager.persist(dokumentInformasjon);
        entityManager.flush();
    }

    public boolean harKvittertDokumentForEksisterendeVurderinger(PleietrengendeSykdomDokument dokument) {
        TypedQuery<PleietrengendeSykdomDokumentHarOppdatertVurderinger> q =
            entityManager.createQuery(
                "SELECT k " +
                    "FROM PleietrengendeSykdomDokumentHarOppdatertVurderinger as k " +
                    "WHERE k.id = :id", PleietrengendeSykdomDokumentHarOppdatertVurderinger.class);

        q.setParameter("id", dokument.getId());

        return q.getResultList().stream().findFirst().isPresent();
    }

    public void kvitterDokumenterMedOppdatertEksisterendeVurderinger(PleietrengendeSykdomDokumentHarOppdatertVurderinger utkvittering) {

        String sql = "INSERT INTO pleietrengende_sykdom_dokument_har_oppdatert_vurderinger " +
            "(pleietrengende_sykdom_dokument_id, opprettet_av, opprettet_tid) " +
            "VALUES(:id, :opprettetAv, :opprettetTidspunkt) " +
            "ON CONFLICT DO NOTHING";

        Query query = entityManager.createNativeQuery(sql)
            .setParameter("id", utkvittering.getId())
            .setParameter("opprettetAv", utkvittering.getOpprettetAv())
            .setParameter("opprettetTidspunkt", utkvittering.getOpprettetTidspunkt());

        query.executeUpdate();
        entityManager.flush();
    }

    public PleietrengendeSykdomInnleggelser hentInnleggelse(AktørId pleietrengende) {
        var sykdomInnleggelser = hentInnleggelseOrNull(pleietrengende);
        if (sykdomInnleggelser != null) {
            return sykdomInnleggelser;
        }
        return new PleietrengendeSykdomInnleggelser(null, null, Collections.emptyList(), null, null);
    }

    PleietrengendeSykdomInnleggelser hentInnleggelseOrNull(AktørId pleietrengende) {
        final TypedQuery<PleietrengendeSykdomInnleggelser> q = entityManager.createQuery(
            "SELECT si "
                + "FROM PleietrengendeSykdomInnleggelser as si "
                + "  inner join si.pleietrengendeSykdom as sv "
                + "  inner join sv.person as p "
                + "where si.versjon = ( "
                + "  select max(si2.versjon) "
                + "  from PleietrengendeSykdomInnleggelser as si2 "
                + "  where si2.pleietrengendeSykdom = si.pleietrengendeSykdom "
                + ") "
                + "and p.aktørId = :aktørId", PleietrengendeSykdomInnleggelser.class);
        q.setParameter("aktørId", pleietrengende);

        final List<PleietrengendeSykdomInnleggelser> result = q.getResultList();
        if (result.isEmpty()) {
            return null;
        }
        if (result.size() != 1) {
            throw new IllegalStateException("Forventer maksimalt én rad som svar.");
        }
        return result.get(0);
    }

    public PleietrengendeSykdomInnleggelser hentInnleggelse(UUID behandlingUUID) {
        var sykdomInnleggelser = hentInnleggelseOrNull(behandlingUUID);
        if (sykdomInnleggelser != null) {
            return sykdomInnleggelser;
        }
        return new PleietrengendeSykdomInnleggelser(null, null, Collections.emptyList(), null, null);
    }

    public PleietrengendeSykdomInnleggelser hentInnleggelseOrNull(UUID behandlingUUID) {
        final TypedQuery<PleietrengendeSykdomInnleggelser> q = entityManager.createQuery(
            "SELECT gi " +
                "FROM MedisinskGrunnlag as sgb " +
                "inner join sgb.grunnlagsdata as sg " +
                "inner join sg.innleggelser as gi " +
                "where sgb.behandlingUuid = :behandlingUuid " +
                "and sgb.versjon = " +
                "( select max(sgb2.versjon) " +
                "From MedisinskGrunnlag as sgb2 " +
                "where sgb2.behandlingUuid = sgb.behandlingUuid )"
            , PleietrengendeSykdomInnleggelser.class);
        q.setParameter("behandlingUuid", behandlingUUID);

        final List<PleietrengendeSykdomInnleggelser> result = q.getResultList();
        if (result.isEmpty()) {
            return null;
        }
        if (result.size() != 1) {
            throw new IllegalStateException("Forventer maksimalt én rad som svar.");
        }
        return result.get(0);
    }

    public void opprettEllerOppdaterInnleggelser(PleietrengendeSykdomInnleggelser innleggelser, AktørId pleietrengende) {
        if (innleggelser.getId() != null) {
            throw new IllegalStateException("Innleggelser skal aldri oppdateres. Man skal alltid inserte ny");
        }
        PleietrengendeSykdom vurderinger = sykdomVurderingRepository.hentEllerLagreSykdomVurderinger(pleietrengende, innleggelser.getOpprettetAv(), innleggelser.getOpprettetTidspunkt());
        innleggelser.setPleietrengendeSykdom(vurderinger);
        boolean lagNy = innleggelser.getVersjon() == null;
        if (lagNy) {
            opprettInnleggelser(innleggelser, pleietrengende);
        } else {
            oppdaterInnleggelser(innleggelser, pleietrengende);
        }
    }

    private void opprettInnleggelser(PleietrengendeSykdomInnleggelser innleggelser, AktørId pleietrengende) {
        innleggelser.setVersjon(0L);
        entityManager.persist(innleggelser);
        entityManager.flush();
    }

    private void oppdaterInnleggelser(PleietrengendeSykdomInnleggelser innleggelser, AktørId pleietrengende) {
        final TypedQuery<PleietrengendeSykdomInnleggelser> q = entityManager.createQuery(
            "Select si " +
                "FROM PleietrengendeSykdomInnleggelser as si " +
                "   inner join si.pleietrengendeSykdom as sv " +
                "   inner join sv.person as sp " +
                "where si.versjon = :versjon " +
                "and sp.aktørId = :aktørId "
            , PleietrengendeSykdomInnleggelser.class);
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

    public PleietrengendeSykdomDiagnoser hentDiagnosekoder(AktørId pleietrengende) {
        var diagnosekoder = hentDiagnosekoderOrNull(pleietrengende);
        if (diagnosekoder != null) {
            return diagnosekoder;
        }
        return new PleietrengendeSykdomDiagnoser(null, null, new ArrayList<>(), null, null);
    }

    PleietrengendeSykdomDiagnoser hentDiagnosekoderOrNull(AktørId pleietrengende) {
        final TypedQuery<PleietrengendeSykdomDiagnoser> q = entityManager.createQuery(
            "SELECT sd " +
                "FROM PleietrengendeSykdomDiagnoser as sd " +
                "   inner join sd.pleietrengendeSykdom as sv " +
                "   inner join sv.person as p " +
                "where sd.versjon = ( " +
                "   select max(sd2.versjon) " +
                "   from PleietrengendeSykdomDiagnoser as sd2 " +
                "   where sd2.pleietrengendeSykdom = sd.pleietrengendeSykdom " +
                ") " +
                "and p.aktørId = :aktørId", PleietrengendeSykdomDiagnoser.class);
        q.setParameter("aktørId", pleietrengende);

        final List<PleietrengendeSykdomDiagnoser> result = q.getResultList();
        if (result.isEmpty()) {
            return null;
        }
        if (result.size() != 1) {
            throw new IllegalStateException("Forventer maksimalt én rad som svar.");
        }
        return result.get(0);
    }

    public void opprettEllerOppdaterDiagnosekoder(PleietrengendeSykdomDiagnoser diagnosekoder, AktørId pleietrengende) {
        if (diagnosekoder.getId() != null) {
            throw new IllegalStateException("Diagnosekoder skal aldri oppdateres. Man skal alltid inserte ny");
        }
        PleietrengendeSykdom vurderinger = sykdomVurderingRepository.hentEllerLagreSykdomVurderinger(pleietrengende, diagnosekoder.getOpprettetAv(), diagnosekoder.getOpprettetTidspunkt());
        diagnosekoder.setPleietrengendeSykdom(vurderinger);
        boolean lagNy = diagnosekoder.getVersjon() == null;
        if (lagNy) {
            opprettDiagnosekoder(diagnosekoder);
        } else {
            oppdaterDiagnosekoder(diagnosekoder, pleietrengende);
        }
    }

    private void opprettDiagnosekoder(PleietrengendeSykdomDiagnoser diagnoser) {
        diagnoser.setVersjon(0L);
        entityManager.persist(diagnoser);
        entityManager.flush();
    }

    private void oppdaterDiagnosekoder(PleietrengendeSykdomDiagnoser diagnosekoder, AktørId pleietrengende) {
        final TypedQuery<PleietrengendeSykdomDiagnoser> q = entityManager.createQuery(
            "Select sd " +
                "FROM PleietrengendeSykdomDiagnoser as sd " +
                "   inner join sd.pleietrengendeSykdom as sv " +
                "   inner join sv.person as sp " +
                "where sd.versjon = :versjon " +
                "and sp.aktørId = :aktørId "
            , PleietrengendeSykdomDiagnoser.class);
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
