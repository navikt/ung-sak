package no.nav.k9.sak.domene.arbeidsforhold.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

import no.nav.k9.kodeverk.arbeidsforhold.ArbeidType;
import no.nav.k9.kodeverk.arbeidsforhold.ArbeidsforholdHandlingType;
import no.nav.k9.kodeverk.arbeidsforhold.ArbeidsforholdKilde;
import no.nav.k9.kodeverk.arbeidsforhold.PermisjonsbeskrivelseType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.Venteårsak;
import no.nav.k9.kodeverk.geografisk.Landkoder;
import no.nav.k9.kodeverk.geografisk.Språkkode;
import no.nav.k9.kodeverk.person.NavBrukerKjønn;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandling.Skjæringstidspunkt;
import no.nav.k9.sak.behandlingslager.aktør.Personinfo;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.k9.sak.behandlingslager.behandling.aksjonspunkt.AksjonspunktTestSupport;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.behandlingslager.virksomhet.VirksomhetEntitet;
import no.nav.k9.sak.db.util.UnittestRepositoryRule;
import no.nav.k9.sak.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.arbeidsforhold.ArbeidsforholdWrapper;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.arbeidsforhold.InntektsmeldingTjeneste;
import no.nav.k9.sak.domene.arbeidsforhold.VurderArbeidsforholdTjeneste;
import no.nav.k9.sak.domene.arbeidsforhold.impl.ArbeidsforholdAdministrasjonTjeneste.UtledArbeidsforholdParametere;
import no.nav.k9.sak.domene.arbeidsforhold.person.PersonIdentTjeneste;
import no.nav.k9.sak.domene.arbeidsforhold.testutilities.behandling.IAYRepositoryProvider;
import no.nav.k9.sak.domene.arbeidsgiver.ArbeidsgiverTjeneste;
import no.nav.k9.sak.domene.arbeidsgiver.ArbeidsgiverTjenesteImpl;
import no.nav.k9.sak.domene.arbeidsgiver.VirksomhetTjeneste;
import no.nav.k9.sak.domene.iay.modell.AktivitetsAvtaleBuilder;
import no.nav.k9.sak.domene.iay.modell.ArbeidsforholdInformasjonBuilder;
import no.nav.k9.sak.domene.iay.modell.ArbeidsforholdOverstyringBuilder;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.k9.sak.domene.iay.modell.InntektsmeldingBuilder;
import no.nav.k9.sak.domene.iay.modell.OppgittAnnenAktivitet;
import no.nav.k9.sak.domene.iay.modell.OppgittOpptjeningBuilder;
import no.nav.k9.sak.domene.iay.modell.Opptjeningsnøkkel;
import no.nav.k9.sak.domene.iay.modell.Permisjon;
import no.nav.k9.sak.domene.iay.modell.PermisjonBuilder;
import no.nav.k9.sak.domene.iay.modell.VersjonType;
import no.nav.k9.sak.domene.iay.modell.YrkesaktivitetBuilder;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.EksternArbeidsforholdRef;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.k9.sak.typer.PersonIdent;
import no.nav.k9.sak.typer.Stillingsprosent;
import no.nav.vedtak.konfig.Tid;

public class ArbeidsforholdAdministrasjonTjenesteTest {

    private static final String ORG1 = "973093681";
    private static final String ORG2 = "52";

    private final LocalDate I_DAG = LocalDate.now();
    private final LocalDate ARBEIDSFORHOLD_FRA = I_DAG.minusMonths(3);
    private final LocalDate ARBEIDSFORHOLD_TIL = I_DAG.plusMonths(2);
    private final InternArbeidsforholdRef ARBEIDSFORHOLD_ID = InternArbeidsforholdRef.nyRef();
    private final EksternArbeidsforholdRef EKSTERN_ARBEIDSFORHOLD_ID = EksternArbeidsforholdRef.ref("123");
    private final AktørId AKTØRID = AktørId.dummy();

    @Rule
    public final UnittestRepositoryRule repoRule = new UnittestRepositoryRule();

    private IAYRepositoryProvider repositoryProvider = new IAYRepositoryProvider(repoRule.getEntityManager());
    private final BehandlingRepository behandlingRepository = repositoryProvider.getBehandlingRepository();
    private FagsakRepository fagsakRepository = new FagsakRepository(repoRule.getEntityManager());
    private InntektArbeidYtelseTjeneste iayTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();
    private InntektsmeldingTjeneste inntektsmeldingTjeneste = new InntektsmeldingTjeneste(iayTjeneste);
    private ArbeidsforholdAdministrasjonTjeneste arbeidsforholdTjeneste;

    private Arbeidsgiver arbeidsgiver;

