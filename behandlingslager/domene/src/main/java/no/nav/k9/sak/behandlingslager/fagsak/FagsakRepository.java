package no.nav.k9.sak.behandlingslager.fagsak;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

import no.nav.k9.kodeverk.behandling.FagsakStatus;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.uttak.Tid;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.vedtak.felles.jpa.HibernateVerktøy;

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

    public List<Fagsak> hentForBruker(AktørId aktørId) {
        TypedQuery<Fagsak> query = entityManager
            .createQuery("from Fagsak f "
                + " where f.brukerAktørId=:aktørId"
                + " and f.skalTilInfotrygd=:ikkestengt"
                + " and f.ytelseType in (:ytelseTyper)",
                Fagsak.class);
        query.setParameter("aktørId", aktørId); // NOSONAR
        query.setParameter("ikkestengt", false); // NOSONAR
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

    public void oppdaterPleietrengende(Long fagsakId, AktørId pleietrengendeAktørId) {
        Fagsak fagsak = finnEksaktFagsak(fagsakId);
        fagsak.setPleietrengende(pleietrengendeAktørId);
        entityManager.persist(fagsak);
        entityManager.flush();
    }

    public void oppdaterSaksnummer(Long fagsakId, Saksnummer saksnummer) {
        Fagsak fagsak = finnEksaktFagsak(fagsakId);
        fagsak.setSaksnummer(saksnummer);
        entityManager.persist(fagsak);
        entityManager.flush();

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

        fagsak.setPeriode(oppdatertFom, oppdatertTom);
        entityManager.persist(fagsak);
        entityManager.flush();
    }

    /**
     * Henter siste fagsak (nyeste) per søker knyttet til angitt pleietrengende (1 fagsak per søker).
     * Pleietrengende her er typisk barn/nærstående avh. av ytelse.
     *
     * @param tom
     */
    @SuppressWarnings("unchecked")
    public List<Fagsak> finnFagsakRelatertTil(FagsakYtelseType ytelseType, AktørId bruker, AktørId pleietrengendeAktørId, LocalDate fom, LocalDate tom) {
        Query query;

        if (pleietrengendeAktørId != null) {
            query = entityManager.createNativeQuery(
                "select f.* from Fagsak f"
                    + " where f.pleietrengende_aktoer_id = :pleietrengendeAktørId"
                    + "   and f.bruker_aktoer_id = :brukerAktørId"
                    + "   and f.ytelse_type = :ytelseType"
                    + "   and f.periode && daterange(cast(:fom as date), cast(:tom as date), '[]') = true",
                Fagsak.class); // NOSONAR
            query.setParameter("pleietrengendeAktørId", pleietrengendeAktørId.getId());
        } else {
            query = entityManager.createNativeQuery(
                "select f.* from Fagsak f"
                    + " where f.ytelse_type = :ytelseType"
                    + "   and f.bruker_aktoer_id = :brukerAktørId"
                    + "   and f.periode && daterange(cast(:fom as date), cast(:tom as date), '[]') = true",
                Fagsak.class); // NOSONAR
        }
        query.setParameter("brukerAktørId", Objects.requireNonNull(bruker, "bruker").getId());
        query.setParameter("ytelseType", Objects.requireNonNull(ytelseType, "ytelseType").getKode());
        query.setParameter("fom", fom == null ? Tid.TIDENES_BEGYNNELSE : fom);
        query.setParameter("tom", tom == null ? Tid.TIDENES_ENDE : tom);

        return query.getResultList();
    }

    /**
     * Henter siste fagsak (nyeste) per søker knyttet til angitt pleietrengende (1 fagsak per søker).
     * Pleietrengende her er typisk barn/nærstående avh. av ytelse.
     *
     * @param tom
     */
    @SuppressWarnings("unchecked")
    public List<Fagsak> finnFagsakRelatertTil(FagsakYtelseType ytelseType, AktørId pleietrengendeAktørId, LocalDate fom, LocalDate tom) {
        Query query;

        if (pleietrengendeAktørId != null) {
            query = entityManager.createNativeQuery(
                "select f.* from Fagsak f"
                    + " where f.pleietrengende_aktoer_id = :pleietrengendeAktørId"
                    + "   and f.ytelse_type = :ytelseType"
                    + "   and f.periode && daterange(cast(:fom as date), cast(:tom as date), '[]') = true",
                Fagsak.class); // NOSONAR
            query.setParameter("pleietrengendeAktørId", pleietrengendeAktørId.getId());
        } else {
            query = entityManager.createNativeQuery(
                "select f.* from Fagsak f"
                    + " where f.ytelse_type = :ytelseType"
                    + "   and f.periode && daterange(cast(:fom as date), cast(:tom as date), '[]') = true",
                Fagsak.class); // NOSONAR
        }
        query.setParameter("ytelseType", Objects.requireNonNull(ytelseType, "ytelseType").getKode());
        query.setParameter("fom", fom == null ? Tid.TIDENES_BEGYNNELSE : fom);
        query.setParameter("tom", tom == null ? Tid.TIDENES_ENDE : tom);

        return query.getResultList();
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
     * @param status - ny status
     */
    public void oppdaterFagsakStatus(Long fagsakId, FagsakStatus status) {
        Fagsak fagsak = finnEksaktFagsak(fagsakId);
        fagsak.oppdaterStatus(status);
        entityManager.persist(fagsak);
        entityManager.flush();
    }

    public void fagsakSkalBehandlesAvInfotrygd(Long fagsakId) {
        Fagsak fagsak = finnEksaktFagsak(fagsakId);
        fagsak.setSkalTilInfotrygd(true);
        entityManager.persist(fagsak);
        entityManager.flush();
    }

}
