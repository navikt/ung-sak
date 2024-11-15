package no.nav.ung.sak.mottak.dokumentmottak;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.kodeverk.dokument.Brevkode;
import no.nav.k9.kodeverk.dokument.DokumentStatus;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.motattdokument.MottattDokument;
import no.nav.ung.sak.behandlingslager.behandling.motattdokument.MottatteDokumentRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingLåsRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.ung.sak.behandlingslager.task.UnderBehandlingProsessTask;
import no.nav.ung.sak.domene.arbeidsforhold.InntektsmeldingTjeneste;
import no.nav.ung.sak.mottak.inntektsmelding.InntektsmeldingParser;
import no.nav.ung.sak.typer.JournalpostId;

/**
 * lagrer inntektsmeldinger til abakus asynk i egen task.
 */
@ApplicationScoped
@ProsessTask(LagreMottattInntektsmeldingerTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = true)
public class LagreMottattInntektsmeldingerTask extends UnderBehandlingProsessTask {

    private static final Logger log = LoggerFactory.getLogger(LagreMottattInntektsmeldingerTask.class);

    static final String MOTTATT_DOKUMENT = "mottatt.dokument";
    static final String TASKTYPE = "lagre.inntektsmeldinger.til.abakus";

    private final InntektsmeldingParser inntektsmeldingParser = new InntektsmeldingParser();
    private InntektsmeldingTjeneste inntektsmeldingTjeneste;
    private MottatteDokumentRepository mottatteDokumentRepository;

    LagreMottattInntektsmeldingerTask() {
        // for proxy
    }

    @Inject
    public LagreMottattInntektsmeldingerTask(BehandlingRepository behandlingRepository,
                                             BehandlingLåsRepository behandlingLåsRepository,
                                             InntektsmeldingTjeneste inntektsmeldingTjeneste,
                                             MottatteDokumentRepository mottatteDokumentRepository) {
        super(behandlingRepository, behandlingLåsRepository);
        this.inntektsmeldingTjeneste = inntektsmeldingTjeneste;
        this.mottatteDokumentRepository = mottatteDokumentRepository;
    }

    @Override
    protected void doProsesser(ProsessTaskData input, Behandling behandling) {

        var fagsakId = behandling.getFagsakId();
        var behandlingId = behandling.getId();
        var saksnummer = behandling.getFagsak().getSaksnummer();

        var mottatteDokumenter = new ArrayList<MottattDokument>();

        gammelPlukkingAvInntektsmeldingTilLagring(input, fagsakId, mottatteDokumenter);

        // ny - henter alle som er til BEHANDLER
        List<MottattDokument> mottatteDokumentBehandler = mottatteDokumentRepository
            .hentMottatteDokumentForBehandling(fagsakId, behandlingId, List.of(Brevkode.INNTEKTSMELDING), true, DokumentStatus.BEHANDLER);
        mottatteDokumenter.addAll(mottatteDokumentBehandler);

        if (mottatteDokumenter.isEmpty()) {
            log.info("Fant ingen inntektsmeldinger å lagre nå - er allerede håndtert. Avbryter task");
            return;
        }

        var inntektsmeldinger = inntektsmeldingParser.parseInntektsmeldinger(mottatteDokumenter);

        mottatteDokumentRepository.oppdaterStatus(mottatteDokumenter, DokumentStatus.GYLDIG);

        // må gjøres til slutt siden vi har å gjøre med et ikke-tx grensesnitt til abakus
        inntektsmeldingTjeneste.lagreInntektsmeldinger(saksnummer, behandlingId, inntektsmeldinger);

    }

    /** @deprecated fjern denne, les alle fra status BEHANDLER i stedet. */
    @Deprecated(forRemoval = true)
    private void gammelPlukkingAvInntektsmeldingTilLagring(ProsessTaskData input, Long fagsakId, ArrayList<MottattDokument> mottatteDokumenter) {
        String dokumenter = input.getPropertyValue(MOTTATT_DOKUMENT);

        // TODO fjern denne - skal alltid lese fra mottatt_dokument status BEHANDLER
        Collection<JournalpostId> journalpostIder = dokumenter == null || dokumenter.isEmpty() ? Collections.emptyList()
            : Arrays.stream(dokumenter.split(",")).map(JournalpostId::new).collect(Collectors.toCollection(LinkedHashSet::new));
        mottatteDokumenter.addAll(mottatteDokumentRepository.hentMottatteDokument(fagsakId, journalpostIder, DokumentStatus.MOTTATT, DokumentStatus.GYLDIG));
    }

}
