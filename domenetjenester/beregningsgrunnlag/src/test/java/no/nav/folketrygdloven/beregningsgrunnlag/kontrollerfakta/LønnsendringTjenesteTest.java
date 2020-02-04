package no.nav.folketrygdloven.beregningsgrunnlag.kontrollerfakta;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import no.nav.folketrygdloven.beregningsgrunnlag.RepositoryProvider;
import no.nav.folketrygdloven.beregningsgrunnlag.adapter.util.BeregningsgrunnlagTestUtil;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.testutilities.BeregningIAYTestUtil;
import no.nav.folketrygdloven.beregningsgrunnlag.testutilities.behandling.TestScenarioBuilder;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdInformasjonBuilder;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdOverstyringBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.Yrkesaktivitet;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetFilter;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.k9.kodeverk.iay.ArbeidType;
import no.nav.k9.kodeverk.iay.ArbeidsforholdHandlingType;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;

@RunWith(CdiRunner.class)
public class LønnsendringTjenesteTest {

    private static final LocalDate SKJÆRINGSTIDSPUNKT_OPPTJENING = LocalDate.of(2018, 9, 30);
    @Rule
    public UnittestRepositoryRule repoRule = new UnittestRepositoryRule();
    private RepositoryProvider repositoryProvider = new RepositoryProvider(repoRule.getEntityManager());
    @Inject
    private BeregningIAYTestUtil iayTestUtil;
    @Inject
    private BeregningsgrunnlagTestUtil beregningTestUtil;

    private BehandlingReferanse behandlingReferanse;

    @Inject
    private InntektArbeidYtelseTjeneste iayTjeneste;

    @Before
    public void setup() {
        TestScenarioBuilder scenario = TestScenarioBuilder.nyttScenario();
        behandlingReferanse = scenario.lagre(repositoryProvider);
    }

    @Test
    public void skalTesteAtAksjonspunktOpprettesNårBrukerHarLønnsendringUtenInntektsmelding() {
        // Arrange
        var arbId = InternArbeidsforholdRef.nyRef();
        String orgnr = "123456780";
        iayTestUtil.byggArbeidForBehandling(behandlingReferanse, SKJÆRINGSTIDSPUNKT_OPPTJENING, SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(1),
            SKJÆRINGSTIDSPUNKT_OPPTJENING.plusMonths(5).minusDays(2), arbId, Arbeidsgiver.virksomhet(orgnr),
            Optional.of(SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(2L)));
        BeregningsgrunnlagEntitet beregningsgrunnlag = beregningTestUtil.lagGjeldendeBeregningsgrunnlag(lagReferanseMedSkjæringstidspunkt(behandlingReferanse), SKJÆRINGSTIDSPUNKT_OPPTJENING);

        // Act
        boolean brukerHarLønnsendring = LønnsendringTjeneste.brukerHarHattLønnsendringOgManglerInntektsmelding(behandlingReferanse.getAktørId(),
            beregningsgrunnlag, iayTjeneste.hentGrunnlag(behandlingReferanse.getBehandlingId()));

        // Assert
        assertThat(brukerHarLønnsendring).isTrue();
    }

    @Test
    public void skalTesteAtAksjonspunktIkkeOpprettesNårBrukerHarLønnsendringUtenforBeregningsperioden() {
        // Arrange
        var arbId = InternArbeidsforholdRef.nyRef();
        String orgnr = "123456780";
        iayTestUtil.byggArbeidForBehandling(behandlingReferanse, SKJÆRINGSTIDSPUNKT_OPPTJENING, SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(1),
            SKJÆRINGSTIDSPUNKT_OPPTJENING.plusMonths(5).minusDays(2), arbId, Arbeidsgiver.virksomhet(orgnr),
            Optional.of(SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(4L)));
        BeregningsgrunnlagEntitet beregningsgrunnlag = beregningTestUtil.lagGjeldendeBeregningsgrunnlag(lagReferanseMedSkjæringstidspunkt(behandlingReferanse), SKJÆRINGSTIDSPUNKT_OPPTJENING);

        // Act
        boolean brukerHarLønnsendring = LønnsendringTjeneste.brukerHarHattLønnsendringOgManglerInntektsmelding(behandlingReferanse.getAktørId(),
            beregningsgrunnlag, iayTjeneste.hentGrunnlag(behandlingReferanse.getBehandlingId()));

        // Assert
        assertThat(brukerHarLønnsendring).isFalse();
    }

