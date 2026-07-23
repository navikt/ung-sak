package no.nav.ung.ytelse.ungdomsprogramytelsen.revurdering.varselopphorvedmaksdato;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.BehandlingStatus;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.behandling.FagsakStatus;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriode;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeRepository;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.test.util.behandling.ungdomsprogramytelse.TestScenarioBuilder;
import no.nav.ung.sak.trigger.ProsessTriggereRepository;
import no.nav.ung.sak.trigger.Trigger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(JpaExtension.class)
@ExtendWith(CdiAwareExtension.class)
class AktuelleFagsakerForMaksdatoVarselRepositoryTest {

    private static final LocalDate FOM = LocalDate.now().minusMonths(6);

    @Inject
    private EntityManager entityManager;

    @Inject
    private UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository;

    @Inject
    private ProsessTriggereRepository prosessTriggereRepository;

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

    @Test
    void skal_ikke_returnere_fagsak_nar_det_finnes_varsel_trigger_med_periode_som_overlapper_maksdato() {
        var behandling = TestScenarioBuilder.builderMedSøknad().lagre(entityManager);
        var maksdato = LocalDate.now().plusWeeks(2);
        lagreUngdomsprogramGrunnlag(behandling, maksdato, maksdato);

        leggTilTrigger(behandling, BehandlingÅrsakType.RE_VARSEL_OPPHOR_VED_MAKSDATO, maksdato.minusDays(1), maksdato.plusDays(1));

        var fagsaker = repository.hentFagsakerRelevantForMaksdatoVarsel();

        assertThat(fagsaker)
            .extracting(f -> f.getId())
            .doesNotContain(behandling.getFagsakId());
    }

    @Test
    void skal_returnere_fagsak_nar_varsel_trigger_ikke_overlapper_maksdato() {
        var behandling = TestScenarioBuilder.builderMedSøknad().lagre(entityManager);
        var maksdato = LocalDate.now().plusWeeks(2);
        lagreUngdomsprogramGrunnlag(behandling, maksdato, maksdato);

        leggTilTrigger(behandling, BehandlingÅrsakType.RE_VARSEL_OPPHOR_VED_MAKSDATO, maksdato.plusDays(1), maksdato.plusDays(2));

        var fagsaker = repository.hentFagsakerRelevantForMaksdatoVarsel();

        assertThat(fagsaker)
            .extracting(f -> f.getId())
            .contains(behandling.getFagsakId());
    }

    @Test
    void skal_returnere_fagsak_nar_overlapper_maksdato_men_trigger_har_annen_arsak() {
        var behandling = TestScenarioBuilder.builderMedSøknad().lagre(entityManager);
        var maksdato = LocalDate.now().plusWeeks(2);
        lagreUngdomsprogramGrunnlag(behandling, maksdato, maksdato);

        leggTilTrigger(behandling, BehandlingÅrsakType.RE_HENDELSE_OPPHØR_UNGDOMSPROGRAM, maksdato.minusDays(1), maksdato.plusDays(1));

        var fagsaker = repository.hentFagsakerRelevantForMaksdatoVarsel();

        assertThat(fagsaker)
            .extracting(f -> f.getId())
            .contains(behandling.getFagsakId());
    }

    @Test
    void skal_ikke_returnere_fagsak_nar_fagsak_allerede_er_avsluttet() {
        var behandling = TestScenarioBuilder.builderMedSøknad().lagre(entityManager);
        var maksdato = LocalDate.now().plusWeeks(2);
        // naturlig avslutning: tom == maksdato, som ellers ville gitt treff på having-betingelsen
        lagreUngdomsprogramGrunnlag(behandling, maksdato, maksdato);

        new FagsakRepository(entityManager).oppdaterFagsakStatus(behandling.getFagsakId(), FagsakStatus.AVSLUTTET);

        var fagsaker = repository.hentFagsakerRelevantForMaksdatoVarsel();

        assertThat(fagsaker)
            .extracting(f -> f.getId())
            .doesNotContain(behandling.getFagsakId());
    }

    @Test
    void skal_ikke_returnere_fagsak_nar_maksdato_allerede_har_passert() {
        var behandling = TestScenarioBuilder.builderMedSøknad().lagre(entityManager);
        // maksdato ligger i fortiden, f.eks. fordi grunnlaget ikke er oppdatert etter et opphør
        var maksdatoIFortiden = LocalDate.now().minusWeeks(1);
        lagreUngdomsprogramGrunnlag(behandling, maksdatoIFortiden, maksdatoIFortiden);

        var fagsaker = repository.hentFagsakerRelevantForMaksdatoVarsel();

        assertThat(fagsaker)
            .extracting(f -> f.getId())
            .doesNotContain(behandling.getFagsakId());
    }

    @Test
    void skal_ikke_returnere_fagsak_nar_maksdato_har_passert_og_periode_er_forlenget() {
        var behandling = TestScenarioBuilder.builderMedSøknad().lagre(entityManager);
        // gammel (allerede passert) maksdato, siden grunnlaget ikke er rukket å bli oppdatert
        // med den forlengede maksdatoen enda
        var gammelMaksdatoIFortiden = LocalDate.now().minusWeeks(1);
        lagreUngdomsprogramGrunnlag(behandling, gammelMaksdatoIFortiden, gammelMaksdatoIFortiden);

        leggTilTrigger(behandling, BehandlingÅrsakType.RE_HENDELSE_FORLENGET_PERIODE_UNGDOMSPROGRAM,
            gammelMaksdatoIFortiden.minusDays(1), gammelMaksdatoIFortiden.plusDays(1));

        var fagsaker = repository.hentFagsakerRelevantForMaksdatoVarsel();

        assertThat(fagsaker)
            .extracting(f -> f.getId())
            .doesNotContain(behandling.getFagsakId());
    }

    @Test
    void skal_ikke_returnere_fagsak_nar_det_finnes_aapen_behandling_med_varsel_opphor_arsak() {
        var maksdato = LocalDate.now().plusWeeks(2);
        var behandling = TestScenarioBuilder.builderMedSøknad()
            .medBehandlingÅrsak(BehandlingÅrsakType.RE_VARSEL_OPPHOR_VED_MAKSDATO)
            .medBehandlingStatus(BehandlingStatus.UTREDES)
            .lagre(entityManager);
        lagreUngdomsprogramGrunnlag(behandling, maksdato, maksdato);

        var fagsaker = repository.hentFagsakerRelevantForMaksdatoVarsel();

        assertThat(fagsaker)
            .extracting(f -> f.getId())
            .doesNotContain(behandling.getFagsakId());
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

    private void leggTilTrigger(Behandling behandling, BehandlingÅrsakType årsak, LocalDate fom, LocalDate tom) {
        var trigger = new Trigger(årsak, DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom));
        prosessTriggereRepository.leggTil(behandling.getId(), Set.of(trigger));
    }
}

