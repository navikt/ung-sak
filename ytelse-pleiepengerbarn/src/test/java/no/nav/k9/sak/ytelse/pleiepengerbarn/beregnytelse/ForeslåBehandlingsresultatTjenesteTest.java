package no.nav.k9.sak.ytelse.pleiepengerbarn.beregnytelse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.konfigurasjon.konfig.Tid;
import no.nav.k9.kodeverk.behandling.BehandlingResultatType;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.kodeverk.vedtak.VedtakResultatType;
import no.nav.k9.kodeverk.vilkår.Avslagsårsak;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandling.Skjæringstidspunkt;
import no.nav.k9.sak.behandling.revurdering.ytelse.DefaultRevurderingBehandlingsresultatutleder;
import no.nav.k9.sak.behandling.revurdering.ytelse.RevurderingBehandlingsresultatutleder;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.BehandlingÅrsak;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.k9.sak.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.k9.sak.behandlingslager.behandling.vedtak.VedtakVarselRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårBuilder;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatBuilder;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.db.util.CdiDbAwareTest;
import no.nav.k9.sak.domene.behandling.steg.foreslåresultat.ForeslåBehandlingsresultatTjeneste;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.test.util.UnitTestLookupInstanceImpl;
import no.nav.k9.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.tjeneste.UttakInMemoryTjeneste;
import no.nav.pleiepengerbarn.uttak.kontrakter.AnnenPart;
import no.nav.pleiepengerbarn.uttak.kontrakter.LukketPeriode;
import no.nav.pleiepengerbarn.uttak.kontrakter.Utenlandsopphold;
import no.nav.pleiepengerbarn.uttak.kontrakter.UtenlandsoppholdÅrsak;
import no.nav.pleiepengerbarn.uttak.kontrakter.UttaksperiodeInfo;
import no.nav.pleiepengerbarn.uttak.kontrakter.Uttaksplan;

@CdiDbAwareTest
public class ForeslåBehandlingsresultatTjenesteTest {
    private static final LocalDate FOM = LocalDate.now();
    private static final LocalDate TOM = FOM.plusWeeks(6);

    private static final LocalDate SKJÆRINGSTIDSPUNKT = FOM;

    @Inject
    private UttakInMemoryTjeneste uttakTjeneste;

    private BehandlingRepositoryProvider repositoryProvider;

    @Inject
    private BehandlingVedtakRepository behandlingVedtakRepository;

    @Inject
    private BehandlingRepository behandlingRepository;

    private RevurderingBehandlingsresultatutleder revurderingBehandlingsresultatutleder;
    private ForeslåBehandlingsresultatTjeneste tjeneste;

    private VilkårsPerioderTilVurderingTjeneste vilkårsPerioderTilVurderingTjeneste = new VilkårsPerioderTilVurderingTjeneste() {
        @Override
        public NavigableSet<DatoIntervallEntitet> utled(Long behandlingId, VilkårType vilkårType) {
            return new TreeSet<>(List.of(DatoIntervallEntitet.fraOgMedTilOgMed(FOM, TOM)));
        }

        @Override
        public Map<VilkårType, NavigableSet<DatoIntervallEntitet>> utledRådataTilUtledningAvVilkårsperioder(Long behandlingId) {
            return null;
        }

        @Override
        public Set<VilkårType> definerendeVilkår() {
            return Set.of(VilkårType.MEDISINSKEVILKÅR_UNDER_18_ÅR, VilkårType.MEDISINSKEVILKÅR_18_ÅR);
        }

        @Override
        public int maksMellomliggendePeriodeAvstand() {
            return 0;
        }
    };
    private VedtakVarselRepository vedtakVarselRepository = mock(VedtakVarselRepository.class);
    private EntityManager entityManager;


    @BeforeEach
    public void setup() {
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        revurderingBehandlingsresultatutleder = Mockito.spy(new DefaultRevurderingBehandlingsresultatutleder());

        tjeneste = new UttakForeslåBehandlingsresultatTjeneste(
            repositoryProvider,
            vedtakVarselRepository,
            new UnitTestLookupInstanceImpl<>(vilkårsPerioderTilVurderingTjeneste),
            revurderingBehandlingsresultatutleder);
    }

