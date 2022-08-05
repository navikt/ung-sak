package no.nav.k9.sak.produksjonsstyring.oppgavebehandling;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import no.nav.k9.felles.jpa.HibernateVerktøy;
import no.nav.k9.kodeverk.produksjonsstyring.OppgaveÅrsak;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingLås;

@Dependent
public class OppgaveBehandlingKoblingRepository {

    private EntityManager entityManager;

    OppgaveBehandlingKoblingRepository() {
        // for CDI proxy
    }

    @Inject
    public OppgaveBehandlingKoblingRepository(EntityManager entityManager) {
        Objects.requireNonNull(entityManager, "entityManager"); //$NON-NLS-1$
        this.entityManager = entityManager;
    }

    /**
     * Lagrer kobling til GSAK oppgave for behandling. Sørger for at samtidige oppdateringer på samme Behandling, ikke kan gjøres samtidig.
     *
     * @see BehandlingLås
     */
    public Long lagre(OppgaveBehandlingKobling oppgaveBehandlingKobling) {
        entityManager.persist(oppgaveBehandlingKobling);
        entityManager.flush();
        return oppgaveBehandlingKobling.getId();
    }


    public Optional<OppgaveBehandlingKobling> hentOppgaveBehandlingKobling(String oppgaveId) {
        TypedQuery<OppgaveBehandlingKobling> query = entityManager.createQuery("from OppgaveBehandlingKobling where oppgaveId=:oppgaveId", //$NON-NLS-1$
            OppgaveBehandlingKobling.class);
        query.setParameter("oppgaveId", oppgaveId); //$NON-NLS-1$
        return HibernateVerktøy.hentUniktResultat(query);
    }


    public List<OppgaveBehandlingKobling> hentOppgaverRelatertTilBehandling(Long behandlingId) {
        TypedQuery<OppgaveBehandlingKobling> query = entityManager.createQuery("from OppgaveBehandlingKobling where behandling.id=:behandlingId", //$NON-NLS-1$
            OppgaveBehandlingKobling.class);
        query.setParameter("behandlingId", behandlingId); //$NON-NLS-1$
        return query.getResultList();
    }


    public List<OppgaveBehandlingKobling> hentUferdigeOppgaverOpprettetTidsrom(LocalDate fom, LocalDate tom, Set<OppgaveÅrsak> oppgaveTyper) {
        TypedQuery<OppgaveBehandlingKobling> query = entityManager.
            createQuery("from OppgaveBehandlingKobling where ferdigstilt=:ferdig and opprettetTidspunkt >= :fom and opprettetTidspunkt <= :tom and oppgaveÅrsak in :aarsaker", //$NON-NLS-1$
            OppgaveBehandlingKobling.class);
        query.setParameter("fom", fom.atStartOfDay()); //$NON-NLS-1$
        query.setParameter("tom", tom.plusDays(1).atStartOfDay().minusMinutes(1)); //$NON-NLS-1$
        query.setParameter("aarsaker", oppgaveTyper); //$NON-NLS-1$
        query.setParameter("ferdig", Boolean.FALSE); //$NON-NLS-1$
        return query.getResultList();
    }
}
