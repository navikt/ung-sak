package no.nav.k9.sak.mottak.dokumentmottak;

import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.abakus.iaygrunnlag.IayGrunnlagJsonMapper;
import no.nav.abakus.iaygrunnlag.request.OppgittOpptjeningMottattRequest;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.kodeverk.dokument.Brevkode;
import no.nav.k9.kodeverk.dokument.DokumentStatus;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingLåsRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.k9.sak.behandlingslager.task.UnderBehandlingProsessTask;
import no.nav.k9.sak.domene.abakus.AbakusTjeneste;
import no.nav.k9.sak.mottak.repo.MottattDokument;
import no.nav.k9.sak.mottak.repo.MottatteDokumentRepository;
import no.nav.k9.sak.typer.JournalpostId;

@ApplicationScoped
@ProsessTask(AsyncAbakusLagreOpptjeningTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = true)
public class AsyncAbakusLagreOpptjeningTask extends UnderBehandlingProsessTask {

    private static final Logger logger = LoggerFactory.getLogger(AsyncAbakusLagreOpptjeningTask.class);

    public static final String TASKTYPE = "abakus.async.lagreopptjening";

    public static final String JOURNALPOST_ID = "opptjening.journalpostId";
    public static final String BREVKODER = "opptjening.brevkoder";

    private AbakusTjeneste abakusTjeneste;
    private MottatteDokumentRepository mottatteDokumentRepository;
    private Boolean lansert;

    AsyncAbakusLagreOpptjeningTask() {
        // for proxy
    }

    @Inject
    AsyncAbakusLagreOpptjeningTask(BehandlingRepository behandlingRepository,
                                   BehandlingLåsRepository behandlingLåsRepository,
                                   AbakusTjeneste abakusTjeneste,
                                   MottatteDokumentRepository mottatteDokumentRepository,
                                   @KonfigVerdi(value = "MOTTAK_SOKNAD_UTBETALING_OMS", defaultVerdi = "true") Boolean lansert) {
        super(behandlingRepository, behandlingLåsRepository);
        this.abakusTjeneste = abakusTjeneste;
        this.mottatteDokumentRepository = mottatteDokumentRepository;
        this.lansert = lansert;
    }

    @Override
    protected void doProsesser(ProsessTaskData input, Behandling behandling) {
        var erFrisinn = input.getPropertyValue(JOURNALPOST_ID) == null;
        if (erFrisinn) {
            // TODO: Når Frisinn utfaset skal det ikke være valgfritt å oppgi journalpostid
            lagreOppgittOpptjening(input, true);
        } else  {
            JournalpostId journalpostId = new JournalpostId(input.getPropertyValue(JOURNALPOST_ID));
            List<Brevkode> brevkoder = getBrevkoder(input.getPropertyValue(BREVKODER));

            //henter alle som er til BEHANDLER
            List<MottattDokument> ubehandledeDokumenter = mottatteDokumentRepository.hentMottatteDokumentForBehandling(behandling.getFagsakId(), behandling.getId(),
                brevkoder, true, DokumentStatus.BEHANDLER);
            if (ubehandledeDokumenter.isEmpty()) {
                logger.info("Fant ingen ubehandlede søknader om utbetaling av omsorgspenger nå - er allerede håndtert. Avbryter task");
                return;
            }

            MottattDokument førsteUbehandledeDokument = ubehandledeDokumenter.stream()
                .min(Comparator.comparing(MottattDokument::getMottattTidspunkt)).orElseThrow();
            if (!førsteUbehandledeDokument.getJournalpostId().equals(journalpostId)) {
                throw new UnsupportedOperationException("Kan ikke lagre oppgitt opptjening. JournalpostId=" + journalpostId +" " +
                    "venter på behandling av tidligere journalpostId=" + førsteUbehandledeDokument.getJournalpostId());
            }
            mottatteDokumentRepository.oppdaterStatus(List.of(førsteUbehandledeDokument), DokumentStatus.GYLDIG);
            // må gjøres til slutt siden vi har å gjøre med et ikke-tx grensesnitt til abakus
            lagreOppgittOpptjening(input, erFrisinn);
        }
    }

    private List<Brevkode> getBrevkoder(String brevkoderStr) {
        if (brevkoderStr != null && !(brevkoderStr = brevkoderStr.trim()).isEmpty()) {
            return Arrays.asList(brevkoderStr.split(",\\s*")).stream().map(Brevkode::fraKode).collect(Collectors.toList());
        } else {
            throw new IllegalArgumentException("Ingen brevkoder oppgitt for dokument for oppgitt opptjening");
        }
    }

    private void lagreOppgittOpptjening(ProsessTaskData input, boolean erFrisinn) {
        var jsonReader = IayGrunnlagJsonMapper.getMapper().readerFor(OppgittOpptjeningMottattRequest.class);

        try {
            OppgittOpptjeningMottattRequest request = jsonReader.readValue(Objects.requireNonNull(input.getPayloadAsString(), "mangler payload"));
            if (!lansert || erFrisinn) {
                abakusTjeneste.lagreOppgittOpptjening(request);
            } else {
                abakusTjeneste.lagreOppgittOpptjeningV2(request);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Kunne ikke lagre abakus oppgitt opptjening", e);
        }
    }
}
