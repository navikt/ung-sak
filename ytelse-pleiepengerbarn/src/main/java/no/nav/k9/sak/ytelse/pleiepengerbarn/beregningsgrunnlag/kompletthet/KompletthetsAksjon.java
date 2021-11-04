package no.nav.k9.sak.ytelse.pleiepengerbarn.beregningsgrunnlag.kompletthet;

import java.time.LocalDateTime;
import java.util.List;

import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;

public class KompletthetsAksjon {

    private List<PeriodeMedMangler> arbeidsgiverDetSkalEtterlysesFra;
    private boolean uavklart = false;
    private AksjonspunktDefinisjon aksjonspunktDefinisjon;
    private LocalDateTime frist;

    public KompletthetsAksjon(AksjonspunktDefinisjon aksjonspunktDefinisjon, LocalDateTime frist, List<PeriodeMedMangler> arbeidsgiverDetSkalEtterlysesFra) {
        this.aksjonspunktDefinisjon = aksjonspunktDefinisjon;
        this.frist = frist;
        this.arbeidsgiverDetSkalEtterlysesFra = arbeidsgiverDetSkalEtterlysesFra;
    }

    public KompletthetsAksjon() {
        this.frist = null;
        this.aksjonspunktDefinisjon = null;
    }

    KompletthetsAksjon(boolean uavklart) {
        this.uavklart = uavklart;
    }

    /**
     * Fortsett uavbrutt, ingen aksjons trengs
     *
     * @return fortsett
     */
    public static KompletthetsAksjon fortsett() {
        return new KompletthetsAksjon();
    }

    /**
     * Trenger videre vurdering
     *
     * @return uavklart
     */
    public static KompletthetsAksjon uavklart() {
        return new KompletthetsAksjon(true);
    }

    public static KompletthetsAksjon automatiskEtterlysning(AksjonspunktDefinisjon aksjonspunktDefinisjon, LocalDateTime frist, List<PeriodeMedMangler> arbeidsgiverDetSkalEtterlysesFra) {
        return new KompletthetsAksjon(aksjonspunktDefinisjon, frist, arbeidsgiverDetSkalEtterlysesFra);
    }

    public static KompletthetsAksjon manuellAvklaring(AksjonspunktDefinisjon aksjonspunktDefinisjon) {
        return new KompletthetsAksjon(aksjonspunktDefinisjon, null, null);
    }

    public boolean erUavklart() {
        return uavklart;
    }

    public boolean kanFortsette() {
        return !erUavklart() && frist == null && aksjonspunktDefinisjon == null;
    }

    public boolean harFrist() {
        return frist != null;
    }

    public AksjonspunktDefinisjon getAksjonspunktDefinisjon() {
        return aksjonspunktDefinisjon;
    }

    public LocalDateTime getFrist() {
        return frist;
    }

    public List<PeriodeMedMangler> getArbeidsgiverDetSkalEtterlysesFra() {
        return arbeidsgiverDetSkalEtterlysesFra;
    }
}
