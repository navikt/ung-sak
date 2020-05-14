package no.nav.k9.sak.behandlingslager.behandling.aksjonspunkt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.SkjermlenkeType;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.typer.Saksnummer;

/**
 * Håndter all endring av aksjonspunkt.
 */
@Dependent
public class AksjonspunktRepository {

    private static final Logger log = LoggerFactory.getLogger(AksjonspunktRepository.class);
    private EntityManager em;

    @Inject
    public AksjonspunktRepository(EntityManager em) {
        this.em = em;
    }

    public void lagre(Aksjonspunkt aks) {
        em.persist(aks);
        em.flush();
    }

    public void lagre(Collection<Aksjonspunkt> aks) {
        aks.forEach(em::persist);
        em.flush();
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

    /** Returnerer aksjonspunkter for behandling. */
    @SuppressWarnings("unchecked")
    public List<Aksjonspunkt> hentAksjonspunkter(Long behandlingId, AksjonspunktStatus... statuser) {
        List<AksjonspunktStatus> statusList = Arrays.asList(statuser == null || statuser.length == 0 ? AksjonspunktStatus.values() : statuser);
        return em.createQuery("Select a from Aksjonspunkt a where a.behandling.id=:id and a.status IN (:statuser)")
            .setParameter("id", behandlingId)
            .setParameter("statuser", statusList)
            .getResultList();
    }

    public void fjernToTrinnsBehandlingKreves(Aksjonspunkt aksjonspunkt) {
        aksjonspunkt.fjernToTrinnsFlagg();
    }

    /** Returnerer aksjonspunkter for en sak. */
    @SuppressWarnings("unchecked")
    public Map<Behandling, List<Aksjonspunkt>> hentAksjonspunkter(Saksnummer saksnummer, AksjonspunktStatus... statuser) {
        List<AksjonspunktStatus> statusList = Arrays.asList(statuser == null || statuser.length == 0 ? AksjonspunktStatus.values() : statuser);
        List<Object[]> list = em.createQuery("Select b, a from Aksjonspunkt a "
            + "JOIN a.behandling b "
            + "JOIN b.fagsak f where a.status IN (:statuser) and f.saksnummer=:saksnummer")
            .setParameter("saksnummer", saksnummer)
            .setParameter("statuser", statusList)
            .getResultList();

        Map<Behandling, List<Aksjonspunkt>> map = new LinkedHashMap<>();
        for (var tuple : list) {
            var beh = (Behandling) tuple[0];
            var aks = (Aksjonspunkt) tuple[1];
            if (!map.containsKey(beh)) {
                map.put(beh, new ArrayList<>());
            }
            map.get(beh).add(aks);
        }

        return map;
    }

    /** Returnerer alle aksjonspunkter og tilknyttede behandlinger. */
    @SuppressWarnings("unchecked")
    public Map<Behandling, List<Aksjonspunkt>> hentAksjonspunkter(AksjonspunktStatus... statuser) {
        List<AksjonspunktStatus> statusList = Arrays.asList(statuser == null || statuser.length == 0 ? AksjonspunktStatus.values() : statuser);
        List<Object[]> list = em.createQuery("Select b, a from Aksjonspunkt a JOIN a.behandling b where a.status IN (:statuser)")
            .setParameter("statuser", statusList)
            .getResultList();

        Map<Behandling, List<Aksjonspunkt>> map = new LinkedHashMap<>();
        for (var tuple : list) {
            var beh = (Behandling) tuple[0];
            var aks = (Aksjonspunkt) tuple[1];
            if (skipBehandling(beh)) {
                continue;
            }
            if (!map.containsKey(beh)) {
                map.put(beh, new ArrayList<>());
            }
            map.get(beh).add(aks);
        }

        return map;
    }

    private boolean skipBehandling(Behandling beh) {
        return beh.getFagsakYtelseType() == FagsakYtelseType.OBSOLETE;
    }

}