    public void setEntityManager(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Test
    public void skalSetteBehandlingsresultatInnvilgetNårVilkårOppfylt() {
        // Arrange
        var scenario = TestScenarioBuilder.builderMedSøknad();
        Behandling behandling = scenario.lagre(repositoryProvider);
        inngangsvilkårOgUttak(behandling, Utfall.OPPFYLT);

        // Act
        foreslåBehandlingsresultat(behandling);

        // Assert
        assertThat(behandling.getBehandlingResultatType()).isEqualTo(BehandlingResultatType.INNVILGET);
    }

    private void foreslåBehandlingsresultat(Behandling behandling) {
        var ref = BehandlingReferanse.fra(behandling,
            Skjæringstidspunkt.builder()
                .medUtledetSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT)
                .build());
        tjeneste.foreslåBehandlingsresultatType(ref, lagKontekst(behandling));
    }

    private BehandlingskontrollKontekst lagKontekst(Behandling behandling) {
        return new BehandlingskontrollKontekst(behandling.getFagsakId(), behandling.getAktørId(), behandlingRepository.taSkriveLås(behandling));
    }

    @Test
    public void skalKalleBestemBehandlingsresultatForRevurderingNårInnvilgetRevurdering() {
        // Arrange
        var scenario = TestScenarioBuilder.builderMedSøknad();
        Behandling behandling = scenario.lagre(repositoryProvider);
        Behandling revurdering = lagRevurdering(behandling);
        inngangsvilkårOgUttak(revurdering, Utfall.OPPFYLT);

        // Act
        foreslåBehandlingsresultat(revurdering);

        // Assert
        verify(revurderingBehandlingsresultatutleder).bestemBehandlingsresultatForRevurdering(Mockito.any());
    }

    @Test
    public void skalSetteBehandlingsresultatAvslåttNårVilkårAvslåttFørstegangsbehandling() {
        // Arrange
        var scenario = TestScenarioBuilder.builderMedSøknad();
        Behandling behandling = scenario.lagre(repositoryProvider);
        lagBehandlingsresultat(behandling);

        // Act
        foreslåBehandlingsresultat(behandling);

        // Assert
        assertThat(behandling.getBehandlingResultatType()).isEqualTo(BehandlingResultatType.AVSLÅTT);
    }

    @Test
    public void skalKalleBestemBehandlingsresultatNårAvslåttRevurdering() {
        // Arrange
        var scenario = TestScenarioBuilder.builderMedSøknad();
        Behandling behandling = scenario.lagre(repositoryProvider);
        Behandling revurdering = lagRevurdering(behandling);
        inngangsvilkårOgUttak(revurdering, Utfall.IKKE_OPPFYLT);

        // Act
        foreslåBehandlingsresultat(revurdering);

        // Assert
        verify(revurderingBehandlingsresultatutleder).bestemBehandlingsresultatForRevurdering(any());
    }

    @Test
    public void skalSetteBehandlingsresultatAvslåttNårVilkårAvslåttFørstegangsbehandlingInfotrygd() {
        // Arrange
        var scenario = TestScenarioBuilder.builderMedSøknad();
        Behandling behandling = scenario.lagre(repositoryProvider);
        behandling.getFagsak().setSkalTilInfotrygd(true);
        lagBehandlingsresultat(behandling);

        // Act
        foreslåBehandlingsresultat(behandling);

        // Assert
        assertThat(behandling.getBehandlingResultatType()).isEqualTo(BehandlingResultatType.AVSLÅTT);
    }

    @Test
    public void skalKalleBestemBehandlingsresultatNårVilkårAvslåttRevurderingInfotrygd() {
        // Arrange
        var scenario = TestScenarioBuilder.builderMedSøknad();
        Behandling behandling = scenario.lagre(repositoryProvider);
        Behandling revurdering = lagRevurdering(behandling);
        inngangsvilkårOgUttak(revurdering, Utfall.IKKE_OPPFYLT);
        revurdering.getFagsak().setSkalTilInfotrygd(true);

        // Act
        foreslåBehandlingsresultat(revurdering);

        // Assert
        verify(revurderingBehandlingsresultatutleder).bestemBehandlingsresultatForRevurdering(any());
    }

