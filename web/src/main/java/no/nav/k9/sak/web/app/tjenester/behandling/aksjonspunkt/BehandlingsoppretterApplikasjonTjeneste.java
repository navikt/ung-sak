package no.nav.k9.sak.web.app.tjenester.behandling.aksjonspunkt;

import static no.nav.vedtak.feil.LogLevel.ERROR;
import static no.nav.vedtak.feil.LogLevel.INFO;

import java.util.Objects;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.produksjonsstyring.OrganisasjonsEnhet;
import no.nav.k9.sak.behandling.revurdering.NyBehandlingTjeneste;
import no.nav.k9.sak.behandling.revurdering.RevurderingFeil;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.mottak.dokumentmottak.SaksbehandlingDokumentmottakTjeneste;
import no.nav.k9.sak.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.vedtak.feil.Feil;
import no.nav.vedtak.feil.FeilFactory;
import no.nav.vedtak.feil.deklarasjon.DeklarerteFeil;
import no.nav.vedtak.feil.deklarasjon.FunksjonellFeil;

@Dependent
public class BehandlingsoppretterApplikasjonTjeneste {

    private BehandlendeEnhetTjeneste behandlendeEnhetTjeneste;
    private BehandlingRepository behandlingRepository;

    BehandlingsoppretterApplikasjonTjeneste() {
        // CDI
    }

    @SuppressWarnings("unused")
    @Inject
    public BehandlingsoppretterApplikasjonTjeneste(BehandlingRepositoryProvider behandlingRepositoryProvider,
                                                   SøknadRepository søknadRepository,
                                                   SaksbehandlingDokumentmottakTjeneste saksbehandlingDokumentmottakTjeneste,
                                                   BehandlendeEnhetTjeneste behandlendeEnhetTjeneste) {
        Objects.requireNonNull(behandlingRepositoryProvider, "behandlingRepositoryProvider");
        this.behandlendeEnhetTjeneste = behandlendeEnhetTjeneste;
        this.behandlingRepository = behandlingRepositoryProvider.getBehandlingRepository();
    }

    public Behandling opprettRevurdering(Fagsak fagsak, BehandlingÅrsakType behandlingÅrsakType) {
        var revurderingTjeneste = getNyBehandlingTjeneste(fagsak.getYtelseType(), BehandlingType.REVURDERING);
        if (!revurderingTjeneste.kanNyBehandlingOpprettes(fagsak)) {
            throw BehandlingsoppretterApplikasjonTjenesteFeil.FACTORY.kanIkkeOppretteRevurdering(fagsak.getSaksnummer()).toException();
        }

        Behandling origBehandling = behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsak.getId())
            .orElseThrow(() -> RevurderingFeil.FACTORY.tjenesteFinnerIkkeBehandlingForRevurdering(fagsak.getId()).toException());

        OrganisasjonsEnhet enhet = behandlendeEnhetTjeneste.finnBehandlendeEnhetFor(fagsak);
        return revurderingTjeneste.opprettManueltNyBehandling(fagsak, origBehandling, behandlingÅrsakType, enhet);
    }

    public Behandling opprettUnntaksbehandling(Fagsak fagsak, BehandlingÅrsakType behandlingÅrsakType) {
        var unntaksbehandlingTjeneste = getNyBehandlingTjeneste(fagsak.getYtelseType(), BehandlingType.UNNTAKSBEHANDLING);
        if (!unntaksbehandlingTjeneste.kanNyBehandlingOpprettes(fagsak)) {
            throw BehandlingsoppretterApplikasjonTjenesteFeil.FACTORY.kanIkkeOppretteUnntaksbehandling(fagsak.getSaksnummer()).toException();
        }

        Behandling origBehandling = behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsak.getId())
            .orElse(null);

        OrganisasjonsEnhet enhet = behandlendeEnhetTjeneste.finnBehandlendeEnhetFor(fagsak);
        return unntaksbehandlingTjeneste.opprettManueltNyBehandling(fagsak, origBehandling, behandlingÅrsakType, enhet);
    }

    private NyBehandlingTjeneste getNyBehandlingTjeneste(FagsakYtelseType ytelseType, BehandlingType behandlingType) {
        return BehandlingTypeRef.Lookup.find(NyBehandlingTjeneste.class, ytelseType, behandlingType)
            .orElseThrow(() -> new UnsupportedOperationException("Ikke implementert for " + NyBehandlingTjeneste.class.getSimpleName() +
                " for ytelsetype " + ytelseType + " , behandlingstype " + behandlingType));
    }

    interface BehandlingsoppretterApplikasjonTjenesteFeil extends DeklarerteFeil {
        BehandlingsoppretterApplikasjonTjenesteFeil FACTORY = FeilFactory.create(BehandlingsoppretterApplikasjonTjenesteFeil.class); // NOSONAR

        @FunksjonellFeil(feilkode = "FP-663487", feilmelding = "Fagsak med saksnummer %s oppfyller ikke kravene for revurdering", løsningsforslag = "", logLevel = INFO)
        Feil kanIkkeOppretteRevurdering(Saksnummer saksnummer);

        @FunksjonellFeil(feilkode = "FP-407002", feilmelding = "Fagsak med saksnummer %s oppfyller ikke kravene for unntaksbehandling", løsningsforslag = "", logLevel = INFO)
        Feil kanIkkeOppretteUnntaksbehandling(Saksnummer saksnummer);

        @FunksjonellFeil(feilkode = "FP-909861", feilmelding = "Det eksisterer allerede en åpen ytelsesbehandling eller det eksisterer ingen avsluttede behandlinger for fagsakId %s", løsningsforslag = "", logLevel = ERROR)
        Feil kanIkkeOppretteNyFørstegangsbehandling(Long fagsakId);

    }
}
