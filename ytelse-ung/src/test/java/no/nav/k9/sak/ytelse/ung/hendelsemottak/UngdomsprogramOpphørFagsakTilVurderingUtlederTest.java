package no.nav.k9.sak.ytelse.ung.hendelsemottak;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.db.util.JpaExtension;
import no.nav.k9.sak.domene.person.pdl.PersoninfoAdapter;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.kontrakt.hendelser.HendelseInfo;
import no.nav.k9.sak.kontrakt.ungdomsytelse.hendelser.UngdomsprogramOpphørHendelse;
import no.nav.k9.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.ytelse.ung.periode.UngdomsprogramPeriode;
import no.nav.k9.sak.ytelse.ung.periode.UngdomsprogramPeriodeRepository;


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
        this.utleder = new UngdomsprogramOpphørFagsakTilVurderingUtleder(new FagsakRepository(entityManager),
            new BehandlingRepository(entityManager), ungdomsprogramPeriodeRepository);
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
    void skal_ikke_returnere_årsak_dersom_ingen_ungdomsprogramperioder_etter_opphørsdato() {
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
