package no.nav.folketrygdloven.beregningsgrunnlag.verdikjede.komponenttest;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import no.nav.folketrygdloven.beregningsgrunnlag.BeregningsgrunnlagTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.RepositoryProvider;
import no.nav.folketrygdloven.beregningsgrunnlag.gradering.AktivitetGradering;
import no.nav.folketrygdloven.beregningsgrunnlag.input.BeregningsgrunnlagInput;
import no.nav.folketrygdloven.beregningsgrunnlag.input.K9BeregningsgrunnlagInput;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BGAndelArbeidsforhold;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPeriode;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.FaktaOmBeregningTilfelle;
import no.nav.folketrygdloven.beregningsgrunnlag.opptjening.OpptjeningAktiviteter;
import no.nav.folketrygdloven.beregningsgrunnlag.output.BeregningAksjonspunktDefinisjon;
import no.nav.folketrygdloven.beregningsgrunnlag.output.BeregningAksjonspunktResultat;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.Periode;
import no.nav.folketrygdloven.beregningsgrunnlag.testutilities.BeregningIAYTestUtil;
import no.nav.folketrygdloven.beregningsgrunnlag.testutilities.behandling.TestScenarioBuilder;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;

@RunWith(CdiRunner.class)
public class KontrollerFaktaBeregningStegImplTest {

    private static final LocalDate SKJÆRINGSTIDSPUNKT_OPPTJENING = LocalDate.of(2018, Month.DECEMBER, 23);

    private static final AktørId AKTØR_ID = BeregningIAYTestUtil.AKTØR_ID;

    @Rule
    public UnittestRepositoryRule repositoryRule = new UnittestRepositoryRule();

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private BehandlingReferanse behandlingReferanse;
    private RepositoryProvider repositoryProvider = new RepositoryProvider(repositoryRule.getEntityManager());

    @Inject
    private BeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste;
    @Inject
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;

    // Test utils
    @Inject
    private BeregningIAYTestUtil iayTestUtil;

    @Test
    public void skal_kunne_opprette_kombinerte_aksjonpunkter_med_tidsbegrenset_atfl_i_samme_org_nyoppstartet_fl_lønnsendring() {
        // Arrange
        LocalDate stp = SKJÆRINGSTIDSPUNKT_OPPTJENING;
        String orgnr = "915933149";
        String orgnr2 = "974760673";
        String orgnr3 = "971032081";
        InternArbeidsforholdRef arbId = InternArbeidsforholdRef.nyRef();
        InternArbeidsforholdRef arbId2 = InternArbeidsforholdRef.nyRef();
        InternArbeidsforholdRef arbId3 = InternArbeidsforholdRef.nyRef();

        var scenario = TestScenarioBuilder.nyttScenario();
        behandlingReferanse = scenario.lagre(repositoryProvider);

        // opptjeningaktiviteter
        var periode = Periode.of(stp.minusMonths(12), stp);
        var opptjeningAktiviteter = new OpptjeningAktiviteter(List.of(
            OpptjeningAktiviteter.nyPeriodeOrgnr(OpptjeningAktivitetType.ARBEID, periode, orgnr),
            OpptjeningAktiviteter.nyPeriodeOrgnr(OpptjeningAktivitetType.ARBEID, periode, orgnr2),
            OpptjeningAktiviteter.nyPeriodeOrgnr(OpptjeningAktivitetType.ARBEID, periode, orgnr3),
            OpptjeningAktiviteter.nyPeriode(OpptjeningAktivitetType.FRILANS, periode),
            OpptjeningAktiviteter.nyPeriodeAktør(OpptjeningAktivitetType.SYKEPENGER, periode, AKTØR_ID.getId())));

        iayTestUtil.leggTilOppgittOpptjeningForFL(behandlingReferanse, true, SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(2));

        // Legg til IAY aktiviteter
        leggTilAT(arbId, orgnr);
        leggTilTidsbegrenset(arbId2, orgnr2);
        leggTilFrilans(arbId2, orgnr2);
        leggTilATMedLønnsendring(arbId3, orgnr3);

        // Act 1
        BehandlingReferanse ref = lagReferanse(behandlingReferanse);
        var iayGrunnlag = iayTestUtil.getIayTjeneste().hentGrunnlag(behandlingReferanse.getId());
        var foreldrepengerGrunnlag = new K9BeregningsgrunnlagInput();
        var input = new BeregningsgrunnlagInput(ref, iayGrunnlag, opptjeningAktiviteter, AktivitetGradering.INGEN_GRADERING, foreldrepengerGrunnlag);
        List<BeregningAksjonspunktResultat> resultat1 = doStegFastsettSkjæringstidspunkt(input);

        // Assert
        assertThat(resultat1).isEmpty();

        // Act 2
        List<BeregningAksjonspunktResultat> resultat2 = doStegKontrollerFaktaBeregning(input);

        // Assert
        assertThat(resultat2.stream().map(BeregningAksjonspunktResultat::getBeregningAksjonspunktDefinisjon)).containsExactly(BeregningAksjonspunktDefinisjon.VURDER_FAKTA_FOR_ATFL_SN);
        var bg = repositoryProvider.getBeregningsgrunnlagRepository().hentBeregningsgrunnlagAggregatForBehandling(behandlingReferanse.getId());

        // sjekk at vi har trigget på flere tilfeller
        assertThat(bg.getFaktaOmBeregningTilfeller()).containsExactlyInAnyOrder(
            FaktaOmBeregningTilfelle.VURDER_AT_OG_FL_I_SAMME_ORGANISASJON,
            FaktaOmBeregningTilfelle.VURDER_TIDSBEGRENSET_ARBEIDSFORHOLD,
            FaktaOmBeregningTilfelle.VURDER_NYOPPSTARTET_FL,
            FaktaOmBeregningTilfelle.VURDER_LØNNSENDRING,
            FaktaOmBeregningTilfelle.VURDER_MOTTAR_YTELSE);

        assertThat(bg.getBeregningsgrunnlagPerioder()).hasSize(1);
        BeregningsgrunnlagPeriode bgPeriode = bg.getBeregningsgrunnlagPerioder().get(0);
        List<BeregningsgrunnlagPrStatusOgAndel> andeler = bgPeriode.getBeregningsgrunnlagPrStatusOgAndelList();
        assertThat(andeler).hasSize(4);
        assertThat(andeler.stream().filter(andel -> andel.getAktivitetStatus().erFrilanser()).count()).isEqualTo(1);
        assertArbeidstakerAndeler(List.of(orgnr, orgnr2, orgnr3), andeler);
    }

