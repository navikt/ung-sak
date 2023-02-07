package no.nav.k9.sak.web.app.tjenester.behandling;

import static no.nav.k9.felles.feil.LogLevel.INFO;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.felles.feil.Feil;
import no.nav.k9.felles.feil.FeilFactory;
import no.nav.k9.felles.feil.deklarasjon.DeklarerteFeil;
import no.nav.k9.felles.feil.deklarasjon.FunksjonellFeil;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandling.revurdering.RevurderingFeil;
import no.nav.k9.sak.behandling.revurdering.RevurderingTjeneste;
import no.nav.k9.sak.behandling.revurdering.UnntaksbehandlingOppretter;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;
import no.nav.k9.sak.trigger.ProsessTriggereRepository;
import no.nav.k9.sak.trigger.Trigger;
import no.nav.k9.sak.typer.Saksnummer;

@ApplicationScoped
public class BehandlingsoppretterTjeneste {

    private BehandlingRepository behandlingRepository;
    private FagsakRepository fagsakRepository;
    private BehandlendeEnhetTjeneste behandlendeEnhetTjeneste;
    private ProsessTriggereRepository prosessTriggereRepository;

    BehandlingsoppretterTjeneste() {
        // CDI
    }

    @Inject
    public BehandlingsoppretterTjeneste(BehandlingRepositoryProvider behandlingRepositoryProvider, BehandlendeEnhetTjeneste behandlendeEnhetTjeneste, ProsessTriggereRepository prosessTriggereRepository) {
        this.behandlendeEnhetTjeneste = behandlendeEnhetTjeneste;
        this.prosessTriggereRepository = prosessTriggereRepository;
        Objects.requireNonNull(behandlingRepositoryProvider, "behandlingRepositoryProvider");
        this.behandlingRepository = behandlingRepositoryProvider.getBehandlingRepository();
        this.fagsakRepository = behandlingRepositoryProvider.getFagsakRepository();
    }

    public Behandling opprettRevurdering(Fagsak fagsak, BehandlingÅrsakType behandlingÅrsakType) {
        var revurderingTjeneste = FagsakYtelseTypeRef.Lookup.find(RevurderingTjeneste.class, fagsak.getYtelseType()).orElseThrow();
        var kanRevurderingOpprettes = kanOppretteRevurdering(fagsak.getId());
        if (!kanRevurderingOpprettes) {
            throw BehandlingsoppretterTjeneste.BehandlingsoppretterTjenesteFeil.FACTORY.kanIkkeOppretteRevurdering(fagsak.getSaksnummer()).toException();
        }
        var origBehandling = behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsak.getId())
            .orElseThrow(() -> RevurderingFeil.FACTORY.tjenesteFinnerIkkeBehandlingForRevurdering(fagsak.getId()).toException());

