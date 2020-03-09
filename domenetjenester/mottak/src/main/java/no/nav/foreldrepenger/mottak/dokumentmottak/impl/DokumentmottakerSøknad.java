package no.nav.foreldrepenger.mottak.dokumentmottak.impl;

import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRevurderingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.domene.uttak.UttakTjeneste;
import no.nav.foreldrepenger.mottak.Behandlingsoppretter;
import no.nav.foreldrepenger.mottak.dokumentmottak.MottatteDokumentTjeneste;
import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.kodeverk.dokument.DokumentKategori;
import no.nav.k9.kodeverk.dokument.DokumentTypeId;

public abstract class DokumentmottakerSøknad extends DokumentmottakerYtelsesesrelatertDokument {

    private BehandlingRevurderingRepository revurderingRepository;
    private BehandlingRepository behandlingRepository;

    public DokumentmottakerSøknad(BehandlingRepositoryProvider repositoryProvider,
                                  DokumentmottakerFelles dokumentmottakerFelles,
                                  MottatteDokumentTjeneste mottatteDokumentTjeneste,
                                  Behandlingsoppretter behandlingsoppretter,
                                  UttakTjeneste uttakTjeneste,
                                  Kompletthetskontroller kompletthetskontroller) {
        super(dokumentmottakerFelles,
            mottatteDokumentTjeneste,
            behandlingsoppretter,
            kompletthetskontroller,
            uttakTjeneste,
            repositoryProvider);
        this.revurderingRepository = repositoryProvider.getBehandlingRevurderingRepository();
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
    }

    @Override
    public void håndterIngenTidligereBehandling(Fagsak fagsak, MottattDokument mottattDokument, BehandlingÅrsakType behandlingÅrsakType) { // #S1
        // Opprett ny førstegangsbehandling
        Behandling behandling = behandlingsoppretter.opprettFørstegangsbehandling(fagsak, behandlingÅrsakType, Optional.empty());
        mottatteDokumentTjeneste.persisterDokumentinnhold(behandling, mottattDokument, Optional.empty());
        dokumentmottakerFelles.opprettTaskForÅStarteBehandling(behandling);
        dokumentmottakerFelles.opprettHistorikk(behandling, mottattDokument.getJournalpostId());
    }

    @Override
    public void håndterAvsluttetTidligereBehandling(MottattDokument mottattDokument, Fagsak fagsak, BehandlingÅrsakType behandlingÅrsakType) {
        if (behandlingsoppretter.erBehandlingOgFørstegangsbehandlingHenlagt(fagsak)) { // #S8
            // Start ny førstegangsbehandling av søknad
            opprettFørstegangsbehandlingMedHistorikkinslagOgKopiAvDokumenter(mottattDokument, fagsak, behandlingÅrsakType);
        } else { // #S9
            // Oppretter revurdering siden det allerede er gjennomført en førstegangsbehandling på fagsaken
            Behandling revurdering = dokumentmottakerFelles.opprettRevurdering(mottattDokument, fagsak, getBehandlingÅrsakType());
            dokumentmottakerFelles.opprettHistorikk(revurdering, mottattDokument.getJournalpostId());
        }
    }

    @Override
    public void oppdaterÅpenBehandlingMedDokument(Behandling behandling, MottattDokument mottattDokument, BehandlingÅrsakType behandlingÅrsakType) {
        dokumentmottakerFelles.opprettHistorikk(behandling, mottattDokument.getJournalpostId());

        behandling.getFagsak();

        if (harMottattSøknadTidligere(behandling.getId())) { // #S2
            // Oppdatere behandling gjennom henleggelse
            Behandling nyBehandling = dokumentmottakerFelles.oppdatereViaHenleggelse(behandling, mottattDokument, behandlingÅrsakType);
            dokumentmottakerFelles.opprettTaskForÅStarteBehandling(nyBehandling);
        } else {
            kompletthetskontroller.persisterDokumentOgVurderKompletthet(behandling, mottattDokument);
        }
    }

