package no.nav.ung.sak.etterlysning;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.ung.brukerdialog.kontrakt.oppgaver.OppgaveRequest;
import no.nav.ung.kodeverk.varsel.EtterlysningStatus;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.etterlysning.Etterlysning;
import no.nav.ung.sak.behandlingslager.etterlysning.EtterlysningRepository;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.test.util.behandling.ungdomsprogramytelse.TestScenarioBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(JpaExtension.class)
@ExtendWith(CdiAwareExtension.class)
class EtterlysningProssesseringTjenesteTest {

    @Inject
    private EtterlysningRepository etterlysningRepository;
    @Inject
    private EntityManager entityManager;

    private final UngBrukerdialogOppgaveKlient oppgaveKlient = mock(UngBrukerdialogOppgaveKlient.class);
    private Behandling behandling;
    private EtterlysningProssesseringTjeneste tjeneste;

    @BeforeEach
    void setUp() {
        behandling = TestScenarioBuilder.builderMedSøknad().lagre(entityManager);
        tjeneste = new EtterlysningProssesseringTjeneste(etterlysningRepository, mock(OpprettOppgaveTjeneste.class), oppgaveKlient, mock(ProsessTaskTjeneste.class));
    }

    @Test
    void avbryter_åpne_etterlysninger_og_kaller_brukerdialog_bare_for_sendte() {
        var ikkeSendt = etterlysningRepository.lagre(etterlysning());
        var sendt = etterlysning();
        sendt.vent(LocalDateTime.now().plusDays(14));
        etterlysningRepository.lagre(sendt);
        var besvart = etterlysning();
        besvart.vent(LocalDateTime.now().plusDays(14));
        besvart.mottaSvar();
        etterlysningRepository.lagre(besvart);

        tjeneste.avbrytÅpneEtterlysninger(behandling);

        assertThat(ikkeSendt.getStatus()).isEqualTo(EtterlysningStatus.AVBRUTT);
        assertThat(sendt.getStatus()).isEqualTo(EtterlysningStatus.AVBRUTT);
        assertThat(besvart.getStatus()).isEqualTo(EtterlysningStatus.MOTTATT_SVAR);
        var request = ArgumentCaptor.forClass(OppgaveRequest.class);
        verify(oppgaveKlient, times(1)).avbrytOppgave(request.capture());
        assertThat(request.getValue().oppgaveReferanse()).isEqualTo(sendt.getEksternReferanse());
    }

    private Etterlysning etterlysning() {
        return Etterlysning.forInntektKontrollUttalelse(behandling.getId(), UUID.randomUUID(), UUID.randomUUID(),
            DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2025, 8, 1), LocalDate.of(2025, 8, 31)));
    }
}
