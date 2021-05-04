package no.nav.k9.sak.ytelse.pleiepengerbarn.mottak;

import java.util.Collection;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.kodeverk.dokument.Brevkode;
import no.nav.k9.kodeverk.dokument.DokumentStatus;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskRepository;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.mottak.dokumentmottak.DokumentGruppeRef;
import no.nav.k9.sak.mottak.dokumentmottak.Dokumentmottaker;
import no.nav.k9.sak.mottak.dokumentmottak.SøknadParser;
import no.nav.k9.sak.mottak.repo.MottattDokument;
import no.nav.k9.sak.mottak.repo.MottatteDokumentRepository;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.k9.søknad.Søknad;
import no.nav.k9.søknad.ytelse.psb.v1.PleiepengerSyktBarnValidator;

@ApplicationScoped
@FagsakYtelseTypeRef("PSB")
@DokumentGruppeRef(Brevkode.PLEIEPENGER_BARN_SOKNAD_KODE)
class DokumentmottakerPleiepengerSyktBarnSøknad implements Dokumentmottaker {

    private SøknadOversetter pleiepengerBarnSoknadOversetter;
    private MottatteDokumentRepository mottatteDokumentRepository;
    private SøknadParser søknadParser;
    private SykdomsDokumentVedleggHåndterer sykdomsDokumentVedleggHåndterer;
    private ProsessTaskRepository prosessTaskRepository;
    private BehandlingRepository behandlingRepository;

    DokumentmottakerPleiepengerSyktBarnSøknad() {
        // for CDI proxy
    }

    @Inject
    DokumentmottakerPleiepengerSyktBarnSøknad(MottatteDokumentRepository mottatteDokumentRepository,
                                              SøknadParser søknadParser,
                                              SøknadOversetter pleiepengerBarnSoknadOversetter,
                                              SykdomsDokumentVedleggHåndterer sykdomsDokumentVedleggHåndterer,
                                              ProsessTaskRepository prosessTaskRepository,
                                              BehandlingRepository behandlingRepository) {
        this.mottatteDokumentRepository = mottatteDokumentRepository;
        this.søknadParser = søknadParser;
        this.sykdomsDokumentVedleggHåndterer = sykdomsDokumentVedleggHåndterer;
        this.pleiepengerBarnSoknadOversetter = pleiepengerBarnSoknadOversetter;
        this.prosessTaskRepository = prosessTaskRepository;
        this.behandlingRepository = behandlingRepository;
    }

    @Override
    public void lagreDokumentinnhold(Collection<MottattDokument> dokumenter, Behandling behandling) {
        var behandlingId = behandling.getId();
        for (MottattDokument dokument : dokumenter) {
            Søknad søknad = søknadParser.parseSøknad(dokument);
            dokument.setBehandlingId(behandlingId);
            dokument.setInnsendingstidspunkt(søknad.getMottattDato().toLocalDateTime());
            mottatteDokumentRepository.lagre(dokument, DokumentStatus.BEHANDLER);
            // Søknadsinnhold som persisteres "lokalt" i k9-sak
            persister(søknad, behandling, dokument.getJournalpostId());
        }
        // Søknadsinnhold som persisteres eksternt (abakus)
        lagreOppgittOpptjeningFraSøknader(behandlingId, dokumenter);
    }

    /**
     * Lagrer oppgitt opptjening til abakus fra mottatt dokument.
     */
    private void lagreOppgittOpptjeningFraSøknader(Long behandlingId, Collection<MottattDokument> dokumenter) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        AktørId aktørId = behandling.getAktørId();
        var saksnummer = behandling.getFagsak().getSaksnummer();

        var enkeltTask = new ProsessTaskData(LagreOppgittOpptjeningFraSøknadTask.TASKTYPE);
        enkeltTask.setBehandling(behandling.getFagsakId(), behandlingId, aktørId.getId());
        enkeltTask.setSaksnummer(saksnummer.getVerdi());
        enkeltTask.setCallIdFraEksisterende();
        prosessTaskRepository.lagre(enkeltTask);
    }

    private void persister(Søknad søknad, Behandling behandling, JournalpostId journalpostId) {
        new PleiepengerSyktBarnValidator().forsikreValidert(søknad.getYtelse());

        pleiepengerBarnSoknadOversetter.persister(søknad, journalpostId, behandling);

        sykdomsDokumentVedleggHåndterer.leggTilDokumenterSomSkalHåndteresVedlagtSøknaden(behandling,
            journalpostId,
            behandling.getFagsak().getPleietrengendeAktørId(),
            søknad.getMottattDato().toLocalDateTime());
    }

    @Override
    public BehandlingÅrsakType getBehandlingÅrsakType() {
        return BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER;
    }
}
