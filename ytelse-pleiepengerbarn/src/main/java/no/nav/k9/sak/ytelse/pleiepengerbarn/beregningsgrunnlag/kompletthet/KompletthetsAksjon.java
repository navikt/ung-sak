package no.nav.k9.sak.ytelse.pleiepengerbarn.beregningsgrunnlag.kompletthet;

import java.time.LocalDateTime;
import java.util.List;

import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.dokument.DokumentMalType;

public class KompletthetsAksjon {

    private List<PeriodeMedMangler> perioderMedMangler;
    private boolean uavklart = false;
    private AksjonspunktDefinisjon aksjonspunktDefinisjon;
    private DokumentMalType dokumentMalType;
    private LocalDateTime frist;

    public KompletthetsAksjon(AksjonspunktDefinisjon aksjonspunktDefinisjon, LocalDateTime frist, List<PeriodeMedMangler> perioderMedMangler, DokumentMalType dokumentMalType) {
        this.aksjonspunktDefinisjon = aksjonspunktDefinisjon;
        this.frist = frist;
        this.perioderMedMangler = perioderMedMangler;
        this.dokumentMalType = dokumentMalType;
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

    public static KompletthetsAksjon automatiskEtterlysning(AksjonspunktDefinisjon aksjonspunktDefinisjon, LocalDateTime frist, List<PeriodeMedMangler> arbeidsgiverDetSkalEtterlysesFra, DokumentMalType dokumentMalType) {
        return new KompletthetsAksjon(aksjonspunktDefinisjon, frist, arbeidsgiverDetSkalEtterlysesFra, dokumentMalType);
    }

    public static KompletthetsAksjon manuellAvklaring(AksjonspunktDefinisjon aksjonspunktDefinisjon) {
        return automatiskEtterlysning(aksjonspunktDefinisjon, null, null, null);
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

    public DokumentMalType getDokumentMalType() {
        return dokumentMalType;
    }

    public LocalDateTime getFrist() {
        return frist;
    }

    public List<PeriodeMedMangler> getPerioderMedMangler() {
        return perioderMedMangler;
    }
}