    @Before
    public void setUp() {
        VirksomhetEntitet virksomhet1 = lagVirksomhet();
        VirksomhetEntitet virksomhet2 = lagAndreVirksomheten();

        var virksomhetRepository = repositoryProvider.getVirksomhetRepository();
        virksomhetRepository.lagre(virksomhet1);
        virksomhetRepository.lagre(virksomhet2);

        arbeidsgiver = Arbeidsgiver.virksomhet(virksomhet1.getOrgnr());

        PersonIdentTjeneste tpsTjeneste = mock(PersonIdentTjeneste.class);
        final PersonIdent t = new PersonIdent("12345678901");
        when(tpsTjeneste.hentFnrForAktør(Mockito.any(AktørId.class))).thenReturn(t);
        var virksomhetTjeneste = new VirksomhetTjeneste(null, virksomhetRepository);

        VurderArbeidsforholdTjeneste vurderArbeidsforholdTjeneste = mock(VurderArbeidsforholdTjeneste.class);

        Arbeidsgiver arbeidsgiver = Arbeidsgiver.virksomhet(virksomhet2.getOrgnr());
        Set<InternArbeidsforholdRef> arbeidsforholdRefSet = new HashSet<>();
        arbeidsforholdRefSet.add(ARBEIDSFORHOLD_ID);
        Map<Arbeidsgiver, Set<InternArbeidsforholdRef>> arbeidsgiverSetMap = new HashMap<>();
        arbeidsgiverSetMap.put(arbeidsgiver, arbeidsforholdRefSet);
        when(vurderArbeidsforholdTjeneste.vurder(any(), any(), any(), Mockito.anyBoolean())).thenReturn(arbeidsgiverSetMap);

        ArbeidsgiverTjeneste arbeidsgiverTjeneste = new ArbeidsgiverTjenesteImpl(tpsTjeneste, virksomhetTjeneste);
        arbeidsforholdTjeneste = new ArbeidsforholdAdministrasjonTjeneste(vurderArbeidsforholdTjeneste,
            arbeidsgiverTjeneste, inntektsmeldingTjeneste, iayTjeneste);
    }

    @Test
    public void skal_utlede_arbeidsforholdwrapper() {
        // Arrange
        LocalDate mottattDato = I_DAG.minusDays(2);
        Behandling behandling = opprettBehandling();
        opprettOppgittOpptjening(behandling);
        opprettInntektArbeidYtelseAggregatForYrkesaktivitet(behandling, AKTØRID, DatoIntervallEntitet.fraOgMedTilOgMed(ARBEIDSFORHOLD_FRA, ARBEIDSFORHOLD_TIL), ARBEIDSFORHOLD_ID, ArbeidType.ORDINÆRT_ARBEIDSFORHOLD, BigDecimal.TEN);
        lagreInntektsmelding(mottattDato, behandling, ARBEIDSFORHOLD_ID, EKSTERN_ARBEIDSFORHOLD_ID);

        // Act
        Set<ArbeidsforholdWrapper> wrapperList = hentArbeidsforholdFerdigUtledet(behandling);

        // Assert
        assertThat(wrapperList).hasSize(1);
        ArbeidsforholdWrapper arbeidsforhold = wrapperList.iterator().next();

        assertThat(arbeidsforhold.getMottattDatoInntektsmelding()).isEqualTo(mottattDato);
        assertThat(arbeidsforhold.getBrukArbeidsforholdet()).isEqualTo(true);
        assertThat(arbeidsforhold.getFortsettBehandlingUtenInntektsmelding()).isEqualTo(false);
        assertThat(arbeidsforhold.getFomDato()).isEqualTo(ARBEIDSFORHOLD_FRA);
        assertThat(arbeidsforhold.getTomDato()).isEqualTo(ARBEIDSFORHOLD_TIL);
    }

    @Test
    public void skal_utlede_arbeidsforholdwrapper_im_uten_ya_ans() {
        // Arrange
        LocalDate mottattDato = I_DAG.minusDays(2);
        Behandling behandling = opprettBehandling();
        opprettInntektArbeidYtelseAggregatForYrkesaktivitetUtenAns(AKTØRID, ARBEIDSFORHOLD_ID, ArbeidType.ORDINÆRT_ARBEIDSFORHOLD, behandling);
        lagreInntektsmelding(mottattDato, behandling, ARBEIDSFORHOLD_ID, EKSTERN_ARBEIDSFORHOLD_ID);

        // Act
        Set<ArbeidsforholdWrapper> wrapperList = hentArbeidsforholdFerdigUtledet(behandling);

        // Assert
        assertThat(wrapperList).hasSize(1);
        ArbeidsforholdWrapper arbeidsforhold = wrapperList.iterator().next();

        assertThat(arbeidsforhold.getMottattDatoInntektsmelding()).isEqualTo(mottattDato);
        assertThat(arbeidsforhold.getKilde()).isEqualTo(ArbeidsforholdKilde.INNTEKTSMELDING);
        assertThat(arbeidsforhold.getBrukArbeidsforholdet()).isEqualTo(true);
        assertThat(arbeidsforhold.getFortsettBehandlingUtenInntektsmelding()).isEqualTo(false);
        assertThat(arbeidsforhold.getFomDato()).isEqualTo(LocalDate.now()); // null-verdi
    }

