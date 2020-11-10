package no.nav.k9.sak.domene.behandling.steg.avklarfakta;

import static java.util.Optional.empty;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.MockitoAnnotations.initMocks;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.Rule;
import org.junit.jupiter.api.Test;
import org.mockito.Spy;

import no.nav.k9.kodeverk.arbeidsforhold.ArbeidType;
import no.nav.k9.kodeverk.arbeidsforhold.InntektsKilde;
import no.nav.k9.kodeverk.arbeidsforhold.InntektspostType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandling.Skjæringstidspunkt;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktUtlederInput;
import no.nav.k9.sak.behandlingskontroll.AksjonspunktResultat;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.opptjening.OpptjeningRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.db.util.UnittestRepositoryRule;
import no.nav.k9.sak.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.iay.modell.AktivitetsAvtaleBuilder;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.k9.sak.domene.iay.modell.InntektBuilder;
import no.nav.k9.sak.domene.iay.modell.InntektspostBuilder;
import no.nav.k9.sak.domene.iay.modell.VersjonType;
import no.nav.k9.sak.domene.iay.modell.YrkesaktivitetBuilder;
import no.nav.k9.sak.domene.opptjening.aksjonspunkt.AksjonspunktutlederForVurderBekreftetOpptjening;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.test.util.behandling.AbstractTestScenario;
import no.nav.k9.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;

public class AksjonspunktutlederForVurderBekreftetOpptjeningTest {
    @Rule
    public UnittestRepositoryRule repoRule = new UnittestRepositoryRule();

    private BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(repoRule.getEntityManager());

    private OpptjeningRepository opptjeningRepository;

    private InntektArbeidYtelseTjeneste iayTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();
    private final Skjæringstidspunkt skjæringstidspunkt = Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(LocalDate.now()).build();

    @Spy
    private AksjonspunktutlederForVurderBekreftetOpptjening utleder = new AksjonspunktutlederForVurderBekreftetOpptjening(
        repositoryProvider.getOpptjeningRepository(),
        iayTjeneste);

    @BeforeEach
    public void oppsett() {
        initMocks(this);
        opptjeningRepository = repositoryProvider.getOpptjeningRepository();
    }

    private Behandling lagre(AbstractTestScenario<?> scenario) {
        return scenario.lagre(repositoryProvider);
    }

    @Test
    public void skal_ikke_opprette_aksjonspunktet_5051() {
        // Arrange
        AktørId aktørId1 = AktørId.dummy();
        var scenario = TestScenarioBuilder.builderMedSøknad().medBruker(aktørId1);
        Behandling behandling = lagre(scenario);

        // Act
        List<AksjonspunktResultat> aksjonspunktResultater = utleder.utledAksjonspunkterFor(lagInput(behandling));

        // Assert
        assertThat(aksjonspunktResultater).isEmpty();
    }

    private AksjonspunktUtlederInput lagInput(Behandling behandling) {
        BehandlingReferanse ref = BehandlingReferanse.fra(behandling, skjæringstidspunkt);
        return new AksjonspunktUtlederInput(ref);
    }

