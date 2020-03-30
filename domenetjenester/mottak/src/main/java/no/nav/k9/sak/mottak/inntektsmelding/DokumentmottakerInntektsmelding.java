package no.nav.k9.sak.mottak.inntektsmelding;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.kodeverk.dokument.DokumentTypeId;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.domene.uttak.UttakTjeneste;
import no.nav.k9.sak.mottak.Behandlingsoppretter;
import no.nav.k9.sak.mottak.dokumentmottak.DokumentGruppeRef;
import no.nav.k9.sak.mottak.dokumentmottak.DokumentmottakerFelles;
import no.nav.k9.sak.mottak.dokumentmottak.DokumentmottakerYtelsesesrelatertDokument;
import no.nav.k9.sak.mottak.dokumentmottak.Kompletthetskontroller;
import no.nav.k9.sak.mottak.dokumentmottak.MottatteDokumentTjeneste;
import no.nav.k9.sak.mottak.repo.MottattDokument;

@ApplicationScoped
@FagsakYtelseTypeRef
@DokumentGruppeRef("INNTEKTSMELDING")
public class DokumentmottakerInntektsmelding extends DokumentmottakerYtelsesesrelatertDokument {

    private static final DokumentTypeId INNTEKTSMELDING = DokumentTypeId.INNTEKTSMELDING;

    @Inject
    public DokumentmottakerInntektsmelding(DokumentmottakerFelles dokumentMottakerFelles,
                                           MottatteDokumentTjeneste mottatteDokumentTjeneste,
                                           Behandlingsoppretter behandlingsoppretter,
                                           Kompletthetskontroller kompletthetskontroller,
                                           UttakTjeneste uttakTjeneste,
                                           BehandlingRepositoryProvider repositoryProvider) {
        super(dokumentMottakerFelles,
            mottatteDokumentTjeneste,
            behandlingsoppretter,
            kompletthetskontroller,
            uttakTjeneste,
            repositoryProvider);
    }

    @Override
    public void håndterIngenTidligereBehandling(Fagsak fagsak, MottattDokument mottattDokument, BehandlingÅrsakType behandlingÅrsakType) { //#I1
        // Opprett ny førstegangsbehandling
        Behandling behandling = getBehandlingsoppretter().opprettFørstegangsbehandling(fagsak, BehandlingÅrsakType.UDEFINERT, Optional.empty());
        getMottatteDokumentTjeneste().persisterDokumentinnhold(behandling, mottattDokument);
        getDokumentmottakerFelles().opprettTaskForÅStarteBehandling(behandling);
        getDokumentmottakerFelles().opprettHistorikkinnslagForVedlegg(fagsak.getId(), mottattDokument.getJournalpostId(), null);
    }

    @Override
    public void håndterAvsluttetTidligereBehandling(MottattDokument mottattDokument, Fagsak fagsak, BehandlingÅrsakType behandlingÅrsakType) {
        if (getBehandlingsoppretter().erBehandlingOgFørstegangsbehandlingHenlagt(fagsak)) { //#I6
            getDokumentmottakerFelles().opprettTaskForÅVurdereInntektsmelding(fagsak, null, mottattDokument);
        } else {
            getDokumentmottakerFelles().opprettRevurdering(mottattDokument, fagsak, getBehandlingÅrsakType());
            getDokumentmottakerFelles().opprettHistorikkinnslagForVedlegg(fagsak.getId(), mottattDokument.getJournalpostId(), null);
        }
    }

    @Override
    public void oppdaterÅpenBehandlingMedDokument(Behandling behandling, MottattDokument mottattDokument, BehandlingÅrsakType behandlingÅrsakType) { //#I2
        getDokumentmottakerFelles().opprettHistorikkinnslagForVedlegg(behandling.getFagsakId(), mottattDokument.getJournalpostId(), null);
        getDokumentmottakerFelles().leggTilBehandlingsårsak(behandling, getBehandlingÅrsakType());
        getDokumentmottakerFelles().opprettHistorikkinnslagForBehandlingOppdatertMedNyeOpplysninger(behandling, BehandlingÅrsakType.RE_OPPLYSNINGER_OM_INNTEKT);
        getKompletthetskontroller().persisterDokumentOgVurderKompletthet(behandling, mottattDokument);
    }

    @Override
    public void håndterAvslåttEllerOpphørtBehandling(MottattDokument mottattDokument, Fagsak fagsak, Behandling avsluttetBehandling, BehandlingÅrsakType behandlingÅrsakType) {
        if (getDokumentmottakerFelles().skalOppretteNyFørstegangsbehandling(avsluttetBehandling.getFagsak())) { //#I3
            opprettNyFørstegangsbehandlingForMottattInntektsmelding(mottattDokument, fagsak, avsluttetBehandling);
        } else if (harAvslåttPeriode(avsluttetBehandling) && getBehandlingsoppretter().harBehandlingsresultatOpphørt(avsluttetBehandling)) { //#I4
            getDokumentmottakerFelles().opprettRevurdering(mottattDokument, fagsak, getBehandlingÅrsakType());
            getDokumentmottakerFelles().opprettHistorikkinnslagForVedlegg(fagsak.getId(), mottattDokument.getJournalpostId(), null);
        } else { //#I5
            getDokumentmottakerFelles().opprettTaskForÅVurdereInntektsmelding(fagsak, avsluttetBehandling, mottattDokument);
        }
    }

    @Override
    protected BehandlingÅrsakType getBehandlingÅrsakType() {
        return BehandlingÅrsakType.RE_ENDRET_INNTEKTSMELDING;
    }

    private void opprettNyFørstegangsbehandlingForMottattInntektsmelding(MottattDokument mottattDokument, Fagsak fagsak, Behandling avsluttetBehandling) {
        getDokumentmottakerFelles().opprettNyFørstegangFraAvslag(mottattDokument, fagsak, avsluttetBehandling, INNTEKTSMELDING);
    }
}
