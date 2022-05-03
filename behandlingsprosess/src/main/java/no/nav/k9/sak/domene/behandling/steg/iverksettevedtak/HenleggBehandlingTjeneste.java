package no.nav.k9.sak.domene.behandling.steg.iverksettevedtak;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.k9.kodeverk.behandling.BehandlingResultatType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.dokument.Brevkode;
import no.nav.k9.kodeverk.dokument.DokumentMalType;
import no.nav.k9.kodeverk.dokument.DokumentStatus;
import no.nav.k9.kodeverk.historikk.HistorikkAktør;
import no.nav.k9.kodeverk.historikk.HistorikkinnslagType;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.k9.sak.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.k9.sak.behandlingslager.behandling.motattdokument.MottattDokument;
import no.nav.k9.sak.behandlingslager.behandling.motattdokument.MottatteDokumentRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakProsessTaskRepository;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.dokument.bestill.DokumentBestillerApplikasjonTjeneste;
import no.nav.k9.sak.historikk.HistorikkInnslagTekstBuilder;
import no.nav.k9.sak.kontrakt.dokument.BestillBrevDto;
import no.nav.k9.sak.produksjonsstyring.oppgavebehandling.task.OpprettOppgaveSendTilInfotrygdTask;

@ApplicationScoped
public class HenleggBehandlingTjeneste {

    private static final Logger log = LoggerFactory.getLogger(HenleggBehandlingTjeneste.class);

    private BehandlingRepository behandlingRepository;
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;
    private DokumentBestillerApplikasjonTjeneste dokumentBestillerApplikasjonTjeneste;
    private ProsessTaskTjeneste prosessTaskRepository;
    private SøknadRepository søknadRepository;
    private FagsakRepository fagsakRepository;
    private HistorikkRepository historikkRepository;
    private FagsakProsessTaskRepository fagsakProsessTaskRepository;
    private MottatteDokumentRepository mottatteDokumentRepository;
    private Instance<HenleggelsePostopsTjeneste> henleggelsePostopsTjenester;

    public HenleggBehandlingTjeneste() {
        // for CDI proxy
    }

    @Inject
    public HenleggBehandlingTjeneste(BehandlingRepositoryProvider repositoryProvider,
                                     BehandlingskontrollTjeneste behandlingskontrollTjeneste,
                                     DokumentBestillerApplikasjonTjeneste dokumentBestillerApplikasjonTjeneste,
                                     FagsakProsessTaskRepository fagsakProsessTaskRepository,
                                     ProsessTaskTjeneste prosessTaskRepository,
                                     MottatteDokumentRepository mottatteDokumentRepository,
                                     @Any Instance<HenleggelsePostopsTjeneste> henleggelsePostopsTjenester) {
        this.fagsakProsessTaskRepository = fagsakProsessTaskRepository;
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
        this.dokumentBestillerApplikasjonTjeneste = dokumentBestillerApplikasjonTjeneste;
        this.prosessTaskRepository = prosessTaskRepository;
        this.søknadRepository = repositoryProvider.getSøknadRepository();
        this.fagsakRepository = repositoryProvider.getFagsakRepository();
        this.historikkRepository = repositoryProvider.getHistorikkRepository();
        this.mottatteDokumentRepository = mottatteDokumentRepository;
        this.henleggelsePostopsTjenester = henleggelsePostopsTjenester;
    }

    public void henleggBehandlingAvSaksbehandler(String behandlingId, BehandlingResultatType årsakKode, String begrunnelse) {
        BehandlingskontrollKontekst kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(behandlingId);
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);

        if (Set.of(FagsakYtelseType.PSB, FagsakYtelseType.PPN).contains(behandling.getFagsakYtelseType())) {
            throw new IllegalArgumentException("Det er p.t. ikke støttet å henlegge behandlinger for fagsak " + behandling.getFagsakYtelseType().getNavn());
        }

        validerÅrsakMotKrav(årsakKode, behandling);

        var søknad = søknadRepository.hentSøknadHvisEksisterer(behandling.getId());
        if (søknad.isEmpty()) {
            // Må ta behandling av vent for å tillate henleggelse (krav i Behandlingskontroll)
            behandlingskontrollTjeneste.taBehandlingAvVentSetAlleAutopunktUtførtForHenleggelse(behandling, kontekst);
        } else {
            // har søknad og saksbehandler forsøker å ta av vent
        }

        fagsakProsessTaskRepository.ryddProsessTasks(behandling.getFagsakId(), behandling.getId());  // rydd tidligere tasks (eks. registerinnhenting, etc)

