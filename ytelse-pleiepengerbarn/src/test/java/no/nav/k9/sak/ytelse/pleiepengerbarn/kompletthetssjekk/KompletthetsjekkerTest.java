package no.nav.k9.sak.ytelse.pleiepengerbarn.kompletthetssjekk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.Period;
import java.util.HashMap;
import java.util.List;
import java.util.TreeSet;

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
import no.nav.k9.sak.db.util.JpaExtension;
import no.nav.k9.sak.dokument.bestill.DokumentBestillerApplikasjonTjeneste;
import no.nav.k9.sak.domene.abakus.ArbeidsforholdTjeneste;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.arbeidsforhold.InntektsmeldingTjeneste;
import no.nav.k9.sak.domene.arbeidsforhold.impl.InntektsmeldingRegisterTjeneste;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.kompletthet.KompletthetResultat;
import no.nav.k9.sak.mottak.inntektsmelding.DefaultKompletthetssjekkerInntektsmelding;
import no.nav.k9.sak.mottak.inntektsmelding.KompletthetssjekkerInntektsmelding;
import no.nav.k9.sak.mottak.kompletthetssjekk.KompletthetsjekkerFelles;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.k9.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.k9.sak.ytelse.pleiepengerbarn.beregningsgrunnlag.PSBInntektsmeldingerRelevantForBeregning;

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
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    @Mock
    private DokumentBestillerApplikasjonTjeneste dokumentBestillerApplikasjonTjenesteMock;
    @Mock
    private InntektsmeldingRegisterTjeneste inntektsmeldingArkivTjeneste;
    @Mock
    private VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste;
    @Mock
    private ArbeidsforholdTjeneste arbeidsforholdTjeneste;
    @Mock
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;

    @Mock
    private InntektsmeldingTjeneste inntektsmeldingTjeneste;

    private KompletthetssjekkerSøknad kompletthetssjekkerSøknad;
    private KompletthetssjekkerInntektsmelding kompletthetssjekkerInntektsmelding;
    private KompletthetsjekkerFelles kompletthetsjekkerFelles;
    private PSBKompletthetsjekker psbKompletthetsjekker;
    private Skjæringstidspunkt skjæringstidspunkt = Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(STARTDATO).build();
    private EntityManager entityManager;

    @BeforeEach
    public void before() {

        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        søknadRepository = repositoryProvider.getSøknadRepository();
        testUtil = new KompletthetssjekkerTestUtil(repositoryProvider);

        when(skjæringstidspunktTjeneste.getSkjæringstidspunkter(Mockito.anyLong())).thenReturn(skjæringstidspunkt);
        when(inntektsmeldingArkivTjeneste.utledManglendeInntektsmeldingerFraAAreg(any(), anyBoolean(), any())).thenReturn(new HashMap<>());
        when(inntektsmeldingArkivTjeneste.utledManglendeInntektsmeldingerFraGrunnlag(any(), anyBoolean(), any())).thenReturn(new HashMap<>());

        kompletthetssjekkerSøknad = new KompletthetssjekkerSøknad(søknadRepository, Period.parse("P4W"));
        kompletthetssjekkerInntektsmelding = new DefaultKompletthetssjekkerInntektsmelding(inntektsmeldingArkivTjeneste);
        kompletthetsjekkerFelles = new KompletthetsjekkerFelles(repositoryProvider, dokumentBestillerApplikasjonTjenesteMock);

        psbKompletthetsjekker = new PSBKompletthetsjekker(
            kompletthetssjekkerSøknad,
            inntektsmeldingTjeneste,
            new KompletthetForBeregningTjeneste(perioderTilVurderingTjeneste, new PSBInntektsmeldingerRelevantForBeregning(), arbeidsforholdTjeneste, inntektArbeidYtelseTjeneste),
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
        when(perioderTilVurderingTjeneste.utled(any(), any())).thenReturn(new TreeSet<>(List.of(DatoIntervallEntitet.fraOgMedTilOgMed(STARTDATO, STARTDATO.plusWeeks(2)))));

        // Act
        KompletthetResultat kompletthetResultat = psbKompletthetsjekker.vurderForsendelseKomplett(lagRef(behandling));

        // Assert
        assertThat(kompletthetResultat.erOppfylt()).isTrue();
        assertThat(kompletthetResultat.getVentefrist()).isNull();
    }

    @Test
    public void skal_finne_at_kompletthet_er_oppfylt_når_vedlegg_til_søknad_finnes_i_joark() {
        // Arrange
        Behandling behandling = TestScenarioBuilder.builderMedSøknad().lagre(repositoryProvider);
        when(perioderTilVurderingTjeneste.utled(any(), any())).thenReturn(new TreeSet<>(List.of(DatoIntervallEntitet.fraOgMedTilOgMed(STARTDATO, STARTDATO.plusWeeks(2)))));

        opprettSøknad(behandling);

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

    private void opprettSøknad(Behandling behandling) {
        SøknadEntitet hentSøknad = søknadRepository.hentSøknad(behandling);
        SøknadEntitet søknad = new SøknadEntitet.Builder(hentSøknad).build();
        søknadRepository.lagreOgFlush(behandling, søknad);
    }
}
