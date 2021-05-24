package no.nav.k9.sak.hendelsemottak.k9fordel.domene;

import java.util.List;
import java.util.Objects;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.kodeverk.hendelser.HåndtertStatusType;

/**
 * Repository for InngåendeHendelse.
 *
 * OBS1: Hvis du legger til nye spørringer er det viktig at de har HåndtertStatus som kriterie,
 * slik at de treffer riktig partisjon. Tabellen er partisjonert på denne statusen, der HÅNDTERT
 * ligger i den historiske (store) partisjonen som vi ikke tror det skal være behov for å spørre på.
 *
 * OBS2: Du treffer ikke riktig index/partisjon hvis du spør på NOT en gitt status,
 * og heller ikke med status1 OR status2 (Oracle 12c R1).
 */
@ApplicationScoped
public class HendelseRepository {
    private static final Logger LOGGER = LoggerFactory.getLogger(HendelseRepository.class);

    private static final String SORTER_STIGENDE_PÅ_OPPRETTET_TIDSPUNKT = "order by opprettetTidspunkt asc"; //$NON-NLS-1$

    private static final String HÅNDTERT_STATUS = "håndtertStatus";
    private static final String HENDELSE_TYPE = "type";
    private static final String AKTØR_ID = "aktørId";

    private EntityManager entityManager;

    HendelseRepository() {
        // for CDI proxy
    }

    @Inject
    public HendelseRepository(EntityManager entityManager) {
        Objects.requireNonNull(entityManager, "entityManager"); //$NON-NLS-1$
        this.entityManager = entityManager;
    }

    public InngåendeHendelse finnEksaktHendelse(Long inngåendeHendelseId) {
        return entityManager.find(InngåendeHendelse.class, inngåendeHendelseId);
    }

    public List<InngåendeHendelse> finnUhåndterteHendelser(InngåendeHendelse inngåendeHendelse) {
        TypedQuery<InngåendeHendelse> query = entityManager.createQuery(
            "from InngåendeHendelse " +
                "where hendelseKilde = :hendelseKilde " +
                "and hendelseType = :hendelseType " +
                "and aktørId = :aktørId " +
                "and håndtertStatus = :håndtertStatus " +
                "order by opprettet_tid ASC",
            InngåendeHendelse.class);
        query.setParameter("hendelseKilde", inngåendeHendelse.getHendelseKilde());
        query.setParameter("hendelseType", inngåendeHendelse.getHendelseType());
        query.setParameter("aktørId", inngåendeHendelse.getAktørId());
        query.setParameter("håndtertStatus", HåndtertStatusType.MOTTATT);
        return query.getResultList();
    }


    public void lagreInngåendeHendelse(InngåendeHendelse inngåendeHendelse) {
        entityManager.persist(inngåendeHendelse);
        entityManager.flush();
    }

    public void oppdaterHåndtertStatus(InngåendeHendelse inngåendeHendelse, String håndtertAvHendelseId, HåndtertStatusType håndtertStatus) {
        inngåendeHendelse.setHåndtertStatus(håndtertStatus);
        inngåendeHendelse.setHåndtertAvHendelseId(håndtertAvHendelseId);
        entityManager.flush();
    }
}
