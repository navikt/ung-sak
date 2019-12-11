package no.nav.folketrygdloven.beregningsgrunnlag.kontrollerfakta;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.aktivitet.AktivitetStatus;
import no.nav.foreldrepenger.domene.iay.modell.Inntektsmelding;

public class KontrollerFaktaBeregningTjeneste {

    private KontrollerFaktaBeregningTjeneste() {
        // Skjul
    }

    public static boolean harAktivitetStatusKunYtelse(BeregningsgrunnlagEntitet beregningsgrunnlag) {
        return beregningsgrunnlag.getAktivitetStatuser().stream()
            .allMatch(bgStatus -> bgStatus.getAktivitetStatus().equals(AktivitetStatus.KUN_YTELSE));
    }

    /** Map av inntektsmeldinger per orgnr. */
    public static Map<String, List<Inntektsmelding>> hentInntektsmeldingerForVirksomheter(Set<String> virksomheterOrgnr, Collection<Inntektsmelding>inntektsmeldinger) {
        return inntektsmeldinger.stream()
            .filter(im -> im.getArbeidsgiver().getErVirksomhet())
            .filter(im -> virksomheterOrgnr.contains(im.getArbeidsgiver().getOrgnr()))
            .collect(Collectors.groupingBy(im-> im.getArbeidsgiver().getOrgnr()));
    }

}
