package no.nav.ung.ytelse.ungdomsprogramytelsen.hendelsehåndtering;

import static no.nav.ung.kodeverk.behandling.BehandlingÅrsakType.RE_HENDELSE_OPPHØR_OPPHEVET_UNGDOMSPROGRAM;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandling.revurdering.ÅrsakOgPerioder;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriode;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeRepository;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.hendelsemottak.tjenester.FinnFagsakerForAktørTjeneste;
import no.nav.ung.sak.kontrakt.hendelser.HendelseInfo;
import no.nav.ung.sak.kontrakt.hendelser.UngdomsprogramOpphørOpphevetHendelse;
import no.nav.ung.sak.test.util.behandling.ungdomsprogramytelse.TestScenarioBuilder;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.ytelse.ungdomsprogramytelsen.ungdomsprogrammet.UngdomsprogramPeriodeTjeneste;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
class UngdomsprogramOpphørOpphevetFagsakTilVurderingUtlederTest {

    public static final LocalDate STP = LocalDate.now();
    public static final LocalDate TIDLIGERE_OPPHØRSDATO = STP.plusDays(100);
    public static final AktørId BRUKER_AKTØR_ID = AktørId.dummy();

    @Inject
    private EntityManager entityManager;
    @Inject
    private BehandlingRepositoryProvider behandlingRepositoryProvider;
    @Inject
    private UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository;

    private UngdomsprogramOpphørOpphevetFagsakTilVurderingUtleder utleder;
    private TestScenarioBuilder scenarioBuilder;

    @BeforeEach
    void setUp() {
        var fagsakRepository = new FagsakRepository(entityManager);
        this.utleder = new UngdomsprogramOpphørOpphevetFagsakTilVurderingUtleder(
            new BehandlingRepository(entityManager),
            new UngdomsprogramPeriodeTjeneste(ungdomsprogramPeriodeRepository),
            new FinnFagsakerForAktørTjeneste(entityManager, fagsakRepository)
        );
        scenarioBuilder = TestScenarioBuilder.builderMedSøknad(FagsakYtelseType.UNGDOMSYTELSE)
            .medBruker(BRUKER_AKTØR_ID);
    }

    @Test
    void skal_ikke_returnere_årsak_dersom_det_ikke_finnes_fagsak_for_person() {
        var behandling = scenarioBuilder.lagre(entityManager);
        scenarioBuilder.lagreFagsak(behandlingRepositoryProvider);
        behandling.avsluttBehandling();
        entityManager.flush();

        var hendelse = lagHendelse(AktørId.dummy(), TIDLIGERE_OPPHØRSDATO);
        var resultat = utleder.finnFagsakerTilVurdering(hendelse);

        assertThat(resultat.isEmpty()).isTrue();
    }

    @Test
    void skal_ikke_returnere_årsak_dersom_perioden_allerede_er_gjenåpnet() {
        var behandling = scenarioBuilder.lagre(entityManager);
        scenarioBuilder.lagreFagsak(behandlingRepositoryProvider);
        // Perioden strekker seg allerede forbi den tidligere opphørsdatoen -> allerede reflektert
        ungdomsprogramPeriodeRepository.lagre(behandling.getId(),
            List.of(new UngdomsprogramPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(STP, TIDLIGERE_OPPHØRSDATO.plusDays(50)))),
            false,
            TIDLIGERE_OPPHØRSDATO.plusDays(50));

        behandling.avsluttBehandling();
        entityManager.flush();

        var hendelse = lagHendelse(BRUKER_AKTØR_ID, TIDLIGERE_OPPHØRSDATO);
        var resultat = utleder.finnFagsakerTilVurdering(hendelse);

        assertThat(resultat.isEmpty()).isTrue();
    }

    @Test
    void skal_returnere_årsak_med_periode_frem_til_maksdato_dersom_opphør_ikke_er_gjenspeilet() {
        var behandling = scenarioBuilder.lagre(entityManager);
        scenarioBuilder.lagreFagsak(behandlingRepositoryProvider);
        var periodeMaksDato = TIDLIGERE_OPPHØRSDATO.plusDays(80);
        // Periodegrunnlaget reflekterer fortsatt det (feilaktige) opphøret
        ungdomsprogramPeriodeRepository.lagre(behandling.getId(),
            List.of(new UngdomsprogramPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(STP, TIDLIGERE_OPPHØRSDATO))),
            false,
            periodeMaksDato);

        behandling.avsluttBehandling();
        entityManager.flush();

        var hendelse = lagHendelse(BRUKER_AKTØR_ID, TIDLIGERE_OPPHØRSDATO);
        var resultat = utleder.finnFagsakerTilVurdering(hendelse);

        validerHarÅrsak(resultat, DatoIntervallEntitet.fraOgMedTilOgMed(TIDLIGERE_OPPHØRSDATO.plusDays(1), periodeMaksDato));
    }

    @Test
    void skal_kaste_exception_dersom_maksdato_ikke_er_etter_tidligere_opphørsdato() {
        var behandling = scenarioBuilder.lagre(entityManager);
        scenarioBuilder.lagreFagsak(behandlingRepositoryProvider);
        // periodeMaksDato er (feilaktig) ikke etter tidligere opphørsdato -- hendelsen burde vært ignorert av kilden
        ungdomsprogramPeriodeRepository.lagre(behandling.getId(),
            List.of(new UngdomsprogramPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(STP, TIDLIGERE_OPPHØRSDATO))),
            false,
            TIDLIGERE_OPPHØRSDATO);

        behandling.avsluttBehandling();
        entityManager.flush();

        var hendelse = lagHendelse(BRUKER_AKTØR_ID, TIDLIGERE_OPPHØRSDATO);

        assertThatThrownBy(() -> utleder.finnFagsakerTilVurdering(hendelse))
            .isInstanceOf(IllegalStateException.class);
    }

    private static UngdomsprogramOpphørOpphevetHendelse lagHendelse(AktørId aktørId, LocalDate tidligereOpphørsdato) {
        var builder = new HendelseInfo.Builder();
        builder.leggTilAktør(aktørId);
        builder.medHendelseId("1");
        builder.medOpprettet(LocalDateTime.now());
        return new UngdomsprogramOpphørOpphevetHendelse(builder.build(), tidligereOpphørsdato);
    }

    private static void validerHarÅrsak(Map<Fagsak, List<ÅrsakOgPerioder>> fagsakBehandlingÅrsakTypeMap, DatoIntervallEntitet forventetPeriode) {
        assertThat(fagsakBehandlingÅrsakTypeMap.keySet().size()).isEqualTo(1);
        assertThat(fagsakBehandlingÅrsakTypeMap.values().iterator().next().getFirst().behandlingÅrsak()).isEqualTo(RE_HENDELSE_OPPHØR_OPPHEVET_UNGDOMSPROGRAM);
        assertThat(fagsakBehandlingÅrsakTypeMap.values().iterator().next().getFirst().perioder().iterator().next()).isEqualTo(forventetPeriode);
    }

}
