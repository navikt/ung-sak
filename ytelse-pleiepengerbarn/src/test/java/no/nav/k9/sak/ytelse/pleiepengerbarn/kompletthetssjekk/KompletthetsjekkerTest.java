package no.nav.k9.sak.ytelse.pleiepengerbarn.kompletthetssjekk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.Period;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import no.nav.k9.kodeverk.dokument.DokumentTypeId;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandling.Skjæringstidspunkt;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.k9.sak.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.k9.sak.behandlingslager.behandling.søknad.SøknadVedleggEntitet;
import no.nav.k9.sak.db.util.JpaExtension;
import no.nav.k9.sak.dokument.arkiv.DokumentArkivTjeneste;
import no.nav.k9.sak.dokument.bestill.DokumentBehandlingTjeneste;
import no.nav.k9.sak.dokument.bestill.DokumentBestillerApplikasjonTjeneste;
import no.nav.k9.sak.domene.arbeidsforhold.InntektsmeldingTjeneste;
import no.nav.k9.sak.domene.arbeidsforhold.impl.InntektsmeldingRegisterTjeneste;
import no.nav.k9.sak.kompletthet.KompletthetResultat;
import no.nav.k9.sak.mottak.inntektsmelding.DefaultKompletthetssjekkerInntektsmelding;
import no.nav.k9.sak.mottak.inntektsmelding.KompletthetssjekkerInntektsmelding;
import no.nav.k9.sak.mottak.kompletthetssjekk.KompletthetsjekkerFelles;
import no.nav.k9.sak.mottak.kompletthetssjekk.KompletthetssjekkerSøknad;
import no.nav.k9.sak.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.k9.sak.test.util.UnitTestLookupInstanceImpl;
import no.nav.k9.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;

