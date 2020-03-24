package no.nav.k9.sak.domene.behandling.steg.avklarfakta;

import static no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon.VURDER_ARBEIDSFORHOLD;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.mockito.Spy;

import no.nav.k9.kodeverk.arbeidsforhold.ArbeidType;
import no.nav.k9.kodeverk.arbeidsforhold.InntektsKilde;
import no.nav.k9.kodeverk.arbeidsforhold.InntektsmeldingInnsendingsårsak;
import no.nav.k9.kodeverk.arbeidsforhold.InntektspostType;
import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandling.Skjæringstidspunkt;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktUtlederInput;
import no.nav.k9.sak.behandlingskontroll.AksjonspunktResultat;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.db.util.UnittestRepositoryRule;
import no.nav.k9.sak.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.arbeidsforhold.InntektsmeldingTjeneste;
import no.nav.k9.sak.domene.arbeidsforhold.VurderArbeidsforholdTjeneste;
import no.nav.k9.sak.domene.arbeidsforhold.impl.InntektsmeldingFilterYtelseImpl;
import no.nav.k9.sak.domene.arbeidsforhold.impl.InntektsmeldingRegisterTjeneste;
import no.nav.k9.sak.domene.arbeidsforhold.impl.PåkrevdeInntektsmeldingerTjeneste;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.k9.sak.domene.iay.modell.InntektBuilder;
import no.nav.k9.sak.domene.iay.modell.InntektsmeldingBuilder;
import no.nav.k9.sak.domene.iay.modell.InntektspostBuilder;
import no.nav.k9.sak.domene.iay.modell.Opptjeningsnøkkel;
import no.nav.k9.sak.domene.iay.modell.YrkesaktivitetBuilder;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.vedtak.felles.testutilities.cdi.UnitTestLookupInstanceImpl;

public class AksjonspunktUtlederForVurderArbeidsforholdTest {

    private static final String ORGNR = "21542512";

    @Rule
    public UnittestRepositoryRule repoRule = new UnittestRepositoryRule();
    private BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(repoRule.getEntityManager());
    private InntektArbeidYtelseTjeneste iayTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();
    private InntektsmeldingTjeneste inntektsmeldingTjeneste = new InntektsmeldingTjeneste(iayTjeneste);
    private InntektsmeldingRegisterTjeneste inntektsmeldingArkivTjeneste = new InntektsmeldingRegisterTjeneste(iayTjeneste, inntektsmeldingTjeneste, null, new UnitTestLookupInstanceImpl<>(new InntektsmeldingFilterYtelseImpl()));
    private PåkrevdeInntektsmeldingerTjeneste påkrevdeInntektsmeldingerTjeneste = new PåkrevdeInntektsmeldingerTjeneste(inntektsmeldingArkivTjeneste, repositoryProvider.getSøknadRepository());
    private VurderArbeidsforholdTjeneste tjeneste = new VurderArbeidsforholdTjeneste(iayTjeneste, påkrevdeInntektsmeldingerTjeneste);

    @Spy
    private AksjonspunktUtlederForVurderArbeidsforhold utleder = new AksjonspunktUtlederForVurderArbeidsforhold(
        repositoryProvider.getBehandlingRepository(),
        iayTjeneste,
        tjeneste);

    @Test
    public void skal_få_aksjonspunkt_når_det_finnes_inntekt_og_ikke_arbeidsforhold() {
        // Arrange
        AktørId aktørId1 = AktørId.dummy();
        var scenario = TestScenarioBuilder.builderMedSøknad().medBruker(aktørId1);
        Behandling behandling = scenario.lagre(repositoryProvider);

        var arbeidsforholdId = InternArbeidsforholdRef.nyRef();
        opprettInntekt(aktørId1, behandling, ORGNR, arbeidsforholdId);

        // Act
        List<AksjonspunktResultat> aksjonspunktResultater = utleder.utledAksjonspunkterFor(lagRef(behandling));

        // Assert
        assertThat(aksjonspunktResultater).hasSize(1);
    }

