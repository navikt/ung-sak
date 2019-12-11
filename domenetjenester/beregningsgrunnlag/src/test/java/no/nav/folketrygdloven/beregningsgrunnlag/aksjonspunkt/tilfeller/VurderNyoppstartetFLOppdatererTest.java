package no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.tilfeller;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.dto.FaktaBeregningLagreDto;
import no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.dto.VurderNyoppstartetFLDto;
import no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.tilfeller.VurderNyoppstartetFLOppdaterer;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPeriode;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.FaktaOmBeregningTilfelle;
import no.nav.foreldrepenger.behandlingslager.behandling.aktivitet.AktivitetStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.aktivitet.Inntektskategori;
import no.nav.foreldrepenger.domene.typer.Beløp;

public class VurderNyoppstartetFLOppdatererTest {


    private static final List<FaktaOmBeregningTilfelle> FAKTA_OM_BEREGNING_TILFELLER = Collections.singletonList(FaktaOmBeregningTilfelle.VURDER_NYOPPSTARTET_FL);
    private final LocalDate SKJÆRINGSTIDSPUNKT = LocalDate.now();
    private final Beløp GRUNNBELØP = new Beløp(600000);

    private VurderNyoppstartetFLOppdaterer vurderNyoppstartetFLOppdaterer;
    private BeregningsgrunnlagEntitet beregningsgrunnlag;
    private BeregningsgrunnlagPrStatusOgAndel frilansAndel;


    @Before
    public void setup() {
        vurderNyoppstartetFLOppdaterer = new VurderNyoppstartetFLOppdaterer();
        beregningsgrunnlag = BeregningsgrunnlagEntitet.builder()
            .medGrunnbeløp(GRUNNBELØP)
            .medSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT)
            .leggTilFaktaOmBeregningTilfeller(FAKTA_OM_BEREGNING_TILFELLER)
            .build();
        BeregningsgrunnlagPeriode periode1 = BeregningsgrunnlagPeriode.builder()
            .medBeregningsgrunnlagPeriode(SKJÆRINGSTIDSPUNKT, SKJÆRINGSTIDSPUNKT.plusMonths(2).minusDays(1))
            .build(beregningsgrunnlag);
        frilansAndel = BeregningsgrunnlagPrStatusOgAndel.builder()
            .medAndelsnr(1L)
            .medLagtTilAvSaksbehandler(false)
            .medInntektskategori(Inntektskategori.FRILANSER)
            .medAktivitetStatus(AktivitetStatus.FRILANSER)
            .build(periode1);

    }

    @Test
    public void skal_sette_nyoppstartet_til_true() {
        // Arrange
        VurderNyoppstartetFLDto nyoppstartetDto = new VurderNyoppstartetFLDto(true);
        FaktaBeregningLagreDto dto = new FaktaBeregningLagreDto(FAKTA_OM_BEREGNING_TILFELLER);
        dto.setVurderNyoppstartetFL(nyoppstartetDto);

        // Act
        vurderNyoppstartetFLOppdaterer.oppdater(dto, null, beregningsgrunnlag, Optional.empty());

        // Assert
        assertThat(frilansAndel.erNyoppstartet().get()).isTrue();
    }

    @Test
    public void skal_sette_nyoppstartet_til_false() {
        // Arrange
        VurderNyoppstartetFLDto nyoppstartetDto = new VurderNyoppstartetFLDto(false );
        FaktaBeregningLagreDto dto = new FaktaBeregningLagreDto(FAKTA_OM_BEREGNING_TILFELLER);
        dto.setVurderNyoppstartetFL(nyoppstartetDto);

        // Act
        vurderNyoppstartetFLOppdaterer.oppdater(dto, null, beregningsgrunnlag, Optional.empty());

        // Assert
        assertThat(frilansAndel.erNyoppstartet().get()).isFalse();
    }

}
