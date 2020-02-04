package no.nav.folketrygdloven.beregningsgrunnlag.skjæringstidspunkt;

import static no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType.ARBEID;
import static no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType.VENTELØNN_VARTPENGER;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;

import no.nav.folketrygdloven.beregningsgrunnlag.AvklarAktiviteterTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningAktivitetAggregatEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningAktivitetEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagAktivitetStatus;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.Periode;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.iay.modell.AktørYtelse;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlagBuilder;
import no.nav.foreldrepenger.domene.iay.modell.VersjonType;
import no.nav.foreldrepenger.domene.iay.modell.YtelseAnvist;
import no.nav.foreldrepenger.domene.iay.modell.YtelseBuilder;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.domene.typer.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.tid.ÅpenDatoIntervallEntitet;
import no.nav.k9.kodeverk.Fagsystem;
import no.nav.k9.kodeverk.behandling.BehandlingStatus;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.iay.AktivitetStatus;
import no.nav.k9.kodeverk.iay.RelatertYtelseTilstand;
import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;
import no.nav.vedtak.util.Tuple;

public class AvklarAktiviteterTjenesteImplTest {
    private static final LocalDate SKJÆRINGSTIDSPUNKT_BEREGNING = LocalDate.of(2018, 9, 30);
    private static final Arbeidsgiver ARBEIDSGIVER = Arbeidsgiver.virksomhet("900050001");
    private static final AktørId AKTØR_ID = AktørId.dummy();
    private static final long FAGSAK_ID = 39827532L;
    private static final long BEHANDLING_ID = 4234034L;

    private BehandlingReferanse behandlingReferanse;
    private AvklarAktiviteterTjeneste avklarAktiviteterTjeneste;

    @Before
    public void setUp() {
        behandlingReferanse = nyBehandling();
        avklarAktiviteterTjeneste = new AvklarAktiviteterTjeneste();
    }

    @Test
    public void skal_returnere_false_om_ingen_aktiviteter() {
        // Arrange
        BeregningAktivitetAggregatEntitet beregningAktivitetAggregat = BeregningAktivitetAggregatEntitet.builder()
            .medSkjæringstidspunktOpptjening(SKJÆRINGSTIDSPUNKT_BEREGNING)
            .build();

        // Act
        boolean harVentelønnEllerVartpenger = avklarAktiviteterTjeneste.harVentelønnEllerVartpengerSomSisteAktivitetIOpptjeningsperioden(beregningAktivitetAggregat);

        // Assert
        assertThat(harVentelønnEllerVartpenger).isFalse();
    }

    @Test
    public void skal_returnere_false_om_aktiviteter_som_ikke_er_ventelønn_vartpenger() {
        // Arrange
        BeregningAktivitetEntitet arbeidAktivitet = lagBeregningAktivitetAggregat(SKJÆRINGSTIDSPUNKT_BEREGNING.minusMonths(10),
            SKJÆRINGSTIDSPUNKT_BEREGNING, ARBEID);
        BeregningAktivitetAggregatEntitet beregningAktivitetAggregat = BeregningAktivitetAggregatEntitet.builder()
            .medSkjæringstidspunktOpptjening(SKJÆRINGSTIDSPUNKT_BEREGNING)
            .leggTilAktivitet(
                arbeidAktivitet
            ).build();

        // Act
        boolean harVentelønnEllerVartpenger = avklarAktiviteterTjeneste.harVentelønnEllerVartpengerSomSisteAktivitetIOpptjeningsperioden(beregningAktivitetAggregat);

        // Assert
        assertThat(harVentelønnEllerVartpenger).isFalse();
    }

    @Test
    public void skal_returnere_false_om_ventelønn_vertpenger_ikke_er_siste_aktivitet() {
        // Arrange
        BeregningAktivitetEntitet arbeidsperiode = lagBeregningAktivitetAggregat(SKJÆRINGSTIDSPUNKT_BEREGNING.minusMonths(10),
            SKJÆRINGSTIDSPUNKT_BEREGNING, ARBEID);
        BeregningAktivitetEntitet ventelønnVartpenger = lagBeregningAktivitetAggregat(SKJÆRINGSTIDSPUNKT_BEREGNING.minusMonths(10),
            SKJÆRINGSTIDSPUNKT_BEREGNING.minusDays(1), VENTELØNN_VARTPENGER);
        BeregningAktivitetAggregatEntitet beregningAktivitetAggregat = BeregningAktivitetAggregatEntitet.builder()
            .medSkjæringstidspunktOpptjening(SKJÆRINGSTIDSPUNKT_BEREGNING)
            .leggTilAktivitet(arbeidsperiode)
            .leggTilAktivitet(ventelønnVartpenger)
            .build();

        // Act
        boolean harVentelønnEllerVartpenger = avklarAktiviteterTjeneste.harVentelønnEllerVartpengerSomSisteAktivitetIOpptjeningsperioden(beregningAktivitetAggregat);

        // Assert
        assertThat(harVentelønnEllerVartpenger).isFalse();
    }

