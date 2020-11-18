package no.nav.k9.sak.domene.arbeidsforhold.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.k9.kodeverk.arbeidsforhold.ArbeidType;
import no.nav.k9.kodeverk.arbeidsforhold.InntektsmeldingInnsendingsårsak;
import no.nav.k9.kodeverk.behandling.BehandlingResultatType;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.kodeverk.behandling.FagsakStatus;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.BehandlingÅrsak;
import no.nav.k9.sak.db.util.JpaExtension;
import no.nav.k9.sak.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.arbeidsforhold.InntektsmeldingTjeneste;
import no.nav.k9.sak.domene.arbeidsforhold.VurderArbeidsforholdTjeneste;
import no.nav.k9.sak.domene.arbeidsforhold.testutilities.behandling.IAYRepositoryProvider;
import no.nav.k9.sak.domene.arbeidsforhold.testutilities.behandling.IAYScenarioBuilder;
import no.nav.k9.sak.domene.iay.modell.InntektsmeldingBuilder;
import no.nav.k9.sak.domene.iay.modell.Opptjeningsnøkkel;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.EksternArbeidsforholdRef;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.vedtak.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.vedtak.felles.testutilities.cdi.UnitTestLookupInstanceImpl;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
public class VurderArbeidsforholdTjenesteImplTest {

    private static final LocalDate IDAG = LocalDate.now();
    private final LocalDate skjæringstidspunkt = IDAG.minusDays(30);

    @Inject
    private EntityManager entityManager;

    private LocalDateTime nåTid ;
    private volatile int nåTidTeller;
    private IAYRepositoryProvider repositoryProvider ;
    private InntektArbeidYtelseTjeneste iayTjeneste;
    private InntektsmeldingTjeneste inntektsmeldingTjeneste ;
    private InntektsmeldingFilterYtelse inntektsmeldingFilterYtelse ;
    private InntektsmeldingRegisterTjeneste inntektsmeldingArkivTjeneste ;
    private Instance<YtelsespesifikkeInntektsmeldingTjeneste> påkrevdeInntektsmeldingerTjeneste ;
    private VurderArbeidsforholdTjeneste tjeneste ;

    @BeforeEach
    public void setup() {
        nåTid = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        repositoryProvider = new IAYRepositoryProvider(entityManager);
        iayTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();
        inntektsmeldingTjeneste = new InntektsmeldingTjeneste(iayTjeneste);
        inntektsmeldingFilterYtelse = new InntektsmeldingFilterYtelseImpl();
        inntektsmeldingArkivTjeneste = new InntektsmeldingRegisterTjeneste(iayTjeneste, inntektsmeldingTjeneste, null, new UnitTestLookupInstanceImpl<>(inntektsmeldingFilterYtelse));
        påkrevdeInntektsmeldingerTjeneste = new UnitTestLookupInstanceImpl<>(new DefaultManglendePåkrevdeInntektsmeldingerTjeneste(inntektsmeldingArkivTjeneste, iayTjeneste, repositoryProvider.getSøknadRepository()));
        tjeneste = new VurderArbeidsforholdTjeneste(påkrevdeInntektsmeldingerTjeneste);
    }

