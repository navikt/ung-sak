package no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.tilfeller;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;

import no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.dto.FaktaBeregningLagreDto;
import no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.dto.VurderLønnsendringDto;
import no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.tilfeller.VurderLønnsendringOppdaterer;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BGAndelArbeidsforhold;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPeriode;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.typer.Beløp;
import no.nav.k9.kodeverk.beregningsgrunnlag.FaktaOmBeregningTilfelle;
import no.nav.k9.kodeverk.iay.AktivitetStatus;
import no.nav.k9.kodeverk.iay.Inntektskategori;

public class VurderLønnsendringOppdatererTest {
    private static final Long ANDELSNR_ARBEIDSTAKER = 2L;
    private static final List<FaktaOmBeregningTilfelle> FAKTA_OM_BEREGNING_TILFELLER = Collections.singletonList(FaktaOmBeregningTilfelle.VURDER_LØNNSENDRING);
    public static final String ORGNR = "8934232423";
    private final LocalDate SKJÆRINGSTIDSPUNKT = LocalDate.now();
    private final Beløp GRUNNBELØP = new Beløp(600000);

    private VurderLønnsendringOppdaterer vurderLønnsendringOppdaterer;
    private BeregningsgrunnlagEntitet beregningsgrunnlag;
    private BeregningsgrunnlagPrStatusOgAndel frilansAndel;
    private BeregningsgrunnlagPrStatusOgAndel arbeidstakerAndel;

    @Before
    public void setup() {
        vurderLønnsendringOppdaterer = new VurderLønnsendringOppdaterer();
        Arbeidsgiver virksomheten = Arbeidsgiver.virksomhet(ORGNR);
        beregningsgrunnlag = BeregningsgrunnlagEntitet.builder()
            .medGrunnbeløp(GRUNNBELØP)
            .medSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT)
            .leggTilFaktaOmBeregningTilfeller(FAKTA_OM_BEREGNING_TILFELLER)
            .build();
        BeregningsgrunnlagPeriode periode1 = BeregningsgrunnlagPeriode.builder()
            .medBeregningsgrunnlagPeriode(SKJÆRINGSTIDSPUNKT, SKJÆRINGSTIDSPUNKT.plusMonths(2).minusDays(1))
            .build(beregningsgrunnlag);
        frilansAndel = BeregningsgrunnlagPrStatusOgAndel.builder()
            .medAndelsnr(3252L)
            .medLagtTilAvSaksbehandler(false)
            .medInntektskategori(Inntektskategori.FRILANSER)
            .medAktivitetStatus(AktivitetStatus.FRILANSER)
            .build(periode1);
        arbeidstakerAndel = BeregningsgrunnlagPrStatusOgAndel.builder()
            .medBGAndelArbeidsforhold(BGAndelArbeidsforhold.builder().medArbeidsgiver(virksomheten))
            .medAndelsnr(ANDELSNR_ARBEIDSTAKER)
            .medLagtTilAvSaksbehandler(false)
            .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .build(periode1);
    }

    @Test
    public void skal_sette_lønnsendring_til_true_på_arbeidstakerandel() {
        // Arrange
        VurderLønnsendringDto lønnsendringDto = new VurderLønnsendringDto(true);
        FaktaBeregningLagreDto dto = new FaktaBeregningLagreDto(FAKTA_OM_BEREGNING_TILFELLER);
        dto.setVurdertLonnsendring(lønnsendringDto);

        // Act
        vurderLønnsendringOppdaterer.oppdater(dto, null, beregningsgrunnlag, Optional.empty());

        // Assert
        assertThat(arbeidstakerAndel.getBgAndelArbeidsforhold().get().erLønnsendringIBeregningsperioden()).isTrue();
        assertThat(frilansAndel.getBgAndelArbeidsforhold()).isNotPresent();
    }

    @Test
    public void skal_ikkje_sette_lønnsendring_til_true_på_arbeidstakerandel() {
        // Arrange
        VurderLønnsendringDto lønnsendringDto = new VurderLønnsendringDto(false);
        FaktaBeregningLagreDto dto = new FaktaBeregningLagreDto(FAKTA_OM_BEREGNING_TILFELLER);
        dto.setVurdertLonnsendring(lønnsendringDto);

        // Act
        vurderLønnsendringOppdaterer.oppdater(dto, null, beregningsgrunnlag, Optional.empty());

        // Assert
        assertThat(arbeidstakerAndel.getBgAndelArbeidsforhold().get().erLønnsendringIBeregningsperioden()).isFalse();
        assertThat(frilansAndel.getBgAndelArbeidsforhold()).isNotPresent();
    }
}