@ExtendWith(JpaExtension.class)
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class KompletthetsjekkerTest {

    private static final LocalDate STARTDATO = LocalDate.now().plusWeeks(1);
    private static final String KODE_INNTEKTSMELDING = DokumentTypeId.INNTEKTSMELDING.getOffisiellKode();

    private BehandlingRepositoryProvider repositoryProvider;
    private SøknadRepository søknadRepository;

    private KompletthetssjekkerTestUtil testUtil;

    @Mock
    private DokumentArkivTjeneste dokumentArkivTjeneste;
    @Mock
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    @Mock
    private DokumentBestillerApplikasjonTjeneste dokumentBestillerApplikasjonTjenesteMock;
    @Mock
    private DokumentBehandlingTjeneste dokumentBehandlingTjenesteMock;
    @Mock
    private InntektsmeldingRegisterTjeneste inntektsmeldingArkivTjeneste;

    @Mock
    private InntektsmeldingTjeneste inntektsmeldingTjeneste;

    private KompletthetssjekkerSøknad kompletthetssjekkerSøknad;
    private KompletthetssjekkerInntektsmelding kompletthetssjekkerInntektsmelding;
    private KompletthetsjekkerFelles kompletthetsjekkerFelles;
    private PsbKompletthetsjekker psbKompletthetsjekker;
    private Skjæringstidspunkt skjæringstidspunkt = Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(STARTDATO).build();
    private EntityManager entityManager;

    @BeforeEach
    public void before() {

        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        søknadRepository = repositoryProvider.getSøknadRepository();
        testUtil = new KompletthetssjekkerTestUtil(repositoryProvider);

        when(skjæringstidspunktTjeneste.getSkjæringstidspunkter(Mockito.anyLong())).thenReturn(skjæringstidspunkt);
        when(inntektsmeldingArkivTjeneste.utledManglendeInntektsmeldingerFraAAreg(any(), anyBoolean())).thenReturn(new HashMap<>());
        when(inntektsmeldingArkivTjeneste.utledManglendeInntektsmeldingerFraGrunnlag(any(), anyBoolean())).thenReturn(new HashMap<>());

        kompletthetssjekkerSøknad = new KompletthetssjekkerSøknad(søknadRepository, Period.parse("P4W"));
        kompletthetssjekkerInntektsmelding = new DefaultKompletthetssjekkerInntektsmelding(inntektsmeldingArkivTjeneste);
        kompletthetsjekkerFelles = new KompletthetsjekkerFelles(repositoryProvider, dokumentBestillerApplikasjonTjenesteMock);

        psbKompletthetsjekker = new PsbKompletthetsjekker(
            new UnitTestLookupInstanceImpl<>(kompletthetssjekkerSøknad),
            new UnitTestLookupInstanceImpl<>(kompletthetssjekkerInntektsmelding),
            inntektsmeldingTjeneste,
            kompletthetsjekkerFelles,
            søknadRepository);
    }

    public void setEntityManager(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Test
    public void skal_finne_at_kompletthet_er_oppfylt() {
        // Arrange
        Behandling behandling = TestScenarioBuilder.builderMedSøknad().lagre(repositoryProvider);

        // Act
        KompletthetResultat kompletthetResultat = psbKompletthetsjekker.vurderForsendelseKomplett(lagRef(behandling));

        // Assert
        assertThat(kompletthetResultat.erOppfylt()).isTrue();
        assertThat(kompletthetResultat.getVentefrist()).isNull();
    }

    @Test
    public void skal_etterlyse_mer_enn_3ukerfør() {
        // Arrange
        LocalDate stp = LocalDate.now().plusDays(2).plusWeeks(3);
        Behandling behandling = TestScenarioBuilder.builderMedSøknad().lagre(repositoryProvider);
        mockManglendeInntektsmeldingGrunnlag();
        testUtil.byggOgLagreFørstegangsSøknadMedMottattdato(behandling, LocalDate.now().minusWeeks(1));
        when(inntektsmeldingTjeneste.hentInntektsmeldinger(any(), any())).thenReturn(Collections.emptyList());

        // Act
        KompletthetResultat kompletthetResultat = psbKompletthetsjekker.vurderEtterlysningInntektsmelding(lagRef(behandling, stp));

        // Assert
        assertThat(kompletthetResultat.erOppfylt()).isFalse();
        assertThat(kompletthetResultat.getVentefrist().toLocalDate()).isEqualTo(LocalDate.now().plusWeeks(3));
        verify(dokumentBestillerApplikasjonTjenesteMock, times(1)).bestillDokument(any(), any());

        // Act 2
        stp = LocalDate.now().plusWeeks(3);
        KompletthetResultat kompletthetResultat2 = psbKompletthetsjekker.vurderEtterlysningInntektsmelding(lagRef(behandling, stp));

        // Assert
        assertThat(kompletthetResultat2.erOppfylt()).isFalse();
        assertThat(kompletthetResultat2.getVentefrist().toLocalDate()).isEqualTo(stp);
        verify(dokumentBestillerApplikasjonTjenesteMock, times(2)).bestillDokument(any(), any());
    }

    @Test
    public void skal_sende_brev_når_inntektsmelding_mangler() {
        // Arrange
        Behandling behandling = TestScenarioBuilder.builderMedSøknad().lagre(repositoryProvider);
        mockManglendeInntektsmeldingGrunnlag();
        testUtil.byggOgLagreFørstegangsSøknadMedMottattdato(behandling, LocalDate.now().minusWeeks(1));
        when(inntektsmeldingTjeneste.hentInntektsmeldinger(any(), any())).thenReturn(Collections.emptyList());

        // Act
        KompletthetResultat kompletthetResultat = psbKompletthetsjekker.vurderEtterlysningInntektsmelding(lagRef(behandling, STARTDATO));

        // Assert
        assertThat(kompletthetResultat.erOppfylt()).isFalse();
        assertThat(kompletthetResultat.getVentefrist().toLocalDate()).isEqualTo(LocalDate.now().plusWeeks(3));
        verify(dokumentBestillerApplikasjonTjenesteMock, times(1)).bestillDokument(any(), any());
    }

    @Test
    public void skal_finne_at_kompletthet_er_oppfylt_når_vedlegg_til_søknad_finnes_i_joark() {
        // Arrange
        Behandling behandling = TestScenarioBuilder.builderMedSøknad().lagre(repositoryProvider);

        opprettSøknadMedPåkrevdVedlegg(behandling);

        // Act
        KompletthetResultat kompletthetResultat = psbKompletthetsjekker.vurderForsendelseKomplett(lagRef(behandling));

        // Assert
        assertThat(kompletthetResultat.erOppfylt()).isTrue();
        assertThat(kompletthetResultat.getVentefrist()).isNull();
    }

    private BehandlingReferanse lagRef(Behandling behandling) {
        var stp = Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(LocalDate.now()).build();
        return BehandlingReferanse.fra(behandling, stp);
    }

    private BehandlingReferanse lagRef(Behandling behandling, LocalDate stpDate) {
        var stp = Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(stpDate).build();
        return BehandlingReferanse.fra(behandling, stp);
    }

    private void opprettSøknadMedPåkrevdVedlegg(Behandling behandling) {
        SøknadEntitet hentSøknad = søknadRepository.hentSøknad(behandling);
        SøknadEntitet søknad = new SøknadEntitet.Builder(hentSøknad).leggTilVedlegg(
            new SøknadVedleggEntitet.Builder()
                .medSkjemanummer(KODE_INNTEKTSMELDING)
                .medErPåkrevdISøknadsdialog(true)
                .build())
            .build();
        søknadRepository.lagreOgFlush(behandling, søknad);
    }

    private void mockManglendeInntektsmeldingGrunnlag() {
        HashMap<Arbeidsgiver, Set<InternArbeidsforholdRef>> manglendeInntektsmeldinger = new HashMap<>();
        manglendeInntektsmeldinger.put(Arbeidsgiver.virksomhet("1"), new HashSet<>());
        when(inntektsmeldingArkivTjeneste.utledManglendeInntektsmeldingerFraGrunnlag(any(), anyBoolean())).thenReturn(manglendeInntektsmeldinger);
    }
}