    @Test
    public void skal_returnere_true_om_ventelønn_vertpenger_avslutter_samtidig_med_siste_aktivitet() {
        // Arrange
        BeregningAktivitetEntitet arbeidsperiode = lagBeregningAktivitetAggregat(SKJÆRINGSTIDSPUNKT_BEREGNING.minusMonths(10),
            SKJÆRINGSTIDSPUNKT_BEREGNING, ARBEID);
        BeregningAktivitetEntitet ventelønnVartpenger = lagBeregningAktivitetAggregat(SKJÆRINGSTIDSPUNKT_BEREGNING.minusMonths(10),
            SKJÆRINGSTIDSPUNKT_BEREGNING, VENTELØNN_VARTPENGER);
        BeregningAktivitetAggregatEntitet beregningAktivitetAggregat = BeregningAktivitetAggregatEntitet.builder()
            .medSkjæringstidspunktOpptjening(SKJÆRINGSTIDSPUNKT_BEREGNING)
            .leggTilAktivitet(arbeidsperiode)
            .leggTilAktivitet(ventelønnVartpenger)
            .build();

        // Act
        boolean harVentelønnEllerVartpenger = avklarAktiviteterTjeneste.harVentelønnEllerVartpengerSomSisteAktivitetIOpptjeningsperioden(beregningAktivitetAggregat);

        // Assert
        assertThat(harVentelønnEllerVartpenger).isTrue();
    }

    @Test
    public void skal_returnere_true_om_ventelønn_vartpenger_avslutter_etter_arbeidsaktivitet_som_slutter_dagen_før_skjæringstidspunkt() {
        // Arrange
        BeregningAktivitetEntitet arbeidsperiode = lagBeregningAktivitetAggregat(SKJÆRINGSTIDSPUNKT_BEREGNING.minusMonths(10),
            SKJÆRINGSTIDSPUNKT_BEREGNING.minusDays(1), ARBEID);
        BeregningAktivitetEntitet ventelønnVartpenger = lagBeregningAktivitetAggregat(SKJÆRINGSTIDSPUNKT_BEREGNING.minusMonths(10),
            SKJÆRINGSTIDSPUNKT_BEREGNING.plusDays(1), VENTELØNN_VARTPENGER);
        BeregningAktivitetAggregatEntitet beregningAktivitetAggregat = BeregningAktivitetAggregatEntitet.builder()
            .medSkjæringstidspunktOpptjening(SKJÆRINGSTIDSPUNKT_BEREGNING)
            .leggTilAktivitet(arbeidsperiode)
            .leggTilAktivitet(ventelønnVartpenger)
            .build();

        // Act
        boolean harVentelønnEllerVartpenger = avklarAktiviteterTjeneste.harVentelønnEllerVartpengerSomSisteAktivitetIOpptjeningsperioden(beregningAktivitetAggregat);

        // Assert
        assertThat(harVentelønnEllerVartpenger).isTrue();
    }

    @Test
    public void skal_returnere_true_om_ventelønn_vartpenger_sammen_med_arbeid_som_starter_på_skjæringstidspunkt() {
        // Arrange
        BeregningAktivitetEntitet arbeidsperiode = lagBeregningAktivitetAggregat(SKJÆRINGSTIDSPUNKT_BEREGNING, SKJÆRINGSTIDSPUNKT_BEREGNING.plusDays(10), ARBEID);
        BeregningAktivitetEntitet ventelønnVartpenger = lagBeregningAktivitetAggregat(SKJÆRINGSTIDSPUNKT_BEREGNING.minusMonths(10),
            SKJÆRINGSTIDSPUNKT_BEREGNING.plusDays(1), VENTELØNN_VARTPENGER);
        BeregningAktivitetAggregatEntitet beregningAktivitetAggregat = BeregningAktivitetAggregatEntitet.builder()
            .medSkjæringstidspunktOpptjening(SKJÆRINGSTIDSPUNKT_BEREGNING)
            .leggTilAktivitet(arbeidsperiode)
            .leggTilAktivitet(ventelønnVartpenger)
            .build();

        // Act
        boolean harVentelønnEllerVartpenger = avklarAktiviteterTjeneste.harVentelønnEllerVartpengerSomSisteAktivitetIOpptjeningsperioden(beregningAktivitetAggregat);

        // Assert
        assertThat(harVentelønnEllerVartpenger).isTrue();
    }

