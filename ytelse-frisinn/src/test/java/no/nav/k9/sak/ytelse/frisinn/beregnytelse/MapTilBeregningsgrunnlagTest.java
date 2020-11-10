package no.nav.k9.sak.ytelse.frisinn.beregnytelse;

import static no.nav.k9.sak.domene.typer.tid.AbstractLocalDateInterval.TIDENES_ENDE;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import no.nav.folketrygdloven.beregningsgrunnlag.modell.Beregningsgrunnlag;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPeriode;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.k9.kodeverk.arbeidsforhold.AktivitetStatus;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.typer.Beløp;

public class MapTilBeregningsgrunnlagTest {

    public static final LocalDate STP = LocalDate.of(2020, 3, 1);

    @Test
    public void skal_mappe_førstegangsbehandling() {

        DatoIntervallEntitet søknadsperiode = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2020, 4, 1), LocalDate.of(2020, 4, 30));
        Beregningsgrunnlag bg = lagBeregningsgrunnlag(søknadsperiode, BigDecimal.valueOf(260));

        Optional<Beregningsgrunnlag> mappetGrunnlag = MapTilBeregningsgrunnlag.mapBeregningsgrunnlagForNyeSøknadsperioder(bg, Optional.empty(), søknadsperiode);

        assertThat(mappetGrunnlag).isPresent();
        assertThat(mappetGrunnlag.get().getBeregningsgrunnlagPerioder().size()).isEqualTo(1);
        assertThat(mappetGrunnlag.get().getBeregningsgrunnlagPerioder().get(0).getDagsats()).isEqualTo(1);
    }

    @Test
    public void skal_mappe_revurdering() {
        DatoIntervallEntitet søknadsperiode = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2020, 4, 1), LocalDate.of(2020, 4, 30));
        Beregningsgrunnlag bg = lagBeregningsgrunnlag(søknadsperiode, BigDecimal.valueOf(260));
        Beregningsgrunnlag bgRevurdering = lagBeregningsgrunnlag(søknadsperiode, BigDecimal.valueOf(260 * 2));

        Optional<Beregningsgrunnlag> mappetGrunnlag = MapTilBeregningsgrunnlag.mapBeregningsgrunnlagForNyeSøknadsperioder(bgRevurdering, Optional.of(bg), søknadsperiode);

        assertThat(mappetGrunnlag).isPresent();
        assertThat(mappetGrunnlag.get().getBeregningsgrunnlagPerioder().size()).isEqualTo(1);
        assertThat(mappetGrunnlag.get().getBeregningsgrunnlagPerioder().get(0).getDagsats()).isEqualTo(2);
    }

    @Test
    public void skal_mappe_tilkommet_mai_søknad() {
        DatoIntervallEntitet søknadsperiodeApril = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2020, 4, 1), LocalDate.of(2020, 4, 30));
        DatoIntervallEntitet søknadsperiodeMai = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2020, 5, 1), LocalDate.of(2020, 5, 31));

        Beregningsgrunnlag bg = lagBeregningsgrunnlag(søknadsperiodeApril, BigDecimal.valueOf(260));

        Beregningsgrunnlag bgRevurderingSøknad = lagBeregningsgrunnlagMed2Søknadsperioer(søknadsperiodeApril, BigDecimal.valueOf(260 * 2), søknadsperiodeMai, BigDecimal.valueOf(260 * 2));

        Optional<Beregningsgrunnlag> mappetGrunnlag = MapTilBeregningsgrunnlag.mapBeregningsgrunnlagForNyeSøknadsperioder(bgRevurderingSøknad, Optional.of(bg), søknadsperiodeMai);

        assertThat(mappetGrunnlag).isPresent();
        assertThat(mappetGrunnlag.get().getBeregningsgrunnlagPerioder().size()).isEqualTo(2);
        assertThat(mappetGrunnlag.get().getBeregningsgrunnlagPerioder().get(0).getDagsats()).isEqualTo(1);
        assertThat(mappetGrunnlag.get().getBeregningsgrunnlagPerioder().get(1).getDagsats()).isEqualTo(2);
    }


    @Test
    public void skal_mappe_tilkommet_juni_søknad_ved_overhopp_i_mai() {
        DatoIntervallEntitet søknadsperiodeApril = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2020, 4, 1), LocalDate.of(2020, 4, 30));
        DatoIntervallEntitet søknadsperiodeJuni = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2020, 6, 1), LocalDate.of(2020, 6, 30));

        Beregningsgrunnlag bg = lagBeregningsgrunnlag(søknadsperiodeApril, BigDecimal.valueOf(260));

        Beregningsgrunnlag bgRevurderingSøknad = lagBeregningsgrunnlagMed2Søknadsperioer(søknadsperiodeApril, BigDecimal.valueOf(260 * 2), søknadsperiodeJuni, BigDecimal.valueOf(260 * 2));

        Optional<Beregningsgrunnlag> mappetGrunnlag = MapTilBeregningsgrunnlag.mapBeregningsgrunnlagForNyeSøknadsperioder(bgRevurderingSøknad, Optional.of(bg), søknadsperiodeJuni);

        assertThat(mappetGrunnlag).isPresent();
        assertThat(mappetGrunnlag.get().getBeregningsgrunnlagPerioder().size()).isEqualTo(2);
        assertThat(mappetGrunnlag.get().getBeregningsgrunnlagPerioder().get(0).getDagsats()).isEqualTo(1);
        assertThat(mappetGrunnlag.get().getBeregningsgrunnlagPerioder().get(1).getDagsats()).isEqualTo(2);
    }


    @Test
    public void skal_mappe_tilkommet_mai_søknad_starter_5_mai() {
        DatoIntervallEntitet søknadsperiodeApril = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2020, 4, 1), LocalDate.of(2020, 4, 30));
        DatoIntervallEntitet søknadsperiodeMai = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2020, 5, 5), LocalDate.of(2020, 5, 31));

        Beregningsgrunnlag bg = lagBeregningsgrunnlag(søknadsperiodeApril, BigDecimal.valueOf(260));

        Beregningsgrunnlag bgRevurderingSøknad = lagBeregningsgrunnlagMed2Søknadsperioer(søknadsperiodeApril, BigDecimal.valueOf(260 * 2), søknadsperiodeMai, BigDecimal.valueOf(260 * 2));

        Optional<Beregningsgrunnlag> mappetGrunnlag = MapTilBeregningsgrunnlag.mapBeregningsgrunnlagForNyeSøknadsperioder(bgRevurderingSøknad, Optional.of(bg), søknadsperiodeMai);

        assertThat(mappetGrunnlag).isPresent();
        assertThat(mappetGrunnlag.get().getBeregningsgrunnlagPerioder().size()).isEqualTo(2);
        assertThat(mappetGrunnlag.get().getBeregningsgrunnlagPerioder().get(0).getDagsats()).isEqualTo(1);
        assertThat(mappetGrunnlag.get().getBeregningsgrunnlagPerioder().get(1).getDagsats()).isEqualTo(2);
    }

    private Beregningsgrunnlag lagBeregningsgrunnlag(DatoIntervallEntitet søknadsperiode, BigDecimal bruttoPrÅr) {
        Beregningsgrunnlag bg = Beregningsgrunnlag.builder()
            .medSkjæringstidspunkt(STP)
            .medGrunnbeløp(Beløp.ZERO)
            .build();
        lagPeriodeMedAndel(bg, BigDecimal.ZERO, STP, søknadsperiode.getFomDato().minusDays(1));
        lagPeriodeMedAndel(bg, bruttoPrÅr, søknadsperiode.getFomDato(), søknadsperiode.getTomDato());
        lagPeriodeMedAndel(bg, BigDecimal.ZERO, søknadsperiode.getTomDato(), TIDENES_ENDE);
        return bg;
    }

    private Beregningsgrunnlag lagBeregningsgrunnlagMed2Søknadsperioer(DatoIntervallEntitet søknadsperiode, BigDecimal bruttoPrÅr, DatoIntervallEntitet søknadsperiode2, BigDecimal brutto2) {
        Beregningsgrunnlag bg = Beregningsgrunnlag.builder()
            .medSkjæringstidspunkt(STP)
            .medGrunnbeløp(Beløp.ZERO)
            .build();
        lagPeriodeMedAndel(bg, BigDecimal.ZERO, STP, søknadsperiode.getFomDato().minusDays(1));
        lagPeriodeMedAndel(bg, bruttoPrÅr, søknadsperiode.getFomDato(), søknadsperiode.getTomDato());
        lagPeriodeMedAndel(bg, brutto2, søknadsperiode2.getFomDato(), søknadsperiode2.getTomDato());
        lagPeriodeMedAndel(bg, BigDecimal.ONE, søknadsperiode2.getTomDato(), TIDENES_ENDE);

        return bg;
    }

    private BeregningsgrunnlagPeriode lagPeriodeMedAndel(Beregningsgrunnlag bg, BigDecimal bruttoPrÅr, LocalDate fomDato, LocalDate tomDato) {
        BeregningsgrunnlagPrStatusOgAndel.Builder andelBuilder = BeregningsgrunnlagPrStatusOgAndel.builder().medAktivitetStatus(AktivitetStatus.FRILANSER)
            .medRedusertPrÅr(bruttoPrÅr)
            .medRedusertBrukersAndelPrÅr(bruttoPrÅr)
            .medRedusertRefusjonPrÅr(BigDecimal.ZERO);
        BeregningsgrunnlagPeriode søknadBgPeriode = BeregningsgrunnlagPeriode.builder()
            .medRedusertPrÅr(bruttoPrÅr)
            .medBruttoPrÅr(bruttoPrÅr)
            .leggTilBeregningsgrunnlagPrStatusOgAndel(andelBuilder)
            .medBeregningsgrunnlagPeriode(fomDato, tomDato)
            .build(bg);

        return søknadBgPeriode;
    }
}
