package no.nav.k9.sak.behandlingslager.behandling.aksjonspunkt;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
import no.nav.k9.sak.typer.AktørId;
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

    /** Returnerer aksjonspunkter for en sak. */
    @SuppressWarnings("unchecked")
    public Map<Behandling, List<Aksjonspunkt>> hentAksjonspunkter(Saksnummer saksnummer, AksjonspunktStatus... statuser) {
        List<AksjonspunktStatus> statusList = Arrays.asList(statuser == null || statuser.length == 0 ? AksjonspunktStatus.values() : statuser);

        String sql = "select b.* from behandling b"
            + " inner join aksjonspunkt a on a.behandling_id=b.id"
            + " inner join fagsak f on f.id=b.fagsak_id"
            + " where a.aksjonspunkt_status IN (:statuser) and f.saksnummer=:saksnummer";
        List<Behandling> list = em
            .createNativeQuery(sql, Behandling.class)
            .setParameter("statuser", statusList.stream().map(AksjonspunktStatus::getKode).collect(Collectors.toSet()))
            .setParameter("saksnummer", saksnummer.getVerdi())
            .getResultList();

        Map<Behandling, List<Aksjonspunkt>> map = new LinkedHashMap<>();
        for (Behandling b : list) {
            Set<Aksjonspunkt> aksjonspunkter = b.getAksjonspunkter()
                .stream()
                .filter(a -> statusList.contains(a.getStatus()))
                .collect(Collectors.toCollection(LinkedHashSet::new));
            map.put(b, List.copyOf(aksjonspunkter));
        }

        return map;
    }

    /** Returnerer alle aksjonspunkter og tilknyttede behandlinger. */
    @SuppressWarnings("unchecked")
    public Map<Behandling, List<Aksjonspunkt>> hentAksjonspunkter(AksjonspunktStatus... statuser) {
        List<AksjonspunktStatus> statusList = Arrays.asList(statuser == null || statuser.length == 0 ? AksjonspunktStatus.values() : statuser);
        String sql = "select distinct b.* from behandling b"
            + " inner join aksjonspunkt a on a.behandling_id=b.id"
            + " where a.aksjonspunkt_status IN (:statuser)";
        List<Behandling> list = em
            .createNativeQuery(sql, Behandling.class)
            .setParameter("statuser", statusList.stream().map(AksjonspunktStatus::getKode).collect(Collectors.toSet()))
            .getResultList();

        Map<Behandling, List<Aksjonspunkt>> map = new LinkedHashMap<>();
        for (Behandling b : list) {
            if (skipBehandling(b)) {
                continue;
            }
            Set<Aksjonspunkt> aksjonspunkter = b.getAksjonspunkter()
                .stream()
                .filter(a -> statusList.contains(a.getStatus()))
                .collect(Collectors.toCollection(LinkedHashSet::new));
            map.put(b, List.copyOf(aksjonspunkter));
        }

        return map;
    }

    @SuppressWarnings("unchecked")
    public List<AktørId> hentAktørerMedAktivtAksjonspunkt(AksjonspunktDefinisjon aksjonspunktDefinisjon) {
        String sql = "select f.bruker_aktoer_id from fagsak f"
            + " inner join behandling b on b.fagsak_id=f.id"
            + " inner join aksjonspunkt a on a.behandling_id=b.id"
            + " where a.aksjonspunkt_status = :status AND a.aksjonspunkt_def = :definisjon";
        Stream<String> stream = em.createNativeQuery(sql)
            .setParameter("status", AksjonspunktStatus.OPPRETTET.getKode())
            .setParameter("definisjon", aksjonspunktDefinisjon.getKode())
            .getResultStream();

        return stream.map(a -> new AktørId(a)).collect(Collectors.toList());
    }

    private boolean skipBehandling(Behandling beh) {
        return beh.getFagsakYtelseType() == FagsakYtelseType.OBSOLETE;
    }

}
