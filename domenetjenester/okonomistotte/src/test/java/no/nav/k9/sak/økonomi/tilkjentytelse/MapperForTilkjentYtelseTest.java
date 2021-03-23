package no.nav.k9.sak.økonomi.tilkjentytelse;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import no.nav.k9.kodeverk.arbeidsforhold.Inntektskategori;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.oppdrag.kontrakt.kodeverk.SatsType;
import no.nav.k9.oppdrag.kontrakt.tilkjentytelse.TilkjentYtelseAndelV1;
import no.nav.k9.oppdrag.kontrakt.tilkjentytelse.TilkjentYtelseFeriepengerV1;
import no.nav.k9.oppdrag.kontrakt.tilkjentytelse.TilkjentYtelsePeriodeV1;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.Beløp;

class MapperForTilkjentYtelseTest {
    LocalDate idag = LocalDate.now();

    Arbeidsgiver virksomhet1 = Arbeidsgiver.virksomhet("111111111");
    Arbeidsgiver virksomhet2 = Arbeidsgiver.virksomhet("222222222");

    BeregningsresultatEntitet beregningsresultat = BeregningsresultatEntitet.builder()
        .medRegelInput("foo")
        .medRegelSporing("bar")
        .build();

    MapperForTilkjentYtelse mapperOMP = new MapperForTilkjentYtelse(FagsakYtelseType.OMSORGSPENGER);

    @Test
    void skal_mappe_periode_med_andel_og_feriepenger() {
        BeregningsresultatPeriode brPeriode = BeregningsresultatPeriode.builder()
            .medBeregningsresultatPeriodeFomOgTom(idag, idag)
            .build(beregningsresultat);

        forhåndsutfylltBuilder()
            .medBrukerErMottaker(false)
            .medArbeidsgiver(virksomhet1)
            .medUtbetalingsgrad(BigDecimal.valueOf(80))
            .medDagsats(500)
            .medFeriepengerÅrsbeløp(new Beløp(BigDecimal.valueOf(51)))
            .buildFor(brPeriode);

        List<TilkjentYtelsePeriodeV1> resultat = mapperOMP.mapTilkjentYtelse(beregningsresultat);

        assertThat(resultat).hasSize(1);
        TilkjentYtelsePeriodeV1 periode = resultat.get(0);
        assertThat(periode.getAndeler()).hasSize(1);
        TilkjentYtelseAndelV1 andel = periode.getAndeler().iterator().next();
        assertThat(andel.getArbeidsgiverOrgNr()).isEqualTo(virksomhet1.getIdentifikator());
        assertThat(andel.getSatsBeløp()).isEqualTo(500L);
        assertThat(andel.getSatsType()).isEqualTo(SatsType.DAG7);
        assertThat(andel.getUtbetalingsgrad()).isEqualTo(BigDecimal.valueOf(80));
        assertThat(andel.getArbeidsgiverAktørId()).isNull();
        assertThat(andel.getUtbetalesTilBruker()).isFalse();

        List<TilkjentYtelseFeriepengerV1> feriepenger = andel.getFeriepenger();
        assertThat(feriepenger).hasSize(1);
        assertThat(feriepenger.get(0).getBeløp()).isEqualTo(51L);
        assertThat(feriepenger.get(0).getOpptjeningsår()).isEqualTo(idag.getYear());
    }


    @Test
    void skal_ha_med_orgnr_når_bruker_har_andel_hos_to_arbeidsgivere_i_samme_periode() {
        BeregningsresultatPeriode brPeriode = BeregningsresultatPeriode.builder()
            .medBeregningsresultatPeriodeFomOgTom(idag, idag)
            .build(beregningsresultat);

        forhåndsutfylltBuilder().medArbeidsgiver(virksomhet1).buildFor(brPeriode);
        forhåndsutfylltBuilder().medArbeidsgiver(virksomhet2).buildFor(brPeriode);

        List<TilkjentYtelsePeriodeV1> resultat = mapperOMP.mapTilkjentYtelse(beregningsresultat);

        assertThat(resultat).hasSize(1);
        TilkjentYtelsePeriodeV1 periode = resultat.get(0);
        assertThat(periode.getAndeler()).hasSize(2);
        List<TilkjentYtelseAndelV1> andeler = new ArrayList<>(periode.getAndeler());
        assertThat(andeler.get(0).getArbeidsgiverOrgNr()).isEqualTo(virksomhet1.getIdentifikator());
        assertThat(andeler.get(1).getArbeidsgiverOrgNr()).isEqualTo(virksomhet2.getIdentifikator());
    }

    @Test
    void skal_ikke_ha_med_orgnr_når_bruker_har_andel_hos_kun_en_arbeidsgiver_siden_det_ikke_er_nødvendig_å_skille() {
        BeregningsresultatPeriode brPeriode = BeregningsresultatPeriode.builder()
            .medBeregningsresultatPeriodeFomOgTom(idag, idag)
            .build(beregningsresultat);

        forhåndsutfylltBuilder().medArbeidsgiver(virksomhet1).buildFor(brPeriode);

        List<TilkjentYtelsePeriodeV1> resultat = mapperOMP.mapTilkjentYtelse(beregningsresultat);

        assertThat(resultat).hasSize(1);
        TilkjentYtelsePeriodeV1 periode = resultat.get(0);
        assertThat(periode.getAndeler()).hasSize(1);
        assertThat(periode.getAndeler().iterator().next().getArbeidsgiverOrgNr()).isNull();
    }

    private BeregningsresultatAndel.Builder forhåndsutfylltBuilder() {
        return BeregningsresultatAndel.builder()
            .medDagsatsFraBg(1000)
            .medDagsats(1000)
            .medUtbetalingsgrad(BigDecimal.valueOf(100))
            .medStillingsprosent(BigDecimal.valueOf(100))
            .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
            .medBrukerErMottaker(true);
    }

}
