package no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.tilfeller;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;

import no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.dto.FaktaBeregningLagreDto;
import no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.dto.VurderMilitærDto;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BGAndelArbeidsforhold;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagAktivitetStatus;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPeriode;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.folketrygdloven.beregningsgrunnlag.testutilities.behandling.TestScenarioBuilder;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.Beløp;
import no.nav.k9.kodeverk.beregningsgrunnlag.FaktaOmBeregningTilfelle;
import no.nav.k9.kodeverk.beregningsgrunnlag.Hjemmel;
import no.nav.k9.kodeverk.iay.AktivitetStatus;
import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;

public class VurderMilitærOppdatererTest {
    private static final LocalDate SKJÆRINGSTIDSPUNKT = LocalDate.of(2019,1,1);
    private static final Beløp GRUNNBELØP = new Beløp(BigDecimal.valueOf(85000));

    private BehandlingReferanse behandlingReferanse;
    private VurderMilitærOppdaterer vurderMilitærOppdaterer;

    @Before
    public void setup() {
        vurderMilitærOppdaterer = new VurderMilitærOppdaterer();
        TestScenarioBuilder scenario = TestScenarioBuilder.nyttScenario();
        behandlingReferanse = scenario.lagMocked();
    }

    @Test
    public void skal_legge_til_militærandel_om_vurdert_til_true_og_andel_ikke_finnes() {
        // Arrange
        BeregningsgrunnlagEntitet beregningsgrunnlag = lagBeregningsgrunnlag(Collections.singletonList(AktivitetStatus.ARBEIDSTAKER));
        VurderMilitærDto militærDto = new VurderMilitærDto(true);
        FaktaBeregningLagreDto dto = new FaktaBeregningLagreDto(Collections.singletonList(FaktaOmBeregningTilfelle.VURDER_MILITÆR_SIVILTJENESTE));
        dto.setVurderMilitaer(militærDto);

        // Act
        BeregningsgrunnlagEntitet nyttBg = beregningsgrunnlag.dypKopi();
        vurderMilitærOppdaterer.oppdater(dto, behandlingReferanse, nyttBg, Optional.empty());


        // Assert
        Optional<BeregningsgrunnlagAktivitetStatus> militærStatus = nyttBg.getAktivitetStatuser().stream().filter(a -> AktivitetStatus.MILITÆR_ELLER_SIVIL.equals(a.getAktivitetStatus())).findFirst();
        boolean harMilitærandel = nyttBg.getBeregningsgrunnlagPerioder().get(0).getBeregningsgrunnlagPrStatusOgAndelList().stream()
            .anyMatch(a -> AktivitetStatus.MILITÆR_ELLER_SIVIL.equals(a.getAktivitetStatus()));
        assertThat(harMilitærandel).isTrue();
        assertThat(militærStatus).isPresent();
        assertThat(militærStatus.get().getAktivitetStatus()).isEqualTo(AktivitetStatus.MILITÆR_ELLER_SIVIL);
        assertThat(militærStatus.get().getHjemmel()).isEqualTo(Hjemmel.F_14_7);
    }

    @Test
    public void skal_ikke_legge_til_militærandel_om_vurdert_til_true_og_andel_finnes_fra_før() {
        // Arrange
        BeregningsgrunnlagEntitet beregningsgrunnlag = lagBeregningsgrunnlag(Arrays.asList(AktivitetStatus.ARBEIDSTAKER, AktivitetStatus.MILITÆR_ELLER_SIVIL));
        VurderMilitærDto militærDto = new VurderMilitærDto(true);
        FaktaBeregningLagreDto dto = new FaktaBeregningLagreDto(Collections.singletonList(FaktaOmBeregningTilfelle.VURDER_MILITÆR_SIVILTJENESTE));
        dto.setVurderMilitaer(militærDto);

        // Act
        BeregningsgrunnlagEntitet nyttBg = beregningsgrunnlag.dypKopi();
        vurderMilitærOppdaterer.oppdater(dto, behandlingReferanse, nyttBg, Optional.empty());


        // Assert
        List<BeregningsgrunnlagAktivitetStatus> militærStatus = nyttBg.getAktivitetStatuser().stream().filter(a -> AktivitetStatus.MILITÆR_ELLER_SIVIL.equals(a.getAktivitetStatus())).collect(Collectors.toList());
        List<BeregningsgrunnlagPrStatusOgAndel> militærAndeler = nyttBg.getBeregningsgrunnlagPerioder().get(0).getBeregningsgrunnlagPrStatusOgAndelList().stream()
            .filter(a -> AktivitetStatus.MILITÆR_ELLER_SIVIL.equals(a.getAktivitetStatus())).collect(Collectors.toList());
        assertThat(militærAndeler).hasSize(1);
        assertThat(militærStatus).hasSize(1);
    }

