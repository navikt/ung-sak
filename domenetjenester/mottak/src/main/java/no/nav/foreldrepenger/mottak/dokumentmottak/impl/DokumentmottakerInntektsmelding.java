package no.nav.foreldrepenger.mottak.dokumentmottak.impl;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.mottak.Behandlingsoppretter;
import no.nav.foreldrepenger.mottak.dokumentmottak.MottatteDokumentTjeneste;
import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;

@ApplicationScoped
@FagsakYtelseTypeRef
@DokumentGruppeRef("INNTEKTSMELDING")
class DokumentmottakerInntektsmelding extends DokumentmottakerYtelsesesrelatertDokument {


    @Inject
    public DokumentmottakerInntektsmelding(DokumentmottakerFelles dokumentmottakerFelles,
                                           MottatteDokumentTjeneste mottatteDokumentTjeneste,
                                           Behandlingsoppretter behandlingsoppretter,
                                           Kompletthetskontroller kompletthetskontroller,
                                           BehandlingRepositoryProvider repositoryProvider) {
        super(dokumentmottakerFelles,
            mottatteDokumentTjeneste,
            behandlingsoppretter,
            kompletthetskontroller,
            repositoryProvider);
    }

    @Override
    public void håndterIngenTidligereBehandling(Fagsak fagsak, MottattDokument mottattDokument, BehandlingÅrsakType behandlingÅrsakType) { //#I1
        // Opprett ny førstegangsbehandling
        Behandling behandling = behandlingsoppretter.opprettFørstegangsbehandling(fagsak, BehandlingÅrsakType.UDEFINERT, Optional.empty());
        mottatteDokumentTjeneste.persisterDokumentinnhold(behandling, mottattDokument, Optional.empty());
        dokumentmottakerFelles.opprettTaskForÅStarteBehandling(behandling);
        dokumentmottakerFelles.opprettHistorikkinnslagForVedlegg(fagsak.getId(), mottattDokument.getJournalpostId(), mottattDokument.getDokumentType());
    }

    @Override
    public void håndterAvsluttetTidligereBehandling(MottattDokument mottattDokument, Fagsak fagsak, BehandlingÅrsakType behandlingÅrsakType) {
        if (behandlingsoppretter.erBehandlingOgFørstegangsbehandlingHenlagt(fagsak)) { //#I6
            dokumentmottakerFelles.opprettTaskForÅVurdereDokument(fagsak, null, mottattDokument);
        } else {
            dokumentmottakerFelles.opprettRevurdering(mottattDokument, fagsak, getBehandlingÅrsakType());
            dokumentmottakerFelles.opprettHistorikkinnslagForVedlegg(fagsak.getId(), mottattDokument.getJournalpostId(), mottattDokument.getDokumentType());
        }
    }

    @Override
    public void oppdaterÅpenBehandlingMedDokument(Behandling behandling, MottattDokument mottattDokument, BehandlingÅrsakType behandlingÅrsakType) { //#I2
        dokumentmottakerFelles.opprettHistorikkinnslagForVedlegg(behandling.getFagsakId(), mottattDokument.getJournalpostId(), mottattDokument.getDokumentType());
        dokumentmottakerFelles.leggTilBehandlingsårsak(behandling, getBehandlingÅrsakType());
        dokumentmottakerFelles.opprettHistorikkinnslagForBehandlingOppdatertMedNyeOpplysninger(behandling, BehandlingÅrsakType.RE_OPPLYSNINGER_OM_INNTEKT);
        kompletthetskontroller.persisterDokumentOgVurderKompletthet(behandling, mottattDokument);
    }

    @Override
    public void håndterAvslåttEllerOpphørtBehandling(MottattDokument mottattDokument, Fagsak fagsak, Behandling avsluttetBehandling, BehandlingÅrsakType behandlingÅrsakType) {
        if (dokumentmottakerFelles.skalOppretteNyFørstegangsbehandling(avsluttetBehandling.getFagsak())) { //#I3
            opprettNyFørstegangsbehandlingForMottattInntektsmelding(mottattDokument, fagsak, avsluttetBehandling);
        } else if (harAvslåttPeriode(avsluttetBehandling) && behandlingsoppretter.harBehandlingsresultatOpphørt(avsluttetBehandling)) { //#I4
            dokumentmottakerFelles.opprettRevurdering(mottattDokument, fagsak, getBehandlingÅrsakType());
            dokumentmottakerFelles.opprettHistorikkinnslagForVedlegg(fagsak.getId(), mottattDokument.getJournalpostId(), mottattDokument.getDokumentType());
        } else { //#I5
            dokumentmottakerFelles.opprettTaskForÅVurdereDokument(fagsak, avsluttetBehandling, mottattDokument);
        }
    }

    @Override
    protected BehandlingÅrsakType getBehandlingÅrsakType() {
        return BehandlingÅrsakType.RE_ENDRET_INNTEKTSMELDING;
    }

    private void opprettNyFørstegangsbehandlingForMottattInntektsmelding(MottattDokument mottattDokument, Fagsak fagsak, Behandling avsluttetBehandling) {
        dokumentmottakerFelles.opprettNyFørstegangFraAvslag(mottattDokument, fagsak, avsluttetBehandling);
    }
}