    @Test
    public void skal_opprette_aksjonspunkt_dersom_bekreftet_frilansoppdrag() {
        AktørId aktørId1 = AktørId.dummy();
        var scenario = TestScenarioBuilder.builderMedSøknad().medBruker(aktørId1);

        LocalDate fraOgMed = LocalDate.now().minusMonths(1);
        LocalDate tilOgMed = LocalDate.now().plusMonths(1);
        var arbeidsforholdId = InternArbeidsforholdRef.nyRef();
        Arbeidsgiver arbeidsgiver = Arbeidsgiver.virksomhet("777777777");

        Behandling behandling = lagre(scenario);

        var builder = InntektArbeidYtelseAggregatBuilder.oppdatere(empty(), VersjonType.REGISTER);
        var aktørArbeid = builder.getAktørArbeidBuilder(aktørId1);
        aktørArbeid.leggTilYrkesaktivitet(
            YrkesaktivitetBuilder.oppdatere(empty())
                .medArbeidType(ArbeidType.FRILANSER_OPPDRAGSTAKER_MED_MER)
                .medArbeidsforholdId(arbeidsforholdId)
                .medArbeidsgiver(arbeidsgiver)
                .leggTilAktivitetsAvtale(AktivitetsAvtaleBuilder.ny()
                    .medProsentsats(BigDecimal.ONE)
                    .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fraOgMed.plusDays(5), fraOgMed.plusDays(6))))
                .build());
        builder.leggTilAktørArbeid(aktørArbeid);

        var aktørInntekt = builder.getAktørInntektBuilder(aktørId1);
        aktørInntekt.leggTilInntekt(InntektBuilder.oppdatere(empty())
            .medArbeidsgiver(arbeidsgiver)
            .medInntektsKilde(InntektsKilde.INNTEKT_OPPTJENING)
            .leggTilInntektspost(InntektspostBuilder.ny()
                .medBeløp(BigDecimal.TEN)
                .medInntektspostType(InntektspostType.LØNN)
                .medPeriode(fraOgMed.plusDays(5), fraOgMed.plusDays(6))));
        builder.leggTilAktørInntekt(aktørInntekt);

        iayTjeneste.lagreIayAggregat(behandling.getId(), builder);

        lagreOpptjeningsPeriode(behandling, tilOgMed);

        // Act
        List<AksjonspunktResultat> aksjonspunktResultater = utleder.utledAksjonspunkterFor(lagInput(behandling));

        // Assert
        assertThat(aksjonspunktResultater).hasSize(1);
        assertThat(aksjonspunktResultater.get(0).getAksjonspunktDefinisjon()).isEqualTo(AksjonspunktDefinisjon.VURDER_PERIODER_MED_OPPTJENING);
    }

    @Test
    public void skal_ikke_opprette_aksjonspunkt_når_ingen_arbeidsavtaler_har_0_stillingsprosent() {
        // Arrange
        AktørId aktørId1 = AktørId.dummy();
        var scenario = TestScenarioBuilder.builderMedSøknad().medBruker(aktørId1);
        Behandling behandling = lagre(scenario);

        // Act
        List<AksjonspunktResultat> aksjonspunktResultater = utleder.utledAksjonspunkterFor(lagInput(behandling));

        // Assert
        assertThat(aksjonspunktResultater).isEmpty();
    }

    @Test
    public void skal_opprette_aksjonspunkt_når_en_arbeidsavtale_har_0_stillingsprosent_for_forenklet_oppgjørsordning() {
        // Arrange
        AktørId aktørId1 = AktørId.dummy();
        var scenario = TestScenarioBuilder.builderMedSøknad().medBruker(aktørId1);
        Behandling behandling = lagre(scenario);
        LocalDate tilOgMed = LocalDate.now().plusMonths(1);
        InntektArbeidYtelseAggregatBuilder builder = InntektArbeidYtelseAggregatBuilder.oppdatere(empty(), VersjonType.REGISTER);
        InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder aktørArbeidBuilder = builder.getAktørArbeidBuilder(aktørId1);
        aktørArbeidBuilder.leggTilYrkesaktivitet(byggYrkesaktivitet(tilOgMed, ArbeidType.FORENKLET_OPPGJØRSORDNING, BigDecimal.ZERO));
        builder.leggTilAktørArbeid(aktørArbeidBuilder);
        iayTjeneste.lagreIayAggregat(behandling.getId(), builder);
        lagreOpptjeningsPeriode(behandling, tilOgMed);

        // Act
        List<AksjonspunktResultat> aksjonspunktResultater = utleder.utledAksjonspunkterFor(lagInput(behandling));

        // Assert
        assertThat(aksjonspunktResultater).hasSize(1);
        assertThat(aksjonspunktResultater.get(0).getAksjonspunktDefinisjon()).isEqualTo(AksjonspunktDefinisjon.VURDER_PERIODER_MED_OPPTJENING);
    }

    private YrkesaktivitetBuilder byggYrkesaktivitet(LocalDate tilOgMed, ArbeidType arbeidType, BigDecimal stillingsprosent) {
        DatoIntervallEntitet periode = DatoIntervallEntitet.fraOgMed(tilOgMed.minusMonths(10));
        return YrkesaktivitetBuilder.oppdatere(empty())
            .medArbeidType(arbeidType)
            .medArbeidsgiver(Arbeidsgiver.virksomhet("9712344321"))
            .leggTilAktivitetsAvtale(AktivitetsAvtaleBuilder.ny()
                .medProsentsats(BigDecimal.ZERO)
                .medPeriode(periode)
                .medSisteLønnsendringsdato(periode.getFomDato()))
            .leggTilAktivitetsAvtale(AktivitetsAvtaleBuilder.ny()
                .medProsentsats(stillingsprosent)
                .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(tilOgMed.minusMonths(10), tilOgMed)));
    }

    @Test
    public void skal_opprette_aksjonspunkt_når_en_arbeidsavtale_har_0_stillingsprosent() {
        // Arrange
        AktørId aktørId1 = AktørId.dummy();
        LocalDate tilOgMed = LocalDate.now().plusMonths(1);
        var scenario = TestScenarioBuilder.builderMedSøknad().medBruker(aktørId1);
        Behandling behandling = lagre(scenario);

        var builder = InntektArbeidYtelseAggregatBuilder.oppdatere(empty(), VersjonType.REGISTER);
        var aktørArbeidBuilder = builder.getAktørArbeidBuilder(aktørId1);
        aktørArbeidBuilder.leggTilYrkesaktivitet(byggYrkesaktivitet(tilOgMed, ArbeidType.ORDINÆRT_ARBEIDSFORHOLD, BigDecimal.ZERO));
        builder.leggTilAktørArbeid(aktørArbeidBuilder);
        iayTjeneste.lagreIayAggregat(behandling.getId(), builder);

        lagreOpptjeningsPeriode(behandling, tilOgMed);

        // Act
        List<AksjonspunktResultat> aksjonspunktResultater = utleder.utledAksjonspunkterFor(lagInput(behandling));

        // Assert
        assertThat(aksjonspunktResultater).hasSize(1);
        assertThat(aksjonspunktResultater.get(0).getAksjonspunktDefinisjon()).isEqualTo(AksjonspunktDefinisjon.VURDER_PERIODER_MED_OPPTJENING);
    }

    @Test
    public void skal_ikke_opprette_aksjonspunkt_når_en_arbeidsavtale_har_0_stillingsprosent_men_utfor_periode() {
        // Arrange
        AktørId aktørId1 = AktørId.dummy();

        LocalDate tilOgMed = LocalDate.now().plusMonths(1);
        var scenario = TestScenarioBuilder.builderMedSøknad().medBruker(aktørId1);
        Behandling behandling = lagre(scenario);

        var builder = InntektArbeidYtelseAggregatBuilder.oppdatere(empty(), VersjonType.REGISTER);
        var aktørArbeidBuilder = builder.getAktørArbeidBuilder(aktørId1);
        aktørArbeidBuilder.leggTilYrkesaktivitet(byggYrkesaktivitet(tilOgMed.minusMonths(11), ArbeidType.ORDINÆRT_ARBEIDSFORHOLD, BigDecimal.ZERO));
        builder.leggTilAktørArbeid(aktørArbeidBuilder);
        iayTjeneste.lagreIayAggregat(behandling.getId(), builder);

        lagreOpptjeningsPeriode(behandling, tilOgMed);

        // Act
        List<AksjonspunktResultat> aksjonspunktResultater = utleder.utledAksjonspunkterFor(lagInput(behandling));

        // Assert
        assertThat(aksjonspunktResultater).isEmpty();
    }

    private void lagreOpptjeningsPeriode(Behandling behandling, LocalDate opptjeningTom) {
        opptjeningRepository.lagreOpptjeningsperiode(behandling, opptjeningTom.minusMonths(10), opptjeningTom, false);
    }
}
