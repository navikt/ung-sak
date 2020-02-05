package no.nav.folketrygdloven.beregningsgrunnlag.rest.fakta;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

import no.nav.folketrygdloven.beregningsgrunnlag.felles.BeregningUtils;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.Ytelse;
import no.nav.foreldrepenger.domene.iay.modell.YtelseAnvist;
import no.nav.foreldrepenger.domene.iay.modell.YtelseFilter;
import no.nav.foreldrepenger.domene.iay.modell.YtelseGrunnlag;
import no.nav.foreldrepenger.domene.typer.Stillingsprosent;
import no.nav.k9.kodeverk.arbeidsforhold.AktivitetStatus;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.typer.Beløp;

class FinnInntektFraYtelse {

    private static final BigDecimal VIRKEDAGER_I_1_ÅR = BigDecimal.valueOf(260);

    private FinnInntektFraYtelse() {
        // Skjul konstruktør
    }

    static Optional<BigDecimal> finnÅrbeløpFraMeldekort(BehandlingReferanse ref, AktivitetStatus aktivitetStatus, InntektArbeidYtelseGrunnlag grunnlag) {
        LocalDate skjæringstidspunkt = ref.getSkjæringstidspunktBeregning();
        var ytelseFilter = new YtelseFilter(grunnlag.getAktørYtelseFraRegister(ref.getAktørId())).før(skjæringstidspunkt);
        if (ytelseFilter.isEmpty()) {
            return Optional.empty();
        }

        Optional<Ytelse> nyesteVedtak = BeregningUtils.sisteVedtakFørStpForType(ytelseFilter, skjæringstidspunkt, Set.of(mapTilYtelseType(aktivitetStatus)));
        if (!nyesteVedtak.isPresent()) {
            return Optional.empty();
        }

        Optional<YtelseAnvist> nyesteMeldekort = BeregningUtils.sisteHeleMeldekortFørStp(ytelseFilter, nyesteVedtak.get(), skjæringstidspunkt, Set.of(mapTilYtelseType(aktivitetStatus)));
        return Optional.of(finnÅrsbeløp(nyesteVedtak.get(), nyesteMeldekort));
    }

    private static FagsakYtelseType mapTilYtelseType(AktivitetStatus aktivitetStatus) {
        if (AktivitetStatus.DAGPENGER.equals(aktivitetStatus)) {
            return FagsakYtelseType.DAGPENGER;
        }
        if (AktivitetStatus.ARBEIDSAVKLARINGSPENGER.equals(aktivitetStatus)) {
            return FagsakYtelseType.ARBEIDSAVKLARINGSPENGER;
        }
        return FagsakYtelseType.UDEFINERT;
    }

    private static BigDecimal finnÅrsbeløp(Ytelse ytelse, Optional<YtelseAnvist> ytelseAnvist) {
        BigDecimal dagsats = ytelse.getYtelseGrunnlag().flatMap(YtelseGrunnlag::getVedtaksDagsats).map(Beløp::getVerdi)
            .orElse(ytelseAnvist.flatMap(YtelseAnvist::getDagsats).map(Beløp::getVerdi).orElse(BigDecimal.ZERO));
        BigDecimal utbetalingsgrad = ytelseAnvist.flatMap(YtelseAnvist::getUtbetalingsgradProsent).map(Stillingsprosent::getVerdi)
            .orElse(BeregningUtils.MAX_UTBETALING_PROSENT_AAP_DAG);
        BigDecimal utbetalingsFaktor = utbetalingsgrad.divide(BeregningUtils.MAX_UTBETALING_PROSENT_AAP_DAG, 10, RoundingMode.HALF_UP);
        return dagsats
            .multiply(utbetalingsFaktor)
            .multiply(VIRKEDAGER_I_1_ÅR);
    }

}