    @Test
    public void skal_utlede_to_arbeidsforholdwrapper() {
        // Arrange
        LocalDate mottattDato = I_DAG.minusDays(2);
        Behandling behandling = opprettBehandling();
        opprettOppgittOpptjening(behandling);
        opprettInntektArbeidYtelseAggregatForYrkesaktivitet(behandling, AKTØRID, DatoIntervallEntitet.fraOgMedTilOgMed(ARBEIDSFORHOLD_FRA, ARBEIDSFORHOLD_TIL), ARBEIDSFORHOLD_ID, ArbeidType.ORDINÆRT_ARBEIDSFORHOLD, BigDecimal.TEN);
        lagreInntektsmelding(mottattDato, behandling, null, null);

        // Act
        Set<ArbeidsforholdWrapper> wrapperList = hentArbeidsforholdFerdigUtledet(behandling);

        // Assert
        assertThat(wrapperList).hasSize(1);
        ArbeidsforholdWrapper arbeidsforhold = wrapperList.iterator().next();

        assertThat(arbeidsforhold.getMottattDatoInntektsmelding()).isEqualTo(mottattDato);
        assertThat(arbeidsforhold.getBrukArbeidsforholdet()).isEqualTo(true);
        assertThat(arbeidsforhold.getFortsettBehandlingUtenInntektsmelding()).isEqualTo(false);
        assertThat(arbeidsforhold.getFomDato()).isEqualTo(ARBEIDSFORHOLD_FRA);
        assertThat(arbeidsforhold.getTomDato()).isEqualTo(ARBEIDSFORHOLD_TIL);
    }

    private Set<ArbeidsforholdWrapper> hentArbeidsforholdFerdigUtledet(Behandling behandling) {
        BehandlingReferanse ref = lagRef(behandling);
        var iayGrunnlag = iayTjeneste.hentGrunnlag(ref.getBehandlingId());
        return arbeidsforholdTjeneste.hentArbeidsforholdFerdigUtledet(ref, iayGrunnlag, null, new UtledArbeidsforholdParametere(true));
    }

    @Test
    public void skal_utlede_to_arbeidsforholdwrapper_max_prosent() {
        // Arrange
        LocalDate mottattDato = I_DAG.minusDays(2);

        Behandling behandling = opprettBehandling();
        opprettOppgittOpptjening(behandling);
        opprettInntektArbeidYtelseAggregatForMultiYrkesaktivitet(AKTØRID, ARBEIDSFORHOLD_ID, ArbeidType.ORDINÆRT_ARBEIDSFORHOLD, BigDecimal.ONE, behandling);
        lagreInntektsmelding(mottattDato, behandling, null, null);

        // Act
        Set<ArbeidsforholdWrapper> wrapperList = hentArbeidsforholdFerdigUtledet(behandling);

        // Assert
        assertThat(wrapperList).hasSize(1);
        ArbeidsforholdWrapper arbeidsforhold = wrapperList.iterator().next();

        assertThat(arbeidsforhold.getMottattDatoInntektsmelding()).isEqualTo(mottattDato);
        assertThat(arbeidsforhold.getBrukArbeidsforholdet()).isEqualTo(true);
        assertThat(arbeidsforhold.getFortsettBehandlingUtenInntektsmelding()).isEqualTo(false);
        assertThat(arbeidsforhold.getFomDato()).isEqualTo(ARBEIDSFORHOLD_FRA);
        assertThat(arbeidsforhold.getTomDato()).isEqualTo(ARBEIDSFORHOLD_TIL);
        assertThat(arbeidsforhold.getStillingsprosent()).isEqualTo(BigDecimal.ONE);
    }

    @Test
    public void skal_utlede_to_arbeidsforholdwrapper_uten_im() {
        // Arrange
        Behandling behandling = opprettBehandling();
        opprettOppgittOpptjening(behandling);
        opprettInntektArbeidYtelseAggregatForMultiYrkesaktivitet(AKTØRID, ARBEIDSFORHOLD_ID, ArbeidType.ORDINÆRT_ARBEIDSFORHOLD, BigDecimal.ONE, behandling);

        // Act
        Set<ArbeidsforholdWrapper> wrapperList = hentArbeidsforholdFerdigUtledet(behandling);

        // Assert
        assertThat(wrapperList).hasSize(1);
        ArbeidsforholdWrapper arbeidsforhold = wrapperList.iterator().next();

        assertThat(arbeidsforhold.getBrukArbeidsforholdet()).isEqualTo(true);
        assertThat(arbeidsforhold.getFortsettBehandlingUtenInntektsmelding()).isEqualTo(false);
        assertThat(arbeidsforhold.getFomDato()).isEqualTo(ARBEIDSFORHOLD_FRA);
        assertThat(arbeidsforhold.getTomDato()).isEqualTo(ARBEIDSFORHOLD_TIL);
        assertThat(arbeidsforhold.getKilde()).isEqualTo(ArbeidsforholdKilde.AAREGISTERET);
        assertThat(arbeidsforhold.getStillingsprosent()).isEqualTo(BigDecimal.ONE);
    }

