package no.nav.foreldrepenger.mottak.dokumentmottak.impl;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.mottak.Behandlingsoppretter;
import no.nav.foreldrepenger.mottak.dokumentmottak.MottatteDokumentTjeneste;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.sak.domene.uttak.UttakTjeneste;

@ApplicationScoped
@FagsakYtelseTypeRef
@DokumentGruppeRef("ENDRINGSSØKNAD")
class DokumentmottakerEndringssøknad extends DokumentmottakerYtelsesesrelatertDokument {

    @Inject
    public DokumentmottakerEndringssøknad(BehandlingRepositoryProvider repositoryProvider,
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
    }

    @Override
    public void oppdaterÅpenBehandlingMedDokument(Behandling behandling, MottattDokument mottattDokument, BehandlingÅrsakType behandlingÅrsakType) {
        dokumentmottakerFelles.opprettHistorikk(behandling, mottattDokument.getJournalpostId());

        BehandlingÅrsakType årsakEndringFraBruker = getBehandlingÅrsakType();
        dokumentmottakerFelles.opprettHistorikkinnslagForBehandlingOppdatertMedNyeOpplysninger(behandling, årsakEndringFraBruker);
        if (BehandlingType.FØRSTEGANGSSØKNAD.equals(behandling.getType())) {
            dokumentmottakerFelles.opprettTaskForÅVurdereDokument(behandling.getFagsak(), behandling, mottattDokument);
        } else if (kompletthetErPassert(behandling)) {
            Behandling nyBehandling = dokumentmottakerFelles.oppdatereViaHenleggelse(behandling, mottattDokument, årsakEndringFraBruker);
            dokumentmottakerFelles.opprettTaskForÅStarteBehandling(nyBehandling);
        } else {
            mottatteDokumentTjeneste.oppdaterMottattDokumentMedBehandling(mottattDokument, behandling.getId());
            // Oppdater åpen behandling med Endringssøknad
            dokumentmottakerFelles.leggTilBehandlingsårsak(behandling, årsakEndringFraBruker);
            kompletthetskontroller.persisterDokumentOgVurderKompletthet(behandling, mottattDokument);
        }
    }

    @Override
    public void håndterAvslåttEllerOpphørtBehandling(MottattDokument mottattDokument, Fagsak fagsak, Behandling avsluttetBehandling, BehandlingÅrsakType behandlingÅrsakType) {
        if (dokumentmottakerFelles.skalOppretteNyFørstegangsbehandling(avsluttetBehandling.getFagsak())) { //#E6
            dokumentmottakerFelles.opprettNyFørstegangFraAvslag(mottattDokument, fagsak, avsluttetBehandling);
        } else if (harAvslåttPeriode(avsluttetBehandling) && behandlingsoppretter.harBehandlingsresultatOpphørt(avsluttetBehandling)) { //#E7
            Behandling revurdering = dokumentmottakerFelles.opprettRevurdering(mottattDokument, fagsak, getBehandlingÅrsakType());
            dokumentmottakerFelles.opprettHistorikk(revurdering, mottattDokument.getJournalpostId());
        } else { //#E8
            dokumentmottakerFelles.opprettTaskForÅVurdereDokument(fagsak, avsluttetBehandling, mottattDokument);
        }
    }

    @Override
    public void håndterAvsluttetTidligereBehandling(MottattDokument mottattDokument, Fagsak fagsak, BehandlingÅrsakType behandlingÅrsakType) {
        if (behandlingsoppretter.erBehandlingOgFørstegangsbehandlingHenlagt(fagsak)) { //#E9
            dokumentmottakerFelles.opprettTaskForÅVurdereDokument(fagsak, null, mottattDokument);
        } else { //#E10
            Behandling revurdering = dokumentmottakerFelles.opprettRevurdering(mottattDokument, fagsak, getBehandlingÅrsakType());
            dokumentmottakerFelles.opprettHistorikk(revurdering, mottattDokument.getJournalpostId());
        }
    }

    @Override
    public void håndterIngenTidligereBehandling(Fagsak fagsak, MottattDokument mottattDokument, BehandlingÅrsakType behandlingÅrsakType) {
        // Kan ikke håndtere endringssøknad når ingen behandling finnes -> Opprett manuell task
        dokumentmottakerFelles.opprettTaskForÅVurdereDokument(fagsak, null, mottattDokument); //#E1
    }

    @Override
    protected BehandlingÅrsakType getBehandlingÅrsakType() {
        return BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER;
    }

    private boolean kompletthetErPassert(Behandling behandling) {
        return behandlingsoppretter.erKompletthetssjekkPassert(behandling);
    }

}
