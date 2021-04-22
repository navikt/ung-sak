package no.nav.k9.sak.ytelse.omsorgspenger.beregningsgrunnlag;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.OpptjeningAktiviteter;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.k9.kodeverk.arbeidsforhold.ArbeidType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.opptjening.OpptjeningRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.db.util.JpaExtension;
import no.nav.k9.sak.domene.iay.modell.AktivitetsAvtaleBuilder;
import no.nav.k9.sak.domene.iay.modell.ArbeidsforholdInformasjonBuilder;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlagBuilder;
import no.nav.k9.sak.domene.iay.modell.VersjonType;
import no.nav.k9.sak.domene.iay.modell.YrkesaktivitetBuilder;
import no.nav.k9.sak.domene.opptjening.OppgittOpptjeningFilter;
import no.nav.k9.sak.domene.opptjening.OppgittOpptjeningFilterProvider;
import no.nav.k9.sak.domene.opptjening.aksjonspunkt.OpptjeningsperioderUtenOverstyringTjeneste;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.test.util.UnitTestLookupInstanceImpl;
import no.nav.k9.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
public class OmsorgspengerOpptjeningForBeregningTjenesteTest {

    public static final LocalDate SKJÆRINGSTIDSPUNKT = LocalDate.now();
    public static final LocalDate FØRSTE_UTTAKSDAG = SKJÆRINGSTIDSPUNKT.minusDays(9);
    public static final Arbeidsgiver ARBEIDSGIVER = Arbeidsgiver.virksomhet("123456789");

    @Inject
    private EntityManager entityManager;

    private BehandlingRepositoryProvider repositoryProvider;
    private OpptjeningRepository opptjeningRepository;
    private OmsorgspengerOpptjeningForBeregningTjeneste tjeneste;
    private BehandlingReferanse ref;
    private AktørId aktørId;
    private OppgittOpptjeningFilterProvider oppgittOpptjeningFilterProvider;
    private OppgittOpptjeningFilter oppgittOpptjeningFilter;

    @BeforeEach
    public void setUp() {
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        opptjeningRepository = repositoryProvider.getOpptjeningRepository();
        oppgittOpptjeningFilter = Mockito.mock(OppgittOpptjeningFilter.class);
        oppgittOpptjeningFilterProvider = Mockito.mock(OppgittOpptjeningFilterProvider.class);
        when(oppgittOpptjeningFilterProvider.finnOpptjeningFilter(anyLong())).thenReturn(oppgittOpptjeningFilter);

        aktørId = AktørId.dummy();
        var scenario = TestScenarioBuilder.builderMedSøknad().medBruker(aktørId);
        Behandling behandling = scenario.lagre(entityManager);
        ref = BehandlingReferanse.fra(behandling);
        opptjeningRepository.lagreOpptjeningsperiode(behandling, FØRSTE_UTTAKSDAG.minusMonths(10), FØRSTE_UTTAKSDAG.minusDays(1), false);
        opptjeningRepository.lagreOpptjeningsperiode(behandling, SKJÆRINGSTIDSPUNKT.minusMonths(10), SKJÆRINGSTIDSPUNKT.minusDays(1), false);
        tjeneste = new OmsorgspengerOpptjeningForBeregningTjeneste(new UnitTestLookupInstanceImpl<>(new OpptjeningsperioderUtenOverstyringTjeneste(opptjeningRepository)), oppgittOpptjeningFilterProvider);
    }

    @Test
    public void skal_mappe_arbeid_for_skjæringtidspunkt_etter_første_uttaksdag() {
        InntektArbeidYtelseGrunnlag iay = lagIAYForArbeidSomSlutterOgStarterRundtFørsteUttaksdag();
        when(oppgittOpptjeningFilter.hentOppgittOpptjening(any(), any(), any())).thenReturn(iay.getOppgittOpptjening());
        OpptjeningAktiviteter opptjeningAktiviteter = tjeneste.hentEksaktOpptjeningForBeregning(ref, iay, DatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT, SKJÆRINGSTIDSPUNKT.plusDays(10)))
            .get();
        List<OpptjeningAktiviteter.OpptjeningPeriode> opptjeningPerioder = opptjeningAktiviteter.getOpptjeningPerioder();
        assertThat(opptjeningPerioder.size()).isEqualTo(2);
    }

    private InntektArbeidYtelseGrunnlag lagIAYForArbeidSomSlutterOgStarterRundtFørsteUttaksdag() {
        InntektArbeidYtelseAggregatBuilder registerBuilder = InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.empty(), VersjonType.REGISTER);
        InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder aktørArbeid = InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder.oppdatere(Optional.empty())
            .medAktørId(aktørId)
            .leggTilYrkesaktivitet(lagArbeid(SKJÆRINGSTIDSPUNKT.minusMonths(10), FØRSTE_UTTAKSDAG.minusDays(1)))
            .leggTilYrkesaktivitet(lagArbeid(FØRSTE_UTTAKSDAG, SKJÆRINGSTIDSPUNKT.plusDays(10)));
        registerBuilder.leggTilAktørArbeid(aktørArbeid);
        return InntektArbeidYtelseGrunnlagBuilder.nytt()
            .medData(registerBuilder)
            .medInformasjon(ArbeidsforholdInformasjonBuilder.oppdatere(Optional.empty()).build())
            .build();
    }

    private YrkesaktivitetBuilder lagArbeid(LocalDate fom, LocalDate tom) {
        return YrkesaktivitetBuilder.oppdatere(Optional.empty())
            .leggTilAktivitetsAvtale(AktivitetsAvtaleBuilder.ny()
                .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom)))
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .medArbeidsgiver(ARBEIDSGIVER)
            .medArbeidsforholdId(InternArbeidsforholdRef.nyRef());
    }
}
