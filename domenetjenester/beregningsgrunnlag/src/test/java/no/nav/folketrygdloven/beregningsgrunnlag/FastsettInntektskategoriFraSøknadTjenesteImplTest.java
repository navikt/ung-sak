package no.nav.folketrygdloven.beregningsgrunnlag;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.Test;

import no.nav.folketrygdloven.beregningsgrunnlag.modell.BGAndelArbeidsforhold;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagAktivitetStatus;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPeriode;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlagBuilder;
import no.nav.foreldrepenger.domene.iay.modell.OppgittOpptjeningBuilder;
import no.nav.foreldrepenger.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.kodeverk.arbeidsforhold.AktivitetStatus;
import no.nav.k9.kodeverk.arbeidsforhold.Inntektskategori;
import no.nav.k9.kodeverk.organisasjon.VirksomhetType;
import no.nav.k9.sak.typer.Arbeidsgiver;

public class FastsettInntektskategoriFraSøknadTjenesteImplTest {


    private static final LocalDate SKJÆRINGSTIDSPUNKT_OPPTJENING = LocalDate.of(2018, Month.MAY, 10);
    private static final BigDecimal GRUNNBELØP = BigDecimal.valueOf(90000);

    private static final String ARBEIDSFORHOLD_ORGNR = "973152351";

    private final FastsettInntektskategoriFraSøknadTjeneste fastsettInntektskategoriFraSøknadTjeneste = new FastsettInntektskategoriFraSøknadTjeneste();

