package no.nav.folketrygdloven.beregningsgrunnlag.adapter.vltilregelmodell.periodisering;

import java.util.List;
import java.util.stream.Collectors;

import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.grunnlag.inntekt.NaturalYtelse;
import no.nav.foreldrepenger.domene.iay.modell.Inntektsmelding;

class MapNaturalytelser {
    private MapNaturalytelser() {
        // skjul public constructor
    }

    static List<NaturalYtelse> mapNaturalytelser(Inntektsmelding im) {
        return im.getNaturalYtelser().stream()
            .map(ny -> new NaturalYtelse(ny.getBeloepPerMnd().getVerdi(), ny.getPeriode().getFomDato(), ny.getPeriode().getTomDato()))
            .collect(Collectors.toList());
    }
}
