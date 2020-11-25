package no.nav.k9.sak.mottak.inntektsmelding;

import java.util.Collection;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.dokument.Brevkode;
import no.nav.k9.sak.behandling.prosessering.task.StartBehandlingTask;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.BehandlingÅrsak;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingLåsRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRevurderingRepository;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.mottak.Behandlingsoppretter;
import no.nav.k9.sak.mottak.dokumentmottak.DokumentGruppeRef;
import no.nav.k9.sak.mottak.dokumentmottak.Dokumentmottaker;
import no.nav.k9.sak.mottak.dokumentmottak.HistorikkinnslagTjeneste;
import no.nav.k9.sak.mottak.dokumentmottak.Kompletthetskontroller;
import no.nav.k9.sak.mottak.dokumentmottak.MottatteDokumentTjeneste;
import no.nav.k9.sak.mottak.repo.MottattDokument;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;

@ApplicationScoped
@FagsakYtelseTypeRef
@DokumentGruppeRef(Brevkode.INNTEKTSMELDING_KODE)
public class DokumentmottakerInntektsmelding implements Dokumentmottaker {

    private Behandlingsoppretter behandlingsoppretter;
    private Kompletthetskontroller kompletthetskontroller;
    private MottatteDokumentTjeneste mottatteDokumentTjeneste;
    private BehandlingRevurderingRepository revurderingRepository;
    private BehandlingRepository behandlingRepository;
    private HistorikkinnslagTjeneste historikkinnslagTjeneste;
    private ProsessTaskRepository prosessTaskRepository;

    private BehandlingLåsRepository behandlingLåsRepository;

    private InntektsmeldingParser inntektsmeldingParser = new InntektsmeldingParser();

    DokumentmottakerInntektsmelding() {
        // for CDI
    }

    @Inject
    public DokumentmottakerInntektsmelding(MottatteDokumentTjeneste mottatteDokumentTjeneste,
                                           Behandlingsoppretter behandlingsoppretter,
                                           Kompletthetskontroller kompletthetskontroller,
                                           BehandlingRepositoryProvider repositoryProvider,
                                           HistorikkinnslagTjeneste historikkinnslagTjeneste,
                                           ProsessTaskRepository prosessTaskRepository) {
        this.mottatteDokumentTjeneste = mottatteDokumentTjeneste;
        this.behandlingsoppretter = behandlingsoppretter;
        this.kompletthetskontroller = kompletthetskontroller;

        this.revurderingRepository = repositoryProvider.getBehandlingRevurderingRepository();
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.behandlingLåsRepository = repositoryProvider.getBehandlingLåsRepository();
        this.historikkinnslagTjeneste = historikkinnslagTjeneste;
        this.prosessTaskRepository = prosessTaskRepository;
    }

    @Override
    public void mottaDokument(Collection<MottattDokument> mottattDokument, Fagsak fagsak) {
        doMottaDokument(fagsak, mottattDokument);
    }

    @Override
    public void validerDokument(MottattDokument mottattDokument, FagsakYtelseType ytelseType) {
        inntektsmeldingParser.parseInntektsmeldinger(mottattDokument);
    }

    void oppdaterÅpenBehandlingMedDokument(Behandling behandling, Collection<MottattDokument> mottattDokument) { // #I2
        mottattDokument.forEach(m -> historikkinnslagTjeneste.opprettHistorikkinnslagForVedlegg(behandling.getFagsakId(), m.getJournalpostId(), m.getType()));
        leggTilBehandlingsårsak(behandling, getBehandlingÅrsakType());
        historikkinnslagTjeneste.opprettHistorikkinnslagForBehandlingOppdatertMedNyeOpplysninger(behandling, BehandlingÅrsakType.RE_OPPLYSNINGER_OM_INNTEKT);
        kompletthetskontroller.persisterDokumentOgVurderKompletthet(behandling, mottattDokument);
    }

    protected BehandlingÅrsakType getBehandlingÅrsakType() {
        return BehandlingÅrsakType.RE_ENDRET_INNTEKTSMELDING;
    }

