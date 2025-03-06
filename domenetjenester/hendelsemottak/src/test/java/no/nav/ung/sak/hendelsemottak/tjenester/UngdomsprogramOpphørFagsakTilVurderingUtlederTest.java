package no.nav.ung.sak.hendelsemottak.tjenester;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import no.nav.ung.sak.ungdomsprogram.UngdomsprogramPeriodeTjeneste;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.kontrakt.hendelser.HendelseInfo;
import no.nav.ung.sak.kontrakt.hendelser.UngdomsprogramOpphørHendelse;
import no.nav.ung.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriode;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeRepository;


@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
public class UngdomsprogramOpphørFagsakTilVurderingUtlederTest {

    public static final LocalDate STP = LocalDate.now();
    public static final LocalDate OPPHØRSDATO = STP.plusDays(100);
    public static final AktørId BRUKER_AKTØR_ID = AktørId.dummy();
    @Inject
    private EntityManager entityManager;
    private UngdomsprogramOpphørFagsakTilVurderingUtleder utleder;

    @Inject
    private BehandlingRepositoryProvider behandlingRepositoryProvider;
    @Inject
    private UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository;
    private TestScenarioBuilder scenarioBuilder;

    @BeforeEach
    void setUp() {
        var fagsakRepository = new FagsakRepository(entityManager);
        this.utleder = new UngdomsprogramOpphørFagsakTilVurderingUtleder(
            new BehandlingRepository(entityManager), new UngdomsprogramPeriodeTjeneste(ungdomsprogramPeriodeRepository), new FinnFagsakerForAktørTjeneste(entityManager, fagsakRepository));
        scenarioBuilder = TestScenarioBuilder.builderMedSøknad(FagsakYtelseType.UNGDOMSYTELSE)
            .medBruker(BRUKER_AKTØR_ID);
    }


    @Test
    void skal_ikke_returnere_årsak_dersom_det_ikke_finnes_fagsak_for_person() {
        var behandling = scenarioBuilder.lagre(entityManager);
        scenarioBuilder.lagreFagsak(behandlingRepositoryProvider);

        behandling.avsluttBehandling();
        entityManager.flush();

        var builder = new HendelseInfo.Builder();
        builder.leggTilAktør(AktørId.dummy());
        builder.medHendelseId("1");
        builder.medOpprettet(LocalDateTime.now());
        var fagsakBehandlingÅrsakTypeMap = utleder.finnFagsakerTilVurdering(new UngdomsprogramOpphørHendelse(builder.build(), OPPHØRSDATO));


        assertThat(fagsakBehandlingÅrsakTypeMap.isEmpty()).isTrue();
    }

    @Test
    void skal_ikke_returnere_årsak_dersom_ungdomsprogramperiode_sluttdato_er_lik_opphørsdato() {
        var behandling = scenarioBuilder.lagre(entityManager);
        scenarioBuilder.lagreFagsak(behandlingRepositoryProvider);
        ungdomsprogramPeriodeRepository.lagre(behandling.getId(), List.of(new UngdomsprogramPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(STP, OPPHØRSDATO))));

        behandling.avsluttBehandling();
        entityManager.flush();

        var builder = new HendelseInfo.Builder();
        builder.leggTilAktør(BRUKER_AKTØR_ID);
        builder.medHendelseId("1");
        builder.medOpprettet(LocalDateTime.now());
        var fagsakBehandlingÅrsakTypeMap = utleder.finnFagsakerTilVurdering(new UngdomsprogramOpphørHendelse(builder.build(), OPPHØRSDATO));


        assertThat(fagsakBehandlingÅrsakTypeMap.isEmpty()).isTrue();
    }

    @Test
    void skal_returnere_årsak_dersom_ungdomsprogramperiode_sluttdato_er_etter_opphørsdato() {
        var behandling = scenarioBuilder.lagre(entityManager);
        scenarioBuilder.lagreFagsak(behandlingRepositoryProvider);
        ungdomsprogramPeriodeRepository.lagre(behandling.getId(), List.of(new UngdomsprogramPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(STP, OPPHØRSDATO.plusDays(1)))));

        behandling.avsluttBehandling();
        entityManager.flush();

        var builder = new HendelseInfo.Builder();
        builder.leggTilAktør(BRUKER_AKTØR_ID);
        builder.medHendelseId("1");
        builder.medOpprettet(LocalDateTime.now());
        var fagsakBehandlingÅrsakTypeMap = utleder.finnFagsakerTilVurdering(new UngdomsprogramOpphørHendelse(builder.build(), OPPHØRSDATO));


        assertThat(fagsakBehandlingÅrsakTypeMap.keySet().size()).isEqualTo(1);
    }

    @Test
    void skal_returnere_årsak_dersom_ungdomsprogramperiode_sluttdato_er_før_opphørsdato() {
        var behandling = scenarioBuilder.lagre(entityManager);
        scenarioBuilder.lagreFagsak(behandlingRepositoryProvider);
        ungdomsprogramPeriodeRepository.lagre(behandling.getId(), List.of(new UngdomsprogramPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(STP, OPPHØRSDATO.minusDays(1)))));

        behandling.avsluttBehandling();
        entityManager.flush();

        var builder = new HendelseInfo.Builder();
        builder.leggTilAktør(BRUKER_AKTØR_ID);
        builder.medHendelseId("1");
        builder.medOpprettet(LocalDateTime.now());
        var fagsakBehandlingÅrsakTypeMap = utleder.finnFagsakerTilVurdering(new UngdomsprogramOpphørHendelse(builder.build(), OPPHØRSDATO));


        assertThat(fagsakBehandlingÅrsakTypeMap.keySet().size()).isEqualTo(1);
    }

    @Test
    void skal_returnere_årsak_dersom_en_ungdomsprogramperiode_som_går_over_opphørsdato() {
        var behandling = scenarioBuilder.lagre(entityManager);
        scenarioBuilder.lagreFagsak(behandlingRepositoryProvider);
        ungdomsprogramPeriodeRepository.lagre(behandling.getId(), List.of(new UngdomsprogramPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(STP, OPPHØRSDATO.plusDays(1)))));

        behandling.avsluttBehandling();
        entityManager.flush();

        var builder = new HendelseInfo.Builder();
        builder.leggTilAktør(BRUKER_AKTØR_ID);
        builder.medHendelseId("1");
        builder.medOpprettet(LocalDateTime.now());
        var fagsakBehandlingÅrsakTypeMap = utleder.finnFagsakerTilVurdering(new UngdomsprogramOpphørHendelse(builder.build(), OPPHØRSDATO));


        assertThat(fagsakBehandlingÅrsakTypeMap.keySet().size()).isEqualTo(1);
    }

}
