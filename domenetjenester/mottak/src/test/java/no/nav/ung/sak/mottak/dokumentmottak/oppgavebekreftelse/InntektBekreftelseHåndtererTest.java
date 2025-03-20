package no.nav.ung.sak.mottak.dokumentmottak.oppgavebekreftelse;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.k9.oppgave.OppgaveBekreftelse;
import no.nav.k9.oppgave.bekreftelse.ung.inntekt.InntektBekreftelse;
import no.nav.k9.oppgave.bekreftelse.ung.inntekt.OppgittInntektForPeriode;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskStatus;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.k9.prosesstask.impl.ProsessTaskRepositoryImpl;
import no.nav.k9.prosesstask.impl.ProsessTaskTjenesteImpl;
import no.nav.k9.søknad.felles.Versjon;
import no.nav.k9.søknad.felles.personopplysninger.Søker;
import no.nav.k9.søknad.felles.type.NorskIdentitetsnummer;
import no.nav.k9.søknad.felles.type.Periode;
import no.nav.k9.søknad.felles.type.SøknadId;
import no.nav.ung.kodeverk.behandling.BehandlingStegType;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.kodeverk.dokument.Brevkode;
import no.nav.ung.kodeverk.etterlysning.EtterlysningStatus;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.etterlysning.EtterlysningEntitet;
import no.nav.ung.sak.behandlingslager.etterlysning.EtterlysningRepository;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.mottak.dokumentmottak.AsyncAbakusLagreOpptjeningTask;
import no.nav.ung.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.ung.sak.typer.JournalpostId;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
class InntektBekreftelseHåndtererTest {

    @Inject
    private EntityManager em;
    private EtterlysningRepository etterlysningRepository;
    private ProsessTaskTjeneste prosessTaskTjeneste;


    @BeforeEach
    void setup() {
        etterlysningRepository = new EtterlysningRepository(em);
        ProsessTaskRepositoryImpl prosessTaskRepository = new ProsessTaskRepositoryImpl(em, null, null);
        prosessTaskTjeneste = new ProsessTaskTjenesteImpl(prosessTaskRepository);

    }

    @Test
    void skalOppdatereEtterlysningOppdatereIayGrunnlagLagreUttalelseOgSetteBehandlingAvVent() {


        // Arrange
        TestScenarioBuilder scenarioBuilder = TestScenarioBuilder.builderMedSøknad()
            .medBehandlingType(BehandlingType.REVURDERING);

        scenarioBuilder.leggTilAksjonspunkt(AksjonspunktDefinisjon.AUTO_SATT_PÅ_VENT_ETTERLYST_INNTEKTUTTALELSE, BehandlingStegType.KONTROLLER_REGISTER_INNTEKT);
        Behandling behandling = scenarioBuilder.lagre(em);

        var periode = DatoIntervallEntitet.fra(LocalDate.now(), LocalDate.now());
        var oppgaveId = UUID.randomUUID();
        var grunnlagsreferanse = UUID.randomUUID();
        var etterlysning = etterlysningRepository.lagre(EtterlysningEntitet.forInntektKontrollUttalelse(
            behandling.getId(),
            grunnlagsreferanse,
            oppgaveId,
            periode));

        etterlysning.vent(LocalDateTime.now().plusDays(1));
        etterlysningRepository.lagre(etterlysning);
        em.flush();


        long journalpostId = 892L;
        int oppgittInntekt = 56321;
        var bekreftelse = new OppgaveBekreftelseInnhold(
            new JournalpostId(journalpostId),
            behandling,
            new OppgaveBekreftelse(
                new SøknadId("456"),
                Versjon.of("1"),
                ZonedDateTime.now(),
                new Søker(NorskIdentitetsnummer.of("12345678910")),
                new InntektBekreftelse(
                    oppgaveId,
                    Set.of(new OppgittInntektForPeriode(
                        new Periode(periode.getFomDato(), periode.getTomDato()),
                        BigDecimal.valueOf(oppgittInntekt),
                        BigDecimal.ZERO)),
                    true,
                    "uttalelse")
            ),
            LocalDateTime.now(),
            Brevkode.UNGDOMSYTELSE_OPPGAVE_BEKREFTELSE
        );

        // Act
        var inntektBekreftelseHåndterer = new InntektBekreftelseHåndterer(etterlysningRepository, prosessTaskTjeneste);
        inntektBekreftelseHåndterer.håndter(bekreftelse);

        // Assert
        //abakus er oppdatert
        List<ProsessTaskData> abakusTasker = prosessTaskTjeneste.finnAlle(AsyncAbakusLagreOpptjeningTask.TASKTYPE, ProsessTaskStatus.KLAR);
        assertThat(abakusTasker).hasSize(1);
        var abakusTask = abakusTasker.getFirst();
        assertThat(abakusTask.getPropertyValue(AsyncAbakusLagreOpptjeningTask.JOURNALPOST_ID)).isEqualTo(String.valueOf(journalpostId));
        assertThat(abakusTask.getPropertyValue(AsyncAbakusLagreOpptjeningTask.BREVKODER)).isEqualTo(Brevkode.UNGDOMSYTELSE_OPPGAVE_BEKREFTELSE.getKode());
        assertThat(abakusTask.getPayloadAsString()).contains(String.valueOf(oppgittInntekt));


        //behandling er ikke lenger på vent
        assertThat(behandling.getAksjonspunkter()).isEmpty();

        //etterlysning er oppdatert
        var oppdatertEtterlysning = etterlysningRepository.hentEtterlysning(etterlysning.getId());
        assertThat(oppdatertEtterlysning.getStatus()).isEqualTo(EtterlysningStatus.MOTTATT_SVAR);
    }
}
