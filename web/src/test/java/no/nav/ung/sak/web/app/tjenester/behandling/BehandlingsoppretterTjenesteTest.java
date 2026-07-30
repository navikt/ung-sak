package no.nav.ung.sak.web.app.tjenester.behandling;

import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.kontroll.KontrollertInntektKilde;
import no.nav.ung.kodeverk.produksjonsstyring.OrganisasjonsEnhet;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.ung.sak.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.ung.sak.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.ung.sak.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårBuilder;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriodeBuilder;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.KontrollertInntektPeriode;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.TilkjentYtelseRepository;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.klage.domenetjenester.KlageVurderingTjeneste;
import no.nav.ung.sak.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;
import no.nav.ung.sak.test.util.behandling.ungdomsprogramytelse.TestScenarioBuilder;
import no.nav.ung.sak.trigger.ProsessTriggere;
import no.nav.ung.sak.trigger.ProsessTriggereRepository;
import no.nav.ung.sak.trigger.Trigger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
class BehandlingsoppretterTjenesteTest {

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
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;

    @Inject
    private KlageVurderingTjeneste klageVurderingTjeneste;

    @Inject
    private HistorikkinnslagRepository historikkinnslagRepository;

    @Inject
    private TilkjentYtelseRepository tilkjentYtelseRepository;

    @Inject
    private VilkårResultatRepository vilkårResultatRepository;

    @Inject
    private PersonopplysningRepository personopplysningRepository;

    @Inject
    @Any
    private Instance<GyldigePerioderForRevurderingPrÅrsakUtleder> gyldigePerioderForRevurderingUtledere;

    private Behandling behandling;
    private BehandlendeEnhetTjeneste behandlendeEnhetTjeneste;

    @BeforeEach
    void setUp() {
        behandlendeEnhetTjeneste = Mockito.mock(BehandlendeEnhetTjeneste.class);
        when(behandlendeEnhetTjeneste.finnBehandlendeEnhetFor(any())).thenReturn(new OrganisasjonsEnhet("1234", "Nav Test"));
        behandlingsoppretterTjeneste = new BehandlingsoppretterTjeneste(repositoryProvider, behandlendeEnhetTjeneste, gyldigePerioderForRevurderingUtledere, behandlingskontrollTjeneste, klageVurderingTjeneste, personopplysningRepository, historikkinnslagRepository);
    }

    @Test
    void skalOppretteProsesstriggerNårPeriodeErOppgitt() {
        behandling = opprettRevurderingsKandidat(FagsakYtelseType.UNGDOMSYTELSE);
        Fagsak fagsak = behandling.getFagsak();
        var periode = fagsak.getPeriode();

        tilkjentYtelseRepository.lagre(behandling.getId(), List.of(
            KontrollertInntektPeriode.ny().medPeriode(periode).medInntekt(BigDecimal.ZERO).medKilde(KontrollertInntektKilde.REGISTER).build()
        ));

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
        behandling = opprettRevurderingsKandidat(FagsakYtelseType.UNGDOMSYTELSE);
        Fagsak fagsak = behandling.getFagsak();
        var revurdering = behandlingsoppretterTjeneste.opprettManuellRevurdering(fagsak, BehandlingÅrsakType.RE_SATS_ENDRING, Optional.empty());
        assertTrue(revurdering.erRevurdering());

        Optional<ProsessTriggere> prosessTriggere = prosessTriggereRepository.hentGrunnlag(revurdering.getId());
        assertTrue(prosessTriggere.isPresent());

        Set<Trigger> triggere = prosessTriggere.get().getTriggere();
        assertEquals(1, triggere.size());
        assertEquals(triggere.iterator().next().getPeriode(), fagsak.getPeriode());
    }

    @Test
    void skalReturnerePerioderMedGjennomfortKontroll() {
        behandling = opprettRevurderingsKandidat(FagsakYtelseType.UNGDOMSYTELSE);
        Fagsak fagsak = behandling.getFagsak();
        var perioderMedGjennomfortKontroll = behandlingsoppretterTjeneste.finnGyldigeVurderingsperioderPrÅrsak(fagsak);
        assertNotNull(perioderMedGjennomfortKontroll);
        assertTrue(perioderMedGjennomfortKontroll.stream().anyMatch(it -> it.årsak() == BehandlingÅrsakType.RE_KONTROLL_REGISTER_INNTEKT));
    }

    @Test
    void skalOppretteRevurderingForEndretBostedNårPeriodeErInnenfor() {
        behandling = opprettRevurderingsKandidat(FagsakYtelseType.AKTIVITETSPENGER);
        Fagsak fagsak = behandling.getFagsak();
        var fagsakPeriode = fagsak.getPeriode();

        opprettBostedsvilkår(behandling, fagsakPeriode);

        var revurdering = behandlingsoppretterTjeneste.opprettManuellRevurdering(fagsak, BehandlingÅrsakType.ENDRET_BOSTED, Optional.of(fagsakPeriode));
        assertTrue(revurdering.erRevurdering());

        Optional<ProsessTriggere> prosessTriggere = prosessTriggereRepository.hentGrunnlag(revurdering.getId());
        assertTrue(prosessTriggere.isPresent());
        Set<Trigger> triggere = prosessTriggere.get().getTriggere();
        assertEquals(1, triggere.size());
        assertEquals(triggere.iterator().next().getPeriode(), fagsakPeriode);
    }

    @Test
    void skalFeileForEndretBostedNårPeriodeErOppgittOgUtenforVilkårsperioder() {
        behandling = opprettRevurderingsKandidat(FagsakYtelseType.AKTIVITETSPENGER);
        Fagsak fagsak = behandling.getFagsak();
        var fagsakPeriode = fagsak.getPeriode();

        opprettBostedsvilkår(behandling, fagsakPeriode);

        var periodeUtenfor = DatoIntervallEntitet.fraOgMedTilOgMed(
            fagsakPeriode.getTomDato().plusDays(1), fagsakPeriode.getTomDato().plusDays(30));

        assertThrows(IllegalArgumentException.class, () ->
            behandlingsoppretterTjeneste.opprettManuellRevurdering(fagsak, BehandlingÅrsakType.ENDRET_BOSTED, Optional.of(periodeUtenfor)));
    }

    private Behandling opprettRevurderingsKandidat(FagsakYtelseType ytelseType) {
        var scenario = TestScenarioBuilder.builderMedSøknad(ytelseType);
        var b = scenario.lagre(repositoryProvider);
        final BehandlingVedtak behandlingVedtak = BehandlingVedtak.builder(b.getId())
            .medAnsvarligSaksbehandler("asdf").build();
        b.avsluttBehandling();
        behandlingRepository.lagre(b, behandlingRepository.taSkriveLås(b));
        behandlingVedtakRepository.lagre(behandlingVedtak, behandlingRepository.taSkriveLås(b));
        return b;
    }

    private void opprettBostedsvilkår(Behandling behandling, DatoIntervallEntitet periode) {
        var vilkårene = Vilkårene.builder()
            .leggTil(new VilkårBuilder(VilkårType.BOSTEDSVILKÅR)
                .leggTil(new VilkårPeriodeBuilder().medPeriode(periode).medUtfall(Utfall.OPPFYLT)))
            .build();
        vilkårResultatRepository.lagre(behandling.getId(), vilkårene);
    }
}
