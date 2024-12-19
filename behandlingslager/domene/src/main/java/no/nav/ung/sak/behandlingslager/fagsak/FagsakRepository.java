package no.nav.ung.sak.behandlingslager.fagsak;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import no.nav.k9.felles.jpa.HibernateVerktøy;
import no.nav.ung.kodeverk.behandling.FagsakStatus;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.uttak.Tid;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.JournalpostId;
import no.nav.ung.sak.typer.Saksnummer;

@Dependent
public class FagsakRepository {

    private EntityManager entityManager;

    FagsakRepository() {
        // for CDI proxy
    }

    @Inject
    public FagsakRepository(EntityManager entityManager) {
        Objects.requireNonNull(entityManager, "entityManager"); //$NON-NLS-1$
        this.entityManager = entityManager;
    }

    public Fagsak finnEksaktFagsak(long fagsakId, boolean taSkriveLås) {
        TypedQuery<Fagsak> query = entityManager.createQuery("from Fagsak where id=:fagsakId", Fagsak.class);
        query.setParameter("fagsakId", fagsakId); // NOSONAR
        if (taSkriveLås) {
            query.setLockMode(LockModeType.PESSIMISTIC_WRITE);
        }
        Fagsak fagsak = HibernateVerktøy.hentEksaktResultat(query);
        entityManager.refresh(fagsak); // hent alltid på nytt
        return fagsak;
    }

    public Fagsak finnEksaktFagsak(long fagsakId) {
        TypedQuery<Fagsak> query = entityManager.createQuery("from Fagsak where id=:fagsakId", Fagsak.class);
        query.setParameter("fagsakId", fagsakId); // NOSONAR
        Fagsak fagsak = HibernateVerktøy.hentEksaktResultat(query);
        entityManager.refresh(fagsak); // hent alltid på nytt
        return fagsak;
    }

    public Optional<Fagsak> finnUnikFagsak(long fagsakId) {
        TypedQuery<Fagsak> query = entityManager.createQuery("from Fagsak where id=:fagsakId", Fagsak.class);
        query.setParameter("fagsakId", fagsakId); // NOSONAR
        Optional<Fagsak> opt = HibernateVerktøy.hentUniktResultat(query);
        if (opt.isPresent()) {
            entityManager.refresh(opt.get());
        }
        return opt;
    }

    // TODO: Burde kanskje ekskludere OBSOLETE her?
    public List<Fagsak> hentForBruker(AktørId aktørId) {
        TypedQuery<Fagsak> query = entityManager
            .createQuery("""
                    from Fagsak f
                      where f.brukerAktørId=:aktørId
                       and f.ytelseType in (:ytelseTyper)
                    """,
                Fagsak.class);
        query.setParameter("aktørId", aktørId); // NOSONAR
        query.setParameter("ytelseTyper", FagsakYtelseType.kodeMap().values()); // søk bare opp støtte ytelsetyper
        return query.getResultList();
    }

    public Optional<Journalpost> hentJournalpost(JournalpostId journalpostId) {
        TypedQuery<Journalpost> query = entityManager.createQuery("from Journalpost where journalpostId=:journalpost",
            Journalpost.class);
        query.setParameter("journalpost", journalpostId); // NOSONAR
        List<Journalpost> journalposter = query.getResultList();
        return journalposter.isEmpty() ? Optional.empty() : Optional.ofNullable(journalposter.get(0));
    }

    public Optional<Fagsak> hentSakGittSaksnummer(Saksnummer saksnummer) {
        TypedQuery<Fagsak> query = entityManager.createQuery("from Fagsak where saksnummer=:saksnummer", Fagsak.class);
        query.setParameter("saksnummer", saksnummer); // NOSONAR

        List<Fagsak> fagsaker = query.getResultList();
        if (fagsaker.size() > 1) {
            throw FagsakFeil.FACTORY.flereEnnEnFagsakForSaksnummer(saksnummer).toException();
        }

        return fagsaker.isEmpty() ? Optional.empty() : Optional.of(fagsaker.get(0));
    }

