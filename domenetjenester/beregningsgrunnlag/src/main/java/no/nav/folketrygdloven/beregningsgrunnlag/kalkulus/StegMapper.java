package no.nav.folketrygdloven.beregningsgrunnlag.kalkulus;

import no.nav.folketrygdloven.kalkulus.kodeverk.BeregningSteg;
import no.nav.k9.kodeverk.behandling.BehandlingStegType;

public class StegMapper {
    private StegMapper() {
    }

    public static BeregningSteg getBeregningSteg(BehandlingStegType steg) {
        return switch (steg) {
            case FASTSETT_SKJÆRINGSTIDSPUNKT_BEREGNING -> BeregningSteg.FASTSETT_STP_BER;
            case KONTROLLER_FAKTA_BEREGNING -> BeregningSteg.KOFAKBER;
            case FORESLÅ_BEREGNINGSGRUNNLAG -> BeregningSteg.FORS_BERGRUNN;
            case FORTSETT_FORESLÅ_BEREGNINGSGRUNNLAG -> BeregningSteg.FORS_BERGRUNN_2;
            case VURDER_VILKAR_BERGRUNN -> BeregningSteg.VURDER_VILKAR_BERGRUNN;
            case VURDER_TILKOMMET_INNTEKT -> BeregningSteg.VURDER_TILKOMMET_INNTEKT;
            case VURDER_REF_BERGRUNN -> BeregningSteg.VURDER_REF_BERGRUNN;
            case FORDEL_BEREGNINGSGRUNNLAG -> BeregningSteg.FORDEL_BERGRUNN;
            case FASTSETT_BEREGNINGSGRUNNLAG -> BeregningSteg.FAST_BERGRUNN;
            default -> throw new IllegalArgumentException(String.format("Steg %s er ikke et beregningssteg", steg.name()));
        };
    }

}
