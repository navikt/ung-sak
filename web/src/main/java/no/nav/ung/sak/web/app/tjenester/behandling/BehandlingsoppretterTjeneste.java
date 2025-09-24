package no.nav.ung.sak.web.app.tjenester.behandling;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.felles.feil.Feil;
import no.nav.k9.felles.feil.FeilFactory;
import no.nav.k9.felles.feil.deklarasjon.DeklarerteFeil;
import no.nav.k9.felles.feil.deklarasjon.FunksjonellFeil;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.sak.behandling.revurdering.RevurderingFeil;
import no.nav.ung.sak.behandling.revurdering.RevurderingTjeneste;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.KontrollertInntektPeriode;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.TilkjentYtelseRepository;
import no.nav.ung.sak.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;
import no.nav.ung.sak.typer.Periode;
import no.nav.ung.sak.typer.Saksnummer;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static no.nav.k9.felles.feil.LogLevel.INFO;

@ApplicationScoped
public class BehandlingsoppretterTjeneste {

    private BehandlingRepository behandlingRepository;
    private BehandlendeEnhetTjeneste behandlendeEnhetTjeneste;
    private TilkjentYtelseRepository tilkjentYtelseRepository;

    BehandlingsoppretterTjeneste() {
        // CDI
    }

    @Inject
    public BehandlingsoppretterTjeneste(BehandlingRepositoryProvider behandlingRepositoryProvider, BehandlendeEnhetTjeneste behandlendeEnhetTjeneste, TilkjentYtelseRepository tilkjentYtelseRepository) {
        this.behandlendeEnhetTjeneste = behandlendeEnhetTjeneste;
        Objects.requireNonNull(behandlingRepositoryProvider, "behandlingRepositoryProvider");
        this.behandlingRepository = behandlingRepositoryProvider.getBehandlingRepository();
        this.tilkjentYtelseRepository = tilkjentYtelseRepository;
    }

    public Behandling opprettManuellRevurdering(Fagsak fagsak, BehandlingÅrsakType behandlingÅrsakType, Optional<DatoIntervallEntitet> periode) {
        return opprettRevurdering(fagsak, behandlingÅrsakType, periode, true);
    }

    public Behandling opprettAutomatiskRevurdering(Fagsak fagsak, BehandlingÅrsakType behandlingÅrsakType) {
        return opprettRevurdering(fagsak, behandlingÅrsakType, Optional.empty(), false);
    }

    private Behandling opprettRevurdering(Fagsak fagsak, BehandlingÅrsakType behandlingÅrsakType, Optional<DatoIntervallEntitet> periode, boolean manuell) {
        var revurderingTjeneste = FagsakYtelseTypeRef.Lookup.find(RevurderingTjeneste.class, fagsak.getYtelseType()).orElseThrow();
        var kanRevurderingOpprettes = kanOppretteRevurdering(fagsak.getId());
        if (!kanRevurderingOpprettes) {
            throw BehandlingsoppretterTjeneste.BehandlingsoppretterTjenesteFeil.FACTORY.kanIkkeOppretteRevurdering(fagsak.getSaksnummer()).toException();
        }
        var origBehandling = behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsak.getId())
            .orElseThrow(() -> RevurderingFeil.FACTORY.tjenesteFinnerIkkeBehandlingForRevurdering(fagsak.getId()).toException());

        var enhet = behandlendeEnhetTjeneste.finnBehandlendeEnhetFor(fagsak);
        return manuell
            ? revurderingTjeneste.opprettManuellRevurdering(origBehandling, behandlingÅrsakType, enhet, periode)
            : revurderingTjeneste.opprettAutomatiskRevurdering(origBehandling, behandlingÅrsakType, enhet);
    }

    public boolean kanOppretteNyBehandlingAvType(Long fagsakId, BehandlingType type) {
        boolean finnesÅpneBehandlingerAvType = behandlingRepository.hentÅpneBehandlingerForFagsakId(fagsakId, type).size() > 0;
        if (finnesÅpneBehandlingerAvType) {
            return false;
        }
        switch (type) {
            case FØRSTEGANGSSØKNAD:
                return kanOppretteFørstegangsbehandling(fagsakId);
            case REVURDERING:
                return kanOppretteRevurdering(fagsakId);
            default:
                return false;
        }
    }

    public Map<BehandlingÅrsakType, List<Periode>> perioderMedGjennomførtKontroll(Long fagsakId) {
        var behandling = behandlingRepository.finnSisteInnvilgetBehandling(fagsakId).orElse(null);
        if (behandling == null) {
            return Map.of(BehandlingÅrsakType.RE_KONTROLL_REGISTER_INNTEKT, List.of());
        }
        if (!behandling.erYtelseBehandling()) {
            throw new IllegalStateException("Behandling må være av ytelsestype for å kunne hente perioder med kontrollert inntekt");
        }
        List<Periode> kontrollertInntektPerioder = tilkjentYtelseRepository.hentKontrollertInntektPerioder(behandling.getId())
            .get()
            .getPerioder()
            .stream()
            .map(KontrollertInntektPeriode::getPeriode)
            .map(p -> new Periode(p.getFomDato(), p.getTomDato()))
            .toList();
        return Map.of(BehandlingÅrsakType.RE_KONTROLL_REGISTER_INNTEKT, kontrollertInntektPerioder);
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

    interface BehandlingsoppretterTjenesteFeil extends DeklarerteFeil {
        BehandlingsoppretterTjenesteFeil FACTORY = FeilFactory.create(BehandlingsoppretterTjenesteFeil.class); // NOSONAR

        @FunksjonellFeil(feilkode = "FP-663487", feilmelding = "Fagsak med saksnummer %s oppfyller ikke kravene for revurdering", løsningsforslag = "", logLevel = INFO)
        Feil kanIkkeOppretteRevurdering(Saksnummer saksnummer);

    }

}
