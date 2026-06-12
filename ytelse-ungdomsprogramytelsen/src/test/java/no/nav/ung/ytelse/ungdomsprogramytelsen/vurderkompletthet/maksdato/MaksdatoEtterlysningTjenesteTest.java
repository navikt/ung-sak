package no.nav.ung.ytelse.ungdomsprogramytelsen.vurderkompletthet.maksdato;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.k9.prosesstask.impl.ProsessTaskTjenesteImpl;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.varsel.EtterlysningStatus;
import no.nav.ung.kodeverk.varsel.EtterlysningType;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.sporing.BehandingprosessSporingRepository;
import no.nav.ung.sak.behandlingslager.etterlysning.Etterlysning;
import no.nav.ung.sak.behandlingslager.etterlysning.EtterlysningRepository;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriode;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeRepository;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.test.util.behandling.ungdomsprogramytelse.TestScenarioBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@ExtendWith(JpaExtension.class)
@ExtendWith(CdiAwareExtension.class)
class MaksdatoEtterlysningTjenesteTest {

    // Maksdato innenfor varslingsvindut (< 3 uker frem)
    private static final LocalDate MAKSDATO_INNENFOR_VARSLINGSVINDU = LocalDate.now().plusWeeks(2);
    // Maksdato utenfor varslingsvindut (> 3 uker frem)
    private static final LocalDate MAKSDATO_UTENFOR_VARSLINGSVINDU = LocalDate.now().plusWeeks(4);

    @Inject
    private EntityManager entityManager;
    @Inject
    private UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository;
    @Inject
    private EtterlysningRepository etterlysningRepository;
    @Inject
    private ProsessTaskTjenesteImpl prosessTaskTjeneste;

    private MaksdatoEtterlysningTjeneste tjeneste;
    private Behandling behandling;
    private BehandlingRepository behandlingRepository;

    @BeforeEach
    void setUp() {
        behandlingRepository = new BehandlingRepository(entityManager);
        behandling = TestScenarioBuilder.builderMedSøknad()
            .medBehandlingÅrsak(BehandlingÅrsakType.RE_VARSEL_OPPHOR_VED_MAKSDATO)
            .lagre(entityManager);

        tjeneste = new MaksdatoEtterlysningTjeneste(
            behandlingRepository,
            ungdomsprogramPeriodeRepository,
            prosessTaskTjeneste,
            etterlysningRepository,
            new BehandingprosessSporingRepository(entityManager)
        );
    }

    @Test
    void skalIkkeOppretteEtterlysning_nårBehandlingManglerRiktigÅrsak() {
        var behandlingUtenÅrsak = TestScenarioBuilder.builderMedSøknad().lagre(entityManager);
        var fom = LocalDate.now().minusMonths(6);
        ungdomsprogramPeriodeRepository.lagre(behandlingUtenÅrsak.getId(),
            List.of(new UngdomsprogramPeriode(fom, MAKSDATO_INNENFOR_VARSLINGSVINDU)),
            false, MAKSDATO_INNENFOR_VARSLINGSVINDU);

        tjeneste.opprettEtterlysningForOpphørVedMaksdatoDersomRelevant(BehandlingReferanse.fra(behandlingUtenÅrsak));

        var etterlysninger = etterlysningRepository.hentEtterlysninger(behandlingUtenÅrsak.getId());
        assertThat(etterlysninger).isEmpty();
    }

    @Test
    void skalOppretteEtterlysning_nårÅrsakErVarselOpphørOgMaksdatoInnenforVarslingsvindu() {
        var fom = LocalDate.now().minusMonths(6);
        ungdomsprogramPeriodeRepository.lagre(behandling.getId(),
            List.of(new UngdomsprogramPeriode(fom, MAKSDATO_INNENFOR_VARSLINGSVINDU)),
            false, MAKSDATO_INNENFOR_VARSLINGSVINDU);

        tjeneste.opprettEtterlysningForOpphørVedMaksdatoDersomRelevant(BehandlingReferanse.fra(behandling));

        var etterlysninger = etterlysningRepository.hentEtterlysninger(behandling.getId());
        assertThat(etterlysninger).hasSize(1);
        var etterlysning = etterlysninger.get(0);
        assertThat(etterlysning.getType()).isEqualTo(EtterlysningType.UTTALELSE_OPPHOR_VED_MAKSDATO);
        assertThat(etterlysning.getStatus()).isEqualTo(EtterlysningStatus.OPPRETTET);
        assertThat(etterlysning.getPeriode()).isEqualTo(DatoIntervallEntitet.fraOgMedTilOgMed(fom, MAKSDATO_INNENFOR_VARSLINGSVINDU));
    }

