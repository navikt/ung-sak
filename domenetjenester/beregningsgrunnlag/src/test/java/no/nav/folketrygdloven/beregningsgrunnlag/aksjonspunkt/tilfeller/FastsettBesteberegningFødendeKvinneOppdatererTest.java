package no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.tilfeller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;

import no.nav.folketrygdloven.beregningsgrunnlag.RepositoryProvider;
import no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.dto.BesteberegningFødendeKvinneAndelDto;
import no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.dto.BesteberegningFødendeKvinneDto;
import no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.dto.DagpengeAndelLagtTilBesteberegningDto;
import no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.dto.FaktaBeregningLagreDto;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BGAndelArbeidsforhold;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPeriode;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.FaktaOmBeregningTilfelle;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.aktivitet.AktivitetStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.aktivitet.Inntektskategori;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Virksomhet;
import no.nav.foreldrepenger.behandlingslager.virksomhet.VirksomhetEntitet;
import no.nav.foreldrepenger.domene.arbeidsgiver.VirksomhetTjeneste;
import no.nav.foreldrepenger.domene.typer.Beløp;

public class FastsettBesteberegningFødendeKvinneOppdatererTest {

    private static final Long ANDELSNR_DAGPENGER = 1L;
    private static final Long ANDELSNR_ARBEIDSTAKER = 2L;
    private static final List<FaktaOmBeregningTilfelle> FAKTA_OM_BEREGNING_TILFELLER = Collections.singletonList(FaktaOmBeregningTilfelle.FASTSETT_BESTEBEREGNING_FØDENDE_KVINNE);
    private final LocalDate SKJÆRINGSTIDSPUNKT = LocalDate.now();
    private final Beløp GRUNNBELØP = new Beløp(600000);

    private RepositoryProvider repositoryProvider = mock(RepositoryProvider.class);
    private AksjonspunktRepository aksjonspunktRepository = mock(AksjonspunktRepository.class);
    private FastsettBesteberegningFødendeKvinneOppdaterer fastsettBesteberegningFødendeKvinneOppdaterer;
    private BeregningsgrunnlagEntitet beregningsgrunnlag;
    private BeregningsgrunnlagPrStatusOgAndel dagpengeAndel;
    private BeregningsgrunnlagPrStatusOgAndel arbeidstakerAndel;
    private Virksomhet virksomhet = new VirksomhetEntitet.Builder().medOrgnr("234432423").build();
    private Arbeidsgiver arbeidsgiver = Arbeidsgiver.fra(virksomhet);