    @Test
    public void skal_returnere_false_om_ventelønn_vartpenger_starter_på_skjæringstidspunkt() {
        // Arrange
        BeregningAktivitetEntitet arbeidsperiode = lagBeregningAktivitetAggregat(SKJÆRINGSTIDSPUNKT_BEREGNING.minusMonths(10),
            SKJÆRINGSTIDSPUNKT_BEREGNING.plusDays(10), ARBEID);
        BeregningAktivitetEntitet ventelønnVartpenger = lagBeregningAktivitetAggregat(SKJÆRINGSTIDSPUNKT_BEREGNING,
            SKJÆRINGSTIDSPUNKT_BEREGNING.plusDays(1), VENTELØNN_VARTPENGER);
        BeregningAktivitetAggregatEntitet beregningAktivitetAggregat = BeregningAktivitetAggregatEntitet.builder()
            .medSkjæringstidspunktOpptjening(SKJÆRINGSTIDSPUNKT_BEREGNING)
            .leggTilAktivitet(arbeidsperiode)
            .leggTilAktivitet(ventelønnVartpenger)
            .build();

        // Act
        boolean harVentelønnEllerVartpenger = avklarAktiviteterTjeneste.harVentelønnEllerVartpengerSomSisteAktivitetIOpptjeningsperioden(beregningAktivitetAggregat);

        // Assert
        assertThat(harVentelønnEllerVartpenger).isFalse();
    }

    @Test
    public void skal_returnere_false_når_ikke_AAP() {
        //Arrange
        BeregningsgrunnlagEntitet beregningsgrunnlag = beregningsgrunnlagMedStatus(AktivitetStatus.ARBEIDSTAKER);

        //Act
        boolean harFullAAPMedAndreAktiviteter = avklarAktiviteterTjeneste.harFullAAPPåStpMedAndreAktiviteter(beregningsgrunnlag, Optional.empty());

        //Assert
        assertThat(harFullAAPMedAndreAktiviteter).isFalse();
    }

    @Test
    public void skal_returnere_false_når_bare_AAP_uten_andre_aktiviteter_på_stp() {
        //Arrange
        BeregningsgrunnlagEntitet beregningsgrunnlag = beregningsgrunnlagMedStatus(AktivitetStatus.ARBEIDSAVKLARINGSPENGER);

        //Act
        boolean harFullAAPMedAndreAktiviteter = avklarAktiviteterTjeneste.harFullAAPPåStpMedAndreAktiviteter(beregningsgrunnlag, Optional.empty());

        //Assert
        assertThat(harFullAAPMedAndreAktiviteter).isFalse();
    }

    @Test
    public void skal_returnere_false_når_AAP_med_andre_aktiviteter_på_stp_med_siste_meldekort_uten_full_utbetaling() {
        //Arrange
        BeregningsgrunnlagEntitet beregningsgrunnlag = beregningsgrunnlagMedStatus(AktivitetStatus.ARBEIDSAVKLARINGSPENGER, AktivitetStatus.ARBEIDSTAKER);

        Tuple<Periode, Integer> meldekort1 = lagMeldekort(SKJÆRINGSTIDSPUNKT_BEREGNING.minusDays(15), 200);
        Tuple<Periode, Integer> meldekort2 = lagMeldekort(SKJÆRINGSTIDSPUNKT_BEREGNING.minusDays(1), 180);
        InntektArbeidYtelseGrunnlag iayGrunnlag = lagAktørYtelse(meldekort1, meldekort2);

        //Act
        boolean harFullAAPMedAndreAktiviteter = avklarAktiviteterTjeneste.harFullAAPPåStpMedAndreAktiviteter(beregningsgrunnlag, getAktørYtelseFraRegister(behandlingReferanse, iayGrunnlag));

        //Assert
        assertThat(harFullAAPMedAndreAktiviteter).isFalse();
    }

    @Test
    public void skal_returnere_true_når_AAP_med_andre_aktiviteter_på_stp_med_siste_meldekort_med_full_utbetaling() {
        //Arrange
        BeregningsgrunnlagEntitet beregningsgrunnlag = beregningsgrunnlagMedStatus(AktivitetStatus.ARBEIDSAVKLARINGSPENGER, AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE);

        Tuple<Periode, Integer> meldekort1 = lagMeldekort(SKJÆRINGSTIDSPUNKT_BEREGNING.minusDays(15), 99);
        Tuple<Periode, Integer> meldekort2 = lagMeldekort(SKJÆRINGSTIDSPUNKT_BEREGNING.minusDays(1), 200);
        Tuple<Periode, Integer> meldekort3 = lagMeldekort(SKJÆRINGSTIDSPUNKT_BEREGNING.plusDays(1), 179); //Skal ikke tas med siden avsluttes etter stp
        InntektArbeidYtelseGrunnlag iayGrunnlag = lagAktørYtelse(meldekort1, meldekort2, meldekort3);

        //Act
        boolean harFullAAPMedAndreAktiviteter = avklarAktiviteterTjeneste.harFullAAPPåStpMedAndreAktiviteter(beregningsgrunnlag, getAktørYtelseFraRegister(behandlingReferanse, iayGrunnlag));

        //Assert
        assertThat(harFullAAPMedAndreAktiviteter).isTrue();
    }


