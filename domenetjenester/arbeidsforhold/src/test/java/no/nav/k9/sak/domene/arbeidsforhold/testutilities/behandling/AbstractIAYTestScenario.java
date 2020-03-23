package no.nav.k9.sak.domene.arbeidsforhold.testutilities.behandling;

import static java.util.Collections.singletonList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.concurrent.atomic.AtomicLong;

import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.behandling.FagsakStatus;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.Behandling.Builder;
import no.nav.k9.sak.behandlingslager.behandling.opptjening.OpptjeningRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.behandlingslager.virksomhet.VirksomhetRepository;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.vedtak.felles.testutilities.Whitebox;

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
abstract class AbstractIAYTestScenario<S extends AbstractIAYTestScenario<S>> {

    private static final AtomicLong FAKE_ID = new AtomicLong(100999L);
    private final FagsakBuilder fagsakBuilder;
    private Behandling behandling;

    private Fagsak fagsak;

    private Long fagsakId = nyId();
    private BehandlingRepository mockBehandlingRepository;
    private BehandlingType behandlingType = BehandlingType.FØRSTEGANGSSØKNAD;

    private IAYRepositoryProvider repositoryProvider;

    protected AbstractIAYTestScenario(FagsakYtelseType fagsakYtelseType) {
        this.fagsakBuilder = FagsakBuilder
            .nyFagsak(fagsakYtelseType)
            .medSaksnummer(new Saksnummer(nyId() + ""));
    }

    private static long nyId() {
        return FAKE_ID.getAndIncrement();
    }

    private BehandlingRepository lagBasicMockBehandlingRepository(IAYRepositoryProvider repositoryProvider) {
        BehandlingRepository behandlingRepository = mock(BehandlingRepository.class);

        when(repositoryProvider.getBehandlingRepository()).thenReturn(behandlingRepository);

        FagsakRepository mockFagsakRepository = mockFagsakRepository();
        VirksomhetRepository virksomhetRepository = mock(VirksomhetRepository.class);
        OpptjeningRepository opptjeningRepository = Mockito.mock(OpptjeningRepository.class);
        // ikke ideelt å la mocks returnere mocks, men forenkler enormt mye test kode, forhindrer feil oppsett, så det
        // blir enklere å refactorere

        when(repositoryProvider.getBehandlingRepository()).thenReturn(behandlingRepository);
        when(repositoryProvider.getFagsakRepository()).thenReturn(mockFagsakRepository);
        when(repositoryProvider.getVirksomhetRepository()).thenReturn(virksomhetRepository);
        when(repositoryProvider.getOpptjeningRepository()).thenReturn(opptjeningRepository);

        return behandlingRepository;
    }

    /**
     * Hjelpe metode for å håndtere mock repository.
     */
    private BehandlingRepository mockBehandlingRepository() {
        if (mockBehandlingRepository != null) {
            return mockBehandlingRepository;
        }
        repositoryProvider = mock(IAYRepositoryProvider.class);
        BehandlingRepository behandlingRepository = lagBasicMockBehandlingRepository(repositoryProvider);

        ArgumentCaptor<Behandling> behandlingCaptor = ArgumentCaptor.forClass(Behandling.class);
        when(behandlingRepository.taSkriveLås(behandlingCaptor.capture())).thenAnswer((Answer<BehandlingLås>) invocation -> {
            Behandling beh = invocation.getArgument(0);
            return new BehandlingLås(beh.getId()) {
            };
        });

        mockBehandlingRepository = behandlingRepository;
        return behandlingRepository;
    }

    private FagsakRepository mockFagsakRepository() {
        FagsakRepository fagsakRepository = mock(FagsakRepository.class);
        when(fagsakRepository.hentForBruker(Mockito.any(AktørId.class))).thenAnswer(a -> singletonList(fagsak));

        ArgumentCaptor<Fagsak> fagsakCaptor = ArgumentCaptor.forClass(Fagsak.class);
        when(fagsakRepository.opprettNy(fagsakCaptor.capture())).thenAnswer(invocation -> {
            Fagsak fagsak = invocation.getArgument(0); // NOSONAR
            Long id = fagsak.getId();
            if (id == null) {
                id = fagsakId;
                Whitebox.setInternalState(fagsak, "id", id);
            }
            return id;
        });

        // oppdater fagsakstatus
        Mockito.doAnswer(invocation -> {
            FagsakStatus status = invocation.getArgument(1);
            Whitebox.setInternalState(fagsak, "fagsakStatus", status);
            return null;
        }).when(fagsakRepository)
            .oppdaterFagsakStatus(eq(fagsakId), Mockito.any(FagsakStatus.class));

        return fagsakRepository;
    }

    public Behandling lagre(IAYRepositoryProvider repositoryProvider) {
        build(repositoryProvider.getBehandlingRepository(), repositoryProvider);
        return behandling;
    }

    private BehandlingRepository lagMockedRepositoryForOpprettingAvBehandlingInternt() {
        if (mockBehandlingRepository != null && behandling != null) {
            return mockBehandlingRepository;
        }
        mockBehandlingRepository = mockBehandlingRepository();

        lagre(repositoryProvider); // NOSONAR //$NON-NLS-1$
        return mockBehandlingRepository;
    }

    public Behandling lagMocked() {
        lagMockedRepositoryForOpprettingAvBehandlingInternt();
        return behandling;
    }

    private void build(BehandlingRepository behandlingRepo, IAYRepositoryProvider repositoryProvider) {
        if (behandling != null) {
            throw new IllegalStateException("build allerede kalt.  Hent Behandling via getBehandling eller opprett nytt scenario.");
        }
        Builder behandlingBuilder = grunnBuild(repositoryProvider);

        this.behandling = behandlingBuilder.build();

        BehandlingLås lås = behandlingRepo.taSkriveLås(behandling);
        behandlingRepo.lagre(behandling, lås);

        repositoryProvider.getBehandlingRepository().lagre(behandling, lås);
    }

    private Builder grunnBuild(IAYRepositoryProvider repositoryProvider) {
        FagsakRepository fagsakRepo = repositoryProvider.getFagsakRepository();

        if (fagsak == null) {
            lagFagsak(fagsakRepo);
        }

        // oppprett og lagre behandling
        Builder behandlingBuilder = Behandling.nyBehandlingFor(fagsak, behandlingType);

        return behandlingBuilder;

    }

    private void lagFagsak(FagsakRepository fagsakRepo) {
        fagsak = fagsakBuilder.build();
        Long fagsakId = fagsakRepo.opprettNy(fagsak); // NOSONAR //$NON-NLS-1$
        fagsak.setId(fagsakId);
    }

    public AktørId getDefaultBrukerAktørId() {
        return fagsakBuilder.getBruker();
    }

}
