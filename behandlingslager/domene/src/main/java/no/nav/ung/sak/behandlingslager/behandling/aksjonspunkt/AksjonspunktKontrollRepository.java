package no.nav.ung.sak.behandlingslager.behandling.aksjonspunkt;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.ung.kodeverk.behandling.BehandlingStegType;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.Venteårsak;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;

/**
 * Håndter all endring av aksjonspunkt for behandlingskontroll. Skal IKKE brukes utenfor Behandlingskontroll uten avklaring
 */
@Dependent
public class AksjonspunktKontrollRepository {

    private static final Logger log = LoggerFactory.getLogger(AksjonspunktKontrollRepository.class);

    private static final Set<AksjonspunktDefinisjon> IKKE_AKTUELL_TOTRINN = Set.of(AksjonspunktDefinisjon.VEDTAK_UTEN_TOTRINNSKONTROLL, AksjonspunktDefinisjon.FORESLÅ_VEDTAK_MANUELT);

    @Inject
    public AksjonspunktKontrollRepository() {
    }

    public Aksjonspunkt settBehandlingPåVent(Behandling behandling, AksjonspunktDefinisjon aksjonspunktDefinisjon,
                                             BehandlingStegType stegType,
                                             LocalDateTime fristTid, Venteårsak venteårsak,
                                             String venteårsakVariant) {

        log.info("Setter behandling på vent for steg={}, aksjonspunkt={}, fristTid={}, venteÅrsak={}", stegType, aksjonspunktDefinisjon, fristTid, venteårsak);

        Aksjonspunkt aksjonspunkt;
        Optional<Aksjonspunkt> eksisterendeAksjonspunkt = behandling.getAksjonspunktMedDefinisjonOptional(aksjonspunktDefinisjon);
        if (eksisterendeAksjonspunkt.isPresent()) {
            // håndter har allerede angit aksjonpunkt, oppdaterer
            aksjonspunkt = eksisterendeAksjonspunkt.get();
            if (!aksjonspunkt.erOpprettet()) {
                this.setReåpnet(aksjonspunkt);
            }
            this.setFrist(aksjonspunkt, fristTid, venteårsak, venteårsakVariant);
        } else {
            // nytt aksjonspunkt
            aksjonspunkt = this.leggTilAksjonspunkt(behandling, aksjonspunktDefinisjon, Optional.empty(),
                Optional.ofNullable(fristTid), Optional.ofNullable(venteårsak), venteårsakVariant, Optional.empty());
        }
        aksjonspunkt.setBehandlingSteg(stegType);
        return aksjonspunkt;
    }

    private Aksjonspunkt leggTilAksjonspunkt(Behandling behandling, AksjonspunktDefinisjon aksjonspunktDefinisjon,
                                             Optional<BehandlingStegType> behandlingStegType, Optional<LocalDateTime> frist, Optional<Venteårsak> venteÅrsak,
                                             String venteårsakVariant,
                                             Optional<Boolean> toTrinnskontroll) {
        // sjekk at alle parametere er spesifisert
        Objects.requireNonNull(behandling, "behandling");
        Objects.requireNonNull(aksjonspunktDefinisjon, "aksjonspunktDefinisjon");
        Objects.requireNonNull(behandlingStegType, "behandlingStegType");
        Objects.requireNonNull(frist, "frist");
        Objects.requireNonNull(venteÅrsak, "venteÅrsak");
        Objects.requireNonNull(toTrinnskontroll, "toTrinnskontroll");

        // slå opp for å få riktig konfigurasjon.
        Aksjonspunkt.Builder adBuilder = behandlingStegType.map(stegType -> new Aksjonspunkt.Builder(aksjonspunktDefinisjon, stegType)).orElseGet(() -> new Aksjonspunkt.Builder(aksjonspunktDefinisjon));

        if (frist.isPresent()) {
            adBuilder.medFristTid(frist.get());
        } else if (aksjonspunktDefinisjon.getFristPeriod() != null) {
            adBuilder.medFristTid(LocalDateTime.now().plus(aksjonspunktDefinisjon.getFristPeriod()));
        }

        if (venteÅrsak.isPresent()) {
            adBuilder.medVenteårsak(venteÅrsak.get(), venteårsakVariant);
        } else {
            adBuilder.medVenteårsak(Venteårsak.UDEFINERT, null);
        }

        Aksjonspunkt aksjonspunkt = adBuilder.buildFor(behandling);
        log.info("Legger til aksjonspunkt: {}", aksjonspunktDefinisjon);
        return aksjonspunkt;
    }

    public Aksjonspunkt leggTilAksjonspunkt(Behandling behandling,
                                            AksjonspunktDefinisjon aksjonspunktDefinisjon,
                                            BehandlingStegType behandlingStegType) {
        Objects.requireNonNull(behandlingStegType, "behandlingStegType");
        return leggTilAksjonspunkt(behandling, aksjonspunktDefinisjon, Optional.ofNullable(behandlingStegType), Optional.empty(), Optional.empty(), null,
            Optional.empty());
    }

    public Aksjonspunkt leggTilAksjonspunkt(Behandling behandling, AksjonspunktDefinisjon aksjonspunktDefinisjon) {
        return leggTilAksjonspunkt(behandling, aksjonspunktDefinisjon, Optional.empty(), Optional.empty(), Optional.empty(), null,
            Optional.empty());
    }

    public void setReåpnet(Aksjonspunkt aksjonspunkt) {
        log.info("Setter aksjonspunkt reåpnet: {}", aksjonspunkt.getAksjonspunktDefinisjon());
        aksjonspunkt.setStatus(AksjonspunktStatus.OPPRETTET, aksjonspunkt.getBegrunnelse());
    }

    public void setReåpnetMedTotrinn(Aksjonspunkt aksjonspunkt, boolean setToTrinn) {
        log.info("Setter aksjonspunkt reåpnet: {}", aksjonspunkt.getAksjonspunktDefinisjon());
        aksjonspunkt.setStatus(AksjonspunktStatus.OPPRETTET, aksjonspunkt.getBegrunnelse());
        if (setToTrinn && !aksjonspunkt.isToTrinnsBehandling() && !IKKE_AKTUELL_TOTRINN.contains(aksjonspunkt.getAksjonspunktDefinisjon())) {
            aksjonspunkt.settToTrinnsFlag();
        }
    }

    public void setTilAvbrutt(Aksjonspunkt aksjonspunkt) {
        log.info("Setter aksjonspunkt avbrutt: {}", aksjonspunkt.getAksjonspunktDefinisjon());
        aksjonspunkt.setStatus(AksjonspunktStatus.AVBRUTT, aksjonspunkt.getBegrunnelse());
    }

    public boolean setTilUtført(Aksjonspunkt aksjonspunkt, String begrunnelse) {
        log.info("Setter aksjonspunkt utført: {}", aksjonspunkt.getAksjonspunktDefinisjon());
        return aksjonspunkt.setStatus(AksjonspunktStatus.UTFØRT, begrunnelse);
    }

    public void setFrist(Aksjonspunkt ap, LocalDateTime fristTid, Venteårsak venteårsak, String venteårsakVariant) {
        ap.setFristTid(fristTid);
        ap.setVenteårsak(venteårsak);
        ap.setVenteårsakVariant(venteårsakVariant);
    }

}
