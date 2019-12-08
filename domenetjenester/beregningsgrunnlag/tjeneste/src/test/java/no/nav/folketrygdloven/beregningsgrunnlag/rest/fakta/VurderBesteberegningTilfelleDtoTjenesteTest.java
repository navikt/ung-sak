package no.nav.folketrygdloven.beregningsgrunnlag.rest.fakta;

import static no.nav.vedtak.felles.jpa.tid.ÅpenDatoIntervallEntitet.fraOgMedTilOgMed;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;

import no.nav.folketrygdloven.beregningsgrunnlag.input.BeregningsgrunnlagInput;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BGAndelArbeidsforhold;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagAktivitetStatus;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagGrunnlagBuilder;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPeriode;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagTilstand;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.FaktaOmBeregningTilfelle;
import no.nav.folketrygdloven.beregningsgrunnlag.rest.dto.FaktaOmBeregningDto;
import no.nav.folketrygdloven.beregningsgrunnlag.testutilities.behandling.TestScenarioBuilder;
import no.nav.folketrygdloven.beregningsgrunnlag.testutilities.behandling.beregningsgrunnlag.BeregningAktivitetTestUtil;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.aktivitet.AktivitetStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.aktivitet.Inntektskategori;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.vedtak.felles.jpa.tid.ÅpenDatoIntervallEntitet;

public class VurderBesteberegningTilfelleDtoTjenesteTest {

    private static final LocalDate STP = LocalDate.of(2019, 1, 1);
    private static final ÅpenDatoIntervallEntitet OPPTJENINGSPERIODE = fraOgMedTilOgMed(STP.minusYears(1), STP.plusYears(10));
    private static final BGAndelArbeidsforhold.Builder bgAndelArbeidsforholdBuilder = BGAndelArbeidsforhold.builder();
    private VurderBesteberegningTilfelleDtoTjeneste dtoTjeneste;

    @Before
    public void setUp() {
        var orgnr = "347289324";
        bgAndelArbeidsforholdBuilder
            .medArbeidsgiver(Arbeidsgiver.virksomhet(orgnr));
        dtoTjeneste = new VurderBesteberegningTilfelleDtoTjeneste();
    }

    @Test
    public void skal_ikke_sette_verdier_på_dto_om_man_ikkje_har_tilfelle() {
        // Arrange
        var beregningAktiviteter = BeregningAktivitetTestUtil.opprettBeregningAktiviteter(STP, OPPTJENINGSPERIODE,
            OpptjeningAktivitetType.ARBEID);
        var beregningsgrunnlag = BeregningsgrunnlagEntitet.builder().medSkjæringstidspunkt(STP)
            .leggTilAktivitetStatus(BeregningsgrunnlagAktivitetStatus.builder().medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)).build();
        var grunnlag = BeregningsgrunnlagGrunnlagBuilder.oppdatere(Optional.empty())
            .medBeregningsgrunnlag(beregningsgrunnlag)
            .medRegisterAktiviteter(beregningAktiviteter)
            .build(1L, BeregningsgrunnlagTilstand.OPPDATERT_MED_ANDELER);

        // Act
        var faktaOmBeregningDto = new FaktaOmBeregningDto();

        var input = new BeregningsgrunnlagInput(lagReferanse(), null, null, null, null)
                .medBeregningsgrunnlagGrunnlag(grunnlag);
        dtoTjeneste.lagDto(input, Optional.empty(), faktaOmBeregningDto);

