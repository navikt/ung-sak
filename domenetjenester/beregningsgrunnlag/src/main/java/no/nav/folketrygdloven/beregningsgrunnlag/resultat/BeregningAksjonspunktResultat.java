package no.nav.folketrygdloven.beregningsgrunnlag.resultat;

import static java.util.Collections.singletonList;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import no.nav.k9.kodeverk.beregningsgrunnlag.BeregningAksjonspunktDefinisjon;
import no.nav.k9.kodeverk.beregningsgrunnlag.BeregningVenteårsak;

public class BeregningAksjonspunktResultat {

    private BeregningAksjonspunktDefinisjon beregningAksjonspunktDefinisjon;
    private BeregningVenteårsak venteårsak;
    private LocalDateTime ventefrist;

    private BeregningAksjonspunktResultat(BeregningAksjonspunktDefinisjon aksjonspunktDefinisjon) {
        this.beregningAksjonspunktDefinisjon = aksjonspunktDefinisjon;
    }

    private BeregningAksjonspunktResultat(BeregningAksjonspunktDefinisjon aksjonspunktDefinisjon, BeregningVenteårsak venteårsak, LocalDateTime ventefrist) {
        this.beregningAksjonspunktDefinisjon = aksjonspunktDefinisjon;
        this.venteårsak = venteårsak;
        this.ventefrist = ventefrist;
    }

    /**
     * Factory-metode direkte basert på {@link BeregningAksjonspunktDefinisjon}. Ingen callback for consumer.
     */
    public static BeregningAksjonspunktResultat opprettFor(BeregningAksjonspunktDefinisjon aksjonspunktDefinisjon) {
        return new BeregningAksjonspunktResultat(aksjonspunktDefinisjon);
    }

    /**
     * Factory-metode direkte basert på {@link BeregningAksjonspunktDefinisjon}, returnerer liste. Ingen callback for consumer.
     */
    public static List<BeregningAksjonspunktResultat> opprettListeFor(BeregningAksjonspunktDefinisjon aksjonspunktDefinisjon) {
        return singletonList(new BeregningAksjonspunktResultat(aksjonspunktDefinisjon));
    }

    /**
     * Factory-metode som linker {@link BeregningAksjonspunktDefinisjon} sammen med callback for consumer-operasjon.
     */
    public static BeregningAksjonspunktResultat opprettMedFristFor(BeregningAksjonspunktDefinisjon aksjonspunktDefinisjon, BeregningVenteårsak venteårsak, LocalDateTime ventefrist) {
        return new BeregningAksjonspunktResultat(aksjonspunktDefinisjon, venteårsak, ventefrist);
    }

    public BeregningAksjonspunktDefinisjon getBeregningAksjonspunktDefinisjon() {
        return beregningAksjonspunktDefinisjon;
    }

    public BeregningVenteårsak getVenteårsak() {
        return venteårsak;
    }

    public LocalDateTime getVentefrist() {
        return ventefrist;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<" +
            beregningAksjonspunktDefinisjon.getKode() + ":" + beregningAksjonspunktDefinisjon.getNavn() +
            ", venteårsak=" + getVenteårsak() +
            ", ventefrist=" + getVentefrist() +
            ">";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof BeregningAksjonspunktResultat))
            return false;

        BeregningAksjonspunktResultat that = (BeregningAksjonspunktResultat) o;

        return beregningAksjonspunktDefinisjon.getKode().equals(that.beregningAksjonspunktDefinisjon.getKode());
    }

    @Override
    public int hashCode() {
        return Objects.hash(beregningAksjonspunktDefinisjon.getKode());
    }

    public boolean harFrist() {
        return null != getVentefrist();
    }
}