    @Test
    public void skal_ikke_gjøre_noe_dersom_militær_er_false_men_det_ikke_ligger_militær_på_grunnlaget() {
        // Arrange
        BeregningsgrunnlagEntitet beregningsgrunnlag = lagBeregningsgrunnlag(Collections.singletonList(AktivitetStatus.ARBEIDSTAKER));
        VurderMilitærDto militærDto = new VurderMilitærDto(false);
        FaktaBeregningLagreDto dto = new FaktaBeregningLagreDto(Collections.singletonList(FaktaOmBeregningTilfelle.VURDER_MILITÆR_SIVILTJENESTE));
        dto.setVurderMilitaer(militærDto);

        // Act
        BeregningsgrunnlagEntitet nyttBg = beregningsgrunnlag.dypKopi();
        vurderMilitærOppdaterer.oppdater(dto, behandlingReferanse, nyttBg, Optional.empty());


        // Assert
        List<BeregningsgrunnlagAktivitetStatus> militærStatus = nyttBg.getAktivitetStatuser().stream().filter(a -> AktivitetStatus.MILITÆR_ELLER_SIVIL.equals(a.getAktivitetStatus())).collect(Collectors.toList());
        List<BeregningsgrunnlagPrStatusOgAndel> militærAndeler = nyttBg.getBeregningsgrunnlagPerioder().get(0).getBeregningsgrunnlagPrStatusOgAndelList().stream()
            .filter(a -> AktivitetStatus.MILITÆR_ELLER_SIVIL.equals(a.getAktivitetStatus())).collect(Collectors.toList());
        assertThat(militærAndeler).hasSize(0);
        assertThat(militærStatus).hasSize(0);
        assertThat(nyttBg.getBeregningsgrunnlagPerioder().get(0).getBeregningsgrunnlagPrStatusOgAndelList()).hasSize(1);
    }

    @Test
    public void skal_fjerne_andel_dersom_militær_er_false_og_det_ligger_militær_på_grunnlaget() {
        // Arrange
        BeregningsgrunnlagEntitet beregningsgrunnlag = lagBeregningsgrunnlag(Arrays.asList(AktivitetStatus.ARBEIDSTAKER, AktivitetStatus.MILITÆR_ELLER_SIVIL));
        VurderMilitærDto militærDto = new VurderMilitærDto(false);
        FaktaBeregningLagreDto dto = new FaktaBeregningLagreDto(Collections.singletonList(FaktaOmBeregningTilfelle.VURDER_MILITÆR_SIVILTJENESTE));
        dto.setVurderMilitaer(militærDto);

        // Act
        BeregningsgrunnlagEntitet nyttBg = beregningsgrunnlag.dypKopi();
        vurderMilitærOppdaterer.oppdater(dto, behandlingReferanse, nyttBg, Optional.empty());


        // Assert
        List<BeregningsgrunnlagAktivitetStatus> militærStatus = nyttBg.getAktivitetStatuser().stream().filter(a -> AktivitetStatus.MILITÆR_ELLER_SIVIL.equals(a.getAktivitetStatus())).collect(Collectors.toList());
        List<BeregningsgrunnlagPrStatusOgAndel> militærAndeler = nyttBg.getBeregningsgrunnlagPerioder().get(0).getBeregningsgrunnlagPrStatusOgAndelList().stream()
            .filter(a -> AktivitetStatus.MILITÆR_ELLER_SIVIL.equals(a.getAktivitetStatus())).collect(Collectors.toList());
        assertThat(militærAndeler).hasSize(0);
        assertThat(militærStatus).hasSize(0);
        assertThat(nyttBg.getBeregningsgrunnlagPerioder().get(0).getBeregningsgrunnlagPrStatusOgAndelList()).hasSize(1);
    }

    private BeregningsgrunnlagEntitet lagBeregningsgrunnlag(List<AktivitetStatus> statuser) {
        BeregningsgrunnlagEntitet bg = BeregningsgrunnlagEntitet.builder()
            .medSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT)
            .medGrunnbeløp(GRUNNBELØP)
            .build();

        BeregningsgrunnlagPeriode periode = buildBeregningsgrunnlagPeriode(bg,
            SKJÆRINGSTIDSPUNKT, null);

        statuser.forEach(status -> {
            BeregningsgrunnlagAktivitetStatus.builder()
                .medAktivitetStatus(status)
                .medHjemmel(Hjemmel.F_14_7).build(bg);
            buildBgPrStatusOgAndel(periode, status);
        });

        return bg;
    }

    private void buildBgPrStatusOgAndel(BeregningsgrunnlagPeriode beregningsgrunnlagPeriode, AktivitetStatus aktivitetStatus) {
        BeregningsgrunnlagPrStatusOgAndel.Builder builder = BeregningsgrunnlagPrStatusOgAndel.builder()
            .medAktivitetStatus(aktivitetStatus)
            .medArbforholdType(OpptjeningAktivitetType.ARBEID);
        if (aktivitetStatus.erArbeidstaker()) {
            builder.medBGAndelArbeidsforhold(BGAndelArbeidsforhold.builder().medArbeidsgiver(Arbeidsgiver.fra(AktørId.dummy())));
        }
        builder
            .build(beregningsgrunnlagPeriode);
    }

    private BeregningsgrunnlagPeriode buildBeregningsgrunnlagPeriode(BeregningsgrunnlagEntitet beregningsgrunnlag, LocalDate fom, LocalDate tom) {
        return BeregningsgrunnlagPeriode.builder()
            .medBeregningsgrunnlagPeriode(fom, tom)
            .build(beregningsgrunnlag);
    }

}