    private AksjonspunktUtlederInput lagRef(Behandling behandling) {
        Skjæringstidspunkt skjæringstidspunkt = Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(LocalDate.now()).build();
        return new AksjonspunktUtlederInput(BehandlingReferanse.fra(behandling, skjæringstidspunkt));
    }

    @Test
    public void skal_få_aksjonspunkt_når_det_ikke_finnes_inntekt_eller_arbeidsforhold() {
        // Arrange
        AktørId aktørId1 = AktørId.dummy();
        var scenario = TestScenarioBuilder.builderMedSøknad().medBruker(aktørId1);
        Behandling behandling = scenario.lagre(repositoryProvider);
        // Act
        List<AksjonspunktResultat> aksjonspunktResultater = utleder.utledAksjonspunkterFor(lagRef(behandling));

        // Assert
        assertThat(aksjonspunktResultater).containsExactly(AksjonspunktResultat.opprettForAksjonspunkt(VURDER_ARBEIDSFORHOLD));
    }

    @Test
    public void skal_få_aksjonspunkt_når_mottatt_inntektsmelding_men_ikke_arbeidsforhold() {
        // Arrange
        AktørId aktørId1 = AktørId.dummy();
        var scenario = TestScenarioBuilder.builderMedSøknad().medBruker(aktørId1);

        Behandling behandling = scenario.lagre(repositoryProvider);

        var arbeidsforholdId = InternArbeidsforholdRef.nyRef();
        sendInnInntektsmeldingPå(behandling, ORGNR, arbeidsforholdId);

        // Act
        List<AksjonspunktResultat> aksjonspunktResultater = utleder.utledAksjonspunkterFor(lagRef(behandling));

        // Assert
        assertThat(aksjonspunktResultater).isNotEmpty();
    }

    @Test
    public void skal_ikke_få_aksjonspunkt_ved_komplett_søknad_med_inntektsmeldinger() {
        // Arrange
        AktørId aktørId1 = AktørId.dummy();
        var scenario = TestScenarioBuilder.builderMedSøknad().medBruker(aktørId1);
        Behandling behandling = scenario.lagre(repositoryProvider);

        var arbeidsforholdId = InternArbeidsforholdRef.nyRef();

        final InntektArbeidYtelseAggregatBuilder builder = iayTjeneste.opprettBuilderForRegister(behandling.getId());
        leggTilArbeidsforholdPåBehandling(behandling, ORGNR, arbeidsforholdId, builder);
        iayTjeneste.lagreIayAggregat(behandling.getId(), builder);

        sendInnInntektsmeldingPå(behandling, ORGNR, arbeidsforholdId);

        // Act
        List<AksjonspunktResultat> aksjonspunktResultater = utleder.utledAksjonspunkterFor(lagRef(behandling));

        // Assert
        assertThat(aksjonspunktResultater).isEmpty();
    }

    @Test
    public void skal_ikke_få_aksjonspunkt_ved_søknad_uten_inntekter() {
        // Arrange
        AktørId aktørId1 = AktørId.dummy();
        var scenario = TestScenarioBuilder.builderMedSøknad().medBruker(aktørId1);
        Behandling behandling = scenario.lagre(repositoryProvider);

        var arbeidsforholdId = InternArbeidsforholdRef.nyRef();

        final InntektArbeidYtelseAggregatBuilder builder = iayTjeneste.opprettBuilderForRegister(behandling.getId());
        leggTilArbeidsforholdPåBehandling(behandling, ORGNR, arbeidsforholdId, builder);
        iayTjeneste.lagreIayAggregat(behandling.getId(), builder);

        // Act
        List<AksjonspunktResultat> aksjonspunktResultater = utleder.utledAksjonspunkterFor(lagRef(behandling));

        // Assert
        assertThat(aksjonspunktResultater).isNotEmpty();  // TODO: Expect empty hvis man ikke venter AP når det ikke foreligger inntekt

        // Arrange + Act
        opprettInntekt(aktørId1, behandling, ORGNR, arbeidsforholdId);
        aksjonspunktResultater = utleder.utledAksjonspunkterFor(lagRef(behandling));

        // Assert
        assertThat(aksjonspunktResultater).isNotEmpty();
    }

