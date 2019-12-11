package no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.tilfeller;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;

import no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.dto.FaktaBeregningLagreDto;
import no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.dto.VurderSelvstendigNæringsdrivendeNyIArbeidslivetDto;
import no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.tilfeller.VurderSelvstendigNæringsdrivendeNyIArbeidslivetOppdaterer;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPeriode;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.FaktaOmBeregningTilfelle;
import no.nav.foreldrepenger.behandlingslager.behandling.aktivitet.AktivitetStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.aktivitet.Inntektskategori;
import no.nav.foreldrepenger.domene.typer.Beløp;

public class VurderSelvstendigNæringsdrivendeNyIArbeidslivetOppdatererTest {

    private static final List<FaktaOmBeregningTilfelle> FAKTA_OM_BEREGNING_TILFELLER = Collections
        .singletonList(FaktaOmBeregningTilfelle.VURDER_SN_NY_I_ARBEIDSLIVET);
    public static final String ORGNR = "8934232423";
    private final LocalDate SKJÆRINGSTIDSPUNKT = LocalDate.now();
    private final Beløp GRUNNBELØP = new Beløp(600000);

    private VurderSelvstendigNæringsdrivendeNyIArbeidslivetOppdaterer vurderSelvstendigNæringsdrivendeNyIArbeidslivetOppdaterer;
    private BeregningsgrunnlagEntitet beregningsgrunnlag;
    private BeregningsgrunnlagPrStatusOgAndel snAndel;

    @Before
    public void setup() {
        vurderSelvstendigNæringsdrivendeNyIArbeidslivetOppdaterer = new VurderSelvstendigNæringsdrivendeNyIArbeidslivetOppdaterer();
        beregningsgrunnlag = BeregningsgrunnlagEntitet.builder()
            .medGrunnbeløp(GRUNNBELØP)
            .medSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT)
            .leggTilFaktaOmBeregningTilfeller(FAKTA_OM_BEREGNING_TILFELLER)
            .build();
        BeregningsgrunnlagPeriode periode1 = BeregningsgrunnlagPeriode.builder()
            .medBeregningsgrunnlagPeriode(SKJÆRINGSTIDSPUNKT, SKJÆRINGSTIDSPUNKT.plusMonths(2).minusDays(1))
            .build(beregningsgrunnlag);
        snAndel = BeregningsgrunnlagPrStatusOgAndel.builder()
            .medAndelsnr(1L)
            .medLagtTilAvSaksbehandler(false)
            .medInntektskategori(Inntektskategori.SELVSTENDIG_NÆRINGSDRIVENDE)
            .medAktivitetStatus(AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE)
            .build(periode1);

    }

    @Test
    public void skal_sette_ny_i_arbeidslivet() {
        // Arrange
        VurderSelvstendigNæringsdrivendeNyIArbeidslivetDto nyIArbeidslivetDto = new VurderSelvstendigNæringsdrivendeNyIArbeidslivetDto(true);
        FaktaBeregningLagreDto dto = new FaktaBeregningLagreDto(FAKTA_OM_BEREGNING_TILFELLER);
        dto.setVurderNyIArbeidslivet(nyIArbeidslivetDto);

        // Act
        vurderSelvstendigNæringsdrivendeNyIArbeidslivetOppdaterer.oppdater(dto, null, beregningsgrunnlag, Optional.empty());

        // Assert
        assertThat(snAndel.getNyIArbeidslivet()).isTrue();
    }

    @Test
    public void skal_sette_ny_i_arbeidslivet_til_false() {
        // Arrange
        VurderSelvstendigNæringsdrivendeNyIArbeidslivetDto nyIArbeidslivetDto = new VurderSelvstendigNæringsdrivendeNyIArbeidslivetDto(false);
        FaktaBeregningLagreDto dto = new FaktaBeregningLagreDto(FAKTA_OM_BEREGNING_TILFELLER);
        dto.setVurderNyIArbeidslivet(nyIArbeidslivetDto);

        // Act
        vurderSelvstendigNæringsdrivendeNyIArbeidslivetOppdaterer.oppdater(dto, null, beregningsgrunnlag, Optional.empty());

        // Assert
        assertThat(snAndel.getNyIArbeidslivet()).isFalse();
    }

}
