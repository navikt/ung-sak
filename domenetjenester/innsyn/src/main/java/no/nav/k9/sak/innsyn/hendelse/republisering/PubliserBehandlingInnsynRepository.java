package no.nav.k9.sak.innsyn.hendelse.republisering;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import no.nav.k9.sikkerhet.context.SubjectHandler;

@Dependent
public class PubliserBehandlingInnsynRepository {
    private static final Logger logger = LoggerFactory.getLogger(PubliserBehandlingInnsynRepository.class);
    private EntityManager entityManager;


    public PubliserBehandlingInnsynRepository() {
    }

    @Inject
    public PubliserBehandlingInnsynRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /**
     * Lager ny rad i arbeidstabell for alle PSB behandlinger.
     */
    void klargjørNyKjøring(UUID kjøringId) {
        entityManager.createNativeQuery(
                "insert into publiser_behandling_arbeidstabell(id, kjøring_id, behandling_id, status, opprettet_av, kjøring_type) " +
                "select nextval('seq_publiser_behandling_arbeidstabell'), :kjoring_id, b.id, 'NY', :opprettetAv, 'INNSYN' " +
                "from behandling b join fagsak f on b.fagsak_id = f.id where f.ytelse_type = 'PSB'")
            .setParameter("kjoring_id", kjøringId)
            .setParameter("opprettetAv", SubjectHandler.getSubjectHandler().getUid())
            .executeUpdate();
    }

    /**
     * Henter neste for en gitt kjøring med lås slik at andre instanser av prosesstask ikke plukker den samme
     */
    @SuppressWarnings("unchecked")
    List<PubliserBehandlingEntitet> hentNesteMedLås(UUID kjøringId, int antall) {
        Query nativeQuery = entityManager.createNativeQuery(
                "select * from publiser_behandling_arbeidstabell " +
                "where status = 'NY' and kjøring_id = :kjoringId " +
                "for update skip locked limit :antall",
                PubliserBehandlingEntitet.class)
            .setParameter("kjoringId", kjøringId)
            .setParameter("antall", antall);


        return (List<PubliserBehandlingEntitet>) nativeQuery.getResultList();

    }

    void oppdater(List<PubliserBehandlingEntitet> rad) {
        rad.forEach(it -> entityManager.merge(it));
        entityManager.flush();
    }

    String kjørerapport(UUID kjøringId) {
        @SuppressWarnings("unchecked")
        Stream<Object[]> resultat = entityManager.createNativeQuery(
                "select status, count(*) from publiser_behandling_arbeidstabell " +
                "where kjøring_id = :kjoringId " +
                "group by status order by status")
            .setParameter("kjoringId", kjøringId)
            .getResultStream();

        return lagRapportString(resultat);
    }

    private static String lagRapportString(Stream<Object[]> resultat) {
        return resultat
            .map(it -> new StatusCount(
                PubliserBehandlingEntitet.Status.valueOf((String) it[0]),
                (Long) it[1]))
            .map(StatusCount::toString)
            .collect(Collectors.joining(", "));
    }

    @SuppressWarnings("unchecked")
    int slettFerdige() {
        Query rapportQuery = entityManager.createNativeQuery(
            "select status, kjøring_type, count(*) from publiser_behandling_arbeidstabell " +
            "group by status, kjøring_type order by status");

        logger.info("Status før slett {}", lagRapportString(rapportQuery.getResultStream()));

        int antallSlettet = entityManager.createNativeQuery("delete from publiser_behandling_arbeidstabell where status in ('FULLFØRT', 'KANSELLERT') and kjøring_type = 'INNSYN'")
            .executeUpdate();

        logger.info("antall slettet {}", antallSlettet);

        logger.info("Status etter slett {}", lagRapportString(rapportQuery.getResultStream()));
        return antallSlettet;

    }

    int kansellerAlleAktive(String endringstekst) {
        return entityManager.createNativeQuery(
                "update publiser_behandling_arbeidstabell set status = 'KANSELLERT', endring = :endring, endret_av = :endretAv, endret_tid = CURRENT_TIMESTAMP where status = 'NY' and kjøring_type = 'INNSYN'" +
                "and kjøring_type = 'INNSYN'")
            .setParameter("endring", endringstekst)
            .setParameter("endretAv", SubjectHandler.getSubjectHandler().getUid())
            .executeUpdate();

    }


    private record StatusCount(PubliserBehandlingEntitet.Status status, Long count) {
        @Override
        public String toString() {
            return "%s:%d".formatted(status.name(), count);
        }
    }
}
