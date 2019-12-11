package no.nav.folketrygdloven.beregningsgrunnlag.kontrollerfakta;


import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.Test;

import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.domene.iay.modell.AktivitetsAvtaleBuilder;
import no.nav.foreldrepenger.domene.iay.modell.Yrkesaktivitet;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetBuilder;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetFilter;
import no.nav.vedtak.felles.jpa.tid.DatoIntervallEntitet;

public class KortvarigArbeidsforholdTjenesteImplTest {

    @Test
    public void under_6_måneder_sammenhengende() {
        BeregningsgrunnlagEntitet beregningsgrunnlag = BeregningsgrunnlagEntitet.builder()
            .medSkjæringstidspunkt(LocalDate.now().minusMonths(1))
            .build();

        YrkesaktivitetBuilder yrkesaktivitetBuilder = YrkesaktivitetBuilder.oppdatere(Optional.empty())
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);
        AktivitetsAvtaleBuilder aktivitetsAvtaleBuilder = yrkesaktivitetBuilder
            .getAktivitetsAvtaleBuilder(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusMonths(5), LocalDate.now()), true);
        yrkesaktivitetBuilder.leggTilAktivitetsAvtale(aktivitetsAvtaleBuilder);

        Yrkesaktivitet yrkesaktivitet = yrkesaktivitetBuilder.build();

        assertThat(KortvarigArbeidsforholdTjeneste.erKortvarigYrkesaktivitetSomAvsluttesEtterSkjæringstidspunkt(new YrkesaktivitetFilter(Optional.empty(), yrkesaktivitet), beregningsgrunnlag, yrkesaktivitet)).isTrue();
    }

    @Test
    public void under_6_måneder_delt_opp_i_flere_deler() {
        LocalDate date = LocalDate.now();
        BeregningsgrunnlagEntitet beregningsgrunnlag = BeregningsgrunnlagEntitet.builder()
            .medSkjæringstidspunkt(date.minusMonths(1))
            .build();

        YrkesaktivitetBuilder yrkesaktivitetBuilder = YrkesaktivitetBuilder.oppdatere(Optional.empty())
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);
        yrkesaktivitetBuilder.leggTilAktivitetsAvtale(yrkesaktivitetBuilder
            .getAktivitetsAvtaleBuilder(DatoIntervallEntitet.fraOgMedTilOgMed(date.minusMonths(5), date.minusMonths(4)), true));
        yrkesaktivitetBuilder.leggTilAktivitetsAvtale(yrkesaktivitetBuilder
            .getAktivitetsAvtaleBuilder(DatoIntervallEntitet.fraOgMedTilOgMed(date.minusMonths(4), date.minusMonths(3)), true));
        yrkesaktivitetBuilder.leggTilAktivitetsAvtale(yrkesaktivitetBuilder
            .getAktivitetsAvtaleBuilder(DatoIntervallEntitet.fraOgMedTilOgMed(date.minusMonths(3), date.minusMonths(2)), true));
        yrkesaktivitetBuilder.leggTilAktivitetsAvtale(yrkesaktivitetBuilder
            .getAktivitetsAvtaleBuilder(DatoIntervallEntitet.fraOgMedTilOgMed(date.minusMonths(2), date.minusMonths(1)), true));
        yrkesaktivitetBuilder.leggTilAktivitetsAvtale(yrkesaktivitetBuilder
            .getAktivitetsAvtaleBuilder(DatoIntervallEntitet.fraOgMedTilOgMed(date.minusMonths(1), date), true));

        Yrkesaktivitet yrkesaktivitet = yrkesaktivitetBuilder.build();

        assertThat(KortvarigArbeidsforholdTjeneste.erKortvarigYrkesaktivitetSomAvsluttesEtterSkjæringstidspunkt(new YrkesaktivitetFilter(Optional.empty(), yrkesaktivitet), beregningsgrunnlag, yrkesaktivitet)).isTrue();
    }

    @Test
    public void over_6_måneder_delt_opp_i_flere_deler() {
        LocalDate date = LocalDate.now();
        BeregningsgrunnlagEntitet beregningsgrunnlag = BeregningsgrunnlagEntitet.builder()
            .medSkjæringstidspunkt(date.minusMonths(1))
            .build();

        YrkesaktivitetBuilder yrkesaktivitetBuilder = YrkesaktivitetBuilder.oppdatere(Optional.empty())
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);
        yrkesaktivitetBuilder.leggTilAktivitetsAvtale(yrkesaktivitetBuilder
            .getAktivitetsAvtaleBuilder(DatoIntervallEntitet.fraOgMedTilOgMed(date.minusMonths(5), date.minusMonths(4)), true));
        yrkesaktivitetBuilder.leggTilAktivitetsAvtale(yrkesaktivitetBuilder
            .getAktivitetsAvtaleBuilder(DatoIntervallEntitet.fraOgMedTilOgMed(date.minusMonths(4), date.minusMonths(3)), true));
        yrkesaktivitetBuilder.leggTilAktivitetsAvtale(yrkesaktivitetBuilder
            .getAktivitetsAvtaleBuilder(DatoIntervallEntitet.fraOgMedTilOgMed(date.minusMonths(3), date.minusMonths(2)), true));
        yrkesaktivitetBuilder.leggTilAktivitetsAvtale(yrkesaktivitetBuilder
            .getAktivitetsAvtaleBuilder(DatoIntervallEntitet.fraOgMedTilOgMed(date.minusMonths(2), date.minusMonths(1)), true));
        yrkesaktivitetBuilder.leggTilAktivitetsAvtale(yrkesaktivitetBuilder
            .getAktivitetsAvtaleBuilder(DatoIntervallEntitet.fraOgMedTilOgMed(date.minusMonths(1), date), true));
        yrkesaktivitetBuilder.leggTilAktivitetsAvtale(yrkesaktivitetBuilder
            .getAktivitetsAvtaleBuilder(DatoIntervallEntitet.fraOgMedTilOgMed(date, date.plusMonths(1)), true));

        Yrkesaktivitet yrkesaktivitet = yrkesaktivitetBuilder.build();

        assertThat(KortvarigArbeidsforholdTjeneste.erKortvarigYrkesaktivitetSomAvsluttesEtterSkjæringstidspunkt(new YrkesaktivitetFilter(Optional.empty(), yrkesaktivitet), beregningsgrunnlag, yrkesaktivitet)).isFalse();
    }


    @Test
    public void over_6_måneder_delt_opp_i_flere_deler_men_med_huller() {
        LocalDate date = LocalDate.now();
        BeregningsgrunnlagEntitet beregningsgrunnlag = BeregningsgrunnlagEntitet.builder()
            .medSkjæringstidspunkt(date.minusMonths(1))
            .build();

        YrkesaktivitetBuilder yrkesaktivitetBuilder = YrkesaktivitetBuilder.oppdatere(Optional.empty())
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);
        yrkesaktivitetBuilder.leggTilAktivitetsAvtale(yrkesaktivitetBuilder
            .getAktivitetsAvtaleBuilder(DatoIntervallEntitet.fraOgMedTilOgMed(date.minusMonths(5), date.minusMonths(4)), true));
        yrkesaktivitetBuilder.leggTilAktivitetsAvtale(yrkesaktivitetBuilder
            .getAktivitetsAvtaleBuilder(DatoIntervallEntitet.fraOgMedTilOgMed(date.minusMonths(4), date.minusMonths(3)), true));
        yrkesaktivitetBuilder.leggTilAktivitetsAvtale(yrkesaktivitetBuilder
            .getAktivitetsAvtaleBuilder(DatoIntervallEntitet.fraOgMedTilOgMed(date.minusMonths(3), date.minusMonths(2)), true));
        yrkesaktivitetBuilder.leggTilAktivitetsAvtale(yrkesaktivitetBuilder
            .getAktivitetsAvtaleBuilder(DatoIntervallEntitet.fraOgMedTilOgMed(date.minusMonths(2), date.minusMonths(1)), true));
        yrkesaktivitetBuilder.leggTilAktivitetsAvtale(yrkesaktivitetBuilder
            .getAktivitetsAvtaleBuilder(DatoIntervallEntitet.fraOgMedTilOgMed(date.minusMonths(1), date), true));
        yrkesaktivitetBuilder.leggTilAktivitetsAvtale(yrkesaktivitetBuilder
            .getAktivitetsAvtaleBuilder(DatoIntervallEntitet.fraOgMedTilOgMed(date.plusMonths(1), date.plusMonths(2)), true));

        Yrkesaktivitet yrkesaktivitet = yrkesaktivitetBuilder.build();

        assertThat(KortvarigArbeidsforholdTjeneste.erKortvarigYrkesaktivitetSomAvsluttesEtterSkjæringstidspunkt(new YrkesaktivitetFilter(Optional.empty(), yrkesaktivitet), beregningsgrunnlag, yrkesaktivitet)).isTrue();
    }
}