    private void oppprettFørstegangsbehandling(Fagsak fagsak, Collection<MottattDokument> mottatteDokumenter) { // #I1
        // Opprett ny førstegangsbehandling
        Behandling behandling = behandlingsoppretter.opprettFørstegangsbehandling(fagsak, BehandlingÅrsakType.UDEFINERT, Optional.empty());
        mottatteDokumentTjeneste.persisterInntektsmeldingOgKobleMottattDokumentTilBehandling(behandling, mottatteDokumenter);
        opprettTaskForÅStarteBehandlingFraInntektsmelding(mottatteDokumenter, behandling);
        mottatteDokumenter.forEach(m -> historikkinnslagTjeneste.opprettHistorikkinnslagForVedlegg(behandling.getFagsakId(), m.getJournalpostId(), m.getType()));
    }

    private void opprettNyBehandling(Collection<MottattDokument> mottatteDokumenter, Behandling tidligereAvsluttetBehandling) {
        var sisteHenlagteFørstegangsbehandling = behandlingsoppretter.sisteHenlagteFørstegangsbehandling(tidligereAvsluttetBehandling.getFagsak());
        Behandling nyBehandling;
        if (sisteHenlagteFørstegangsbehandling.isPresent()) { // #I6
            nyBehandling = behandlingsoppretter.opprettNyFørstegangsbehandling(mottatteDokumenter, sisteHenlagteFørstegangsbehandling.get().getFagsak(), sisteHenlagteFørstegangsbehandling.get());
        } else {
            nyBehandling = behandlingsoppretter.opprettRevurdering(tidligereAvsluttetBehandling, getBehandlingÅrsakType());
        }
        mottatteDokumentTjeneste.persisterInntektsmeldingOgKobleMottattDokumentTilBehandling(nyBehandling, mottatteDokumenter);
        opprettTaskForÅStarteBehandlingFraInntektsmelding(mottatteDokumenter, nyBehandling);
        mottatteDokumenter.forEach(m -> historikkinnslagTjeneste.opprettHistorikkinnslagForVedlegg(nyBehandling.getFagsakId(), m.getJournalpostId(), m.getType()));
    }

    private void doMottaDokument(Fagsak fagsak, Collection<MottattDokument> mottatteDokumenter) {
        var sisteYtelsesbehandling = revurderingRepository.hentSisteYtelsesbehandling(fagsak.getId()).orElse(null);

        if (sisteYtelsesbehandling == null) {
            oppprettFørstegangsbehandling(fagsak, mottatteDokumenter);
            return;
        }

        sjekkBehandlingKanLåses(sisteYtelsesbehandling); // sjekker at kan låses (dvs ingen andre prosesserer den samtidig, hvis ikke kommer vi tilbake senere en gang)

        if (sisteYtelsesbehandling.erStatusFerdigbehandlet()) {
            var sisteAvsluttedeBehandling = behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsak.getId())
                .orElse(sisteYtelsesbehandling);
            opprettNyBehandling(mottatteDokumenter, sisteAvsluttedeBehandling);
        } else {
            oppdaterÅpenBehandlingMedDokument(sisteYtelsesbehandling, mottatteDokumenter);
        }
    }

    private void sjekkBehandlingKanLåses(Behandling behandling) {
        var lås = behandlingLåsRepository.taLåsHvisLedig(behandling.getId());
        if (lås == null) {
            // noen andre holder på siden vi ikke fikk fatt på lås, så avbryter denne gang
            throw MottattInntektsmeldingFeil.FACTORY.behandlingPågårAvventerKnytteMottattDokumentTilBehandling(behandling.getId()).toException();
        }
    }

    private void opprettTaskForÅStarteBehandlingFraInntektsmelding(Collection<MottattDokument> mottattDokument, Behandling behandling) {
        ProsessTaskData prosessTaskData = new ProsessTaskData(StartBehandlingTask.TASKTYPE);
        prosessTaskData.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());
        prosessTaskData.setCallIdFraEksisterende();
        prosessTaskRepository.lagre(prosessTaskData);
    }

    private void leggTilBehandlingsårsak(Behandling behandling, BehandlingÅrsakType behandlingÅrsak) {
        BehandlingÅrsak.Builder builder = BehandlingÅrsak.builder(behandlingÅrsak);
        builder.buildFor(behandling);

        BehandlingLås behandlingLås = behandlingRepository.taSkriveLås(behandling);
        behandlingRepository.lagre(behandling, behandlingLås);
    }
}