    @Test
    public void skalFåAksjonspunktNårOverstyrtPeriodeInneholderSkjæringstidspunkt() {
        // Arrange
        ArbeidType arbeidType = ArbeidType.ORDINÆRT_ARBEIDSFORHOLD;
        var arbId = InternArbeidsforholdRef.nyRef();
        String orgnr = "123456780";
        Arbeidsgiver arbeidsgiver = Arbeidsgiver.virksomhet(orgnr);
        Optional<LocalDate> lønnsendringsdato = Optional.of(SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(1L));
        InntektArbeidYtelseAggregatBuilder inntektArbeidYtelseAggregatBuilder;
        Long behandlingId = behandlingReferanse.getId();
        AktørId aktørId = behandlingReferanse.getAktørId();
        inntektArbeidYtelseAggregatBuilder = iayTjeneste.opprettBuilderForRegister(behandlingId);
        iayTestUtil.byggArbeidInntekt(behandlingReferanse.getId(), behandlingReferanse.getAktørId(), SKJÆRINGSTIDSPUNKT_OPPTJENING, SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(1),
            null, arbId, arbeidsgiver, arbeidType, singletonList(BigDecimal.TEN), arbeidsgiver != null, false, lønnsendringsdato,
            inntektArbeidYtelseAggregatBuilder);
        medOverstyrtPeriode(arbeidType, arbeidsgiver, SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(10), SKJÆRINGSTIDSPUNKT_OPPTJENING, behandlingId, aktørId);
        BeregningsgrunnlagEntitet beregningsgrunnlag = beregningTestUtil.lagGjeldendeBeregningsgrunnlag(lagReferanseMedSkjæringstidspunkt(behandlingReferanse), SKJÆRINGSTIDSPUNKT_OPPTJENING);

        // Act
        boolean brukerHarLønnsendring = LønnsendringTjeneste.brukerHarHattLønnsendringOgManglerInntektsmelding(behandlingReferanse.getAktørId(),
            beregningsgrunnlag, iayTjeneste.hentGrunnlag(behandlingReferanse.getBehandlingId()));
        // Assert
        assertThat(brukerHarLønnsendring).isTrue();
    }

    @Test
    public void skalIkkeFåAksjonspunktNårOverstyrtPeriodeIkkeInneholderSkjæringstidspunkt() {
        // Arrange
        ArbeidType arbeidType = ArbeidType.ORDINÆRT_ARBEIDSFORHOLD;
        var arbId = InternArbeidsforholdRef.nyRef();
        String orgnr = "123456780";
        Arbeidsgiver arbeidsgiver = Arbeidsgiver.virksomhet(orgnr);
        Optional<LocalDate> lønnsendringsdato = Optional.of(SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(1L));
        InntektArbeidYtelseAggregatBuilder inntektArbeidYtelseAggregatBuilder;
        Long behandlingId = behandlingReferanse.getId();
        inntektArbeidYtelseAggregatBuilder = iayTjeneste.opprettBuilderForRegister(behandlingId);
        LocalDate fraOgMed = SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(1);
        iayTestUtil.byggArbeidInntekt(behandlingReferanse.getId(), behandlingReferanse.getAktørId(), SKJÆRINGSTIDSPUNKT_OPPTJENING, fraOgMed, null, arbId, arbeidsgiver,
            arbeidType, singletonList(BigDecimal.TEN), arbeidsgiver != null, false, lønnsendringsdato, inntektArbeidYtelseAggregatBuilder);
        medOverstyrtPeriode(arbeidType, arbeidsgiver, fraOgMed, SKJÆRINGSTIDSPUNKT_OPPTJENING.minusDays(1), behandlingId, behandlingReferanse.getAktørId());
        BeregningsgrunnlagEntitet beregningsgrunnlag = beregningTestUtil.lagGjeldendeBeregningsgrunnlag(lagReferanseMedSkjæringstidspunkt(behandlingReferanse), SKJÆRINGSTIDSPUNKT_OPPTJENING);

        // Act
        boolean brukerHarLønnsendring = LønnsendringTjeneste.brukerHarHattLønnsendringOgManglerInntektsmelding(behandlingReferanse.getAktørId(),
            beregningsgrunnlag, iayTjeneste.hentGrunnlag(behandlingReferanse.getBehandlingId()));

        // Assert
        assertThat(brukerHarLønnsendring).isFalse();
    }

    private void medOverstyrtPeriode(ArbeidType arbeidType, Arbeidsgiver arbeidsgiver, LocalDate fom, LocalDate tom, Long behandlingId, AktørId aktørId) {
        InntektArbeidYtelseGrunnlag grunnlag = iayTjeneste.hentGrunnlag(behandlingId);
        var filter = new YrkesaktivitetFilter(grunnlag.getArbeidsforholdInformasjon(), grunnlag.getAktørArbeidFraRegister(behandlingReferanse.getAktørId()))
            .før(tom);

        if (!filter.getYrkesaktiviteter().isEmpty()) {
            Yrkesaktivitet yrkesaktivitet = BeregningIAYTestUtil.finnKorresponderendeYrkesaktivitet(filter, arbeidType, arbeidsgiver);

            ArbeidsforholdInformasjonBuilder informasjonBuilder = ArbeidsforholdInformasjonBuilder
                .oppdatere(iayTjeneste.finnGrunnlag(behandlingId));

            ArbeidsforholdOverstyringBuilder overstyringBuilderFor = informasjonBuilder.getOverstyringBuilderFor(yrkesaktivitet.getArbeidsgiver(),
                yrkesaktivitet.getArbeidsforholdRef());
            overstyringBuilderFor.medHandling(ArbeidsforholdHandlingType.BRUK_MED_OVERSTYRT_PERIODE);
            overstyringBuilderFor.leggTilOverstyrtPeriode(fom, tom);
            informasjonBuilder.leggTil(overstyringBuilderFor);

            iayTjeneste.lagreArbeidsforhold(behandlingId, aktørId, informasjonBuilder);
        }
    }

    private static BehandlingReferanse lagReferanseMedSkjæringstidspunkt(BehandlingReferanse behandlingReferanse) {
        return behandlingReferanse.medSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT_OPPTJENING);
    }
}
