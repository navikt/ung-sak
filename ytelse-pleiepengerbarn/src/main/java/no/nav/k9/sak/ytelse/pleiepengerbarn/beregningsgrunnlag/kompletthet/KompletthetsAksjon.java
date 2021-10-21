package no.nav.k9.sak.ytelse.pleiepengerbarn.beregningsgrunnlag.kompletthet;

import java.time.LocalDateTime;
import java.util.Objects;

import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;

public class KompletthetsAksjon {

    private AksjonspunktDefinisjon aksjonspunktDefinisjon;
    private LocalDateTime frist;
    private boolean skalSendeBrev;
    private boolean skalAvslåResterende = false;

    public KompletthetsAksjon(AksjonspunktDefinisjon aksjonspunktDefinisjon, LocalDateTime frist, boolean skalSendeBrev) {
        this.aksjonspunktDefinisjon = aksjonspunktDefinisjon;
        this.skalSendeBrev = skalSendeBrev;
        this.frist = aksjonspunktDefinisjon != null && skalSendeBrev ? Objects.requireNonNull(frist) : frist;
    }

    public KompletthetsAksjon() {
        this.skalSendeBrev = false;
        this.frist = null;
        this.aksjonspunktDefinisjon = null;
    }

    /**
     * Fortsett uavbrutt, ingen aksjons trengs
     *
     * @return
     */
    public static KompletthetsAksjon fortsett() {
        return new KompletthetsAksjon();
    }

    public static KompletthetsAksjon automatiskEtterlysning(AksjonspunktDefinisjon aksjonspunktDefinisjon, LocalDateTime frist, boolean skalSendeBrev) {
        return new KompletthetsAksjon(aksjonspunktDefinisjon, frist, skalSendeBrev);
    }

    public static KompletthetsAksjon manuellAvklaring(AksjonspunktDefinisjon aksjonspunktDefinisjon) {
        return new KompletthetsAksjon(aksjonspunktDefinisjon, null, false);
    }

    public boolean erKomplett() {
        return frist == null && aksjonspunktDefinisjon == null;
    }

    public AksjonspunktDefinisjon getAksjonspunktDefinisjon() {
        return aksjonspunktDefinisjon;
    }

    public LocalDateTime getFrist() {
        return frist;
    }

    public boolean isSkalSendeBrev() {
        return skalSendeBrev;
    }

    public boolean isSkalAvslåResterende() {
        return skalAvslåResterende;
    }
}