    @Test
    public void skal_ikke_gi_aksjonspunkt() {
        var scenario = IAYScenarioBuilder.nyttScenario(FagsakYtelseType.FORELDREPENGER);

        var behandling = scenario.lagre(repositoryProvider);

        var builder = iayTjeneste.opprettBuilderForRegister(behandling.getId());
        var arbeidBuilder = builder.getAktørArbeidBuilder(behandling.getAktørId());
        var yrkesBuilder = arbeidBuilder.getYrkesaktivitetBuilderForType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);
        var virksomhet = Arbeidsgiver.virksomhet(opprettVirksomhet("123123123"));
        var ref = EksternArbeidsforholdRef.ref("ref");
        var internRef = builder.medNyInternArbeidsforholdRef(virksomhet, ref);
        yrkesBuilder.medArbeidsgiver(virksomhet)
            .medArbeidsforholdId(internRef);
        var avtaleBuilder = yrkesBuilder.getAktivitetsAvtaleBuilder();
        avtaleBuilder.medPeriode(DatoIntervallEntitet.fraOgMed(IDAG.minusYears(1)))
            .medProsentsats(BigDecimal.TEN);
        var avtaleBuilder1 = yrkesBuilder.getAktivitetsAvtaleBuilder();
        avtaleBuilder1.medPeriode(DatoIntervallEntitet.fraOgMed(IDAG.minusYears(1)));
        yrkesBuilder.leggTilAktivitetsAvtale(avtaleBuilder).leggTilAktivitetsAvtale(avtaleBuilder1);
        arbeidBuilder.leggTilYrkesaktivitet(yrkesBuilder);
        builder.leggTilAktørArbeid(arbeidBuilder);
        iayTjeneste.lagreIayAggregat(behandling.getId(), builder);
        sendNyInntektsmelding(behandling, virksomhet, ref);

        Map<Arbeidsgiver, Set<InternArbeidsforholdRef>> vurder = hentArbeidsforhold(behandling);
        assertThat(vurder).isEmpty();

        avsluttBehandlingOgFagsak(behandling);

        @SuppressWarnings("unused")
        var revurdering = opprettRevurderingsbehandling(behandling);

        sendInnInntektsmelding(behandling, virksomhet, null);

