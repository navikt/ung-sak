package no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.vedtak.felles.jpa.VLPersistenceUnit;
import no.nav.vedtak.util.FPDateUtil;

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
    public AksjonspunktRepository(@VLPersistenceUnit EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /*
     * Bruk behandlingskontroll.lagreFunnet eller OppdateringResultat.medEkstraResultat
     */
    @Deprecated
    public Aksjonspunkt leggTilAksjonspunkt(Behandling behandling, AksjonspunktDefinisjon aksjonspunktDefinisjon,
                                            BehandlingStegType behandlingStegType) {
        Objects.requireNonNull(behandlingStegType, "behandlingStegType");
        return leggTilAksjonspunkt(behandling, aksjonspunktDefinisjon, Optional.ofNullable(behandlingStegType), Optional.empty(), Optional.empty(),
            Optional.empty());
    }

    /*
     * Bruk behandlingskontroll.lagreFunnet eller OppdateringResultat.medEkstraResultat
     */
    @Deprecated
    public Aksjonspunkt leggTilAksjonspunkt(Behandling behandling, AksjonspunktDefinisjon aksjonspunktDefinisjon) {
        return leggTilAksjonspunkt(behandling, aksjonspunktDefinisjon, Optional.empty(), Optional.empty(), Optional.empty(),
            Optional.empty());
    }

    private Aksjonspunkt leggTilAksjonspunkt(Behandling behandling, AksjonspunktDefinisjon aksjonspunktDefinisjon,
                                             Optional<BehandlingStegType> behandlingStegType, Optional<LocalDateTime> frist, Optional<Venteårsak> venteÅrsak,
                                             Optional<Boolean> toTrinnskontroll) {
        // sjekk at alle parametere er spesifisert
        Objects.requireNonNull(behandling, "behandling");
        Objects.requireNonNull(aksjonspunktDefinisjon, "aksjonspunktDefinisjon");
        Objects.requireNonNull(behandlingStegType, "behandlingStegType");
        Objects.requireNonNull(frist, "frist");
        Objects.requireNonNull(venteÅrsak, "venteÅrsak");
        Objects.requireNonNull(toTrinnskontroll, "toTrinnskontroll");

        // slå opp for å få riktig konfigurasjon.
        Aksjonspunkt.Builder adBuilder = behandlingStegType.isPresent()
            ? new Aksjonspunkt.Builder(aksjonspunktDefinisjon, behandlingStegType.get())
            : new Aksjonspunkt.Builder(aksjonspunktDefinisjon);

        if (frist.isPresent()) {
            adBuilder.medFristTid(frist.get());
        } else if (aksjonspunktDefinisjon.getFristPeriod() != null) {
            adBuilder.medFristTid(FPDateUtil.nå().plus(aksjonspunktDefinisjon.getFristPeriod()));
        }

        if (venteÅrsak.isPresent()) {
            adBuilder.medVenteårsak(venteÅrsak.get());
        } else {
            adBuilder.medVenteårsak(Venteårsak.UDEFINERT);
        }

        Aksjonspunkt aksjonspunkt = adBuilder.buildFor(behandling);
        log.info("Legger til aksjonspunkt: {}", aksjonspunktDefinisjon);
        return aksjonspunkt;

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
                setReåpnet(aksjonspunkt);
            }
            log.info("Setter totrinnskontroll kreves for aksjonspunkt: {}", aksjonspunkt.getAksjonspunktDefinisjon());
            aksjonspunkt.settToTrinnsFlag();
        }
    }

    public void fjernToTrinnsBehandlingKreves(Aksjonspunkt aksjonspunkt) {
        aksjonspunkt.fjernToTrinnsFlagg();
    }

    /*
     * Bruk behandlingskontroll.lagreUtført eller  eller OppdateringResultat.medEkstraResultat
     */
    @Deprecated
    public boolean setTilUtført(Aksjonspunkt aksjonspunkt, String begrunnelse) {
        log.info("Setter aksjonspunkt utført: {}", aksjonspunkt.getAksjonspunktDefinisjon());
        return aksjonspunkt.setStatus(AksjonspunktStatus.UTFØRT, begrunnelse);
    }

    /*
     * Bruk behandlingskontroll.lagreAvbrutt eller  eller OppdateringResultat.medEkstraResultat
     */
    @Deprecated
    public void setTilAvbrutt(Aksjonspunkt aksjonspunkt) {
        log.info("Setter aksjonspunkt avbrutt: {}", aksjonspunkt.getAksjonspunktDefinisjon());
        aksjonspunkt.setStatus(AksjonspunktStatus.AVBRUTT, aksjonspunkt.getBegrunnelse());
    }

    /*
     * Bruk behandlingskontroll.lagreFunnet eller  eller OppdateringResultat.medEkstraResultat
     */
    @Deprecated
    public void setReåpnet(Aksjonspunkt aksjonspunkt) {
        log.info("Setter aksjonspunkt reåpnet: {}", aksjonspunkt.getAksjonspunktDefinisjon());
        aksjonspunkt.setStatus(AksjonspunktStatus.OPPRETTET, aksjonspunkt.getBegrunnelse());
    }

    public void setFrist(Aksjonspunkt ap, LocalDateTime fristTid, Venteårsak venteårsak) {
        ap.setFristTid(fristTid);
        ap.setVenteårsak(venteårsak);
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