    @Test
    public void skal_ikke_få_aksjonspunkt_ved_komplett_søknad_med_to_arbeidsforhold_etter_begge_inntektsmeldinger() {
        // Arrange
        AktørId aktørId1 = AktørId.dummy();
        var scenario = TestScenarioBuilder.builderMedSøknad().medBruker(aktørId1);
        Behandling behandling = scenario.lagre(repositoryProvider);
        String virksomhetOrgnr2 = "648751348";
        var arbeidsforholdId1 = InternArbeidsforholdRef.nyRef();
        var arbeidsforholdId2 = InternArbeidsforholdRef.nyRef();

        final InntektArbeidYtelseAggregatBuilder builder = iayTjeneste.opprettBuilderForRegister(behandling.getId());
        leggTilArbeidsforholdPåBehandling(behandling, ORGNR, arbeidsforholdId1, builder);
        leggTilArbeidsforholdPåBehandling(behandling, virksomhetOrgnr2, arbeidsforholdId2, builder);
        iayTjeneste.lagreIayAggregat(behandling.getId(), builder);
        opprettInntekt(aktørId1, behandling, ORGNR, arbeidsforholdId1);
        opprettInntekt(aktørId1, behandling, virksomhetOrgnr2, arbeidsforholdId2);

        sendInnInntektsmeldingPå(behandling, ORGNR, arbeidsforholdId1);

        // Act
        List<AksjonspunktResultat> aksjonspunktResultater = utleder.utledAksjonspunkterFor(lagRef(behandling));
        // Assert
        assertThat(aksjonspunktResultater).hasSize(1);

        // Arrange
        sendInnInntektsmeldingPå(behandling, virksomhetOrgnr2, arbeidsforholdId2);
        // Act
        aksjonspunktResultater = utleder.utledAksjonspunkterFor(lagRef(behandling));
        // Assert
        assertThat(aksjonspunktResultater).isEmpty();
    }

    @Test
    public void skal_ikke_få_aksjonspunkt_på_berørt_behandling() {
        // Arrange
        AktørId aktørId1 = AktørId.dummy();
        var scenario = TestScenarioBuilder.builderMedSøknad().medBruker(aktørId1);
        Behandling originalBehandling = scenario.lagre(repositoryProvider);
        scenario = TestScenarioBuilder.builderMedSøknad().medBruker(aktørId1);
        scenario.medOriginalBehandling(originalBehandling, BehandlingÅrsakType.BERØRT_BEHANDLING);
        Behandling behandling = scenario.lagre(repositoryProvider);
        String virksomhetOrgnr2 = "648751348";
        var arbeidsforholdId1 = InternArbeidsforholdRef.nyRef();
        var arbeidsforholdId2 = InternArbeidsforholdRef.nyRef();

        final InntektArbeidYtelseAggregatBuilder builder = iayTjeneste.opprettBuilderForRegister(behandling.getId());
        leggTilArbeidsforholdPåBehandling(behandling, ORGNR, arbeidsforholdId1, builder);
        leggTilArbeidsforholdPåBehandling(behandling, virksomhetOrgnr2, arbeidsforholdId2, builder);
        iayTjeneste.lagreIayAggregat(behandling.getId(), builder);
        opprettInntekt(aktørId1, behandling, ORGNR, arbeidsforholdId1);
        opprettInntekt(aktørId1, behandling, virksomhetOrgnr2, arbeidsforholdId2);

        // Act
        List<AksjonspunktResultat> aksjonspunktResultater = utleder.utledAksjonspunkterFor(lagRef(behandling));

        // Assert
        assertThat(aksjonspunktResultater).isEmpty();
    }

