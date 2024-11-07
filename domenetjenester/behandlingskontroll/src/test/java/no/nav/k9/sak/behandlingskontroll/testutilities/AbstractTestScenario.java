package no.nav.k9.sak.behandlingskontroll.testutilities;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import no.nav.k9.kodeverk.behandling.BehandlingStatus;
import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.sak.behandlingskontroll.spi.BehandlingskontrollServiceProvider;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.Behandling.Builder;
import no.nav.k9.sak.behandlingslager.behandling.InternalManipulerBehandling;
import no.nav.k9.sak.behandlingslager.behandling.aksjonspunkt.AksjonspunktTestSupport;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Saksnummer;

/**
 * Default test scenario builder for å definere opp testdata med enkle defaults.
 * <p>
 * Oppretter en default behandling, inkludert default grunnlag med søknad + tomt innangsvilkårresultat.
 * <p>
 * Kan bruke settere (evt. legge til) for å tilpasse utgangspunktet.
 * <p>
 * Mer avansert bruk er ikke gitt at kan bruke denne
 * klassen.
 */
@SuppressWarnings("deprecation")
public abstract class AbstractTestScenario<S extends AbstractTestScenario<S>> {
    private static final AtomicLong FAKE_ID = new AtomicLong(100999L);
    private final FagsakBuilder fagsakBuilder;
    private Behandling behandling;

    private Fagsak fagsak;
    private BehandlingStegType startSteg;
    private BehandlingStatus status = BehandlingStatus.UTREDES;

    private Map<AksjonspunktDefinisjon, BehandlingStegType> aksjonspunktDefinisjoner = new HashMap<>();
    private BehandlingType behandlingType = BehandlingType.FØRSTEGANGSSØKNAD;

    protected AbstractTestScenario(FagsakYtelseType fagsakYtelseType) {
        this.fagsakBuilder = FagsakBuilder.nyFagsak(fagsakYtelseType);
    }

    public Behandling lagre(BehandlingskontrollServiceProvider repositoryProvider) {
        build(repositoryProvider);
        return behandling;
    }

    public void leggTilAksjonspunkt(AksjonspunktDefinisjon apDef, BehandlingStegType stegType) {
        aksjonspunktDefinisjoner.put(apDef, stegType);
    }

    @SuppressWarnings("unchecked")
    public S medBehandlingStegStart(BehandlingStegType startSteg) {
        this.startSteg = startSteg;
        return (S) this;
    }

    private void build(BehandlingskontrollServiceProvider repositoryProvider) {
        if (behandling != null) {
            throw new IllegalStateException("build allerede kalt.  Hent Behandling via getBehandling eller opprett nytt scenario.");
        }
        BehandlingRepository behandlingRepo = repositoryProvider.getBehandlingRepository();
        Builder behandlingBuilder = grunnBuild(repositoryProvider);

        this.behandling = behandlingBuilder.build();

        if (startSteg != null) {
            new InternalManipulerBehandling().forceOppdaterBehandlingSteg(behandling, startSteg);
        }

        leggTilAksjonspunkter(behandling);

        BehandlingLås lås = behandlingRepo.taSkriveLås(behandling);
        behandlingRepo.lagre(behandling, lås);

        // opprett og lagre resulater på behandling

        // få med behandlingsresultat etc.
        behandlingRepo.lagre(behandling, lås);
    }

    private Builder grunnBuild(BehandlingskontrollServiceProvider repositoryProvider) {
        FagsakRepository fagsakRepo = repositoryProvider.getFagsakRepository();

        lagFagsak(fagsakRepo);

        // oppprett og lagre behandling
        return Behandling.nyBehandlingFor(fagsak, behandlingType).medBehandlingStatus(status);
    }

    private void lagFagsak(FagsakRepository fagsakRepo) {
        fagsak = fagsakBuilder.build();
        Long fagsakId = fagsakRepo.opprettNy(fagsak); // NOSONAR //$NON-NLS-1$
        fagsak.setId(fagsakId);
    }

    private void leggTilAksjonspunkter(Behandling behandling) {
        aksjonspunktDefinisjoner.forEach(
            (apDef, stegType) -> {
                if (stegType != null) {
                    new AksjonspunktTestSupport().leggTilAksjonspunkt(behandling, apDef, stegType);
                } else {
                    new AksjonspunktTestSupport().leggTilAksjonspunkt(behandling, apDef);
                }
            });
    }

    public static class FagsakBuilder {

        private FagsakYtelseType fagsakYtelseType;
        private AktørId aktørId = AktørId.dummy();

        private FagsakBuilder(FagsakYtelseType fagsakYtelseType) {
            this.fagsakYtelseType = fagsakYtelseType;
        }

        public static FagsakBuilder nyFagsak(FagsakYtelseType fagsakYtelseType) {
            return new FagsakBuilder(fagsakYtelseType);
        }

        public AktørId getAktørId() {
            return aktørId;
        }

        public FagsakBuilder medBruker(AktørId aktørId) {
            this.aktørId = aktørId;
            return this;
        }

        public Fagsak build() {
            return Fagsak.opprettNy(fagsakYtelseType, aktørId, new Saksnummer("" + FAKE_ID.getAndIncrement()));
        }
    }

}