    @Test
    void skalOppretteEtterlysningMedRiktigGrunnlagsreferanse() {
        var fom = LocalDate.now().minusMonths(6);
        ungdomsprogramPeriodeRepository.lagre(behandling.getId(),
            List.of(new UngdomsprogramPeriode(fom, MAKSDATO_INNENFOR_VARSLINGSVINDU)),
            false, MAKSDATO_INNENFOR_VARSLINGSVINDU);
        var grunnlag = ungdomsprogramPeriodeRepository.hentGrunnlag(behandling.getId()).orElseThrow();

        tjeneste.opprettEtterlysningForOpphørVedMaksdatoDersomRelevant(BehandlingReferanse.fra(behandling));

        var etterlysninger = etterlysningRepository.hentEtterlysninger(behandling.getId());
        assertThat(etterlysninger).hasSize(1);
        assertThat(etterlysninger.get(0).getGrunnlagsreferanse()).isEqualTo(grunnlag.getGrunnlagsreferanse());
    }

    @Test
    void skalIkkeOppretteEtterlysning_nårMaksdatoUtenforVarslingsvindu() {
        var fom = LocalDate.now().minusMonths(6);
        ungdomsprogramPeriodeRepository.lagre(behandling.getId(),
            List.of(new UngdomsprogramPeriode(fom, MAKSDATO_UTENFOR_VARSLINGSVINDU)),
            false, MAKSDATO_UTENFOR_VARSLINGSVINDU);

        tjeneste.opprettEtterlysningForOpphørVedMaksdatoDersomRelevant(BehandlingReferanse.fra(behandling));

        var etterlysninger = etterlysningRepository.hentEtterlysninger(behandling.getId());
        assertThat(etterlysninger).isEmpty();
    }

    @Test
    void skalMarkereSomSkalAvbrytes_nårEksisterendeOpprettetEtterlysningOgNyMaksdato() {
        var fom = LocalDate.now().minusMonths(6);
        var gammelMaksdato = MAKSDATO_INNENFOR_VARSLINGSVINDU.minusWeeks(1);

        // Lagre grunnlag med gammel maksdato og opprett første etterlysning
        ungdomsprogramPeriodeRepository.lagre(behandling.getId(),
            List.of(new UngdomsprogramPeriode(fom, gammelMaksdato)),
            false, gammelMaksdato);
        tjeneste.opprettEtterlysningForOpphørVedMaksdatoDersomRelevant(BehandlingReferanse.fra(behandling));

        // Lagre nytt grunnlag med ny maksdato
        ungdomsprogramPeriodeRepository.lagre(behandling.getId(),
            List.of(new UngdomsprogramPeriode(fom, MAKSDATO_INNENFOR_VARSLINGSVINDU)),
            false, MAKSDATO_INNENFOR_VARSLINGSVINDU);

        tjeneste.opprettEtterlysningForOpphørVedMaksdatoDersomRelevant(BehandlingReferanse.fra(behandling));

        var etterlysninger = etterlysningRepository.hentEtterlysninger(behandling.getId());
        assertThat(etterlysninger).hasSize(2);

        var statuser = etterlysninger.stream().map(Etterlysning::getStatus).toList();
        assertThat(statuser).contains(EtterlysningStatus.SKAL_AVBRYTES);
        assertThat(statuser).contains(EtterlysningStatus.OPPRETTET);
    }

    @Test
    void skalIkkeOppretteNyEtterlysning_nårEksisterendeOpprettetEtterlysningOgSammeMaksdato() {
        var fom = LocalDate.now().minusMonths(6);
        ungdomsprogramPeriodeRepository.lagre(behandling.getId(),
            List.of(new UngdomsprogramPeriode(fom, MAKSDATO_INNENFOR_VARSLINGSVINDU)),
            false, MAKSDATO_INNENFOR_VARSLINGSVINDU);

        // Kall to ganger med samme data
        tjeneste.opprettEtterlysningForOpphørVedMaksdatoDersomRelevant(BehandlingReferanse.fra(behandling));
        tjeneste.opprettEtterlysningForOpphørVedMaksdatoDersomRelevant(BehandlingReferanse.fra(behandling));

        var etterlysninger = etterlysningRepository.hentEtterlysninger(behandling.getId());
        assertThat(etterlysninger).hasSize(1);
    }
}
