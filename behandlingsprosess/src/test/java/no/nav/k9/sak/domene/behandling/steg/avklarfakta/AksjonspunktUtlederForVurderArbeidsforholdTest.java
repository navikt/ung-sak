package no.nav.k9.sak.domene.behandling.steg.avklarfakta;

import static no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon.VURDER_ARBEIDSFORHOLD;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.k9.kodeverk.arbeidsforhold.ArbeidType;
import no.nav.k9.kodeverk.arbeidsforhold.InntektsKilde;
import no.nav.k9.kodeverk.arbeidsforhold.InntektsmeldingInnsendingsårsak;
import no.nav.k9.kodeverk.arbeidsforhold.InntektspostType;
import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandling.Skjæringstidspunkt;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktUtlederInput;
import no.nav.k9.sak.behandlingskontroll.AksjonspunktResultat;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.db.util.JpaExtension;
import no.nav.k9.sak.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.arbeidsforhold.InntektsmeldingTjeneste;
import no.nav.k9.sak.domene.arbeidsforhold.VurderArbeidsforholdTjeneste;
import no.nav.k9.sak.domene.arbeidsforhold.impl.DefaultManglendePåkrevdeInntektsmeldingerTjeneste;
import no.nav.k9.sak.domene.arbeidsforhold.impl.YtelsespesifikkeInntektsmeldingTjeneste;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.k9.sak.domene.iay.modell.InntektBuilder;
import no.nav.k9.sak.domene.iay.modell.InntektsmeldingBuilder;
import no.nav.k9.sak.domene.iay.modell.InntektspostBuilder;
import no.nav.k9.sak.domene.iay.modell.Opptjeningsnøkkel;
import no.nav.k9.sak.domene.iay.modell.YrkesaktivitetBuilder;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.test.util.UnitTestLookupInstanceImpl;
import no.nav.k9.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;
import no.nav.k9.sak.typer.JournalpostId;


@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class AksjonspunktUtlederForVurderArbeidsforholdTest {

    private static final String ORGNR = "21542512";

    @Inject
    private EntityManager entityManager;

    private BehandlingRepositoryProvider repositoryProvider ;
    private InntektArbeidYtelseTjeneste iayTjeneste  ;
    private InntektsmeldingTjeneste inntektsmeldingTjeneste ;
    private Instance<YtelsespesifikkeInntektsmeldingTjeneste> påkrevdeInntektsmeldingerTjeneste ;
    private VurderArbeidsforholdTjeneste tjeneste  ;

    @Spy
    private AksjonspunktUtlederForVurderArbeidsforhold utleder ;

    @BeforeEach
    public void setupt(){
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        iayTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();
        inntektsmeldingTjeneste = new InntektsmeldingTjeneste(iayTjeneste);
        påkrevdeInntektsmeldingerTjeneste = new UnitTestLookupInstanceImpl<>(new DefaultManglendePåkrevdeInntektsmeldingerTjeneste());
        tjeneste = new VurderArbeidsforholdTjeneste(iayTjeneste, påkrevdeInntektsmeldingerTjeneste);

        utleder = new AksjonspunktUtlederForVurderArbeidsforhold(
            iayTjeneste,
            tjeneste);
    }

    private AksjonspunktUtlederInput lagRef(Behandling behandling) {
        Skjæringstidspunkt skjæringstidspunkt = Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(LocalDate.now()).build();
        return new AksjonspunktUtlederInput(BehandlingReferanse.fra(behandling, skjæringstidspunkt));
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
        assertThat(aksjonspunktResultater.get(0).getAksjonspunktDefinisjon()).isEqualTo(VURDER_ARBEIDSFORHOLD);
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
        InntektsmeldingBuilder inntektsmeldingBuilder = InntektsmeldingBuilder.builder()
            .medArbeidsgiver(Arbeidsgiver.virksomhet(virksomhetOrgnr))
            .medKanalreferanse("AR" + LocalDateTime.now())
            .medInnsendingstidspunkt(LocalDateTime.now())
            .medArbeidsforholdId(arbeidsforholdId)
            .medJournalpostId(journalpostId)
            .medBeløp(BigDecimal.TEN)
            .medStartDatoPermisjon(LocalDate.now())
            .medNærRelasjon(false)
            .medInntektsmeldingaarsak(InntektsmeldingInnsendingsårsak.NY);
        inntektsmeldingTjeneste.lagreInntektsmeldinger(behandling.getFagsak().getSaksnummer(), behandling.getId(), List.of(inntektsmeldingBuilder));
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
