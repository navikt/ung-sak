package no.nav.k9.sak.mottak.dokumentmottak;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import no.nav.k9.kodeverk.dokument.DokumentStatus;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.domene.iay.modell.InntektsmeldingBuilder;
import no.nav.k9.sak.mottak.inntektsmelding.InntektsmeldingParser;
import no.nav.k9.sak.mottak.repo.MottattDokument;
import no.nav.k9.sak.mottak.repo.MottatteDokumentRepository;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;

@Dependent
public class MottatteDokumentTjeneste {

    private final InntektsmeldingParser inntektsmeldingParser = new InntektsmeldingParser();

    private MottatteDokumentRepository mottatteDokumentRepository;
    private BehandlingRepository behandlingRepository;
    private ProsessTaskRepository prosessTaskRepository;

    @Inject
    public MottatteDokumentTjeneste(MottatteDokumentRepository mottatteDokumentRepository,
                                    ProsessTaskRepository prosessTaskRepository,
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

        var inntektsmeldinger = inntektsmeldingParser.parseInntektsmeldinger(dokumenter);
        if (inntektsmeldinger.size() != 1) {
            throw new IllegalStateException("Forventet 1 inntektsmelding, men har " + inntektsmeldinger.size());
        }
        for (var dokument : dokumenter) {
            InntektsmeldingBuilder im = inntektsmeldinger.get(0); // sendte bare ett dokument her, så forventer kun et svar
            var arbeidsgiver = im.getArbeidsgiver(); // NOSONAR
            dokument.setArbeidsgiver(arbeidsgiver.getIdentifikator());
            dokument.setBehandlingId(behandlingId);
            dokument.setInnsendingstidspunkt(im.getInnsendingstidspunkt());
            dokument.setKildesystem(im.getKildesystem());

            mottatteDokumentRepository.lagre(dokument, DokumentStatus.BEHANDLER);// oppdateres senere til GYLDIG når er lagret i Abakus
        }

        //TODO journalpostId på prosesstasken er død kode. Beholder for nå i tilfelle det skulle være nødvendig å rulle tilbake til logikk som ikke henter dokumenter kun basert på BEHANDLER-statusen
        //TODO kan fjernes etter 15.februar 2021
        var journalpostder = dokumenter.stream().map(MottattDokument::getJournalpostId).collect(Collectors.toCollection(LinkedHashSet::new));

        lagreInntektsmeldinger(behandlingId, journalpostder);
    }

    /**
     * Lagrer inntektsmeldinger til abakus fra mottatt dokument.
     */
    private void lagreInntektsmeldinger(Long behandlingId, Collection<JournalpostId> mottatteDokumenter) {

        var behandling = behandlingRepository.hentBehandling(behandlingId);
        AktørId aktørId = behandling.getAktørId();
        var saksnummer = behandling.getFagsak().getSaksnummer();

        var enkeltTask = new ProsessTaskData(LagreMottattInntektsmeldingerTask.TASKTYPE);
        enkeltTask.setBehandling(behandling.getFagsakId(), behandlingId, aktørId.getId());
        enkeltTask.setSaksnummer(saksnummer.getVerdi());
        enkeltTask.setCallIdFraEksisterende();
        List<String> journalpostIder = mottatteDokumenter.stream().map(j -> j.getVerdi()).collect(Collectors.toList());
        enkeltTask.setProperty(LagreMottattInntektsmeldingerTask.MOTTATT_DOKUMENT, String.join(",", journalpostIder));
        prosessTaskRepository.lagre(enkeltTask);
    }

    Long lagreMottattDokumentPåFagsak(MottattDokument dokument) {
        MottattDokument mottattDokument = mottatteDokumentRepository.lagre(dokument, DokumentStatus.MOTTATT);
        return mottattDokument.getId();
    }

    List<MottattDokument> hentMottatteDokumentPåFagsak(long fagsakId, boolean taSkriveLås, DokumentStatus... statuser) {
        return mottatteDokumentRepository.hentMottatteDokumentMedFagsakId(fagsakId, taSkriveLås, statuser);
    }

    void oppdaterStatus(List<MottattDokument> mottatteDokumenter, DokumentStatus nyStatus) {
        mottatteDokumentRepository.oppdaterStatus(mottatteDokumenter, nyStatus);
    }

}