    @Test
    public void skal_utlede_arbeidsforholdwrapper_etter_overstyring() {
        // Arrange
        LocalDate mottattDato = I_DAG.minusDays(2);
        Behandling behandling = opprettBehandling();
        opprettOppgittOpptjening(behandling);
        opprettInntektArbeidYtelseAggregatForYrkesaktivitet(behandling, AKTØRID, DatoIntervallEntitet.fraOgMedTilOgMed(ARBEIDSFORHOLD_FRA, ARBEIDSFORHOLD_TIL), ARBEIDSFORHOLD_ID, ArbeidType.ORDINÆRT_ARBEIDSFORHOLD, BigDecimal.TEN);
        lagreInntektsmelding(mottattDato, behandling, ARBEIDSFORHOLD_ID, EKSTERN_ARBEIDSFORHOLD_ID);

        // Act
        ArbeidsforholdInformasjonBuilder informasjonBuilder = arbeidsforholdTjeneste.opprettBuilderFor(behandling.getId());
        ArbeidsforholdOverstyringBuilder overstyringBuilder = informasjonBuilder.getOverstyringBuilderFor(arbeidsgiver, ARBEIDSFORHOLD_ID);
        overstyringBuilder.medHandling(ArbeidsforholdHandlingType.BRUK);
        informasjonBuilder.leggTil(overstyringBuilder);
        arbeidsforholdTjeneste.lagre(behandling.getId(), behandling.getAktørId(), informasjonBuilder);
        Set<ArbeidsforholdWrapper> wrapperList = hentArbeidsforholdFerdigUtledet(behandling);

        // Assert
        assertThat(wrapperList).hasSize(1);
        ArbeidsforholdWrapper arbeidsforhold = wrapperList.iterator().next();
        assertThat(arbeidsforhold.getMottattDatoInntektsmelding()).isEqualTo(mottattDato);
        assertThat(arbeidsforhold.getBrukArbeidsforholdet()).isEqualTo(true);
        assertThat(arbeidsforhold.getFortsettBehandlingUtenInntektsmelding()).isEqualTo(false);
        assertThat(arbeidsforhold.getFomDato()).isEqualTo(ARBEIDSFORHOLD_FRA);
        assertThat(arbeidsforhold.getTomDato()).isEqualTo(ARBEIDSFORHOLD_TIL);
    }

    @Test
    public void skal_utlede_arbeidsforholdwrapper_etter_overstyring_uten_arbeidsforhold() {
        // Arrange
        LocalDate mottattDato = I_DAG.minusDays(2);
        Behandling behandling = opprettBehandling();

        // Act
        ArbeidsforholdInformasjonBuilder informasjonBuilder = arbeidsforholdTjeneste.opprettBuilderFor(behandling.getId());
        ArbeidsforholdOverstyringBuilder overstyringBuilder = informasjonBuilder.getOverstyringBuilderFor(arbeidsgiver, ARBEIDSFORHOLD_ID);
        overstyringBuilder.medHandling(ArbeidsforholdHandlingType.LAGT_TIL_AV_SAKSBEHANDLER);
        overstyringBuilder.medAngittStillingsprosent(Stillingsprosent.HUNDRED);
        overstyringBuilder.leggTilOverstyrtPeriode(mottattDato.minusYears(1L), Tid.TIDENES_ENDE);
        informasjonBuilder.leggTil(overstyringBuilder);
        arbeidsforholdTjeneste.lagre(behandling.getId(), behandling.getAktørId(), informasjonBuilder);
        Set<ArbeidsforholdWrapper> wrapperList = hentArbeidsforholdFerdigUtledet(behandling);

        // Assert
        assertThat(wrapperList).hasSize(1);
        ArbeidsforholdWrapper arbeidsforhold = wrapperList.iterator().next();
        assertThat(arbeidsforhold.getBrukArbeidsforholdet()).isEqualTo(true);
        assertThat(arbeidsforhold.getLagtTilAvSaksbehandler()).isEqualTo(true);
        assertThat(arbeidsforhold.getFortsettBehandlingUtenInntektsmelding()).isEqualTo(true);
        assertThat(arbeidsforhold.getFomDato()).isEqualTo(mottattDato.minusYears(1L));
        assertThat(arbeidsforhold.getTomDato()).isEqualTo(Tid.TIDENES_ENDE);
    }

    @Test
    public void test_hentArbeidsforholdFerdigUtledet_med_aksjonspunkt() {
        // Arrange
        Behandling behandling = opprettBehandling();
        opprettOppgittOpptjening(behandling);
        opprettInntektArbeidYtelseAggregatForMultiYrkesaktivitet(AKTØRID, ARBEIDSFORHOLD_ID, ArbeidType.ORDINÆRT_ARBEIDSFORHOLD, BigDecimal.ONE, behandling);
        opprettAksjonspunkt(behandling, AksjonspunktDefinisjon.VURDER_ARBEIDSFORHOLD, LocalDateTime.now());

        // Act
        Set<ArbeidsforholdWrapper> wrapperList = hentArbeidsforholdFerdigUtledet(behandling);

        // Assert
        assertThat(wrapperList).hasSize(1);
        ArbeidsforholdWrapper arbeidsforhold = wrapperList.iterator().next();

        assertThat(arbeidsforhold.getBrukArbeidsforholdet()).isEqualTo(true);
        assertThat(arbeidsforhold.getFortsettBehandlingUtenInntektsmelding()).isEqualTo(false);
        assertThat(arbeidsforhold.getFomDato()).isEqualTo(ARBEIDSFORHOLD_FRA);
        assertThat(arbeidsforhold.getTomDato()).isEqualTo(ARBEIDSFORHOLD_TIL);
        assertThat(arbeidsforhold.getKilde()).isEqualTo(ArbeidsforholdKilde.AAREGISTERET);
        assertThat(arbeidsforhold.getStillingsprosent()).isEqualTo(BigDecimal.ONE);
    }

