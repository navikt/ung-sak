package no.nav.k9.sak.mottak.dokumentmottak;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingLåsRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.k9.sak.behandlingslager.task.UnderBehandlingProsessTask;
import no.nav.k9.sak.domene.arbeidsforhold.InntektsmeldingTjeneste;
import no.nav.k9.sak.mottak.inntektsmelding.InntektsmeldingParser;
import no.nav.k9.sak.mottak.repo.MottatteDokumentRepository;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

/**
 * lagrer inntektsmeldinger til abakus asynk i egen task.
 */
@ApplicationScoped
@ProsessTask(LagreMottattInntektsmeldingerTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = true)
public class LagreMottattInntektsmeldingerTask extends UnderBehandlingProsessTask {
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

        var journalpostIder = Arrays.asList(input.getPropertyValue(MOTTATT_DOKUMENT).split(",")).stream().map(s -> new JournalpostId(s)).collect(Collectors.toCollection(LinkedHashSet::new));

        var mottatteDokumenter = mottatteDokumentRepository.hentMottatteDokument(fagsakId, journalpostIder);

        var inntektsmeldinger = inntektsmeldingParser.parseInntektsmeldinger(mottatteDokumenter);

        inntektsmeldingTjeneste.lagreInntektsmeldinger(saksnummer, behandlingId, inntektsmeldinger);

    }

}
