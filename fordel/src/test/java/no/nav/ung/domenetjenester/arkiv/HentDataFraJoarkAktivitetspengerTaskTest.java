package no.nav.ung.domenetjenester.arkiv;


import no.nav.k9.felles.integrasjon.saf.Journalstatus;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.ung.domenetjenester.arkiv.journalpostvurderer.AlleredeMottattJournalpost;
import no.nav.ung.domenetjenester.arkiv.journalpostvurderer.IgnorertJournalpost;
import no.nav.ung.domenetjenester.arkiv.journalpostvurderer.Journalpostvurderer;
import no.nav.ung.domenetjenester.arkiv.journalpostvurderer.StrukturertJournalpost;
import no.nav.ung.domenetjenester.arkiv.journalpostvurderer.UhåndtertJournalpost;
import no.nav.ung.fordel.handler.FordelProsessTaskTjeneste;
import no.nav.ung.fordel.handler.MottattMelding;
import no.nav.ung.fordel.handler.MottattMeldingTjeneste;
import no.nav.ung.fordel.repo.journalpost.JournalpostMottattEntitet;
import no.nav.ung.fordel.repo.journalpost.JournalpostRepository;
import no.nav.ung.kodeverk.dokument.Brevkode;
import no.nav.ung.kodeverk.produksjonsstyring.OmrådeTema;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.JournalpostId;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.calls;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class HentDataFraJoarkAktivitetspengerTaskTest {

    private AtomicInteger counter = new AtomicInteger(0);

    private ProsessTaskTjeneste prosessTaskTjeneste = mock(ProsessTaskTjeneste.class);
    private JournalpostRepository journalpostRepository = mock(JournalpostRepository.class);
    private MottattMeldingTjeneste meldingTjeneste = mock(MottattMeldingTjeneste.class);
    private FordelProsessTaskTjeneste fordelProsessTaskTjeneste;

    @Spy
    private IgnorertJournalpost ignorertJournalpost = new IgnorertJournalpost();
    @Spy
    private AlleredeMottattJournalpost alleredeMottattJournalpost = new AlleredeMottattJournalpost(journalpostRepository);
    @Spy
    private StrukturertJournalpost strukturertJournalpost = new StrukturertJournalpost(true, true);
    @Spy
    private UhåndtertJournalpost uhåndtertJournalpost = new UhåndtertJournalpost();

    @Mock
    private ArkivTjeneste arkivTjeneste;

    static String hent(String prefix, String navn, String suffix) {
        InputStream inputStream = HentDataFraJoarkAktivitetspengerTaskTest.class.getClassLoader().getResourceAsStream(String.format("%s/%s.%s", prefix, navn, suffix));
        if (inputStream == null)
            throw new IllegalArgumentException("Finner ikke '" + navn + "'.");
        try {
            return IOUtils.toString(inputStream, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalArgumentException("Feil ved lesing av json '" + navn + "'", e);
        }
    }

    @BeforeEach
    public void beforeEach() {
        fordelProsessTaskTjeneste = new FordelProsessTaskTjeneste(prosessTaskTjeneste, meldingTjeneste);
        reset(ignorertJournalpost, alleredeMottattJournalpost, strukturertJournalpost, uhåndtertJournalpost, journalpostRepository, arkivTjeneste);
    }

    @Test
    void skal_ikke_behandle_dersom_allerede_mottatt() {
        var melding = mottattMeldingFraNavNo("aktivitetspenger/gyldigSøknad");
        //TODO tema for aktivitetspenger
        melding.setTema(OmrådeTema.UNG);
        melding.setBrevkode(Brevkode.AKTIVITETSPENGER_SOKNAD.getOffisiellKode());

        var task = initTask(melding, Journalstatus.MOTTATT);

        // allerede mottatt
        when(journalpostRepository.finnJournalpostMottatt(any())).thenReturn(Optional.of(mock(JournalpostMottattEntitet.class)));

        var nyMelding = task.doTask(melding);

        assertThat(nyMelding).isNull();
        verifyBlittVurdertAv(alleredeMottattJournalpost);
        verifyNoInteractions(strukturertJournalpost, uhåndtertJournalpost);
        verifyIkkeLagretIJournalpostRepository();
    }

    @Test
    void skal_route_aktivitetspenger() {
        var melding = mottattMeldingFraNavNo("aktivitetspenger/gyldigSøknad");
        //TODO tema for aktivitetspenger
        melding.setTema(OmrådeTema.UNG);
        melding.setBrevkode(Brevkode.AKTIVITETSPENGER_SOKNAD.getOffisiellKode());

        var task = initTask(melding, Journalstatus.MOTTATT);

        var nyMelding = task.doTask(melding);

        assertThat(nyMelding.getProsessTaskData().getTaskType()).isEqualTo(VurderStrukturertDokumentTask.TASKTYPE);
        verifyBlittVurdertAv(strukturertJournalpost);
        verifyNoInteractions(uhåndtertJournalpost);
        verifyLagretIJournalpostRepository();
    }

    private JournalpostInfo journalpostInfoFraMelding(MottattMelding melding, Journalstatus journalstatus) {
        var journalpostInfo = new JournalpostInfo();
        journalpostInfo.setIdent(melding.getAktørId().map(AktørId::new).orElse(null));
        journalpostInfo.setStrukturertPayload(melding.getPayloadAsString().orElse(null));
        journalpostInfo.setForsendelseTidspunkt(melding.getForsendelseMottattTidspunkt().orElseThrow());
        journalpostInfo.setBrevkode(Optional.ofNullable(melding.getBrevkode()).orElse(Brevkode.AKTIVITETSPENGER_SOKNAD_KODE));
        journalpostInfo.setTema(no.nav.k9.felles.integrasjon.saf.Tema.valueOf(melding.getTema().getKode()));
        journalpostInfo.setJournalstatus(journalstatus);
        return journalpostInfo;
    }

    private HentDataFraJoarkTask initTask(MottattMelding melding, Journalstatus journalstatus) {
        return initTask(journalpostInfoFraMelding(melding, journalstatus));
    }

    private HentDataFraJoarkTask initTask(JournalpostInfo journalpostInfo) {
        when(arkivTjeneste.hentJournalpostInfo(any())).thenReturn(journalpostInfo);
        var task = new HentDataFraJoarkTask(fordelProsessTaskTjeneste, arkivTjeneste, journalpostRepository,
                strukturertJournalpost, alleredeMottattJournalpost, ignorertJournalpost, uhåndtertJournalpost);
        return task;
    }

    private void verifyBlittVurdertAv(Journalpostvurderer journalpostvurderer) {
        inOrder(journalpostvurderer).verify(journalpostvurderer, calls(1)).vurder(any(), any());
    }

    private void verifyLagretIJournalpostRepository() {
        inOrder(journalpostRepository).verify(journalpostRepository, calls(1)).lagreMottatt(any());
    }

    private void verifyIkkeLagretIJournalpostRepository() {
        inOrder(journalpostRepository).verify(journalpostRepository, never()).lagreMottatt(any());
    }

    private MottattMelding mottattMeldingFraNavNo(String navn) {
        var mm = new MottattMelding(new ProsessTaskData(HentDataFraJoarkTask.class).medSekvens("1"));

        mm.setJournalPostId(new JournalpostId("journalpost-" + counter.incrementAndGet()));
        mm.setPayload(hent("søknader", navn, "json"));
        mm.setAktørId("min-aktørid");
        mm.setForsendelseMottattTidspunkt(LocalDateTime.now());
        return mm;
    }
}