    @Test
    public void skal_bruke_datoer_for_yrkesaktivitet_som_kommer_etter_skjæringstidspunktet_uten_inntektsmelding() {
        // Arrange
        Behandling behandling = opprettBehandling();
        DatoIntervallEntitet periodeFør = DatoIntervallEntitet.fraOgMedTilOgMed(I_DAG.minusYears(1), I_DAG.minusDays(1));
        DatoIntervallEntitet periodeEtter = DatoIntervallEntitet.fraOgMedTilOgMed(I_DAG.plusDays(1), I_DAG.plusYears(1));
        InntektArbeidYtelseAggregatBuilder builder = InntektArbeidYtelseAggregatBuilder
            .oppdatere(Optional.empty(), VersjonType.REGISTER);
        InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder aktørArbeidBuilder = builder.getAktørArbeidBuilder(AKTØRID);
        final var informasjonBuilder = ArbeidsforholdInformasjonBuilder.builder(Optional.empty());
        leggTilYrkesaktivitet(aktørArbeidBuilder, informasjonBuilder, InternArbeidsforholdRef.nyRef(), ArbeidType.FORENKLET_OPPGJØRSORDNING, BigDecimal.valueOf(100), periodeFør, periodeFør);
        leggTilYrkesaktivitet(aktørArbeidBuilder, informasjonBuilder, InternArbeidsforholdRef.nyRef(), ArbeidType.ORDINÆRT_ARBEIDSFORHOLD, BigDecimal.valueOf(100), periodeEtter, periodeEtter);
        builder.leggTilAktørArbeid(aktørArbeidBuilder);
        iayTjeneste.lagreIayAggregat(behandling.getId(), builder);
        iayTjeneste.lagreArbeidsforhold(behandling.getId(), behandling.getAktørId(), informasjonBuilder);
        // Act
        Set<ArbeidsforholdWrapper> wrapperList = hentArbeidsforholdFerdigUtledet(behandling);
        // Assert
        assertThat(wrapperList).hasSize(1);
        assertThat(wrapperList.iterator().next().getFomDato()).isEqualTo(I_DAG.plusDays(1));
        assertThat(wrapperList.iterator().next().getTomDato()).isEqualTo(I_DAG.plusYears(1));
    }

    @Test
    public void skal_bruke_datoer_for_yrkesaktivitet_som_overlapper_skjæringstidspunktet_uten_inntektsmelding() {
        // Arrange
        Behandling behandling = opprettBehandling();
        DatoIntervallEntitet periodeFør = DatoIntervallEntitet.fraOgMedTilOgMed(I_DAG.minusYears(1), I_DAG);
        DatoIntervallEntitet periodeEtter = DatoIntervallEntitet.fraOgMedTilOgMed(I_DAG.plusDays(1), I_DAG.plusYears(1));
        InntektArbeidYtelseAggregatBuilder builder = InntektArbeidYtelseAggregatBuilder .oppdatere(Optional.empty(), VersjonType.REGISTER);

        InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder aktørArbeidBuilder = builder.getAktørArbeidBuilder(AKTØRID);
        final var informasjonBuilder = ArbeidsforholdInformasjonBuilder.builder(Optional.empty());
        leggTilYrkesaktivitet(aktørArbeidBuilder, informasjonBuilder, ARBEIDSFORHOLD_ID, ArbeidType.FORENKLET_OPPGJØRSORDNING, BigDecimal.valueOf(100), periodeFør, periodeFør);
        leggTilYrkesaktivitet(aktørArbeidBuilder, informasjonBuilder, ARBEIDSFORHOLD_ID, ArbeidType.ORDINÆRT_ARBEIDSFORHOLD, BigDecimal.valueOf(100), periodeEtter, periodeEtter);
        builder.leggTilAktørArbeid(aktørArbeidBuilder);

        iayTjeneste.lagreIayAggregat(behandling.getId(), builder);
        iayTjeneste.lagreArbeidsforhold(behandling.getId(), behandling.getAktørId(), informasjonBuilder);

        // Act
        Set<ArbeidsforholdWrapper> wrapperList = hentArbeidsforholdFerdigUtledet(behandling);

        // Assert
        assertThat(wrapperList).hasSize(1);
        assertThat(wrapperList.iterator().next().getFomDato()).isEqualTo(I_DAG.minusYears(1));
        assertThat(wrapperList.iterator().next().getTomDato()).isEqualTo(I_DAG);
    }

