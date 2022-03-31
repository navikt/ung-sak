package no.nav.k9.sak.fagsak.prosessering.avsluttning;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.hibernate.jpa.QueryHints;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.kodeverk.behandling.BehandlingStatus;
import no.nav.k9.kodeverk.behandling.FagsakStatus;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;

@Dependent
public class FagsakAvsluttningRepository {

    private final Set<BehandlingStatus> ikkeFerdigeStatuser;

    private final EntityManager entityManager;

    @Inject
    public FagsakAvsluttningRepository(EntityManager entityManager) {
        this.entityManager = entityManager;

        var behandlingStatuses = EnumSet.allOf(BehandlingStatus.class);
        behandlingStatuses.removeAll(BehandlingStatus.getFerdigbehandletStatuser());
        ikkeFerdigeStatuser = behandlingStatuses;
    }

    /**
     * Finner fagsaker som treffer på følgende kritterier
     * - LØPENDE status
     * - Har ikke en behandling som er åpen
     * - Fagsaksperiode
     * @return
     */
    public Set<Fagsak> finnKandidaterFagsakerSomKanAvsluttes() {
        var query = entityManager.createQuery("SELECT f FROM Fagsak f " +
            "WHERE f.fagsakStatus = :fagsakStatus " +
            "AND NOT EXISTS " +
            "(SELECT 1 FROM Behandling b " +
            "WHERE b.fagsak = f " +
            "AND b.status IN :behandlingStatus)", Fagsak.class);

        query.setParameter("fagsakStatus", FagsakStatus.LØPENDE)
            .setParameter("behandlingStatus", ikkeFerdigeStatuser)
            .setHint(QueryHints.HINT_READONLY, "true"); //$NON-NLS-1$

        return query.getResultStream()
            .filter(this::fagsaksPeriodeHarUtløptMedToMåneder)
            .collect(Collectors.toSet());
    }

    boolean fagsaksPeriodeHarUtløptMedToMåneder(Fagsak it) {
        return it.getPeriode().getTomDato().isBefore(LocalDate.now().plusMonths(2));
    }
}