    private BeregningsgrunnlagEntitet beregningsgrunnlagMedStatus(AktivitetStatus... aktivitetStatus) {
        BeregningsgrunnlagEntitet.Builder builder = BeregningsgrunnlagEntitet.builder();
        builder.medSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT_BEREGNING);
        Stream.of(aktivitetStatus).forEach(status -> builder.leggTilAktivitetStatus(BeregningsgrunnlagAktivitetStatus.builder().medAktivitetStatus(status)));
        return builder.build();
    }


    private BeregningAktivitetEntitet lagBeregningAktivitetAggregat(LocalDate fom, LocalDate tom, OpptjeningAktivitetType type) {
        return BeregningAktivitetEntitet.builder()
            .medArbeidsgiver(ARBEIDSGIVER)
            .medOpptjeningAktivitetType(type)
            .medPeriode(ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(fom, tom))
            .build();
    }

    @SafeVarargs
    private InntektArbeidYtelseGrunnlag lagAktørYtelse(Tuple<Periode, Integer>... meldekortPerioder) {
        InntektArbeidYtelseAggregatBuilder inntektArbeidYtelseAggregatBuilder = InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.empty(), VersjonType.REGISTER);
        InntektArbeidYtelseAggregatBuilder.AktørYtelseBuilder aktørYtelseBuilder = inntektArbeidYtelseAggregatBuilder.getAktørYtelseBuilder(AKTØR_ID);
        
        YtelseBuilder ytelseBuilder = YtelseBuilder.oppdatere(Optional.empty())
                .medKilde(Fagsystem.ARENA)
                .medYtelseType(FagsakYtelseType.ARBEIDSAVKLARINGSPENGER)
                .medSaksnummer(Saksnummer.arena("12345"));
        
        ytelseBuilder.medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT_BEREGNING.minusMonths(6), SKJÆRINGSTIDSPUNKT_BEREGNING.minusDays(1)));
        ytelseBuilder.medStatus(RelatertYtelseTilstand.LØPENDE);
        if (meldekortPerioder != null && meldekortPerioder.length > 0) {
            Stream.of(meldekortPerioder).forEach(meldekort -> ytelseBuilder.medYtelseAnvist(lagYtelseAnvist(ytelseBuilder, meldekort.getElement1(), meldekort.getElement2())));
        }
        aktørYtelseBuilder.leggTilYtelse(ytelseBuilder);
        inntektArbeidYtelseAggregatBuilder.leggTilAktørYtelse(aktørYtelseBuilder);
        return InntektArbeidYtelseGrunnlagBuilder.nytt().medData(inntektArbeidYtelseAggregatBuilder).build();
    }

    private YtelseAnvist lagYtelseAnvist(YtelseBuilder ytelseBuilder, Periode periode, int utbetalingsgrad) {
        return ytelseBuilder.getAnvistBuilder()
            .medAnvistPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(periode.getFom(), periode.getTom()))
            .medUtbetalingsgradProsent(BigDecimal.valueOf(utbetalingsgrad))
            .medDagsats(BigDecimal.valueOf(1000))
            .medBeløp(BigDecimal.valueOf(10000))
            .build();
    }

    private Tuple<Periode, Integer> lagMeldekort(LocalDate tom, int utbetalingsgrad) {
        return new Tuple<>(Periode.of(tom.minusDays(13), tom), utbetalingsgrad);
    }

    private BehandlingReferanse nyBehandling() {
        return BehandlingReferanse.fra(
            FagsakYtelseType.FORELDREPENGER,
            BehandlingType.FØRSTEGANGSSØKNAD,
            AKTØR_ID,
            new Saksnummer("2398040234"),
            FAGSAK_ID,
            BEHANDLING_ID,
            UUID.randomUUID(),
            Optional.empty(),
            BehandlingStatus.UTREDES,
            Skjæringstidspunkt.builder().medFørsteUttaksdato(SKJÆRINGSTIDSPUNKT_BEREGNING).build()
        );
    }

    private Optional<AktørYtelse> getAktørYtelseFraRegister(BehandlingReferanse ref, InntektArbeidYtelseGrunnlag iayGrunnlag) {
        return iayGrunnlag.getAktørYtelseFraRegister(ref.getAktørId());
    }

}