        vurder = hentArbeidsforhold(behandling);
        assertThat(vurder).isEmpty();
    }

    private BehandlingReferanse lagRef(Behandling behandling) {
        return BehandlingReferanse.fra(behandling, skjæringstidspunkt);
    }

    @Test
    public void skal_ikke_gi_aksjonspunkt_2() {
        var scenario = IAYScenarioBuilder.nyttScenario(FagsakYtelseType.FORELDREPENGER);

        var behandling = scenario.lagre(repositoryProvider);

        var builder = iayTjeneste.opprettBuilderForRegister(behandling.getId());
        var arbeidBuilder = builder.getAktørArbeidBuilder(behandling.getAktørId());
        var yrkesBuilder = arbeidBuilder.getYrkesaktivitetBuilderForType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);
        var virksomhet = Arbeidsgiver.virksomhet(opprettVirksomhet("123123123"));
        var ref = EksternArbeidsforholdRef.ref("ref");
        var internRef = builder.medNyInternArbeidsforholdRef(virksomhet, ref);
        yrkesBuilder.medArbeidsgiver(virksomhet)
            .medArbeidsforholdId(internRef);
        var avtaleBuilder = yrkesBuilder.getAktivitetsAvtaleBuilder();
        avtaleBuilder.medPeriode(DatoIntervallEntitet.fraOgMed(IDAG.minusYears(1)))
            .medProsentsats(BigDecimal.TEN);
        var avtaleBuilder1 = yrkesBuilder.getAktivitetsAvtaleBuilder();
        avtaleBuilder1.medPeriode(DatoIntervallEntitet.fraOgMed(IDAG.minusYears(1)));
        yrkesBuilder.leggTilAktivitetsAvtale(avtaleBuilder)
            .leggTilAktivitetsAvtale(avtaleBuilder1);
        arbeidBuilder.leggTilYrkesaktivitet(yrkesBuilder);
        builder.leggTilAktørArbeid(arbeidBuilder);
        iayTjeneste.lagreIayAggregat(behandling.getId(), builder);

        sendNyInntektsmelding(behandling, virksomhet, ref);

        Map<Arbeidsgiver, Set<InternArbeidsforholdRef>> vurder = hentArbeidsforhold(behandling);
        assertThat(vurder).isEmpty();

        avsluttBehandlingOgFagsak(behandling);

        @SuppressWarnings("unused")
        var revurdering = opprettRevurderingsbehandling(behandling);

        sendInnInntektsmelding(behandling, virksomhet, ref);

        vurder = hentArbeidsforhold(behandling);
        assertThat(vurder).isEmpty();
    }

    private Map<Arbeidsgiver, Set<InternArbeidsforholdRef>> hentArbeidsforhold(Behandling behandling) {
        var iayGrunnlag = iayTjeneste.hentGrunnlag(behandling.getId());
        Map<Arbeidsgiver, Set<InternArbeidsforholdRef>> vurder = tjeneste.vurder(lagRef(behandling));
        return vurder;
    }

    @Test
    public void skal_ikke_gi_aksjonspunkt_3() {
        var scenario = IAYScenarioBuilder.nyttScenario(FagsakYtelseType.FORELDREPENGER);
        var behandling = scenario.lagre(repositoryProvider);

        var builder = iayTjeneste.opprettBuilderForRegister(behandling.getId());
        var arbeidBuilder = builder.getAktørArbeidBuilder(behandling.getAktørId());
        String orgnummer = "123123123";
        var virksomhet = Arbeidsgiver.virksomhet(opprettVirksomhet(orgnummer));
        var ref = EksternArbeidsforholdRef.ref("ref");
        var internRef = builder.medNyInternArbeidsforholdRef(virksomhet, ref);

        var yrkesBuilder = arbeidBuilder.getYrkesaktivitetBuilderForNøkkelAvType(Opptjeningsnøkkel.forArbeidsforholdIdMedArbeidgiver(internRef, virksomhet),
            ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);
        yrkesBuilder.medArbeidsgiver(virksomhet)
            .medArbeidsforholdId(internRef);
        var avtaleBuilder = yrkesBuilder.getAktivitetsAvtaleBuilder();
        avtaleBuilder.medPeriode(DatoIntervallEntitet.fraOgMed(IDAG.minusYears(1)))
            .medProsentsats(BigDecimal.TEN);
        var avtaleBuilder3 = yrkesBuilder.getAktivitetsAvtaleBuilder()
            .medPeriode(DatoIntervallEntitet.fraOgMed(IDAG.minusYears(1)));
        yrkesBuilder.leggTilAktivitetsAvtale(avtaleBuilder).leggTilAktivitetsAvtale(avtaleBuilder3);
        arbeidBuilder.leggTilYrkesaktivitet(yrkesBuilder);
        var ref1 = EksternArbeidsforholdRef.ref("ref1");
        var internRef1 = builder.medNyInternArbeidsforholdRef(virksomhet, ref1);
        var yrkesBuilder1 = arbeidBuilder.getYrkesaktivitetBuilderForNøkkelAvType(Opptjeningsnøkkel.forArbeidsforholdIdMedArbeidgiver(internRef1, virksomhet),
            ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);
        yrkesBuilder1.medArbeidsgiver(virksomhet)
            .medArbeidsforholdId(internRef1);
        var avtaleBuilder1 = yrkesBuilder1.getAktivitetsAvtaleBuilder();
        avtaleBuilder1.medPeriode(DatoIntervallEntitet.fraOgMed(IDAG.minusYears(1)))
            .medProsentsats(BigDecimal.TEN);
        var avtaleBuilder2 = yrkesBuilder1.getAktivitetsAvtaleBuilder();
        avtaleBuilder2.medPeriode(DatoIntervallEntitet.fraOgMed(IDAG.minusYears(1)));
        yrkesBuilder1.leggTilAktivitetsAvtale(avtaleBuilder1).leggTilAktivitetsAvtale(avtaleBuilder2);
        arbeidBuilder.leggTilYrkesaktivitet(yrkesBuilder1);
        builder.leggTilAktørArbeid(arbeidBuilder);
        iayTjeneste.lagreIayAggregat(behandling.getId(), builder);

        sendNyInntektsmelding(behandling, virksomhet, ref);
        sendNyInntektsmelding(behandling, virksomhet, ref1);

        var iayGrunnlag = iayTjeneste.hentGrunnlag(behandling.getId());
        Map<Arbeidsgiver, Set<InternArbeidsforholdRef>> vurder = tjeneste.vurder(lagRef(behandling));
        assertThat(vurder).isEmpty();
    }

    private void sendNyInntektsmelding(Behandling behandling, Arbeidsgiver arbeidsgiver, EksternArbeidsforholdRef ref) {

        var journalpostId = new JournalpostId(1L);
        var inntektsmeldingBuilder = InntektsmeldingBuilder.builder()
            .medArbeidsgiver(arbeidsgiver)
            .medArbeidsforholdId(ref)
            .medBeløp(BigDecimal.TEN)
            .medStartDatoPermisjon(skjæringstidspunkt)
            .medInntektsmeldingaarsak(InntektsmeldingInnsendingsårsak.NY)
            .medKanalreferanse(nyKanalreferanse())
            .medInnsendingstidspunkt(nyTid()).medJournalpostId(journalpostId);

        inntektsmeldingTjeneste.lagreInntektsmeldinger(behandling.getFagsak().getSaksnummer(), behandling.getId(), List.of(inntektsmeldingBuilder));
    }

    private String nyKanalreferanse() {
        return "AR" + LocalDateTime.now();
    }

    private LocalDateTime nyTid() {
        return nåTid.plusSeconds((nåTidTeller++));
    }

    private void sendInnInntektsmelding(Behandling behandling, Arbeidsgiver arbeidsgiver, EksternArbeidsforholdRef ref) {
        var inntektsmeldingBuilder = InntektsmeldingBuilder.builder()
            .medArbeidsgiver(arbeidsgiver)
            .medArbeidsforholdId(ref)
            .medBeløp(BigDecimal.TEN)
            .medStartDatoPermisjon(skjæringstidspunkt)
            .medInntektsmeldingaarsak(InntektsmeldingInnsendingsårsak.ENDRING)
            .medKanalreferanse(nyKanalreferanse())
            .medInnsendingstidspunkt(nyTid()).medJournalpostId(new JournalpostId("123"));

        inntektsmeldingTjeneste.lagreInntektsmeldinger(behandling.getFagsak().getSaksnummer(), behandling.getId(), List.of(inntektsmeldingBuilder));
    }

    private void avsluttBehandlingOgFagsak(Behandling behandling) {
        var lås = repositoryProvider.getBehandlingRepository().taSkriveLås(behandling);
        behandling.setBehandlingResultatType(BehandlingResultatType.INNVILGET);
        behandling.avsluttBehandling();
        repositoryProvider.getBehandlingRepository().lagre(behandling, lås);
        var fagsakRepository = repositoryProvider.getFagsakRepository();
        fagsakRepository.oppdaterFagsakStatus(behandling.getFagsakId(), FagsakStatus.LØPENDE);
    }

    private Behandling opprettRevurderingsbehandling(Behandling opprinneligBehandling) {
        var behandlingType = BehandlingType.REVURDERING;
        var revurderingÅrsak = BehandlingÅrsak.builder(BehandlingÅrsakType.RE_HENDELSE_FØDSEL);
        var revurdering = Behandling.fraTidligereBehandling(opprinneligBehandling, behandlingType)
            .medBehandlingÅrsak(revurderingÅrsak).build();
        repositoryProvider.getBehandlingRepository().lagre(revurdering, repositoryProvider.getBehandlingRepository().taSkriveLås(revurdering));
        iayTjeneste.kopierGrunnlagFraEksisterendeBehandling(opprinneligBehandling.getId(), revurdering.getId());
        return revurdering;
    }

    private String opprettVirksomhet(String orgnummer) {
        return orgnummer;
    }
}
