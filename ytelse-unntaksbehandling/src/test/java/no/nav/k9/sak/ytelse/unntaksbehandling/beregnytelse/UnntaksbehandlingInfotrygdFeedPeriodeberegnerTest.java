package no.nav.k9.sak.ytelse.unntaksbehandling.beregnytelse;

import static no.nav.k9.sak.ytelse.unntaksbehandling.beregning.Datoer.dato;

import java.math.BigDecimal;
import java.time.LocalDate;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.foreldrepenger.domene.vedtak.infotrygdfeed.InfotrygdFeedPeriode;
import no.nav.k9.kodeverk.arbeidsforhold.AktivitetStatus;
import no.nav.k9.kodeverk.arbeidsforhold.Inntektskategori;
import no.nav.k9.kodeverk.behandling.BehandlingResultatType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;
import no.nav.k9.kodeverk.vedtak.VedtakResultatType;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.db.util.JpaExtension;
import no.nav.k9.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.vedtak.felles.testutilities.cdi.CdiAwareExtension;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
class UnntaksbehandlingInfotrygdFeedPeriodeberegnerTest {

    @Inject
    private EntityManager entityManager;

    private BeregningsresultatRepository beregningsresultatRepository;

    private Behandling behandling;
    private BeregningsresultatEntitet beregningsresultat;
    private UnntaksbehandlingInfotrygdFeedPeriodeberegner testSubject;
    private Saksnummer saksnummer;

    @BeforeEach
    void setUp() {
        BehandlingRepository behandlingRepository = new BehandlingRepository(entityManager);
        FagsakRepository fagsakRepository = new FagsakRepository(entityManager);
        beregningsresultatRepository = new BeregningsresultatRepository(entityManager);

        TestScenarioBuilder scenario = TestScenarioBuilder
            .builderMedSøknad(FagsakYtelseType.OMSORGSPENGER)
            .medBehandlingsresultat(BehandlingResultatType.INNVILGET);

        BehandlingVedtak.Builder vedtakBuilder = scenario.medBehandlingVedtak()
            .medBeslutning(true)
            .medVedtakResultatType(VedtakResultatType.INNVILGET);

        behandling = scenario.lagre(entityManager);

        behandling.avsluttBehandling();
        BehandlingLås behandlingLås = behandlingRepository.taSkriveLås(behandling);
        behandlingRepository.lagre(behandling, behandlingLås);

        //TODO Trenger sette opp Behanlingedtak

        saksnummer = behandling.getFagsak().getSaksnummer();
        testSubject = new UnntaksbehandlingInfotrygdFeedPeriodeberegner(fagsakRepository, behandlingRepository, beregningsresultatRepository);

    }

    @Test
    void infotrygd_feed_periode_skal_tilsvare_første_og_siste_dato_på_beregningsresultat() {
        // Arrange
        beregningsresultat = buildBeregningsresultat();

        // Act
        InfotrygdFeedPeriode infotrygdFeedPeriode = testSubject.finnInnvilgetPeriode(saksnummer);


        // Assert
        Assertions.assertThat(infotrygdFeedPeriode.getFom()).isEqualTo(dato("2020.11.14"));
        Assertions.assertThat(infotrygdFeedPeriode.getTom()).isEqualTo(dato("2020.11.19"));

    }

    private BeregningsresultatEntitet buildBeregningsresultat() {
        BeregningsresultatEntitet.Builder builder = BeregningsresultatEntitet.builder()
            //TODO Hva brukes regel-greier til?
            .medRegelInput("ikkeInteressant")
            .medRegelSporing("ikkeInteressant");
        BeregningsresultatEntitet beregningsresultat = builder.build();
        BeregningsresultatPeriode brPeriode = buildBeregningsresultatPeriode(beregningsresultat);
        buildBeregningsresultatAndel(brPeriode);
        beregningsresultatRepository.lagre(behandling, beregningsresultat);
        return beregningsresultat;
    }

    private BeregningsresultatPeriode buildBeregningsresultatPeriode(BeregningsresultatEntitet beregningsresultat) {
        return BeregningsresultatPeriode.builder()
            .medBeregningsresultatPeriodeFomOgTom(LocalDate.now().minusDays(20), LocalDate.now().minusDays(15))
            .build(beregningsresultat);
    }

    private BeregningsresultatAndel buildBeregningsresultatAndel(BeregningsresultatPeriode beregningsresultatPeriode) {
        return BeregningsresultatAndel.builder()
            .medBrukerErMottaker(true)
            .medArbeidsforholdType(OpptjeningAktivitetType.ARBEID)
            .medArbeidsgiver(Arbeidsgiver.person(AktørId.dummy()))
            .medDagsats(2160)
            .medDagsatsFraBg(2160)
            .medUtbetalingsgrad(BigDecimal.valueOf(100))
            .medStillingsprosent(BigDecimal.valueOf(100))
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
            .buildFor(beregningsresultatPeriode);
    }

}