    @Test
    public void skal_ikke_få_aksjonspunkt_ved_endring_i_antall_arbeidsforhold_og_komplett() {
        // Arrange
        AktørId aktørId1 = AktørId.dummy();
        var scenario = TestScenarioBuilder.builderMedSøknad().medBruker(aktørId1);
        scenario.medBehandlingStegStart(BehandlingStegType.KONTROLLER_FAKTA);

        Behandling behandling = scenario.lagre(repositoryProvider);

        var arbeidsforholdId = InternArbeidsforholdRef.nyRef();

        final InntektArbeidYtelseAggregatBuilder builder = iayTjeneste.opprettBuilderForRegister(behandling.getId());
        leggTilArbeidsforholdPåBehandling(behandling, ORGNR, arbeidsforholdId, builder);
        iayTjeneste.lagreIayAggregat(behandling.getId(), builder);
        final InntektArbeidYtelseAggregatBuilder builder2 = iayTjeneste.opprettBuilderForRegister(behandling.getId());
        leggTilArbeidsforholdPåBehandling(behandling, ORGNR, arbeidsforholdId, builder2);
        var arbeidsforholdId1 = InternArbeidsforholdRef.nyRef();
        leggTilArbeidsforholdPåBehandling(behandling, ORGNR, arbeidsforholdId1, builder2);
        iayTjeneste.lagreIayAggregat(behandling.getId(), builder2);

        sendInnInntektsmeldingPå(behandling, ORGNR, arbeidsforholdId);
        sendInnInntektsmeldingPå(behandling, ORGNR, arbeidsforholdId1); // Kommer inntektsmelding på arbeidsforhold vi ikke kjenner før STP

        // Act
        List<AksjonspunktResultat> aksjonspunktResultater = utleder.utledAksjonspunkterFor(lagRef(behandling));

        // Assert
        assertThat(aksjonspunktResultater).isEmpty();
    }

    @Test
    public void skal_få_autopunkt_for_inntektsmelding_med_arbeidsforholdId_uten_match_i_aareg() {
        // Arrange
        AktørId aktørId1 = AktørId.dummy();
        var scenario = TestScenarioBuilder.builderMedSøknad().medBruker(aktørId1);
        Behandling behandling = scenario.lagre(repositoryProvider);

        var arbeidsforholdId = InternArbeidsforholdRef.nyRef();
        var arbeidsforholdId_2 = InternArbeidsforholdRef.nyRef();
        final InntektArbeidYtelseAggregatBuilder builder = iayTjeneste.opprettBuilderForRegister(behandling.getId());
        leggTilArbeidsforholdPåBehandling(behandling, ORGNR, arbeidsforholdId, builder);
        iayTjeneste.lagreIayAggregat(behandling.getId(), builder);
        sendInnInntektsmeldingPå(behandling, ORGNR, arbeidsforholdId_2);

        // Act
        List<AksjonspunktResultat> aksjonspunktResultater = utleder.utledAksjonspunkterFor(lagRef(behandling));

        // Assert
        assertThat(aksjonspunktResultater).hasSize(1);
        assertThat(aksjonspunktResultater.get(0).getAksjonspunktDefinisjon())
            .isEqualTo(AksjonspunktDefinisjon.AUTO_VENT_INNTEKTSMELDING_MED_UGYLDIG_ARBEIDSFORHOLDID);
    }