    @Test
    public void skal_kunne_opprette_kombinerte_aksjonpunkter_med_SN_ny_i_arbeidslivet_nyoppstartet_fl() {
        // Arrange
        LocalDate stp = SKJÆRINGSTIDSPUNKT_OPPTJENING;
        InternArbeidsforholdRef arbId = InternArbeidsforholdRef.nyRef();
        String orgnr = "915933149";
        String orgnr2 = "974760673";
        String orgnr3 = "971032081";

        var scenario = TestScenarioBuilder.nyttScenario();
        behandlingReferanse = scenario.lagre(repositoryProvider);

        // opptjeningaktiviteter
        var periode = Periode.of(stp.minusMonths(12), stp);
        var opptjeningAktiviteter = new OpptjeningAktiviteter(List.of(
            OpptjeningAktiviteter.nyPeriodeOrgnr(OpptjeningAktivitetType.ARBEID, periode, orgnr),
            OpptjeningAktiviteter.nyPeriodeOrgnr(OpptjeningAktivitetType.FRILANS, periode, orgnr2),
            OpptjeningAktiviteter.nyPeriodeOrgnr(OpptjeningAktivitetType.NÆRING, periode, orgnr3),
            OpptjeningAktiviteter.nyPeriodeAktør(OpptjeningAktivitetType.SYKEPENGER, periode, AKTØR_ID.getId())));

        iayTestUtil.leggTilOppgittOpptjeningForFLOgSN(behandlingReferanse, stp, true, true);

        // legg til IAY aktiviteter
        leggTilAT(arbId, orgnr);

        // Act
        BehandlingReferanse ref = lagReferanse(behandlingReferanse);
        var iayGrunnlag = iayTestUtil.getIayTjeneste().hentGrunnlag(behandlingReferanse.getId());
        var foreldrepengerGrunnlag = new K9BeregningsgrunnlagInput();
        var input = new BeregningsgrunnlagInput(ref, iayGrunnlag, opptjeningAktiviteter, AktivitetGradering.INGEN_GRADERING, foreldrepengerGrunnlag);
        List<BeregningAksjonspunktResultat> resultat1 = doStegFastsettSkjæringstidspunkt(input);

        // Assert
        assertThat(resultat1).isEmpty();

        // Act
        List<BeregningAksjonspunktResultat> resultat = doStegKontrollerFaktaBeregning(input);

        // Assert
        assertThat(resultat.stream().map(BeregningAksjonspunktResultat::getBeregningAksjonspunktDefinisjon)).containsExactly(BeregningAksjonspunktDefinisjon.VURDER_FAKTA_FOR_ATFL_SN);
        var bg = repositoryProvider.getBeregningsgrunnlagRepository().hentBeregningsgrunnlagAggregatForBehandling(behandlingReferanse.getId());

        // Assert - har fått riktig tilfeller utledet
        assertThat(bg.getFaktaOmBeregningTilfeller()).containsExactlyInAnyOrder(
            FaktaOmBeregningTilfelle.VURDER_NYOPPSTARTET_FL,
            FaktaOmBeregningTilfelle.VURDER_SN_NY_I_ARBEIDSLIVET,
            FaktaOmBeregningTilfelle.VURDER_MOTTAR_YTELSE);

        assertThat(bg.getBeregningsgrunnlagPerioder()).hasSize(1);
        BeregningsgrunnlagPeriode bgPeriode = bg.getBeregningsgrunnlagPerioder().get(0);
        List<BeregningsgrunnlagPrStatusOgAndel> andeler = bgPeriode.getBeregningsgrunnlagPrStatusOgAndelList();
        assertThat(andeler).hasSize(3);
        assertThat(andeler.stream().filter(andel -> andel.getAktivitetStatus().erSelvstendigNæringsdrivende()).count()).isEqualTo(1);
        assertArbeidstakerAndeler(List.of(orgnr), andeler);
    }