    public Long opprettNy(Fagsak fagsak) {
        if (fagsak.getId() != null) {
            throw new IllegalStateException("Fagsak [" + fagsak.getId() + "] eksisterer. Kan ikke opprette på ny");
        }
        entityManager.persist(fagsak);
        entityManager.flush();
        return fagsak.getId();
    }

    public void oppdaterPeriode(Long fagsakId, LocalDate fom, LocalDate tom) {
        Fagsak fagsak = finnEksaktFagsak(fagsakId);
        fagsak.setPeriode(fom, tom);
        entityManager.persist(fagsak);
        entityManager.flush();
    }

    public void utvidPeriode(Long fagsakId, LocalDate fom, LocalDate tom) {
        Fagsak fagsak = finnEksaktFagsak(fagsakId);

        var eksisterendeFom = fagsak.getPeriode().getFomDato();
        var eksisterendeTom = fagsak.getPeriode().getTomDato();
        var oppdatertFom = eksisterendeFom.isBefore(fom) && !Tid.TIDENES_BEGYNNELSE.equals(eksisterendeFom) ? eksisterendeFom : fom;
        var oppdatertTom = eksisterendeTom.isAfter(tom) && !Tid.TIDENES_ENDE.equals(eksisterendeTom) ? eksisterendeTom : tom;

        oppdaterPeriode(fagsakId, oppdatertFom, oppdatertTom);
    }

    /**
     * Henter siste fagsak (nyeste) per søker.
     *
     * @param tom
     */
    @SuppressWarnings("unchecked")
    public List<Fagsak> finnFagsakRelatertTil(
        FagsakYtelseType ytelseType,
        AktørId brukerId,
        LocalDate fom,
        LocalDate tom) {
        Objects.requireNonNull(brukerId);

        String sqlString = """
                    select f.* from Fagsak f
                      where f.ytelse_type = :ytelseType
                         and f.periode && daterange(cast(:fom as date), cast(:tom as date), '[]') = true
            """
            + (brukerId == null ? "" : "and f.bruker_aktoer_id = :brukerAktørId"); // NOSONAR (avsjekket dynamisk sql)

        Query query = entityManager.createNativeQuery(sqlString, Fagsak.class); // NOSONAR

        query.setParameter("ytelseType", Objects.requireNonNull(ytelseType, "ytelseType").getKode());
        query.setParameter("fom", fom == null ? Tid.TIDENES_BEGYNNELSE : fom);
        query.setParameter("tom", tom == null ? Tid.TIDENES_ENDE : tom);

        return query.getResultList();
    }

    /**
     * Henter siste fagsak (nyeste) per søker
     * @param tom
     */
    @SuppressWarnings("unchecked")
    public List<Fagsak> finnFagsakRelatertTil(
        FagsakYtelseType ytelseType,
        LocalDate fom,
        LocalDate tom) {


        return finnFagsakRelatertTil(ytelseType, null, fom, tom);
    }

    public Optional<Fagsak> hentSakGittSaksnummer(Saksnummer saksnummer, boolean taSkriveLås) {
        TypedQuery<Fagsak> query = entityManager.createQuery("from Fagsak where saksnummer=:saksnummer", Fagsak.class);
        query.setParameter("saksnummer", saksnummer); // NOSONAR
        if (taSkriveLås) {
            query.setLockMode(LockModeType.PESSIMISTIC_WRITE);
        }

        List<Fagsak> fagsaker = query.getResultList();
        if (fagsaker.size() > 1) {
            throw FagsakFeil.FACTORY.flereEnnEnFagsakForSaksnummer(saksnummer).toException();
        }

        return fagsaker.isEmpty() ? Optional.empty() : Optional.of(fagsaker.get(0));
    }

    public Long lagre(Journalpost journalpost) {
        entityManager.persist(journalpost);
        return journalpost.getId();
    }

    /**
     * Oppderer status på fagsak.
     *
     * @param fagsakId - id på fagsak
     * @param status   - ny status
     */
    public void oppdaterFagsakStatus(Long fagsakId, FagsakStatus status) {
        Fagsak fagsak = finnEksaktFagsak(fagsakId);
        fagsak.oppdaterStatus(status);
        entityManager.persist(fagsak);
        entityManager.flush();
    }

}