    private void leggTilArbeidsforholdPåBehandling(Behandling behandling, String virksomhetOrgnr, InternArbeidsforholdRef ref,
                                                   InntektArbeidYtelseAggregatBuilder builder) {
        final Arbeidsgiver arbeidsgiver = Arbeidsgiver.virksomhet(virksomhetOrgnr);
        final InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder arbeidBuilder = builder.getAktørArbeidBuilder(behandling.getAktørId());
        final Opptjeningsnøkkel nøkkel = Opptjeningsnøkkel.forArbeidsforholdIdMedArbeidgiver(ref, arbeidsgiver);
        final YrkesaktivitetBuilder yrkesaktivitetBuilderForType = arbeidBuilder.getYrkesaktivitetBuilderForNøkkelAvType(nøkkel,
            ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);
        yrkesaktivitetBuilderForType
            .medArbeidsgiver(arbeidsgiver)
            .medArbeidsforholdId(ref)
            .leggTilAktivitetsAvtale(yrkesaktivitetBuilderForType
                .getAktivitetsAvtaleBuilder(DatoIntervallEntitet.fraOgMed(LocalDate.now().minusMonths(3)), false)
                .medProsentsats(BigDecimal.valueOf(100)))
            .leggTilAktivitetsAvtale(yrkesaktivitetBuilderForType
                .getAktivitetsAvtaleBuilder(DatoIntervallEntitet.fraOgMed(LocalDate.now().minusMonths(3)), true));
        arbeidBuilder.leggTilYrkesaktivitet(yrkesaktivitetBuilderForType);
        builder.leggTilAktørArbeid(arbeidBuilder);
    }

    private void sendInnInntektsmeldingPå(Behandling behandling, String virksomhetOrgnr, InternArbeidsforholdRef arbeidsforholdId) {
        JournalpostId journalpostId = new JournalpostId(1L);
        final InntektsmeldingBuilder inntektsmeldingBuilder = InntektsmeldingBuilder.builder()
            .medArbeidsgiver(Arbeidsgiver.virksomhet(virksomhetOrgnr))
            .medInnsendingstidspunkt(LocalDateTime.now())
            .medArbeidsforholdId(arbeidsforholdId)
            .medJournalpostId(journalpostId)
            .medBeløp(BigDecimal.TEN)
            .medStartDatoPermisjon(LocalDate.now())
            .medNærRelasjon(false)
            .medInntektsmeldingaarsak(InntektsmeldingInnsendingsårsak.NY);
        inntektsmeldingTjeneste.lagreInntektsmelding(behandling.getFagsak().getSaksnummer(), behandling.getId(), inntektsmeldingBuilder);
    }

    private void opprettInntekt(AktørId aktørId1, Behandling behandling, String virksomhetOrgnr, InternArbeidsforholdRef arbeidsforholdRef) {
        InntektArbeidYtelseAggregatBuilder builder = iayTjeneste.opprettBuilderForRegister(behandling.getId());
        InntektArbeidYtelseAggregatBuilder.AktørInntektBuilder inntektBuilder = builder.getAktørInntektBuilder(aktørId1);
        final Arbeidsgiver arbeidsgiver = Arbeidsgiver.virksomhet(virksomhetOrgnr);
        final Opptjeningsnøkkel opptjeningsnøkkel = Opptjeningsnøkkel.forArbeidsforholdIdMedArbeidgiver(arbeidsforholdRef, arbeidsgiver);
        InntektBuilder tilInntektspost = inntektBuilder.getInntektBuilder(InntektsKilde.INNTEKT_OPPTJENING, opptjeningsnøkkel);
        tilInntektspost.medArbeidsgiver(arbeidsgiver);
        InntektspostBuilder inntektspostBuilder = tilInntektspost.getInntektspostBuilder();

        InntektspostBuilder inntektspost = inntektspostBuilder
            .medBeløp(BigDecimal.TEN)
            .medPeriode(LocalDate.now().minusMonths(1), LocalDate.now())
            .medInntektspostType(InntektspostType.LØNN);

        tilInntektspost
            .leggTilInntektspost(inntektspost)
            .medInntektsKilde(InntektsKilde.INNTEKT_OPPTJENING);

        InntektArbeidYtelseAggregatBuilder.AktørInntektBuilder aktørInntekt = inntektBuilder
            .leggTilInntekt(tilInntektspost);

        builder.leggTilAktørInntekt(aktørInntekt);

        iayTjeneste.lagreIayAggregat(behandling.getId(), builder);
    }
}