    private void leggTilAT(InternArbeidsforholdRef arbId, String orgnr) {
        iayTestUtil.byggArbeidForBehandling(behandlingReferanse, SKJÆRINGSTIDSPUNKT_OPPTJENING, SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(10),
            SKJÆRINGSTIDSPUNKT_OPPTJENING.plusMonths(10), arbId, Arbeidsgiver.virksomhet(orgnr));
    }

    private void leggTilATMedLønnsendring(InternArbeidsforholdRef arbId, String orgnr) {
        iayTestUtil.byggArbeidForBehandling(behandlingReferanse, SKJÆRINGSTIDSPUNKT_OPPTJENING, SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(10),
            SKJÆRINGSTIDSPUNKT_OPPTJENING.plusMonths(10), arbId, Arbeidsgiver.virksomhet(orgnr), Optional.of(SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(2L)));
    }

    private void leggTilFrilans(InternArbeidsforholdRef arbId2, String orgnr2) {
        iayTestUtil.byggArbeidForBehandling(behandlingReferanse, SKJÆRINGSTIDSPUNKT_OPPTJENING, SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(2),
            SKJÆRINGSTIDSPUNKT_OPPTJENING.plusMonths(1), arbId2, Arbeidsgiver.virksomhet(orgnr2),
            ArbeidType.FRILANSER_OPPDRAGSTAKER_MED_MER, singletonList(BigDecimal.TEN), false, Optional.empty());
    }

    private void leggTilTidsbegrenset(InternArbeidsforholdRef arbId2, String orgnr2) {
        iayTestUtil.byggArbeidForBehandling(behandlingReferanse, SKJÆRINGSTIDSPUNKT_OPPTJENING, SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(2),
            SKJÆRINGSTIDSPUNKT_OPPTJENING.plusMonths(1), arbId2, Arbeidsgiver.virksomhet(orgnr2));
    }

    private List<BeregningAksjonspunktResultat> doStegKontrollerFaktaBeregning(BeregningsgrunnlagInput input) {
        behandlingskontrollTjeneste.initBehandlingskontroll(input.getBehandlingReferanse().getBehandlingId());
        return kontrollerFaktaBeregningsgrunnlag(input);
    }

    private List<BeregningAksjonspunktResultat> kontrollerFaktaBeregningsgrunnlag(BeregningsgrunnlagInput input) {
        return beregningsgrunnlagTjeneste.kontrollerFaktaBeregningsgrunnlag(input);
    }

    private List<BeregningAksjonspunktResultat> doStegFastsettSkjæringstidspunkt(BeregningsgrunnlagInput input) {
        return beregningsgrunnlagTjeneste.fastsettBeregningsaktiviteter(input);
    }

    private BehandlingReferanse lagReferanse(BehandlingReferanse behandlingReferanse) {
        return behandlingReferanse.medSkjæringstidspunkt(Skjæringstidspunkt.builder()
            .medUtledetSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT_OPPTJENING)
            .medSkjæringstidspunktOpptjening(SKJÆRINGSTIDSPUNKT_OPPTJENING)
            .medFørsteUttaksdato(SKJÆRINGSTIDSPUNKT_OPPTJENING.plusDays(1))
            .build());
    }

    private static void assertArbeidstakerAndeler(List<String> orgnrs, List<BeregningsgrunnlagPrStatusOgAndel> andeler) {
        orgnrs.forEach(
            orgnr -> assertThat(andeler.stream().filter(andel -> andel.getBgAndelArbeidsforhold()
                .map(BGAndelArbeidsforhold::getArbeidsgiver)
                .map(Arbeidsgiver::getOrgnr)
                .filter(orgnr::equals)
                .isPresent()).count()).isEqualTo(1));
    }
}
