package no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt;

import java.util.List;
import java.util.Objects;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Håndter all endring av aksjonspunkt.
 */
@ApplicationScoped
public class AksjonspunktRepository {

    private static final Logger log = LoggerFactory.getLogger(AksjonspunktRepository.class);

    private EntityManager entityManager;

    AksjonspunktRepository() {
        // CDI
    }

    @Inject
    public AksjonspunktRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public void setToTrinnsBehandlingKreves(Aksjonspunkt aksjonspunkt) {
        AksjonspunktDefinisjon apDef = aksjonspunkt.getAksjonspunktDefinisjon();
        if (apDef.getSkjermlenkeType() == null || SkjermlenkeType.UDEFINERT.equals(apDef.getSkjermlenkeType())) {
            log.info("Aksjonspunkt prøver sette totrinnskontroll uten skjermlenke: {}", aksjonspunkt.getAksjonspunktDefinisjon());
            if (AksjonspunktDefinisjon.VEDTAK_UTEN_TOTRINNSKONTROLL.equals(apDef) || AksjonspunktDefinisjon.FORESLÅ_VEDTAK_MANUELT.equals(apDef)) {
                return;
            }
        }
        if (!aksjonspunkt.isToTrinnsBehandling()) {
            if (!aksjonspunkt.erÅpentAksjonspunkt()) {
                aksjonspunkt.setStatus(AksjonspunktStatus.OPPRETTET, aksjonspunkt.getBegrunnelse());
            }
            log.info("Setter totrinnskontroll kreves for aksjonspunkt: {}", aksjonspunkt.getAksjonspunktDefinisjon());
            aksjonspunkt.settToTrinnsFlag();
        }
    }

    public void fjernToTrinnsBehandlingKreves(Aksjonspunkt aksjonspunkt) {
        aksjonspunkt.fjernToTrinnsFlagg();
    }

    @Deprecated
    public void setTilAvbrutt(Aksjonspunkt aksjonspunkt) {
        log.info("Setter aksjonspunkt avbrutt: {}", aksjonspunkt.getAksjonspunktDefinisjon());
        aksjonspunkt.setStatus(AksjonspunktStatus.AVBRUTT, aksjonspunkt.getBegrunnelse());
    }

    public List<Aksjonspunkt> hentAksjonspunkterForBehandling(Long behandlingId) {
        Objects.requireNonNull(behandlingId, "BehandlingId må være satt"); //$NON-NLS-1$

        TypedQuery<Aksjonspunkt> query = entityManager.createQuery(
            "SELECT ap from Aksjonspunkt ap WHERE ap.behandling.id=:behandlingId", //$NON-NLS-1$
            Aksjonspunkt.class);
        query.setParameter("behandlingId", behandlingId); //$NON-NLS-1$
        return query.getResultList();
    }
}
