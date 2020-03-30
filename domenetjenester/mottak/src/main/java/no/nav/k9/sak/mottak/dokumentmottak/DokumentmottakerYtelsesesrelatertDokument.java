package no.nav.k9.sak.mottak.dokumentmottak;

import java.util.Optional;

import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRevurderingRepository;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.domene.uttak.UttakTjeneste;
import no.nav.k9.sak.mottak.Behandlingsoppretter;
import no.nav.k9.sak.mottak.repo.MottattDokument;

// Dokumentmottaker for ytelsesrelaterte dokumenter har felles protokoll som fanges her
// Variasjoner av protokollen håndteres utenfro
public abstract class DokumentmottakerYtelsesesrelatertDokument implements Dokumentmottaker {

    private DokumentmottakerFelles dokumentmottakerFelles;
    private MottatteDokumentTjeneste mottatteDokumentTjeneste;
    private Behandlingsoppretter behandlingsoppretter;
    private Kompletthetskontroller kompletthetskontroller;

    private BehandlingRepository behandlingRepository;
    private BehandlingRevurderingRepository revurderingRepository;
    private UttakTjeneste uttakTjeneste;

    protected DokumentmottakerYtelsesesrelatertDokument() {
        // For CDI proxy
    }

    @Inject
    public DokumentmottakerYtelsesesrelatertDokument(DokumentmottakerFelles dokumentmottakerFelles,
                                                     MottatteDokumentTjeneste mottatteDokumentTjeneste,
                                                     Behandlingsoppretter behandlingsoppretter,
                                                     Kompletthetskontroller kompletthetskontroller,
                                                     UttakTjeneste uttakTjeneste,
                                                     BehandlingRepositoryProvider repositoryProvider) {
        this.dokumentmottakerFelles = dokumentmottakerFelles;
        this.mottatteDokumentTjeneste = mottatteDokumentTjeneste;
        this.behandlingsoppretter = behandlingsoppretter;
        this.kompletthetskontroller = kompletthetskontroller;
        this.uttakTjeneste = uttakTjeneste;
        this.revurderingRepository = repositoryProvider.getBehandlingRevurderingRepository();
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
    }

    /* TEMPLATE-metoder som må håndteres spesifikt for hver type av ytelsesdokumenter - START */
    public abstract  void håndterIngenTidligereBehandling(Fagsak fagsak, MottattDokument mottattDokument, BehandlingÅrsakType behandlingÅrsakType);

    public abstract void håndterAvsluttetTidligereBehandling(MottattDokument mottattDokument, Fagsak fagsak, BehandlingÅrsakType behandlingÅrsakType);

    public abstract void oppdaterÅpenBehandlingMedDokument(Behandling behandling, MottattDokument mottattDokument, BehandlingÅrsakType behandlingÅrsakType);

    public abstract void håndterAvslåttEllerOpphørtBehandling(MottattDokument mottattDokument, Fagsak fagsak, Behandling avsluttetBehandling, BehandlingÅrsakType behandlingÅrsakType);

    protected abstract BehandlingÅrsakType getBehandlingÅrsakType();
    /* TEMPLATE-metoder SLUTT */

    protected Behandlingsoppretter getBehandlingsoppretter() {
        return behandlingsoppretter;
    }
    
    protected DokumentmottakerFelles getDokumentmottakerFelles() {
        return dokumentmottakerFelles;
    }
    
    protected Kompletthetskontroller getKompletthetskontroller() {
        return kompletthetskontroller;
    }
    
    protected MottatteDokumentTjeneste getMottatteDokumentTjeneste() {
        return mottatteDokumentTjeneste;
    }
    
    @Override
    public final void mottaDokument(MottattDokument mottattDokument, Fagsak fagsak, BehandlingÅrsakType behandlingÅrsakType) {
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

    protected boolean harAvslåttPeriode(Behandling avsluttetBehandling) {
        return uttakTjeneste.harAvslåttUttakPeriode(avsluttetBehandling.getUuid());
    }
}
