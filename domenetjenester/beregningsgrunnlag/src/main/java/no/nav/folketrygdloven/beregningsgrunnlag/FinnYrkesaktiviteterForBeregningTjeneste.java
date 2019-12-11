package no.nav.folketrygdloven.beregningsgrunnlag;

import java.time.LocalDate;
import java.util.Collection;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;

import no.nav.folketrygdloven.beregningsgrunnlag.adapter.vltilregelmodell.periodisering.ErFjernetIOverstyrt;
import no.nav.folketrygdloven.beregningsgrunnlag.adapter.vltilregelmodell.periodisering.FinnAnsettelsesPeriode;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningAktivitetAggregatEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.Yrkesaktivitet;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetFilter;

@ApplicationScoped
public class FinnYrkesaktiviteterForBeregningTjeneste {


    private FinnYrkesaktiviteterForBeregningTjeneste() {
        // Skjul
    }



    public static Collection<Yrkesaktivitet> finnYrkesaktiviteter(BehandlingReferanse behandlingReferanse, InntektArbeidYtelseGrunnlag iayGrunnlag, BeregningsgrunnlagGrunnlagEntitet grunnlag) {
        YrkesaktivitetFilter filter = new YrkesaktivitetFilter(iayGrunnlag.getArbeidsforholdInformasjon(), iayGrunnlag.getAktørArbeidFraRegister(behandlingReferanse.getAktørId()));
        Collection<Yrkesaktivitet> yrkesaktiviteterForBeregning = filter.getYrkesaktiviteterForBeregning();
        LocalDate skjæringstidspunktBeregning = behandlingReferanse.getSkjæringstidspunktBeregning();
        BeregningAktivitetAggregatEntitet overstyrtEllerRegisterAktiviteter = grunnlag.getOverstyrteEllerRegisterAktiviteter();
        return yrkesaktiviteterForBeregning.stream()
            .filter(yrkesaktivitet ->
                !ErFjernetIOverstyrt.erFjernetIOverstyrt(filter, yrkesaktivitet, overstyrtEllerRegisterAktiviteter, skjæringstidspunktBeregning))
            .filter(ya -> FinnAnsettelsesPeriode.finnMinMaksPeriode(filter.getAnsettelsesPerioder(ya), skjæringstidspunktBeregning).isPresent())
            .collect(Collectors.toList());
    }
}