    private BeregningsgrunnlagEntitet lagBeregningsgrunnlag(AktivitetStatus aktivitetStatus) {
        BeregningsgrunnlagEntitet beregningsgrunnlag = BeregningsgrunnlagEntitet.builder()
            .medSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT_OPPTJENING)
            .medGrunnbeløp(GRUNNBELØP)
            .build();
        BeregningsgrunnlagAktivitetStatus.builder()
            .medAktivitetStatus(aktivitetStatus)
            .build(beregningsgrunnlag);
        BeregningsgrunnlagPeriode periode = BeregningsgrunnlagPeriode.builder()
            .medBeregningsgrunnlagPeriode(SKJÆRINGSTIDSPUNKT_OPPTJENING, null)
            .build(beregningsgrunnlag);
        BGAndelArbeidsforhold.Builder bga = BGAndelArbeidsforhold
            .builder()
            .medArbeidsperiodeFom(LocalDate.now().minusYears(1))
            .medArbeidsperiodeTom(LocalDate.now().plusYears(2))
            .medArbeidsgiver(Arbeidsgiver.virksomhet(ARBEIDSFORHOLD_ORGNR));
        BeregningsgrunnlagPrStatusOgAndel.builder()
            .medBGAndelArbeidsforhold(bga)
            .medAktivitetStatus(aktivitetStatus)
            .build(periode);
        return beregningsgrunnlag;
    }


    private InntektArbeidYtelseGrunnlag opprettOppgittOpptjening(List<VirksomhetType> næringtyper) {
        OppgittOpptjeningBuilder oob = OppgittOpptjeningBuilder.ny();
        ArrayList<OppgittOpptjeningBuilder.EgenNæringBuilder> egneNæringBuilders = new ArrayList<>();
        LocalDate fraOgMed = LocalDate.now().minusMonths(1);
        LocalDate tilOgMed = LocalDate.now().plusMonths(1);
        DatoIntervallEntitet periode = DatoIntervallEntitet.fraOgMedTilOgMed(fraOgMed, tilOgMed);
        for (VirksomhetType type : næringtyper) {
            egneNæringBuilders.add(OppgittOpptjeningBuilder.EgenNæringBuilder.ny().medVirksomhetType(type).medPeriode(periode));
        }
        oob.leggTilEgneNæringer(egneNæringBuilders);
        return InntektArbeidYtelseGrunnlagBuilder.nytt().medOppgittOpptjening(oob).build();
    }

    @Test
    public void arbeidstakerSkalTilRiktigInntektskategori() {
        // Arrange
        final var grunnlag = InntektArbeidYtelseGrunnlagBuilder.nytt().build();
        BeregningsgrunnlagEntitet beregningsgrunnlag = lagBeregningsgrunnlag(AktivitetStatus.ARBEIDSTAKER);

        // Act
        fastsettInntektskategoriFraSøknadTjeneste.fastsettInntektskategori(beregningsgrunnlag, grunnlag);

        // Assert
        BeregningsgrunnlagPeriode periode = beregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0);
        List<BeregningsgrunnlagPrStatusOgAndel> andeler = periode.getBeregningsgrunnlagPrStatusOgAndelList();
        assertThat(andeler).hasSize(1);
        assertThat(andeler.get(0).getInntektskategori()).isEqualTo(Inntektskategori.ARBEIDSTAKER);
    }

    @Test
    public void frilanserSkalTilRiktigInntektskategori() {
        // Arrange
        final var grunnlag = InntektArbeidYtelseGrunnlagBuilder.nytt().build();
        BeregningsgrunnlagEntitet beregningsgrunnlag = lagBeregningsgrunnlag(AktivitetStatus.FRILANSER);

        // Act
        fastsettInntektskategoriFraSøknadTjeneste.fastsettInntektskategori(beregningsgrunnlag, grunnlag);

        // Assert
        BeregningsgrunnlagPeriode periode = beregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0);
        List<BeregningsgrunnlagPrStatusOgAndel> andeler = periode.getBeregningsgrunnlagPrStatusOgAndelList();
        assertThat(andeler).hasSize(1);
        assertThat(andeler.get(0).getInntektskategori()).isEqualTo(Inntektskategori.FRILANSER);
    }


    @Test
    public void dagpengerSkalTilRiktigInntektskategori() {
        // Arrange
        final var grunnlag = InntektArbeidYtelseGrunnlagBuilder.nytt().build();
        BeregningsgrunnlagEntitet beregningsgrunnlag = lagBeregningsgrunnlag(AktivitetStatus.DAGPENGER);

        // Act
        fastsettInntektskategoriFraSøknadTjeneste.fastsettInntektskategori(beregningsgrunnlag, grunnlag);

        // Assert
        BeregningsgrunnlagPeriode periode = beregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0);
        List<BeregningsgrunnlagPrStatusOgAndel> andeler = periode.getBeregningsgrunnlagPrStatusOgAndelList();
        assertThat(andeler).hasSize(1);
        assertThat(andeler.get(0).getInntektskategori()).isEqualTo(Inntektskategori.DAGPENGER);
    }

    @Test
    public void arbeidsavklaringspengerSkalTilRiktigInntektskategori() {
        // Arrange
        final var grunnlag = InntektArbeidYtelseGrunnlagBuilder.nytt().build();
        BeregningsgrunnlagEntitet beregningsgrunnlag = lagBeregningsgrunnlag(AktivitetStatus.ARBEIDSAVKLARINGSPENGER);

        // Act
        fastsettInntektskategoriFraSøknadTjeneste.fastsettInntektskategori(beregningsgrunnlag, grunnlag);

        // Assert
        BeregningsgrunnlagPeriode periode = beregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0);
        List<BeregningsgrunnlagPrStatusOgAndel> andeler = periode.getBeregningsgrunnlagPrStatusOgAndelList();
        assertThat(andeler).hasSize(1);
        assertThat(andeler.get(0).getInntektskategori()).isEqualTo(Inntektskategori.ARBEIDSAVKLARINGSPENGER);
    }


    @Test
    public void SNUtenFiskeJordbrukEllerDagmammaSkalTilRiktigInntektskategori() {
        // Arrange
        final var grunnlag = opprettOppgittOpptjening(Collections.singletonList(VirksomhetType.ANNEN));
        BeregningsgrunnlagEntitet beregningsgrunnlag = lagBeregningsgrunnlag(AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE);

        // Act
        fastsettInntektskategoriFraSøknadTjeneste.fastsettInntektskategori(beregningsgrunnlag, grunnlag);

        // Assert
        BeregningsgrunnlagPeriode periode = beregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0);
        List<BeregningsgrunnlagPrStatusOgAndel> andeler = periode.getBeregningsgrunnlagPrStatusOgAndelList();
        assertThat(andeler).hasSize(1);
        assertThat(andeler.get(0).getInntektskategori()).isEqualTo(Inntektskategori.SELVSTENDIG_NÆRINGSDRIVENDE);
    }

    @Test
    public void SNMedFiskeSkalTilRiktigInntektskategori() {
        // Arrange
        final var grunnlag = opprettOppgittOpptjening(Collections.singletonList(VirksomhetType.FISKE));
        BeregningsgrunnlagEntitet beregningsgrunnlag = lagBeregningsgrunnlag(AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE);

        // Act
        fastsettInntektskategoriFraSøknadTjeneste.fastsettInntektskategori(beregningsgrunnlag, grunnlag);

        // Assert
        BeregningsgrunnlagPeriode periode = beregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0);
        List<BeregningsgrunnlagPrStatusOgAndel> andeler = periode.getBeregningsgrunnlagPrStatusOgAndelList();
        assertThat(andeler).hasSize(1);
        assertThat(andeler.get(0).getInntektskategori()).isEqualTo(Inntektskategori.FISKER);
    }

    @Test
    public void SNMedJorbrukSkalTilRiktigInntektskategori() {
        // Arrange
        final var grunnlag = opprettOppgittOpptjening(Collections.singletonList(VirksomhetType.JORDBRUK_SKOGBRUK));
        BeregningsgrunnlagEntitet beregningsgrunnlag = lagBeregningsgrunnlag(AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE);

        // Act
        fastsettInntektskategoriFraSøknadTjeneste.fastsettInntektskategori(beregningsgrunnlag, grunnlag);

        // Assert
        BeregningsgrunnlagPeriode periode = beregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0);
        List<BeregningsgrunnlagPrStatusOgAndel> andeler = periode.getBeregningsgrunnlagPrStatusOgAndelList();
        assertThat(andeler).hasSize(1);
        assertThat(andeler.get(0).getInntektskategori()).isEqualTo(Inntektskategori.JORDBRUKER);
    }

    @Test
    public void SNMedDagmammaSkalTilRiktigInntektskategori() {
        // Arrange
        final var grunnlag = opprettOppgittOpptjening(Collections.singletonList(VirksomhetType.DAGMAMMA));
        BeregningsgrunnlagEntitet beregningsgrunnlag = lagBeregningsgrunnlag(AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE);

        // Act
        fastsettInntektskategoriFraSøknadTjeneste.fastsettInntektskategori(beregningsgrunnlag, grunnlag);

        // Assert
        BeregningsgrunnlagPeriode periode = beregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0);
        List<BeregningsgrunnlagPrStatusOgAndel> andeler = periode.getBeregningsgrunnlagPrStatusOgAndelList();
        assertThat(andeler).hasSize(1);
        assertThat(andeler.get(0).getInntektskategori()).isEqualTo(Inntektskategori.DAGMAMMA);
    }

    @Test
    public void SNMedFiskeOgJordbrukSkalMappeTilInntektskategoriFisker() {
        // Arrange
        final var grunnlag = opprettOppgittOpptjening(List.of(VirksomhetType.FISKE, VirksomhetType.JORDBRUK_SKOGBRUK));
        BeregningsgrunnlagEntitet beregningsgrunnlag = lagBeregningsgrunnlag(AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE);

        // Act
        fastsettInntektskategoriFraSøknadTjeneste.fastsettInntektskategori(beregningsgrunnlag, grunnlag);

        // Assert
        BeregningsgrunnlagPeriode periode = beregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0);
        List<BeregningsgrunnlagPrStatusOgAndel> andeler = periode.getBeregningsgrunnlagPrStatusOgAndelList();
        assertThat(andeler).hasSize(1);
        assertThat(andeler.get(0).getInntektskategori()).isEqualTo(Inntektskategori.FISKER);
    }

    @Test
    public void SNMedFiskeOgDagmammaSkalMappeTilInntektskategoriFisker() {
        // Arrange
        final var grunnlag = opprettOppgittOpptjening(List.of(VirksomhetType.DAGMAMMA, VirksomhetType.FISKE));
        BeregningsgrunnlagEntitet beregningsgrunnlag = lagBeregningsgrunnlag(AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE);

        // Act
        fastsettInntektskategoriFraSøknadTjeneste.fastsettInntektskategori(beregningsgrunnlag, grunnlag);

        // Assert
        BeregningsgrunnlagPeriode periode = beregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0);
        List<BeregningsgrunnlagPrStatusOgAndel> andeler = periode.getBeregningsgrunnlagPrStatusOgAndelList();
        assertThat(andeler).hasSize(1);
        assertThat(andeler.get(0).getInntektskategori()).isEqualTo(Inntektskategori.FISKER);
    }

    @Test
    public void SNMedJordbrukOgDagmammaSkalMappeTilInntektskategoriJordbruker() {
        // Arrange
        final var grunnlag = opprettOppgittOpptjening(List.of(VirksomhetType.DAGMAMMA, VirksomhetType.JORDBRUK_SKOGBRUK));
        BeregningsgrunnlagEntitet beregningsgrunnlag = lagBeregningsgrunnlag(AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE);

        // Act
        fastsettInntektskategoriFraSøknadTjeneste.fastsettInntektskategori(beregningsgrunnlag, grunnlag);

        // Assert
        BeregningsgrunnlagPeriode periode = beregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0);
        List<BeregningsgrunnlagPrStatusOgAndel> andeler = periode.getBeregningsgrunnlagPrStatusOgAndelList();
        assertThat(andeler).hasSize(1);
        assertThat(andeler.get(0).getInntektskategori()).isEqualTo(Inntektskategori.JORDBRUKER);
    }

    @Test
    public void SNMedJordbrukOgOrdinærNæringSkalMappeTilInntektskategoriJordbruker() {
        // Arrange
        final var grunnlag = opprettOppgittOpptjening(List.of(VirksomhetType.ANNEN, VirksomhetType.JORDBRUK_SKOGBRUK));
        BeregningsgrunnlagEntitet beregningsgrunnlag = lagBeregningsgrunnlag(AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE);

        // Act
        fastsettInntektskategoriFraSøknadTjeneste.fastsettInntektskategori(beregningsgrunnlag, grunnlag);

        // Assert
        BeregningsgrunnlagPeriode periode = beregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0);
        List<BeregningsgrunnlagPrStatusOgAndel> andeler = periode.getBeregningsgrunnlagPrStatusOgAndelList();
        assertThat(andeler).hasSize(1);
        assertThat(andeler.get(0).getInntektskategori()).isEqualTo(Inntektskategori.JORDBRUKER);
    }

    @Test
    public void SNMedDagmammaOgOrdinærNæringSkalMappeTilInntektskategoriJordbruker() {
        // Arrange
        final var grunnlag = opprettOppgittOpptjening(List.of(VirksomhetType.ANNEN, VirksomhetType.DAGMAMMA));
        BeregningsgrunnlagEntitet beregningsgrunnlag = lagBeregningsgrunnlag(AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE);

        // Act
        fastsettInntektskategoriFraSøknadTjeneste.fastsettInntektskategori(beregningsgrunnlag, grunnlag);

        // Assert
        BeregningsgrunnlagPeriode periode = beregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0);
        List<BeregningsgrunnlagPrStatusOgAndel> andeler = periode.getBeregningsgrunnlagPrStatusOgAndelList();
        assertThat(andeler).hasSize(1);
        assertThat(andeler.get(0).getInntektskategori()).isEqualTo(Inntektskategori.DAGMAMMA);
    }

    @Test
    public void SNMedFiskeOgOrdinærNæringSkalMappeTilInntektskategoriFisker() {
        // Arrange
        final var grunnlag = opprettOppgittOpptjening(List.of(VirksomhetType.ANNEN, VirksomhetType.FISKE));
        BeregningsgrunnlagEntitet beregningsgrunnlag = lagBeregningsgrunnlag(AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE);

        // Act
        fastsettInntektskategoriFraSøknadTjeneste.fastsettInntektskategori(beregningsgrunnlag, grunnlag);

        // Assert
        BeregningsgrunnlagPeriode periode = beregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0);
        List<BeregningsgrunnlagPrStatusOgAndel> andeler = periode.getBeregningsgrunnlagPrStatusOgAndelList();
        assertThat(andeler).hasSize(1);
        assertThat(andeler.get(0).getInntektskategori()).isEqualTo(Inntektskategori.FISKER);
    }

    @Test
    public void skalReturnereFiskerSomHøgastPrioriterteInntektskategori() {
        List<Inntektskategori> inntektskategoriList = List.of(Inntektskategori.FISKER, Inntektskategori.SELVSTENDIG_NÆRINGSDRIVENDE, Inntektskategori.DAGMAMMA, Inntektskategori.JORDBRUKER);
        Optional<Inntektskategori> prioritert = fastsettInntektskategoriFraSøknadTjeneste.finnHøyestPrioriterteInntektskategoriForSN(inntektskategoriList);
        assertThat(prioritert.get()).isEqualTo(Inntektskategori.FISKER);
    }

    @Test
    public void skalReturnereJordbrukerSomHøgastPrioriterteInntektskategori() {
        List<Inntektskategori> inntektskategoriList = List.of(Inntektskategori.SELVSTENDIG_NÆRINGSDRIVENDE, Inntektskategori.DAGMAMMA, Inntektskategori.JORDBRUKER);
        Optional<Inntektskategori> prioritert = fastsettInntektskategoriFraSøknadTjeneste.finnHøyestPrioriterteInntektskategoriForSN(inntektskategoriList);
        assertThat(prioritert.get()).isEqualTo(Inntektskategori.JORDBRUKER);
    }

    @Test
    public void skalReturnereDagmammaSomHøgastPrioriterteInntektskategori() {
        List<Inntektskategori> inntektskategoriList = List.of(Inntektskategori.SELVSTENDIG_NÆRINGSDRIVENDE, Inntektskategori.DAGMAMMA);
        Optional<Inntektskategori> prioritert = fastsettInntektskategoriFraSøknadTjeneste.finnHøyestPrioriterteInntektskategoriForSN(inntektskategoriList);
        assertThat(prioritert.get()).isEqualTo(Inntektskategori.DAGMAMMA);
    }

    @Test
    public void skalReturnereSelvstendigNæringsdrivendeSomHøgastPrioriterteInntektskategori() {
        List<Inntektskategori> inntektskategoriList = List.of(Inntektskategori.SELVSTENDIG_NÆRINGSDRIVENDE);
        Optional<Inntektskategori> prioritert = fastsettInntektskategoriFraSøknadTjeneste.finnHøyestPrioriterteInntektskategoriForSN(inntektskategoriList);
        assertThat(prioritert.get()).isEqualTo(Inntektskategori.SELVSTENDIG_NÆRINGSDRIVENDE);
    }
}