    @Test
    public void skal_bruke_datoer_for_yrkesaktivitet_som_kommer_etter_skjæringstidspunktet_med_inntektsmelding() {
        // Arrange
        Behandling behandling = opprettBehandling();
        lagreInntektsmelding(I_DAG.minusDays(3), behandling, ARBEIDSFORHOLD_ID, EKSTERN_ARBEIDSFORHOLD_ID);
        DatoIntervallEntitet periodeFør = DatoIntervallEntitet.fraOgMedTilOgMed(I_DAG.minusYears(1), I_DAG.minusDays(1));
        DatoIntervallEntitet periodeEtter = DatoIntervallEntitet.fraOgMedTilOgMed(I_DAG.plusDays(1), I_DAG.plusYears(1));
        InntektArbeidYtelseAggregatBuilder builder = InntektArbeidYtelseAggregatBuilder
            .oppdatere(Optional.empty(), VersjonType.REGISTER);
        InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder aktørArbeidBuilder = builder.getAktørArbeidBuilder(AKTØRID);
        final var informasjonBuilder = ArbeidsforholdInformasjonBuilder.builder(Optional.empty());
        leggTilYrkesaktivitet(aktørArbeidBuilder, informasjonBuilder, InternArbeidsforholdRef.nyRef(), ArbeidType.FORENKLET_OPPGJØRSORDNING, BigDecimal.valueOf(100), periodeFør, periodeFør);
        leggTilYrkesaktivitet(aktørArbeidBuilder, informasjonBuilder, InternArbeidsforholdRef.nyRef(), ArbeidType.ORDINÆRT_ARBEIDSFORHOLD, BigDecimal.valueOf(100), periodeEtter, periodeEtter);
        builder.leggTilAktørArbeid(aktørArbeidBuilder);
        iayTjeneste.lagreIayAggregat(behandling.getId(), builder);
        iayTjeneste.lagreArbeidsforhold(behandling.getId(), behandling.getAktørId(), informasjonBuilder);
        // Act
        Set<ArbeidsforholdWrapper> wrapperList = hentArbeidsforholdFerdigUtledet(behandling);
        // Assert
        assertThat(wrapperList).hasSize(1);
        assertThat(wrapperList.iterator().next().getFomDato()).isEqualTo(I_DAG.plusDays(1));
        assertThat(wrapperList.iterator().next().getTomDato()).isEqualTo(I_DAG.plusYears(1));
    }

    @Test
    public void skal_bruke_datoer_for_yrkesaktivitet_som_overlapper_skjæringstidspunktet_med_inntektsmelding() {
        // Arrange
        Behandling behandling = opprettBehandling();
        lagreInntektsmelding(I_DAG.minusDays(3), behandling, ARBEIDSFORHOLD_ID, EKSTERN_ARBEIDSFORHOLD_ID);
        DatoIntervallEntitet periodeFør = DatoIntervallEntitet.fraOgMedTilOgMed(I_DAG.minusYears(1), I_DAG);
        DatoIntervallEntitet periodeEtter = DatoIntervallEntitet.fraOgMedTilOgMed(I_DAG.plusDays(1), I_DAG.plusYears(1));
        InntektArbeidYtelseAggregatBuilder builder = InntektArbeidYtelseAggregatBuilder
            .oppdatere(Optional.empty(), VersjonType.REGISTER);
        InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder aktørArbeidBuilder = builder.getAktørArbeidBuilder(AKTØRID);
        final var informasjonBuilder = ArbeidsforholdInformasjonBuilder.builder(Optional.empty());
        leggTilYrkesaktivitet(aktørArbeidBuilder, informasjonBuilder, ARBEIDSFORHOLD_ID, ArbeidType.FORENKLET_OPPGJØRSORDNING, BigDecimal.valueOf(100), periodeFør, periodeFør);
        leggTilYrkesaktivitet(aktørArbeidBuilder, informasjonBuilder, ARBEIDSFORHOLD_ID, ArbeidType.ORDINÆRT_ARBEIDSFORHOLD, BigDecimal.valueOf(100), periodeEtter, periodeEtter);
        builder.leggTilAktørArbeid(aktørArbeidBuilder);
        iayTjeneste.lagreIayAggregat(behandling.getId(), builder);
        iayTjeneste.lagreArbeidsforhold(behandling.getId(), behandling.getAktørId(), informasjonBuilder);
        // Act
        Set<ArbeidsforholdWrapper> wrapperList = hentArbeidsforholdFerdigUtledet(behandling);
        // Assert
        assertThat(wrapperList).hasSize(1);
        assertThat(wrapperList.iterator().next().getFomDato()).isEqualTo(I_DAG.minusYears(1));
        assertThat(wrapperList.iterator().next().getTomDato()).isEqualTo(I_DAG);
    }

    private BehandlingReferanse lagRef(Behandling behandling) {
        Skjæringstidspunkt skjæringstidspunkt = Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(I_DAG).build();
        return BehandlingReferanse.fra(behandling, skjæringstidspunkt);
    }