    @Before
    public void setup() {
        VirksomhetTjeneste virksomhetTjeneste = mock(VirksomhetTjeneste.class);
        when(virksomhetTjeneste.hentOgLagreOrganisasjon(any())).thenReturn(virksomhet);
        when(repositoryProvider.getAksjonspunktRepository()).thenReturn(aksjonspunktRepository);
        fastsettBesteberegningFødendeKvinneOppdaterer = new FastsettBesteberegningFødendeKvinneOppdaterer();
        beregningsgrunnlag = BeregningsgrunnlagEntitet.builder()
            .medGrunnbeløp(GRUNNBELØP)
            .medSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT)
            .leggTilFaktaOmBeregningTilfeller(FAKTA_OM_BEREGNING_TILFELLER)
            .build();
        BeregningsgrunnlagPeriode periode1 = BeregningsgrunnlagPeriode.builder()
            .medBeregningsgrunnlagPeriode(SKJÆRINGSTIDSPUNKT, SKJÆRINGSTIDSPUNKT.plusMonths(2).minusDays(1))
            .build(beregningsgrunnlag);
        dagpengeAndel = BeregningsgrunnlagPrStatusOgAndel.builder()
            .medAndelsnr(ANDELSNR_DAGPENGER)
            .medLagtTilAvSaksbehandler(false)
            .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
            .medAktivitetStatus(AktivitetStatus.DAGPENGER)
            .medBGAndelArbeidsforhold(BGAndelArbeidsforhold.builder().medArbeidsgiver(arbeidsgiver))
            .build(periode1);
        arbeidstakerAndel = BeregningsgrunnlagPrStatusOgAndel.builder()
            .medAndelsnr(ANDELSNR_ARBEIDSTAKER)
            .medLagtTilAvSaksbehandler(false)
            .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
            .medBGAndelArbeidsforhold(BGAndelArbeidsforhold.builder().medArbeidsgiver(arbeidsgiver))
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .build(periode1);
    }

    @Test
    public void skal_sette_inntekt_på_andeler() {
        // Arrange
        FaktaBeregningLagreDto dto = new FaktaBeregningLagreDto(FAKTA_OM_BEREGNING_TILFELLER);
        int dagpengerBeregnet = 10000;
        BesteberegningFødendeKvinneAndelDto dpDto = new BesteberegningFødendeKvinneAndelDto(ANDELSNR_DAGPENGER, dagpengerBeregnet, Inntektskategori.DAGPENGER, false);
        int arbeidstakerBeregnet = 20000;
        BesteberegningFødendeKvinneAndelDto atDto = new BesteberegningFødendeKvinneAndelDto(ANDELSNR_ARBEIDSTAKER, arbeidstakerBeregnet,
            Inntektskategori.ARBEIDSTAKER, false);
        BesteberegningFødendeKvinneDto bbDto = new BesteberegningFødendeKvinneDto(List.of(dpDto, atDto));
        dto.setBesteberegningAndeler(bbDto);

        // Act);
        fastsettBesteberegningFødendeKvinneOppdaterer.oppdater(dto, null, beregningsgrunnlag, Optional.empty());

        // Assert
        assertThat(dagpengeAndel.getBesteberegningPrÅr()).isEqualByComparingTo(BigDecimal.valueOf(dagpengerBeregnet*12));
        assertThat(dagpengeAndel.getFastsattAvSaksbehandler()).isTrue();
        assertThat(arbeidstakerAndel.getBeregnetPrÅr()).isEqualByComparingTo(BigDecimal.valueOf(arbeidstakerBeregnet*12));
        assertThat(arbeidstakerAndel.getFastsattAvSaksbehandler()).isTrue();

    }

    @Test
    public void skal_sette_inntekt_på_andeler_og_legge_til_ny_dagpengeandel() {
        // Arrange
        BeregningsgrunnlagEntitet bg = lagBGUtenDagpenger();
        FaktaBeregningLagreDto dto = new FaktaBeregningLagreDto(Collections.singletonList(FaktaOmBeregningTilfelle.FASTSETT_BESTEBEREGNING_FØDENDE_KVINNE));
        int dagpengerBeregnet = 10000;
        var dpDto = new DagpengeAndelLagtTilBesteberegningDto(dagpengerBeregnet, Inntektskategori.DAGPENGER);
        int arbeidstakerBeregnet = 20000;
        var atDto = new BesteberegningFødendeKvinneAndelDto(ANDELSNR_ARBEIDSTAKER, arbeidstakerBeregnet,
            Inntektskategori.ARBEIDSTAKER, false);
        BesteberegningFødendeKvinneDto bbDto = new BesteberegningFødendeKvinneDto(List.of(atDto), dpDto);
        dto.setBesteberegningAndeler(bbDto);

        // Act
        fastsettBesteberegningFødendeKvinneOppdaterer.oppdater(dto, null, bg, Optional.empty());

        // Assert
        BeregningsgrunnlagPrStatusOgAndel dpAndel = bg.getBeregningsgrunnlagPerioder().get(0).getBeregningsgrunnlagPrStatusOgAndelList().stream()
            .filter(andel -> andel.getLagtTilAvSaksbehandler())
            .findFirst().get();
        assertThat(dpAndel.getBesteberegningPrÅr()).isEqualByComparingTo(BigDecimal.valueOf(dagpengerBeregnet*12));
        assertThat(dpAndel.getBeregnetPrÅr()).isEqualByComparingTo(BigDecimal.valueOf(dagpengerBeregnet*12));
        assertThat(dpAndel.getFastsattAvSaksbehandler()).isTrue();
        BeregningsgrunnlagPrStatusOgAndel atAndel = bg.getBeregningsgrunnlagPerioder().get(0).getBeregningsgrunnlagPrStatusOgAndelList().stream()
            .filter(andel -> !andel.getLagtTilAvSaksbehandler())
            .findFirst().get();
        assertThat(atAndel.getBeregnetPrÅr()).isEqualByComparingTo(BigDecimal.valueOf(arbeidstakerBeregnet*12));
        assertThat(atAndel.getFastsattAvSaksbehandler()).isTrue();

    }


    @Test
    public void skal_kunne_bekrefte_aksjonspunkt_på_nytt_med_dagpengeandel() {
        // Arrange
        BeregningsgrunnlagEntitet nyttBg = lagBGUtenDagpenger();
        FaktaBeregningLagreDto dto = new FaktaBeregningLagreDto(Collections.singletonList(FaktaOmBeregningTilfelle.FASTSETT_BESTEBEREGNING_FØDENDE_KVINNE));
        int dagpengerBeregnet = 10000;
        BesteberegningFødendeKvinneAndelDto dpDto = new BesteberegningFødendeKvinneAndelDto(ANDELSNR_DAGPENGER, dagpengerBeregnet,
            Inntektskategori.DAGPENGER,
            true);
        int arbeidstakerBeregnet = 20000;
        BesteberegningFødendeKvinneAndelDto atDto = new BesteberegningFødendeKvinneAndelDto(ANDELSNR_ARBEIDSTAKER, arbeidstakerBeregnet,
            Inntektskategori.ARBEIDSTAKER, false);
        BesteberegningFødendeKvinneDto bbDto = new BesteberegningFødendeKvinneDto(List.of(dpDto, atDto));
        dto.setBesteberegningAndeler(bbDto);

        // Act
        fastsettBesteberegningFødendeKvinneOppdaterer.oppdater(dto, null, nyttBg,
            Optional.of(beregningsgrunnlag));

        // Assert
        BeregningsgrunnlagPrStatusOgAndel dpAndel = nyttBg.getBeregningsgrunnlagPerioder().get(0).getBeregningsgrunnlagPrStatusOgAndelList().stream().filter(andel -> andel.getLagtTilAvSaksbehandler())
            .findFirst().get();
        assertThat(dpAndel.getBesteberegningPrÅr()).isEqualByComparingTo(BigDecimal.valueOf(dagpengerBeregnet*12));
        assertThat(dpAndel.getBeregnetPrÅr()).isEqualByComparingTo(BigDecimal.valueOf(dagpengerBeregnet*12));
        assertThat(dpAndel.getFastsattAvSaksbehandler()).isTrue();
        BeregningsgrunnlagPrStatusOgAndel atAndel = nyttBg.getBeregningsgrunnlagPerioder().get(0).getBeregningsgrunnlagPrStatusOgAndelList().stream()
            .filter(andel -> !andel.getLagtTilAvSaksbehandler())
            .findFirst().get();
        assertThat(atAndel.getBeregnetPrÅr()).isEqualByComparingTo(BigDecimal.valueOf(arbeidstakerBeregnet*12));
        assertThat(atAndel.getFastsattAvSaksbehandler()).isTrue();

    }
    private BeregningsgrunnlagEntitet lagBGUtenDagpenger() {
        BeregningsgrunnlagEntitet bg = BeregningsgrunnlagEntitet.builder()
            .medGrunnbeløp(GRUNNBELØP)
            .medSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT)
            .leggTilFaktaOmBeregningTilfeller(FAKTA_OM_BEREGNING_TILFELLER)
            .build();
        BeregningsgrunnlagPeriode periode1 = BeregningsgrunnlagPeriode.builder()
            .medBeregningsgrunnlagPeriode(SKJÆRINGSTIDSPUNKT, SKJÆRINGSTIDSPUNKT.plusMonths(2).minusDays(1))
            .build(bg);
        BeregningsgrunnlagPrStatusOgAndel.builder()
            .medAndelsnr(ANDELSNR_ARBEIDSTAKER)
            .medLagtTilAvSaksbehandler(false)
            .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .medBGAndelArbeidsforhold(BGAndelArbeidsforhold.builder().medArbeidsgiver(arbeidsgiver))
            .build(periode1);
        return bg;
    }

}
