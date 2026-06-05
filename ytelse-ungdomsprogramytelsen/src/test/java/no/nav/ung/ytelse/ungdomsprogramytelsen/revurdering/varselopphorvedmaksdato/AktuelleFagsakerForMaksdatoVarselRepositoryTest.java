package no.nav.ung.ytelse.ungdomsprogramytelsen.revurdering.varselopphorvedmaksdato;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriode;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeRepository;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.test.util.behandling.ungdomsprogramytelse.TestScenarioBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(JpaExtension.class)
@ExtendWith(CdiAwareExtension.class)
class AktuelleFagsakerForMaksdatoVarselRepositoryTest {

    private static final LocalDate FOM = LocalDate.now().minusMonths(6);

    @Inject
    private EntityManager entityManager;

    @Inject
    private UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository;

    private AktuelleFagsakerForMaksdatoVarselRepository repository;
    private BehandlingRepository behandlingRepository;

    @BeforeEach
    void setUp() {
        repository = new AktuelleFagsakerForMaksdatoVarselRepository(entityManager);
        behandlingRepository = new BehandlingRepository(entityManager);
    }

    @Test
    void skal_returnere_fagsak_nar_maksdato_er_innenfor_varselvindu_og_tom_er_lik_eller_etter_maksdato() {
        var behandling = TestScenarioBuilder.builderMedSøknad().lagre(entityManager);
        var maksdato = LocalDate.now().plusWeeks(2);

        lagreUngdomsprogramGrunnlag(behandling, maksdato, maksdato);

        var fagsaker = repository.hentFagsakerRelevantForMaksdatoVarsel();

        assertThat(fagsaker)
            .extracting(f -> f.getId())
            .contains(behandling.getFagsakId());
    }

    @Test
    void skal_ikke_returnere_fagsak_nar_maksdato_er_utenfor_varselvindu() {
        var behandling = TestScenarioBuilder.builderMedSøknad().lagre(entityManager);
        var maksdatoUtenforVindu = LocalDate.now().plusWeeks(4);

        lagreUngdomsprogramGrunnlag(behandling, maksdatoUtenforVindu, maksdatoUtenforVindu);

        var fagsaker = repository.hentFagsakerRelevantForMaksdatoVarsel();

        assertThat(fagsaker)
            .extracting(f -> f.getId())
            .doesNotContain(behandling.getFagsakId());
    }

    @Test
    void skal_ikke_returnere_fagsak_nar_opphor_er_satt_tidligere_enn_maksdato() {
        var behandling = TestScenarioBuilder.builderMedSøknad().lagre(entityManager);
        var maksdato = LocalDate.now().plusWeeks(2);
        var tomForPeriode = maksdato.minusDays(1);

        lagreUngdomsprogramGrunnlag(behandling, tomForPeriode, maksdato);

        var fagsaker = repository.hentFagsakerRelevantForMaksdatoVarsel();

        assertThat(fagsaker)
            .extracting(f -> f.getId())
            .doesNotContain(behandling.getFagsakId());
    }

    @Test
    void skal_ikke_returnere_fagsak_for_andre_ytelsestyper() {
        var behandling = TestScenarioBuilder.builderMedSøknad(FagsakYtelseType.AKTIVITETSPENGER).lagre(entityManager);
        var maksdato = LocalDate.now().plusWeeks(2);

        lagreUngdomsprogramGrunnlag(behandling, maksdato, maksdato);

        var fagsaker = repository.hentFagsakerRelevantForMaksdatoVarsel();

        assertThat(fagsaker)
            .extracting(f -> f.getId())
            .doesNotContain(behandling.getFagsakId());
    }

    @Test
    void skal_vurdere_siste_ytelsesbehandling_for_fagsaken() {
        var førstegangsbehandling = TestScenarioBuilder.builderMedSøknad().lagre(entityManager);
        var maksdatoInnenforVindu = LocalDate.now().plusWeeks(2);
        lagreUngdomsprogramGrunnlag(førstegangsbehandling, maksdatoInnenforVindu, maksdatoInnenforVindu);

        var revurdering = lagreRevurderingPåSammeFagsak(førstegangsbehandling);
        var maksdatoUtenforVindu = LocalDate.now().plusWeeks(4);
        lagreUngdomsprogramGrunnlag(revurdering, maksdatoUtenforVindu, maksdatoUtenforVindu);

        var fagsaker = repository.hentFagsakerRelevantForMaksdatoVarsel();

        assertThat(fagsaker)
            .extracting(f -> f.getId())
            .doesNotContain(førstegangsbehandling.getFagsakId());
    }

    private void lagreUngdomsprogramGrunnlag(Behandling behandling, LocalDate tom, LocalDate maksdato) {
        ungdomsprogramPeriodeRepository.lagre(
            behandling.getId(),
            List.of(new UngdomsprogramPeriode(FOM, tom)),
            false,
            maksdato
        );
    }

    private Behandling lagreRevurderingPåSammeFagsak(Behandling forrigeBehandling) {
        var revurdering = Behandling.fraTidligereBehandling(forrigeBehandling, BehandlingType.REVURDERING).build();
        var lås = behandlingRepository.taSkriveLås(revurdering);
        behandlingRepository.lagre(revurdering, lås);
        entityManager.flush();
        return revurdering;
    }
}