    private void lagreInntektsmelding(LocalDate mottattDato, Behandling behandling, InternArbeidsforholdRef arbeidsforholdId, EksternArbeidsforholdRef eksternArbeidsforholdRef) {
        JournalpostId journalPostId = new JournalpostId("123");
        var inntektsmeldingBuilder = InntektsmeldingBuilder.builder()
        .medStartDatoPermisjon(I_DAG)
        .medArbeidsgiver(arbeidsgiver)
        .medBeløp(BigDecimal.TEN)
        .medNærRelasjon(false)
        .medArbeidsforholdId(arbeidsforholdId)
        .medArbeidsforholdId(eksternArbeidsforholdRef)
        .medMottattDato(mottattDato)
        .medInnsendingstidspunkt(LocalDateTime.now()).medJournalpostId(journalPostId);

        inntektsmeldingTjeneste.lagreInntektsmeldinger(behandling.getFagsak().getSaksnummer(), behandling.getId(), List.of(inntektsmeldingBuilder));

    }

    private void opprettOppgittOpptjening(Behandling behandling) {
        DatoIntervallEntitet periode = DatoIntervallEntitet.fraOgMedTilOgMed(I_DAG.minusMonths(2), I_DAG.plusMonths(1));
        OppgittOpptjeningBuilder oppgitt = OppgittOpptjeningBuilder.ny();
        oppgitt.leggTilAnnenAktivitet(new OppgittAnnenAktivitet(periode, ArbeidType.MILITÆR_ELLER_SIVILTJENESTE));
        iayTjeneste.lagreOppgittOpptjening(behandling.getId(), oppgitt);
    }

    private void opprettInntektArbeidYtelseAggregatForYrkesaktivitet(Behandling behandling, AktørId aktørId,
                                                                     DatoIntervallEntitet periode,
                                                                     InternArbeidsforholdRef arbeidsforhold,
                                                                     ArbeidType type, BigDecimal prosentsats) {

        InntektArbeidYtelseAggregatBuilder builder = InntektArbeidYtelseAggregatBuilder
            .oppdatere(Optional.empty(), VersjonType.REGISTER);

        InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder aktørArbeidBuilder = builder.getAktørArbeidBuilder(aktørId);
        final var informasjonBuilder = ArbeidsforholdInformasjonBuilder.builder(Optional.empty());

        leggTilYrkesaktivitet(aktørArbeidBuilder, informasjonBuilder, arbeidsforhold, type, prosentsats, periode, periode);
        builder.leggTilAktørArbeid(aktørArbeidBuilder);

        iayTjeneste.lagreIayAggregat(behandling.getId(), builder);
        iayTjeneste.lagreArbeidsforhold(behandling.getId(), behandling.getAktørId(), informasjonBuilder);
    }

    private void opprettInntektArbeidYtelseAggregatForYrkesaktivitetUtenAns(AktørId aktørId, InternArbeidsforholdRef arbeidsforhold,
                                                                            ArbeidType type, Behandling behandling) {

        InntektArbeidYtelseAggregatBuilder builder = InntektArbeidYtelseAggregatBuilder
            .oppdatere(Optional.empty(), VersjonType.REGISTER);

        InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder aktørArbeidBuilder = builder.getAktørArbeidBuilder(aktørId);

        leggTilYrkesaktivitetUtenAnsperiode(aktørArbeidBuilder, arbeidsforhold, type);
        builder.leggTilAktørArbeid(aktørArbeidBuilder);

        iayTjeneste.lagreIayAggregat(behandling.getId(), builder);
    }


    private void opprettInntektArbeidYtelseAggregatForMultiYrkesaktivitet(AktørId aktørId, InternArbeidsforholdRef arbeidsforhold,
                                                                          ArbeidType type, BigDecimal prosentsats,
                                                                          Behandling behandling) {
        DatoIntervallEntitet periodeFørst = DatoIntervallEntitet.fraOgMedTilOgMed(ARBEIDSFORHOLD_FRA.minusMonths(3), ARBEIDSFORHOLD_FRA.minusMonths(1));
        DatoIntervallEntitet periodeFørstAA = DatoIntervallEntitet.fraOgMedTilOgMed(I_DAG.minusMonths(6), Tid.TIDENES_ENDE);
        DatoIntervallEntitet periode = DatoIntervallEntitet.fraOgMedTilOgMed(ARBEIDSFORHOLD_FRA, ARBEIDSFORHOLD_TIL);
        DatoIntervallEntitet periodeAA = DatoIntervallEntitet.fraOgMedTilOgMed(ARBEIDSFORHOLD_FRA, Tid.TIDENES_ENDE);

        InntektArbeidYtelseAggregatBuilder builder = InntektArbeidYtelseAggregatBuilder
            .oppdatere(Optional.empty(), VersjonType.REGISTER);

        InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder aktørArbeidBuilder = builder.getAktørArbeidBuilder(aktørId);
        final var informasjonBuilder = ArbeidsforholdInformasjonBuilder.builder(Optional.empty());
        leggTilYrkesaktivitet(aktørArbeidBuilder, informasjonBuilder, arbeidsforhold, type, BigDecimal.TEN, periodeFørst, periodeFørstAA);
        leggTilYrkesaktivitet(aktørArbeidBuilder, informasjonBuilder, arbeidsforhold, type, prosentsats, periode, periodeAA);
        builder.leggTilAktørArbeid(aktørArbeidBuilder);

        iayTjeneste.lagreIayAggregat(behandling.getId(), builder);
        iayTjeneste.lagreArbeidsforhold(behandling.getId(), behandling.getAktørId(), informasjonBuilder);
    }

