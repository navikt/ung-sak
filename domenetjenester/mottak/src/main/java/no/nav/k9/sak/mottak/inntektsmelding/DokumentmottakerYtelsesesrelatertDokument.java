package no.nav.k9.sak.mottak.inntektsmelding;

import java.util.Optional;

import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRevurderingRepository;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.mottak.Behandlingsoppretter;
import no.nav.k9.sak.mottak.dokumentmottak.Dokumentmottaker;
import no.nav.k9.sak.mottak.repo.MottattDokument;

// Dokumentmottaker for ytelsesrelaterte dokumenter har felles protokoll som fanges her
// Variasjoner av protokollen håndteres utenfro
public abstract class DokumentmottakerYtelsesesrelatertDokument implements Dokumentmottaker {

    private Behandlingsoppretter behandlingsoppretter;
    private BehandlingRepository behandlingRepository;
    private BehandlingRevurderingRepository revurderingRepository;

    protected DokumentmottakerYtelsesesrelatertDokument() {
        // For CDI proxy
    }

    protected DokumentmottakerYtelsesesrelatertDokument(Behandlingsoppretter behandlingsoppretter,
                                                        BehandlingRepositoryProvider repositoryProvider) {
        this.behandlingsoppretter = behandlingsoppretter;
        this.revurderingRepository = repositoryProvider.getBehandlingRevurderingRepository();
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
    }

    /* TEMPLATE-metoder som må håndteres spesifikt for hver type av ytelsesdokumenter - START */
    protected abstract void håndterIngenTidligereBehandling(Fagsak fagsak, MottattDokument mottattDokument, BehandlingÅrsakType behandlingÅrsakType);

    protected abstract void håndterAvsluttetTidligereBehandling(MottattDokument mottattDokument, Fagsak fagsak, BehandlingÅrsakType behandlingÅrsakType);

    protected abstract void oppdaterÅpenBehandlingMedDokument(Behandling behandling, MottattDokument mottattDokument, BehandlingÅrsakType behandlingÅrsakType);

    protected abstract void håndterAvslåttEllerOpphørtBehandling(MottattDokument mottattDokument, Fagsak fagsak, Behandling avsluttetBehandling, BehandlingÅrsakType behandlingÅrsakType);

    protected abstract BehandlingÅrsakType getBehandlingÅrsakType();
    /* TEMPLATE-metoder SLUTT */

    @Override
    public void mottaDokument(MottattDokument mottattDokument, Fagsak fagsak, BehandlingÅrsakType behandlingÅrsakType) {
        Optional<Behandling> sisteYtelsesbehandling = revurderingRepository.hentSisteYtelsesbehandling(fagsak.getId());

        if (sisteYtelsesbehandling.isEmpty()) {
            håndterIngenTidligereBehandling(fagsak, mottattDokument, behandlingÅrsakType);
            return;
        }

        Behandling behandling = sisteYtelsesbehandling.get();
        boolean sisteYtelseErFerdigbehandlet = sisteYtelsesbehandling.map(Behandling::erSaksbehandlingAvsluttet).orElse(Boolean.FALSE);
        if (sisteYtelseErFerdigbehandlet) {
            Optional<Behandling> sisteAvsluttetBehandling = behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsak.getId());
            behandling = sisteAvsluttetBehandling.orElse(behandling);
            // Håndter avsluttet behandling
            if (behandlingsoppretter.erAvslåttBehandling(behandling)
                || behandlingsoppretter.harBehandlingsresultatOpphørt(behandling)) {
                håndterAvslåttEllerOpphørtBehandling(mottattDokument, fagsak, behandling, behandlingÅrsakType);
            } else {
                håndterAvsluttetTidligereBehandling(mottattDokument, fagsak, behandlingÅrsakType);
            }
        } else {
            oppdaterÅpenBehandlingMedDokument(behandling, mottattDokument, behandlingÅrsakType);
        }
    }

    protected final boolean erAvslag(Behandling avsluttetBehandling) {
        return avsluttetBehandling.getBehandlingResultatType().isBehandlingsresultatAvslått();
    }

}