        // Assert
        assertThat(faktaOmBeregningDto.getVurderBesteberegning()).isNull();

    }

    @Test
    public void skal_sette_verdier_på_dto() {
        // Arrange
        var beregningAktiviteter = BeregningAktivitetTestUtil.opprettBeregningAktiviteter(STP, OPPTJENINGSPERIODE,
            OpptjeningAktivitetType.ARBEID);
        var beregningsgrunnlag = BeregningsgrunnlagEntitet.builder().medSkjæringstidspunkt(STP)
            .leggTilFaktaOmBeregningTilfeller(Collections.singletonList(FaktaOmBeregningTilfelle.VURDER_BESTEBEREGNING))
            .leggTilAktivitetStatus(BeregningsgrunnlagAktivitetStatus.builder().medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)).build();
        var periode = BeregningsgrunnlagPeriode.builder().medBeregningsgrunnlagPeriode(STP, null).build(beregningsgrunnlag);
        BeregningsgrunnlagPrStatusOgAndel.builder().medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .medBGAndelArbeidsforhold(bgAndelArbeidsforholdBuilder)
            .medInntektskategori(Inntektskategori.JORDBRUKER)
            .build(periode);
        var grunnlag = BeregningsgrunnlagGrunnlagBuilder.oppdatere(Optional.empty())
            .medBeregningsgrunnlag(beregningsgrunnlag)
            .medRegisterAktiviteter(beregningAktiviteter)
            .build(1L, BeregningsgrunnlagTilstand.OPPDATERT_MED_ANDELER);

        // Act
        var faktaOmBeregningDto = new FaktaOmBeregningDto();

        var input = new BeregningsgrunnlagInput(lagReferanse(), null, null, null, null)
                .medBeregningsgrunnlagGrunnlag(grunnlag);
        dtoTjeneste.lagDto(input, Optional.empty(), faktaOmBeregningDto);

        // Assert
        assertThat(faktaOmBeregningDto.getVurderBesteberegning().getSkalHaBesteberegning()).isNull();
    }

    @Test
    public void skal_sette_verdier_på_dto_fra_forrige_grunnlag() {
        // Arrange
        var beregningAktiviteter = BeregningAktivitetTestUtil.opprettBeregningAktiviteter(STP, OPPTJENINGSPERIODE,
            OpptjeningAktivitetType.ARBEID);
        var beregningsgrunnlag = BeregningsgrunnlagEntitet.builder().medSkjæringstidspunkt(STP)
            .leggTilFaktaOmBeregningTilfeller(Collections.singletonList(FaktaOmBeregningTilfelle.VURDER_BESTEBEREGNING))
            .leggTilAktivitetStatus(BeregningsgrunnlagAktivitetStatus.builder().medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)).build();
        var periode = BeregningsgrunnlagPeriode.builder().medBeregningsgrunnlagPeriode(STP, null).build(beregningsgrunnlag);
        BeregningsgrunnlagPrStatusOgAndel.builder().medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .medBGAndelArbeidsforhold(bgAndelArbeidsforholdBuilder)
            .medInntektskategori(Inntektskategori.JORDBRUKER)
            .build(periode);
        var grunnlag = BeregningsgrunnlagGrunnlagBuilder.oppdatere(Optional.empty())
            .medBeregningsgrunnlag(beregningsgrunnlag)
            .build(1L, BeregningsgrunnlagTilstand.OPPDATERT_MED_ANDELER);

        var forrigeBg = beregningsgrunnlag.dypKopi();
        var andelFraForrigeGrunnlag = forrigeBg.getBeregningsgrunnlagPerioder().get(0).getBeregningsgrunnlagPrStatusOgAndelList()
            .get(0);
        BigDecimal fastsatt = BigDecimal.TEN;
        BeregningsgrunnlagPrStatusOgAndel.builder(andelFraForrigeGrunnlag)
            .medFastsattAvSaksbehandler(true)
            .medInntektskategori(Inntektskategori.JORDBRUKER)
            .medBesteberegningPrÅr(fastsatt.multiply(BigDecimal.valueOf(12)));
        var forrigeGrunnlag = BeregningsgrunnlagGrunnlagBuilder.oppdatere(Optional.empty())
            .medBeregningsgrunnlag(forrigeBg)
            .medRegisterAktiviteter(beregningAktiviteter)
            .build(1L, BeregningsgrunnlagTilstand.KOFAKBER_UT);

        // Act
        var faktaOmBeregningDto = new FaktaOmBeregningDto();
        var input = new BeregningsgrunnlagInput(lagReferanse(), null, null, null, null)
                .medBeregningsgrunnlagGrunnlag(grunnlag);

        dtoTjeneste.lagDto(input, Optional.of(forrigeGrunnlag), faktaOmBeregningDto);

        // Assert
        assertThat(faktaOmBeregningDto.getVurderBesteberegning().getSkalHaBesteberegning()).isTrue();
    }

    private BehandlingReferanse lagReferanse() {
        return TestScenarioBuilder.nyttScenario().lagMocked();
    }
}