        var enhet = behandlendeEnhetTjeneste.finnBehandlendeEnhetFor(fagsak);
        return revurderingTjeneste.opprettManuellRevurdering(origBehandling, behandlingÅrsakType, enhet);
    }

    public Behandling opprettRevurdering(Fagsak fagsak, Map<DatoIntervallEntitet, BehandlingÅrsakType> behandlingArsakType) {
        var revurderingTjeneste = FagsakYtelseTypeRef.Lookup.find(RevurderingTjeneste.class, fagsak.getYtelseType()).orElseThrow();
        var kanRevurderingOpprettes = kanOppretteRevurdering(fagsak.getId());
        if (!kanRevurderingOpprettes) {
            throw BehandlingsoppretterTjeneste.BehandlingsoppretterTjenesteFeil.FACTORY.kanIkkeOppretteRevurdering(fagsak.getSaksnummer()).toException();
        }
        var origBehandling = behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsak.getId())
            .orElseThrow(() -> RevurderingFeil.FACTORY.tjenesteFinnerIkkeBehandlingForRevurdering(fagsak.getId()).toException());

        var enhet = behandlendeEnhetTjeneste.finnBehandlendeEnhetFor(fagsak);
        var behandlingÅrsakType = behandlingArsakType.values().stream().findFirst().orElseThrow();
        var behandling = revurderingTjeneste.opprettManuellRevurdering(origBehandling, behandlingÅrsakType, enhet);
        // Legg til perioder og årsaker
        prosessTriggereRepository.leggTil(behandling.getId(), behandlingArsakType.entrySet().stream().map(it -> new Trigger(it.getValue(), it.getKey())).collect(Collectors.toSet()));

        return behandling;
    }

    public Behandling opprettUnntaksbehandling(Fagsak fagsak, BehandlingÅrsakType behandlingÅrsakType) {
        var unntaksbehandlingOppretterTjeneste = getUnntaksbehandlingOppretterTjeneste(fagsak.getYtelseType());
        var kanOppretteUnntaksbehandling = kanOppretteUnntaksbehandling(fagsak.getId());
        if (!kanOppretteUnntaksbehandling) {
            throw BehandlingsoppretterTjeneste.BehandlingsoppretterTjenesteFeil.FACTORY.kanIkkeOppretteUnntaksbehandling(fagsak.getSaksnummer()).toException();
        }

        var origBehandling = behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsak.getId())
            .orElse(null);

        var enhet = behandlendeEnhetTjeneste.finnBehandlendeEnhetFor(fagsak);
        return unntaksbehandlingOppretterTjeneste.opprettNyBehandling(fagsak, origBehandling, behandlingÅrsakType, enhet);
    }

    public boolean kanOppretteNyBehandlingAvType(Long fagsakId, BehandlingType type) {
        boolean finnesÅpneBehandlingerAvType = behandlingRepository.hentÅpneBehandlingerForFagsakId(fagsakId, type).size() > 0;
        if (finnesÅpneBehandlingerAvType) {
            return false;
        }
        return switch (type) {
            case FØRSTEGANGSSØKNAD -> kanOppretteFørstegangsbehandling(fagsakId);
            case REVURDERING -> kanOppretteRevurdering(fagsakId);
            case UNNTAKSBEHANDLING -> kanOppretteUnntaksbehandling(fagsakId);
            default -> false;
        };
    }

    private boolean kanOppretteFørstegangsbehandling(Long fagsakId) {
        return false; // Ikke støttet i k9
    }

    private boolean kanOppretteRevurdering(Long fagsakId) {
        var åpneBehandlinger = behandlingRepository.hentÅpneBehandlingerForFagsakId(fagsakId, BehandlingType.FØRSTEGANGSSØKNAD, BehandlingType.REVURDERING);
        //Strengere versjon var behandling = behandlingRepository.finnSisteInnvilgetBehandling(fagsakId).orElse(null);
        var behandling = behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(fagsakId).orElse(null);
        if (åpneBehandlinger.size() > 0 || behandling == null) {
            return false;
        }
        RevurderingTjeneste revurderingTjeneste = FagsakYtelseTypeRef.Lookup.find(RevurderingTjeneste.class, behandling.getFagsakYtelseType()).orElseThrow();
        return revurderingTjeneste.kanRevurderingOpprettes(behandling.getFagsak());
    }

    private boolean kanOppretteUnntaksbehandling(Long fagsakId) {
        var fagsak = fagsakRepository.finnEksaktFagsak(fagsakId);
        return FagsakYtelseTypeRef.Lookup.find(UnntaksbehandlingOppretter.class, fagsak.getYtelseType()).orElseThrow()
            .kanNyBehandlingOpprettes(fagsak);
    }

    private UnntaksbehandlingOppretter getUnntaksbehandlingOppretterTjeneste(FagsakYtelseType ytelseType) {
        return BehandlingTypeRef.Lookup.find(UnntaksbehandlingOppretter.class, ytelseType, BehandlingType.UNNTAKSBEHANDLING)
            .orElseThrow(() -> new UnsupportedOperationException("Ikke implementert for " + UnntaksbehandlingOppretter.class.getSimpleName() +
                " for ytelsetype " + ytelseType + " , behandlingstype " + BehandlingType.UNNTAKSBEHANDLING));
    }

    interface BehandlingsoppretterTjenesteFeil extends DeklarerteFeil {
        BehandlingsoppretterTjenesteFeil FACTORY = FeilFactory.create(BehandlingsoppretterTjenesteFeil.class); // NOSONAR

        @FunksjonellFeil(feilkode = "FP-663487", feilmelding = "Fagsak med saksnummer %s oppfyller ikke kravene for revurdering", løsningsforslag = "", logLevel = INFO)
        Feil kanIkkeOppretteRevurdering(Saksnummer saksnummer);

        @FunksjonellFeil(feilkode = "FP-407002", feilmelding = "Fagsak med saksnummer %s oppfyller ikke kravene for unntaksbehandling", løsningsforslag = "", logLevel = INFO)
        Feil kanIkkeOppretteUnntaksbehandling(Saksnummer saksnummer);

    }

}