    @Override
    public void håndterAvslåttEllerOpphørtBehandling(MottattDokument mottattDokument, Fagsak fagsak, Behandling avsluttetBehandling,
                                                     BehandlingÅrsakType behandlingÅrsakType) {
        if (erAvslag(avsluttetBehandling)) { // #S4
            opprettFørstegangsbehandlingMedHistorikkinslagOgKopiAvDokumenter(mottattDokument, fagsak, behandlingÅrsakType);
        } else if (harAvslåttPeriode(avsluttetBehandling) && behandlingsoppretter.harBehandlingsresultatOpphørt(avsluttetBehandling)) { // #S5
            BehandlingÅrsakType brukÅrsakType = BehandlingÅrsakType.UDEFINERT.equals(behandlingÅrsakType) ? getBehandlingÅrsakType() : behandlingÅrsakType;
            Behandling revurdering = dokumentmottakerFelles.opprettRevurdering(mottattDokument, fagsak, brukÅrsakType);
            dokumentmottakerFelles.opprettHistorikk(revurdering, mottattDokument.getJournalpostId());
        } else if (BehandlingÅrsakType.ETTER_KLAGE.equals(behandlingÅrsakType)) { // #S6
            opprettFørstegangsbehandlingMedHistorikkinslagOgKopiAvDokumenter(mottattDokument, fagsak, behandlingÅrsakType);
        } else { // #S7
            dokumentmottakerFelles.opprettTaskForÅVurdereDokument(fagsak, avsluttetBehandling, mottattDokument);
        }
    }

    @Override
    public void opprettFraTidligereAvsluttetBehandling(Fagsak fagsak, Long avsluttetMedSøknadBehandlingId, MottattDokument mottattDokument,
                                                       BehandlingÅrsakType behandlingÅrsakType) {
        Behandling avsluttetBehandlingMedSøknad = behandlingRepository.hentBehandling(avsluttetMedSøknadBehandlingId);
        boolean harÅpenBehandling = !revurderingRepository.hentSisteYtelsesbehandling(fagsak.getId()).map(Behandling::erSaksbehandlingAvsluttet)
            .orElse(Boolean.TRUE);
        if (harÅpenBehandling || erAvslag(avsluttetBehandlingMedSøknad) || avsluttetBehandlingMedSøknad.isBehandlingHenlagt()) {
            opprettFørstegangsbehandlingMedHistorikkinslagOgKopiAvDokumenter(mottattDokument, avsluttetBehandlingMedSøknad, fagsak, behandlingÅrsakType);
        } else {
            BehandlingÅrsakType brukÅrsakType = BehandlingÅrsakType.UDEFINERT.equals(behandlingÅrsakType) ? getBehandlingÅrsakType() : behandlingÅrsakType;
            Behandling revurdering = dokumentmottakerFelles.opprettManuellRevurdering(fagsak, brukÅrsakType);
            dokumentmottakerFelles.opprettHistorikk(revurdering, mottattDokument.getJournalpostId());
        }
    }

    protected void opprettFørstegangsbehandlingMedHistorikkinslagOgKopiAvDokumenter(MottattDokument mottattDokument, Fagsak fagsak,
                                                                                    BehandlingÅrsakType behandlingÅrsakType) {
        Behandling behandling = behandlingsoppretter.opprettNyFørstegangsbehandlingMedImOgVedleggFraForrige(behandlingÅrsakType, fagsak);
        mottatteDokumentTjeneste.persisterDokumentinnhold(behandling, mottattDokument, Optional.empty());
        dokumentmottakerFelles.opprettTaskForÅStarteBehandling(behandling);
        dokumentmottakerFelles.opprettHistorikk(behandling, mottattDokument.getJournalpostId());
    }

    protected void opprettFørstegangsbehandlingMedHistorikkinslagOgKopiAvDokumenter(MottattDokument mottattDokument, Behandling behandlingMedGrunnlag,
                                                                                    Fagsak fagsak, BehandlingÅrsakType behandlingÅrsakType) {
        dokumentmottakerFelles.opprettNyFørstegangFraBehandlingMedSøknad(fagsak, behandlingÅrsakType, behandlingMedGrunnlag, mottattDokument);
    }

    @Override
    protected BehandlingÅrsakType getBehandlingÅrsakType() {
        return BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER;
    }

    protected boolean harMottattSøknadTidligere(Long behandlingId) {
        return mottatteDokumentTjeneste.harMottattDokumentSet(behandlingId, DokumentTypeId.getSøknadTyper()) ||
            mottatteDokumentTjeneste.harMottattDokumentSet(behandlingId, DokumentTypeId.getEndringSøknadTyper()) ||
            mottatteDokumentTjeneste.harMottattDokumentKat(behandlingId, DokumentKategori.SØKNAD);
    }
}
