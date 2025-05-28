package no.nav.ung.sak.behandlingslager.behandling.repository;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import no.nav.ung.kodeverk.behandling.BehandlingStatus;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktType;
import no.nav.ung.kodeverk.etterlysning.EtterlysningStatus;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import org.hibernate.jpa.QueryHints;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Ulike spesialmetoder for å hente opp behandlinger som er kandidater for videre spesiell prosessering, slik som
 * etterkontroll gjenopptagelse av behandlinger på vent og lignende.
 * <p>
 * Disse vil bil brukt i en trigging av videre prosessering, behandling, kontroll, evt. henlegging eller avslutting.
 */

@Dependent
public class BehandlingKandidaterRepository {

    private static final Set<AksjonspunktDefinisjon> AUTOPUNKTER = List.of(AksjonspunktDefinisjon.values()).stream().filter(a -> AksjonspunktType.AUTOPUNKT.equals(a.getAksjonspunktType()))
        .collect(Collectors.toSet());
    private static final Set<BehandlingStatus> AVSLUTTENDE_STATUS = BehandlingStatus.getFerdigbehandletStatuser();
    private static final String AVSLUTTENDE_KEY = "avsluttetOgIverksetterStatus";
    private EntityManager entityManager;

    BehandlingKandidaterRepository() {
        // for CDI proxy
    }

    @Inject
    public BehandlingKandidaterRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    protected EntityManager getEntityManager() {
        return entityManager;
    }

    /**
     * Hent alle behandlinger som er avsluttet etter oppgitt tidspunkt.
     */
    public List<Behandling> hentAvsluttedeBehandlingerEtter(LocalDateTime avsluttetEtter) {
        Objects.requireNonNull(avsluttetEtter, "avsluttetEtter");

        TypedQuery<Behandling> query = getEntityManager().createQuery(
            "SELECT beh from Behandling AS beh WHERE beh.status = :status and beh.avsluttetDato > :avsluttetEtter",
            Behandling.class);
        query.setParameter("status", BehandlingStatus.AVSLUTTET); // NOSONAR@
        query.setParameter("avsluttetEtter", avsluttetEtter); // NOSONAR
        query.setHint(QueryHints.HINT_READONLY, "true");
        return query.getResultList();
    }


    @SuppressWarnings("unchecked")
    public List<Behandling> finnBehandlingerForAutomatiskGjenopptagelse() {

        Set<AksjonspunktDefinisjon> autopunktKoder = new HashSet<>(AUTOPUNKTER);

        LocalDateTime naa = LocalDateTime.now();

        String sql = " SELECT DISTINCT b.* " +
            " FROM aksjonspunkt ap " +
            " INNER JOIN behandling b on b.id=ap.behandling_id " +
            " INNER JOIN fagsak f on f.id=b.fagsak_id" +
            " WHERE ap.aksjonspunkt_status IN :aapneAksjonspunktKoder " +
            "   AND f.ytelse_type != 'OBSOLETE'" +
            "   AND ap.aksjonspunkt_def IN (:autopunktKoder) " +
            "   AND ap.frist_tid < :naa ";
        var query = getEntityManager().createNativeQuery(sql, Behandling.class)
            .setHint(QueryHints.HINT_READONLY, "true")
            .setParameter("aapneAksjonspunktKoder", AksjonspunktStatus.getÅpneAksjonspunktStatuser().stream().map(a -> a.getKode()).collect(Collectors.toList()))
            .setParameter("autopunktKoder", autopunktKoder.stream().map(a -> a.getKode()).collect(Collectors.toList()))
            .setParameter("naa", naa);

        return query.getResultList();
    }

    public List<Behandling> finnBehandlingerIkkeAvsluttetPåAngittEnhet(String enhetId) {

        TypedQuery<Behandling> query = entityManager.createQuery(
            "FROM Behandling behandling " +
                "WHERE behandling.status NOT IN (:avsluttetOgIverksetterStatus) " +
                "   AND b.fagsak.ytelseType != 'OBSOLETE'" +
                "  AND behandling.behandlendeEnhet = :enhet ",
            Behandling.class);

        query.setParameter("enhet", enhetId);
        query.setParameter(AVSLUTTENDE_KEY, AVSLUTTENDE_STATUS);
        query.setHint(QueryHints.HINT_READONLY, "true");
        return query.getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Behandling> finnÅpneBehandlingerUtenÅpneAksjonspunktEllerAutopunkt() {

        String sql = "SELECT b.* FROM Behandling b "
            + " WHERE b.behandling_status NOT IN (:avsluttetOgIverksetterStatus) "
            + " AND NOT EXISTS (SELECT 1 FROM Aksjonspunkt ap WHERE ap.behandling_id=b.id AND ap.aksjonspunkt_status = :status) ";

        var query = entityManager.createNativeQuery(sql, Behandling.class)
            .setParameter(AVSLUTTENDE_KEY, AVSLUTTENDE_STATUS.stream().map(BehandlingStatus::getKode).collect(Collectors.toList()))
            .setParameter("status", AksjonspunktStatus.OPPRETTET.getKode())
            .setHint(QueryHints.HINT_READONLY, "true");
        return query.getResultList();
    }

    public List<Behandling> finnBehandlingerForUtløptEtterlysning() {
        LocalDateTime naa = LocalDateTime.now();

        String sql = " SELECT DISTINCT b.* " +
            " FROM etterlysning e " +
            " INNER JOIN behandling b on b.id=e.behandling_id " +
            " INNER JOIN fagsak f on f.id=b.fagsak_id" +
            " WHERE e.status = :status " +
            "   AND f.ytelse_type != 'OBSOLETE'" +
            "   AND e.frist < :naa ";
        var query = getEntityManager().createNativeQuery(sql, Behandling.class)
            .setHint(QueryHints.HINT_READONLY, "true")
            .setParameter("status", EtterlysningStatus.VENTER.getKode())
            .setParameter("naa", naa);

        return query.getResultList();
    }

}
