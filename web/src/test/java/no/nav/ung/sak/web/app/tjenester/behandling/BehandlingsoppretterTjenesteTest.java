package no.nav.ung.sak.web.app.tjenester.behandling;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.produksjonsstyring.OrganisasjonsEnhet;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.ung.sak.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.ung.sak.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.TilkjentYtelseRepository;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;
import no.nav.ung.sak.test.util.UnitTestLookupInstanceImpl;
import no.nav.ung.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.ung.sak.trigger.ProsessTriggere;
import no.nav.ung.sak.trigger.ProsessTriggereRepository;
import no.nav.ung.sak.trigger.Trigger;
import no.nav.ung.sak.web.app.tjenester.behandling.kontroll.GyldigePerioderForRevurderingAvInntektskontrollPrÅrsakUtleder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
class BehandlingsoppretterTjenesteTest {
    @Inject
    private EntityManager entityManager;

    private BehandlingsoppretterTjeneste behandlingsoppretterTjeneste;

    @Inject
    private BehandlingRepository behandlingRepository;

    @Inject
    private BehandlingRepositoryProvider repositoryProvider;

    @Inject
    private ProsessTriggereRepository prosessTriggereRepository;

    @Inject
    private BehandlingVedtakRepository behandlingVedtakRepository;

    @Inject
    TilkjentYtelseRepository tilkjentYtelseRepository;

    private Behandling behandling;
    private BehandlendeEnhetTjeneste behandlendeEnhetTjeneste;

    @BeforeEach
    void setUp() {
        opprettRevurderingsKandidat();
        behandlendeEnhetTjeneste = Mockito.mock(BehandlendeEnhetTjeneste.class);
        when(behandlendeEnhetTjeneste.finnBehandlendeEnhetFor(any())).thenReturn(new OrganisasjonsEnhet("1234", "Nav Test"));
        this.behandlingsoppretterTjeneste = new BehandlingsoppretterTjeneste(repositoryProvider, behandlendeEnhetTjeneste, new UnitTestLookupInstanceImpl<>(new GyldigePerioderForRevurderingAvInntektskontrollPrÅrsakUtleder(tilkjentYtelseRepository, behandlingRepository)));
    }

    @Test
    void skalOppretteProsesstriggerNårPeriodeErOppgitt() {
        Fagsak fagsak = behandling.getFagsak();
        var periode = behandling.getFagsak().getPeriode();
        var revurdering = behandlingsoppretterTjeneste.opprettManuellRevurdering(fagsak, BehandlingÅrsakType.RE_KONTROLL_REGISTER_INNTEKT, Optional.of(periode));
        assertTrue(revurdering.erRevurdering());

        Optional<ProsessTriggere> prosessTriggere = prosessTriggereRepository.hentGrunnlag(revurdering.getId());
        assertTrue(prosessTriggere.isPresent());

        Set<Trigger> triggere = prosessTriggere.get().getTriggere();
        assertEquals(1, triggere.size());
        assertEquals(triggere.iterator().next().getPeriode(), periode);
    }

    @Test
    void skalOppretteProsesstriggerNårPeriodeIkkeErOppgitt() {
        Fagsak fagsak = behandling.getFagsak();
        var revurdering = behandlingsoppretterTjeneste.opprettManuellRevurdering(fagsak, BehandlingÅrsakType.RE_KONTROLL_REGISTER_INNTEKT, Optional.empty());
        assertTrue(revurdering.erRevurdering());

        Optional<ProsessTriggere> prosessTriggere = prosessTriggereRepository.hentGrunnlag(revurdering.getId());
        assertTrue(prosessTriggere.isPresent());

        Set<Trigger> triggere = prosessTriggere.get().getTriggere();
        assertEquals(1, prosessTriggere.get().getTriggere().size());
        assertEquals(triggere.iterator().next().getPeriode(), fagsak.getPeriode());
    }

    @Test
    void skalReturnerePerioderMedGjennomfortKontroll() {
        Fagsak fagsak = behandling.getFagsak();
        var perioderMedGjennomfortKontroll = behandlingsoppretterTjeneste.finnGyldigeVurderingsperioderPrÅrsak(fagsak.getId());
        assertNotNull(perioderMedGjennomfortKontroll);
        assertTrue(perioderMedGjennomfortKontroll.stream().anyMatch(it -> it.årsak() == BehandlingÅrsakType.RE_KONTROLL_REGISTER_INNTEKT));
    }

    private Behandling opprettRevurderingsKandidat() {

        var scenario = TestScenarioBuilder.builderMedSøknad();

        behandling = scenario.lagre(repositoryProvider);
        final BehandlingVedtak behandlingVedtak = BehandlingVedtak.builder(behandling.getId())
            .medAnsvarligSaksbehandler("asdf").build();
        behandling.avsluttBehandling();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
        behandlingVedtakRepository.lagre(behandlingVedtak, behandlingRepository.taSkriveLås(behandling));

        return behandling;
    }
}