    private InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder leggTilYrkesaktivitet(InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder builder,
                                                                                        ArbeidsforholdInformasjonBuilder informasjonBuilder,
                                                                                        InternArbeidsforholdRef ref, ArbeidType type, BigDecimal prosentsats,
                                                                                        DatoIntervallEntitet periodeYA, DatoIntervallEntitet periodeAA) {
        YrkesaktivitetBuilder yrkesaktivitetBuilder = builder.getYrkesaktivitetBuilderForNøkkelAvType(new Opptjeningsnøkkel(ref, arbeidsgiver.getIdentifikator(), null),
            ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);
        AktivitetsAvtaleBuilder aktivitetsAvtaleBuilder = yrkesaktivitetBuilder.getAktivitetsAvtaleBuilder(periodeAA, false);
        PermisjonBuilder permisjonBuilder = yrkesaktivitetBuilder.getPermisjonBuilder();
        informasjonBuilder.leggTil(arbeidsgiver, ref, EksternArbeidsforholdRef.ref(ref.getReferanse()));

        AktivitetsAvtaleBuilder aktivitetsAvtale = aktivitetsAvtaleBuilder
            .medPeriode(periodeAA)
            .medProsentsats(prosentsats)
            .medBeskrivelse("Ser greit ut");
        AktivitetsAvtaleBuilder ansettelsesPeriode = yrkesaktivitetBuilder.getAktivitetsAvtaleBuilder(periodeYA, true);

        Permisjon permisjon = permisjonBuilder
            .medPermisjonsbeskrivelseType(PermisjonsbeskrivelseType.UTDANNINGSPERMISJON)
            .medPeriode(periodeYA.getFomDato(), periodeYA.getTomDato())
            .medProsentsats(BigDecimal.valueOf(100))
            .build();

        yrkesaktivitetBuilder
            .medArbeidType(type)
            .medArbeidsgiver(arbeidsgiver)
            .medArbeidsforholdId(ref)
            .leggTilPermisjon(permisjon)
            .leggTilAktivitetsAvtale(aktivitetsAvtale)
            .leggTilAktivitetsAvtale(ansettelsesPeriode);

        return builder.leggTilYrkesaktivitet(yrkesaktivitetBuilder);
    }

    private InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder leggTilYrkesaktivitetUtenAnsperiode(InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder builder,
                                                                                                       InternArbeidsforholdRef arbeidsforhold, ArbeidType type) {
        YrkesaktivitetBuilder yrkesaktivitetBuilder = builder.getYrkesaktivitetBuilderForNøkkelAvType(new Opptjeningsnøkkel(arbeidsforhold, arbeidsgiver),
            ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);

        yrkesaktivitetBuilder
            .medArbeidType(type)
            .medArbeidsgiver(arbeidsgiver)
            .medArbeidsforholdId(ARBEIDSFORHOLD_ID);

        return builder.leggTilYrkesaktivitet(yrkesaktivitetBuilder);
    }

    private VirksomhetEntitet lagVirksomhet() {
        return new VirksomhetEntitet.Builder()
        .medOrgnr(ORG1)
        .medNavn("Virksomheten")
        .medRegistrert(I_DAG.minusYears(2L))
        .medOppstart(I_DAG.minusYears(1L))
        .oppdatertOpplysningerNå()
        .build();
    }

    private VirksomhetEntitet lagAndreVirksomheten() {
        return new VirksomhetEntitet.Builder()
        .medOrgnr(ORG2)
        .medNavn("OrgA")
        .medRegistrert(I_DAG.minusYears(2L))
        .medOppstart(I_DAG.minusYears(1L))
        .oppdatertOpplysningerNå()
        .build();
    }

    private Behandling opprettBehandling() {
        final Personinfo personinfo = new Personinfo.Builder()
            .medNavn("Navn navnesen")
            .medAktørId(AKTØRID)
            .medFødselsdato(I_DAG.minusYears(20))
            .medLandkode(Landkoder.NOR)
            .medKjønn(NavBrukerKjønn.KVINNE)
            .medPersonIdent(new PersonIdent("12312312312"))
            .medForetrukketSpråk(Språkkode.nb)
            .build();
        final Fagsak fagsak = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, personinfo.getAktørId());
        fagsakRepository.opprettNy(fagsak);
        final Behandling.Builder builder = Behandling.forFørstegangssøknad(fagsak);
        final Behandling behandling = builder.build();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
        return behandling;
    }

    private Aksjonspunkt opprettAksjonspunkt(Behandling behandling,
                                             AksjonspunktDefinisjon aksjonspunktDefinisjon,
                                             LocalDateTime frist) {

        var aksjonspunktTestSupport = new AksjonspunktTestSupport();
        Aksjonspunkt aksjonspunkt = aksjonspunktTestSupport.leggTilAksjonspunkt(behandling, aksjonspunktDefinisjon);
        aksjonspunktTestSupport.setFrist(aksjonspunkt, frist, Venteårsak.UDEFINERT);
        return aksjonspunkt;
    }
}
