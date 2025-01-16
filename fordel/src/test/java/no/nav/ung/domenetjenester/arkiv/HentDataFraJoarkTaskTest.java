package no.nav.ung.domenetjenester.arkiv;


import no.nav.k9.felles.exception.TekniskException;
import no.nav.k9.felles.integrasjon.saf.Journalstatus;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.ung.domenetjenester.arkiv.journalpostvurderer.*;
import no.nav.ung.fordel.handler.FordelProsessTaskTjeneste;
import no.nav.ung.fordel.handler.MottattMelding;
import no.nav.ung.fordel.handler.MottattMeldingTjeneste;
import no.nav.ung.fordel.repo.journalpost.JournalpostMottattEntitet;
import no.nav.ung.fordel.repo.journalpost.JournalpostRepository;
import no.nav.ung.kodeverk.produksjonsstyring.OmrådeTema;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.JournalpostId;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Assertions;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class HentDataFraJoarkTaskTest {

    private static final String EN_BREVKODE = "4936" /* inntektsmelding */;

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
    private StrukturertJournalpost strukturertJournalpost = new StrukturertJournalpost(true);
    @Spy
    private UhåndtertJournalpost uhåndtertJournalpost = new UhåndtertJournalpost();

    @Mock
    private ArkivTjeneste arkivTjeneste;

    static String hent(String prefix, String navn, String suffix) {
        InputStream inputStream = HentDataFraJoarkTaskTest.class.getClassLoader().getResourceAsStream(String.format("%s/%s.%s", prefix, navn, suffix));
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
        var melding = mottattMeldingFraNavNo("ungdomsytelse/gyldigSøknad");
        melding.setTema(OmrådeTema.OMS);
        melding.setBrevkode(no.nav.ung.kodeverk.dokument.Brevkode.UNGDOMSYTELSE_SOKNAD.getOffisiellKode());

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
    void skal_route_ungdomsytelsen() {
        var melding = mottattMeldingFraNavNo("ungdomsytelse/gyldigSøknad");
        melding.setTema(OmrådeTema.OMS); // TODO: Bruk UNG når det er støttet
        melding.setBrevkode(no.nav.ung.kodeverk.dokument.Brevkode.UNGDOMSYTELSE_SOKNAD.getOffisiellKode());

        var task = initTask(melding, Journalstatus.MOTTATT);

        var nyMelding = task.doTask(melding);

        assertThat(nyMelding.getProsessTaskData().getTaskType()).isEqualTo(VurderStrukturertDokumentTask.TASKTYPE);
        verifyBlittVurdertAv(strukturertJournalpost);
        verifyNoInteractions(uhåndtertJournalpost);
        verifyLagretIJournalpostRepository();
    }

    @Test
    void journalføringshendelse_som_er_mottatt_men_journalført_i_arkiv_skal_ikke_håndteres() {
        var melding = papirsøknad("NAV 09-11.05");
        melding.setJournalføringHendelsetype(JournalføringHendelsetype.MOTTATT);
        var task = initTask(melding, Journalstatus.JOURNALFOERT);
        var nyMelding = task.doTask(melding);
        verifyBlittVurdertAv(ignorertJournalpost);
        verifyNoInteractions(uhåndtertJournalpost);
        assertThat(nyMelding).isNull();
        verifyIkkeLagretIJournalpostRepository();
    }

    @Test
    void journalføringshendelse_som_har_endret_tema_etter_at_den_er_mottatt_med_tema_oms_skal_ikke_håndteres() {
        var melding = papirsøknad("NAV 09-11.05");
        TekniskException cause = new TekniskException("F-240613", "F-240613:Kunne ikke hente informasjon for query mot SAF: http://saf.teamdokumenthandtering/rest/hentdokument/666586623/683701390/ORIGINAL, HTTP status=HTTP/1.1 403 . HTTP Errormessage={\"timestamp\":\"2024-06-28T05:30:00.334+00:00\",\"status\":403,\"error\":\"Forbidden\",\"message\":\"Avvist av SAF tilgangskontroll: Tilgang til ressurs (journalpost/dokument) ble avvist. System har ikke tilgang til tema ressursen tilhører. \"tema_gru\" må ligge i feltet \"roles\" i Azure IAC-konfigurasjonen for konsumenten i saf sin nais-konfigurasjon. Hvis dette ikke fungerer; kontakt oss på #team_dokumentløsninger\",\"path\":\"/rest/hentdokument/666586623/683701390/ORIGINAL\"}");
        TekniskException tekniskException = new TekniskException("F-240613", "F-240613:Forespørsel til SAF feilet for spørring HentDokumentQuery", cause);
        var task = initTask(tekniskException);
        var nyMelding = task.doTask(melding);
        verifyBlittVurdertAv(ignorertJournalpost);
        verifyNoInteractions(uhåndtertJournalpost);
        assertThat(nyMelding).isNull();
        verifyIkkeLagretIJournalpostRepository();
    }

    @Test
    void journalføringshendelse_som_feiler_pga_andre_tekniske_feil_feile_videre() {
        var melding = papirsøknad("NAV 09-11.05");
        var task = initTask(new TekniskException("F-240613", "F-240613:Kunne ikke hente informasjon for query mot SAF: http://saf.teamdokumenthandtering/rest/hentdokument/666586623/683701390/ORIGINAL, HTTP status=HTTP/1.1 403 . HTTP Errormessage={\"timestamp\":\"2024-06-28T05:30:00.334+00:00\",\"status\":403,\"error\":\"Forbidden\",\"message\":\"Ukjent feil. Hvis dette ikke fungerer; kontakt oss på #team_dokumentløsninger\",\"path\":\"/rest/hentdokument/666586623/683701390/ORIGINAL\"}"));
        Assertions.assertThrows(TekniskException.class, () -> task.doTask(melding));
        verifyNoInteractions(ignorertJournalpost);
        verifyNoInteractions(uhåndtertJournalpost);
        verifyIkkeLagretIJournalpostRepository();
    }

    private JournalpostInfo journalpostInfoFraMelding(MottattMelding melding, Journalstatus journalstatus) {
        var journalpostInfo = new JournalpostInfo();
        journalpostInfo.setIdent(melding.getAktørId().map(AktørId::new).orElse(null));
        journalpostInfo.setStrukturertPayload(melding.getPayloadAsString().orElse(null));
        journalpostInfo.setForsendelseTidspunkt(melding.getForsendelseMottattTidspunkt().orElseThrow());
        journalpostInfo.setBrevkode(Optional.ofNullable(melding.getBrevkode()).orElse(EN_BREVKODE));
        // TODO: no.nav.k9.felles.integrasjon.saf.Tema trenger å støtte UNG
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

    private HentDataFraJoarkTask initTask(TekniskException tekniskException) {
        when(arkivTjeneste.hentJournalpostInfo(any())).thenThrow(tekniskException);
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

    private MottattMelding papirsøknad(String brevkode) {
        var mm = new MottattMelding(new ProsessTaskData(HentDataFraJoarkTask.class).medSekvens("1"));
        mm.setJournalPostId(new JournalpostId("journalpost-" + counter.incrementAndGet()));
        mm.setBrevkode(brevkode);
        mm.setTema(OmrådeTema.OMS);
        mm.setForsendelseMottattTidspunkt(LocalDateTime.now());
        mm.setJournalføringHendelsetype(JournalføringHendelsetype.MOTTATT);
        return mm;
    }

}
