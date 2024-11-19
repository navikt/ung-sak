package no.nav.ung.sak.mottak.dokumentmottak;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.dokument.DokumentStatus;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.motattdokument.MottattDokument;
import no.nav.ung.sak.behandlingslager.behandling.motattdokument.MottatteDokumentRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.ung.sak.domene.iay.modell.InntektsmeldingBuilder;
import no.nav.ung.sak.mottak.inntektsmelding.InntektsmeldingParser;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.JournalpostId;

@Dependent
public class MottatteDokumentTjeneste {

    private static final Logger logger = LoggerFactory.getLogger(MottatteDokumentTjeneste.class);

    private final InntektsmeldingParser inntektsmeldingParser = new InntektsmeldingParser();

    private MottatteDokumentRepository mottatteDokumentRepository;
    private BehandlingRepository behandlingRepository;
    private ProsessTaskTjeneste prosessTaskRepository;

    @Inject
    public MottatteDokumentTjeneste(MottatteDokumentRepository mottatteDokumentRepository,
                                    ProsessTaskTjeneste prosessTaskRepository,
                                    BehandlingRepositoryProvider behandlingRepositoryProvider) {
        this.mottatteDokumentRepository = mottatteDokumentRepository;
        this.prosessTaskRepository = prosessTaskRepository;
        this.behandlingRepository = behandlingRepositoryProvider.getBehandlingRepository();
    }

    public void persisterInntektsmeldingForBehandling(Behandling behandling, Collection<MottattDokument> dokumenter) {
        if (dokumenter.stream().noneMatch(MottattDokument::harPayload)) {
            return; // quick return
        }
        Long behandlingId = behandling.getId();

        for (var dokument : dokumenter) {
            var inntektsmeldinger = inntektsmeldingParser.parseInntektsmeldinger(dokument);
            if (inntektsmeldinger.size() != 1) {
                throw new IllegalStateException("Forventet 1 inntektsmelding, men har " + inntektsmeldinger.size());
            }
            InntektsmeldingBuilder im = inntektsmeldinger.get(0); // sendte bare ett dokument her, så forventer kun et svar
            var arbeidsgiver = im.getArbeidsgiver(); // NOSONAR
            dokument.setArbeidsgiver(arbeidsgiver.getIdentifikator());
            dokument.setBehandlingId(behandlingId);
            dokument.setInnsendingstidspunkt(im.getInnsendingstidspunkt());
            dokument.setKildesystem(im.getKildesystem());

            mottatteDokumentRepository.oppdater(dokument);// lagrer, endrer ikke status
        }

        var journalpostder = dokumenter.stream().map(MottattDokument::getJournalpostId)
            .collect(Collectors.toCollection(LinkedHashSet::new));

        lagreInntektsmeldinger(behandlingId, journalpostder);
    }

    /**
     * Lagrer inntektsmeldinger til abakus fra mottatt dokument.
     */
    private void lagreInntektsmeldinger(Long behandlingId, Collection<JournalpostId> mottatteDokumenter) {

        var behandling = behandlingRepository.hentBehandling(behandlingId);
        AktørId aktørId = behandling.getAktørId();
        var saksnummer = behandling.getFagsak().getSaksnummer();

        var enkeltTask = ProsessTaskData.forProsessTask(LagreMottattInntektsmeldingerTask.class);
        enkeltTask.setBehandling(behandling.getFagsakId(), behandlingId, aktørId.getId());
        enkeltTask.setSaksnummer(saksnummer.getVerdi());
        enkeltTask.setCallIdFraEksisterende();
        List<String> journalpostIder = mottatteDokumenter.stream().map(j -> j.getVerdi()).sorted().collect(Collectors.toList());
        enkeltTask.setProperty(LagreMottattInntektsmeldingerTask.MOTTATT_DOKUMENT, String.join(",", journalpostIder));
        prosessTaskRepository.lagre(enkeltTask);
    }

    Long lagreMottattDokumentPåFagsak(MottattDokument dokument) {
        var eksisterende = mottatteDokumentRepository.hentMottatteDokument(dokument.getFagsakId(), dokument.getJournalpostId() != null ? List.of(dokument.getJournalpostId()) : List.of())
            .stream()
            .filter(it -> Objects.equals(it.getType(), dokument.getType()))
            .findFirst();
        if (eksisterende.isPresent()) {
            logger.info("Dokument med journalpostId {} er allerede lagret på fagsak.", dokument.getJournalpostId());
            return eksisterende.get().getId();
        }

        DokumentStatus nyStatus = DokumentStatus.MOTTATT;
        if (dokument.getStatus() == DokumentStatus.UGYLDIG) {
            logger.info("Mottok ugyldig dokument med jounalpostId={} på fagsak={}", dokument.getJournalpostId().getVerdi(), dokument.getFagsakId());
            nyStatus = DokumentStatus.UGYLDIG;
        }
        MottattDokument mottattDokument = mottatteDokumentRepository.lagre(dokument, nyStatus);
        return mottattDokument.getId();
    }

    List<MottattDokument> hentMottatteDokumentPåFagsak(long fagsakId, boolean taSkriveLås, DokumentStatus... statuser) {
        return mottatteDokumentRepository.hentMottatteDokumentMedFagsakId(fagsakId, taSkriveLås, statuser);
    }

    void oppdaterStatus(List<MottattDokument> mottatteDokumenter, DokumentStatus nyStatus) {
        mottatteDokumentRepository.oppdaterStatus(mottatteDokumenter, nyStatus);
    }

}