        doHenleggBehandling(behandlingId, årsakKode, begrunnelse, HistorikkAktør.SAKSBEHANDLER);
    }

    private void validerÅrsakMotKrav(BehandlingResultatType årsakKode, Behandling behandling) {
        if (Objects.equals(BehandlingResultatType.HENLAGT_FEILOPPRETTET, årsakKode)) {
            var gyldigeDokumenterPåBehandling = mottatteDokumentRepository.hentMottatteDokumentMedFagsakId(behandling.getFagsakId())
                .stream()
                .filter(it -> Objects.equals(it.getBehandlingId(), behandling.getId()))
                .toList();

            if (!gyldigeDokumenterPåBehandling.isEmpty()) {
                throw new IllegalStateException("Prøver å henlegge behandling som feilopprettet men det finnes gyldige/ubehandlede dokumenter på sak");
            }
        }
    }

    private void doHenleggBehandling(String behandlingId, BehandlingResultatType årsakKode, String begrunnelse, HistorikkAktør historikkAktør) {
        BehandlingskontrollKontekst kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(behandlingId);
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);

        behandlingskontrollTjeneste.henleggBehandling(kontekst, årsakKode);

        henleggDokumenter(behandling);

        if (BehandlingResultatType.HENLAGT_SØKNAD_TRUKKET.equals(årsakKode)) {
            sendHenleggelsesbrev(behandling.getId(), HistorikkAktør.VEDTAKSLØSNINGEN);
        } else if (BehandlingResultatType.MANGLER_BEREGNINGSREGLER.equals(årsakKode)) {
            fagsakRepository.fagsakSkalBehandlesAvInfotrygd(behandling.getFagsakId());
            opprettOppgaveTilInfotrygd(behandling);
        }
        lagHistorikkinnslagForHenleggelse(behandling.getId(), årsakKode, begrunnelse, historikkAktør);

        HenleggelsePostopsTjeneste.finnTjeneste(henleggelsePostopsTjenester, behandling.getFagsakYtelseType())
            .ifPresent(postOps -> postOps.utfør(behandling));
    }

    private void henleggDokumenter(Behandling behandling) {

        Set<Brevkode> kravdokumentTyper = new HashSet<>(Brevkode.SØKNAD_TYPER);

        if (Objects.equals(behandling.getFagsakYtelseType(), FagsakYtelseType.OMP)) {
            kravdokumentTyper.addAll(List.of(Brevkode.INNTEKTSMELDING, Brevkode.FRAVÆRSKORRIGERING_IM_OMS));
        }

        List<MottattDokument> gyldigeDokumenterFagsak = mottatteDokumentRepository.hentGyldigeDokumenterMedFagsakId(behandling.getFagsakId());
        List<MottattDokument> gyldigeKravdokumenterBehandling = gyldigeDokumenterFagsak.stream()
            .filter(dok -> behandling.getId().equals(dok.getBehandlingId()))
            .filter(dok -> kravdokumentTyper.contains(dok.getType()))
            .toList();

        log.info("Henlegger behandling og {} dokumenter", gyldigeDokumenterFagsak.size());
        for (MottattDokument kravdokument : gyldigeKravdokumenterBehandling) {
            log.info("Henlegger kravdokument med journalpostId {} og type {}", kravdokument.getJournalpostId().getVerdi(), kravdokument.getType().getKode());
            mottatteDokumentRepository.lagre(kravdokument, DokumentStatus.HENLAGT);
        }
    }

    /**
     * Henlegger helt - for forvaltning først og fremst.
     */
    public void henleggBehandlingOgAksjonspunkter(String behandlingId, BehandlingResultatType årsakKode, String begrunnelse) {
        BehandlingskontrollKontekst kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(behandlingId);
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);

        fagsakProsessTaskRepository.ryddProsessTasks(behandling.getFagsakId(), behandling.getId());  // rydd tidligere tasks (eks. registerinnhenting, etc)

        if (behandling.isBehandlingHenlagt()) {
            // er allerede henlagt - en saksbehandler kom oss i forkjøpet
            Fagsak fagsak = behandling.getFagsak();
            log.warn("Behandling [fagsakId={}, saksnummer={}, behandlingId={}, ytelseType={}] er allerede henlagt", fagsak.getId(), fagsak.getSaksnummer(), behandling.getId(), fagsak.getYtelseType());
            return;
        }

        if (behandling.isBehandlingPåVent()) {
            behandlingskontrollTjeneste.taBehandlingAvVentSetAlleAutopunktUtførtForHenleggelse(behandling, kontekst);
        }

        doHenleggBehandling(behandlingId, årsakKode, begrunnelse, HistorikkAktør.VEDTAKSLØSNINGEN);
    }

    private void opprettOppgaveTilInfotrygd(Behandling behandling) {
        ProsessTaskData data = ProsessTaskData.forProsessTask(OpprettOppgaveSendTilInfotrygdTask.class);
        data.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());
        data.setCallIdFraEksisterende();
        prosessTaskRepository.lagre(data);
    }

    private void sendHenleggelsesbrev(long behandlingId, HistorikkAktør aktør) {
        BestillBrevDto bestillBrevDto = new BestillBrevDto(behandlingId, DokumentMalType.HENLEGG_BEHANDLING_DOK);
        dokumentBestillerApplikasjonTjeneste.bestillDokument(bestillBrevDto, aktør);
    }

    public void lagHistorikkInnslagForHenleggelseFraSteg(Long behandlingId, BehandlingResultatType årsakKode, String begrunnelse) {
        lagHistorikkinnslagForHenleggelse(behandlingId, årsakKode, begrunnelse, HistorikkAktør.VEDTAKSLØSNINGEN);
    }

    private void lagHistorikkinnslagForHenleggelse(Long behandlingsId, BehandlingResultatType aarsak, String begrunnelse, HistorikkAktør aktør) {
        HistorikkInnslagTekstBuilder builder = new HistorikkInnslagTekstBuilder()
            .medHendelse(HistorikkinnslagType.AVBRUTT_BEH)
            .medÅrsak(aarsak)
            .medBegrunnelse(begrunnelse);
        Historikkinnslag historikkinnslag = new Historikkinnslag();
        historikkinnslag.setType(HistorikkinnslagType.AVBRUTT_BEH);
        historikkinnslag.setBehandlingId(behandlingsId);
        builder.build(historikkinnslag);

        historikkinnslag.setAktør(aktør);
        historikkRepository.lagre(historikkinnslag);
    }
}
