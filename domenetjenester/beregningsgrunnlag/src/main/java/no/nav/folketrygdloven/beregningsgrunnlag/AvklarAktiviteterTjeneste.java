package no.nav.folketrygdloven.beregningsgrunnlag;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;

import no.nav.folketrygdloven.beregningsgrunnlag.felles.BeregningUtils;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningAktivitetAggregatEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningAktivitetEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagAktivitetStatus;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.iay.modell.AktørYtelse;
import no.nav.foreldrepenger.domene.iay.modell.Ytelse;
import no.nav.foreldrepenger.domene.iay.modell.YtelseAnvist;
import no.nav.foreldrepenger.domene.iay.modell.YtelseFilter;
import no.nav.foreldrepenger.domene.typer.Stillingsprosent;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.iay.AktivitetStatus;
import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;

@ApplicationScoped
public class AvklarAktiviteterTjeneste {

    public AvklarAktiviteterTjeneste() {
    }

    boolean skalAvklareAktiviteter(BeregningsgrunnlagEntitet beregningsgrunnlag, BeregningAktivitetAggregatEntitet beregningAktivitetAggregat, Optional<AktørYtelse> aktørYtelse) {
        return harVentelønnEllerVartpengerSomSisteAktivitetIOpptjeningsperioden(beregningAktivitetAggregat)
            || harFullAAPPåStpMedAndreAktiviteter(beregningsgrunnlag, aktørYtelse);
    }

    public boolean harVentelønnEllerVartpengerSomSisteAktivitetIOpptjeningsperioden(BeregningAktivitetAggregatEntitet beregningAktivitetAggregat) {
        List<BeregningAktivitetEntitet> relevanteAktiviteter = beregningAktivitetAggregat.getBeregningAktiviteter();
        LocalDate skjæringstidspunkt = beregningAktivitetAggregat.getSkjæringstidspunktOpptjening();
        List<BeregningAktivitetEntitet> aktiviteterPåStp = relevanteAktiviteter.stream()
            .filter(opptjeningsperiode -> opptjeningsperiode.getPeriode().getFomDato().isBefore(skjæringstidspunkt))
            .filter(opptjeningsperiode -> !opptjeningsperiode.getPeriode().getTomDato().isBefore(skjæringstidspunkt))
            .collect(Collectors.toList());
        return aktiviteterPåStp.stream()
            .anyMatch(aktivitet -> aktivitet.getOpptjeningAktivitetType().equals(OpptjeningAktivitetType.VENTELØNN_VARTPENGER));
    }

    public boolean harFullAAPPåStpMedAndreAktiviteter(BeregningsgrunnlagEntitet beregningsgrunnlag, Optional<AktørYtelse> aktørYtelse) {
        List<BeregningsgrunnlagAktivitetStatus> aktivitetStatuser = beregningsgrunnlag.getAktivitetStatuser();
        if (aktivitetStatuser.stream().noneMatch(as -> as.getAktivitetStatus().equals(AktivitetStatus.ARBEIDSAVKLARINGSPENGER))) {
            return false;
        }
        LocalDate skjæringstidspunkt = beregningsgrunnlag.getSkjæringstidspunkt();
        if (aktivitetStatuser.size() <= 1) {
            return false;
        }
        return hentUtbetalingsprosentAAP(aktørYtelse, skjæringstidspunkt)
            .filter(verdi -> verdi.compareTo(BeregningUtils.MAX_UTBETALING_PROSENT_AAP_DAG) == 0)
            .isPresent();
    }

    private Optional<BigDecimal> hentUtbetalingsprosentAAP(Optional<AktørYtelse> aktørYtelse, LocalDate skjæringstidspunkt) {
        var ytelseFilter = new YtelseFilter(aktørYtelse).før(skjæringstidspunkt);

        Optional<Ytelse> nyligsteVedtak = BeregningUtils.sisteVedtakFørStpForType(ytelseFilter, skjæringstidspunkt, Set.of(FagsakYtelseType.ARBEIDSAVKLARINGSPENGER));
        if (nyligsteVedtak.isEmpty()) {
            return Optional.empty();
        }

        Optional<YtelseAnvist> nyligsteMeldekort = BeregningUtils.sisteHeleMeldekortFørStp(ytelseFilter, nyligsteVedtak.get(), skjæringstidspunkt, Set.of(FagsakYtelseType.ARBEIDSAVKLARINGSPENGER));
        return Optional.of(nyligsteMeldekort.flatMap(YtelseAnvist::getUtbetalingsgradProsent).map(Stillingsprosent::getVerdi).orElse(BeregningUtils.MAX_UTBETALING_PROSENT_AAP_DAG));
    }

}