    private Behandling lagRevurdering(Behandling originalBehandling) {
        Behandling revurdering = Behandling.fraTidligereBehandling(originalBehandling, BehandlingType.REVURDERING)
            .medBehandlingÅrsak(
                BehandlingÅrsak.builder(BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER)
                    .medManueltOpprettet(true))
            .build();
        behandlingRepository.lagre(revurdering, behandlingRepository.taSkriveLås(revurdering));
        return revurdering;
    }

    private void inngangsvilkårOgUttak(Behandling behandling, Utfall utfall) {
        BehandlingLås lås = behandlingRepository.taSkriveLås(behandling);

        var vilkårsresultatBuilder = Vilkårene.builder();
        leggTilVilkårMedUtfall(Utfall.OPPFYLT, vilkårsresultatBuilder, VilkårType.MEDISINSKEVILKÅR_UNDER_18_ÅR, null);
        if (utfall.equals(Utfall.OPPFYLT)) {
            leggTilVilkårMedUtfall(utfall, vilkårsresultatBuilder, VilkårType.OPPTJENINGSVILKÅRET, null);
            leggTilVilkårMedUtfall(utfall, vilkårsresultatBuilder, VilkårType.MEDLEMSKAPSVILKÅRET, null);
        } else {
            leggTilVilkårMedUtfall(utfall, vilkårsresultatBuilder, VilkårType.MEDLEMSKAPSVILKÅRET, Avslagsårsak.SØKER_ER_UTVANDRET);

        }
        final var vilkårResultat = vilkårsresultatBuilder.build();
        behandlingRepository.lagre(behandling, lås);
        repositoryProvider.getVilkårResultatRepository().lagre(behandling.getId(), vilkårResultat);
        if (utfall.equals(Utfall.OPPFYLT)) {
            lagreUttak(behandling);
        }
    }

    private void leggTilVilkårMedUtfall(Utfall utfall, VilkårResultatBuilder vilkårsresultatBuilder, VilkårType opptjeningsvilkåret,
                                        Avslagsårsak søkerErUtvandret) {
        final var opptjeningBuilder = vilkårsresultatBuilder.hentBuilderFor(opptjeningsvilkåret);
        opptjeningBuilder
            .leggTil(opptjeningBuilder.hentBuilderFor(Tid.TIDENES_BEGYNNELSE, Tid.TIDENES_ENDE).medUtfall(utfall).medAvslagsårsak(søkerErUtvandret));
        vilkårsresultatBuilder.leggTil(opptjeningBuilder);
    }

    private void lagreUttak(Behandling behandling) {
        var periode = new LukketPeriode(FOM, TOM);
        var uttaksplan = new Uttaksplan(Map.of(periode, new UttaksperiodeInfo(no.nav.pleiepengerbarn.uttak.kontrakter.Utfall.OPPFYLT,
            BigDecimal.valueOf(100), List.of(), BigDecimal.valueOf(100), null, Set.of(), Map.of(), BigDecimal.valueOf(100), null, Set.of(), behandling.getUuid().toString(), AnnenPart.ALENE, null, null, null, false, new Utenlandsopphold(null,
        UtenlandsoppholdÅrsak.INGEN))), List.of());

        uttakTjeneste.lagreUttakResultatPerioder(behandling.getFagsak().getSaksnummer(), behandling.getUuid(), uttaksplan);
    }

    private void lagBehandlingsresultat(Behandling behandling) {
        behandling.setBehandlingResultatType(BehandlingResultatType.AVSLÅTT);
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));

        BehandlingVedtak behandlingVedtak = BehandlingVedtak.builder(behandling.getId())
            .medVedtakResultatType(VedtakResultatType.AVSLAG)
            .medAnsvarligSaksbehandler("asdf").build();
        behandlingVedtakRepository.lagre(behandlingVedtak, behandlingRepository.taSkriveLås(behandling));

        final var vilkårBuilder = new VilkårBuilder(VilkårType.MEDLEMSKAPSVILKÅRET);
        final var vilkårResultat = Vilkårene.builder()
            .leggTil(vilkårBuilder.leggTil(vilkårBuilder.hentBuilderFor(Tid.TIDENES_BEGYNNELSE, Tid.TIDENES_ENDE).medUtfall(Utfall.IKKE_OPPFYLT)))
            .build();
        repositoryProvider.getVilkårResultatRepository().lagre(behandling.getId(), vilkårResultat);
    }

}
