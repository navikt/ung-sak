package no.nav.k9.sak.web.app.tjenester.behandling.aksjonspunkt;

import static no.nav.vedtak.feil.LogLevel.ERROR;
import static no.nav.vedtak.feil.LogLevel.INFO;

import java.util.Objects;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.kodeverk.produksjonsstyring.OrganisasjonsEnhet;
import no.nav.k9.sak.behandling.revurdering.RevurderingFeil;
import no.nav.k9.sak.behandling.revurdering.RevurderingTjeneste;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
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
        RevurderingTjeneste revurderingTjeneste = FagsakYtelseTypeRef.Lookup.find(RevurderingTjeneste.class, fagsak.getYtelseType()).orElseThrow();
        Boolean kanRevurderingOpprettes = revurderingTjeneste.kanRevurderingOpprettes(fagsak);
        if (!kanRevurderingOpprettes) {
            throw BehandlingsoppretterApplikasjonTjenesteFeil.FACTORY.kanIkkeOppretteRevurdering(fagsak.getSaksnummer()).toException();
        }

        // TODO (essv): Behandling til revurdering skal mottas fra GUI, ikke utledes backend
        Behandling origBehandling = behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsak.getId())
            .orElseThrow(() -> RevurderingFeil.FACTORY.tjenesteFinnerIkkeBehandlingForRevurdering(fagsak.getId()).toException());

        OrganisasjonsEnhet enhet = behandlendeEnhetTjeneste.finnBehandlendeEnhetFor(fagsak);
        return revurderingTjeneste.opprettManuellRevurdering(origBehandling, behandlingÅrsakType, enhet);
    }

    interface BehandlingsoppretterApplikasjonTjenesteFeil extends DeklarerteFeil {
        BehandlingsoppretterApplikasjonTjenesteFeil FACTORY = FeilFactory.create(BehandlingsoppretterApplikasjonTjenesteFeil.class); // NOSONAR

        @FunksjonellFeil(feilkode = "FP-663487", feilmelding = "Fagsak med saksnummer %s oppfyller ikke kravene for revurdering", løsningsforslag = "", logLevel = INFO)
        Feil kanIkkeOppretteRevurdering(Saksnummer saksnummer);

        @FunksjonellFeil(feilkode = "FP-909861", feilmelding = "Det eksisterer allerede en åpen ytelsesbehandling eller det eksisterer ingen avsluttede behandlinger for fagsakId %s", løsningsforslag = "", logLevel = ERROR)
        Feil kanIkkeOppretteNyFørstegangsbehandling(Long fagsakId);

    }
}
