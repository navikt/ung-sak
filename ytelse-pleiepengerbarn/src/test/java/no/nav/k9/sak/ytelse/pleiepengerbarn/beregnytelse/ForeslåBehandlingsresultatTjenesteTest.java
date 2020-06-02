package no.nav.k9.sak.ytelse.pleiepengerbarn.beregnytelse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.BeregningTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.BeregningsgrunnlagTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.KalkulusApiTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.KalkulusInMermoryTjeneste;
import no.nav.k9.kodeverk.behandling.BehandlingResultatType;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.kodeverk.uttak.UttakArbeidType;
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
import no.nav.k9.sak.db.util.UnittestRepositoryRule;
import no.nav.k9.sak.domene.behandling.steg.foreslåresultat.ForeslåBehandlingsresultatTjeneste;
import no.nav.k9.sak.domene.medlem.MedlemTjeneste;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.domene.uttak.UttakInMemoryTjeneste;
import no.nav.k9.sak.domene.uttak.repo.Søknadsperiode;
import no.nav.k9.sak.domene.uttak.repo.Søknadsperioder;
import no.nav.k9.sak.domene.uttak.repo.UttakAktivitet;
import no.nav.k9.sak.domene.uttak.repo.UttakAktivitetPeriode;
import no.nav.k9.sak.domene.uttak.repo.UttakRepository;
import no.nav.k9.sak.domene.uttak.uttaksplan.InnvilgetUttaksplanperiode;
import no.nav.k9.sak.domene.uttak.uttaksplan.Uttaksplan;
import no.nav.k9.sak.kontrakt.uttak.Periode;
import no.nav.k9.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.k9.sak.ytelse.beregning.grunnlag.BeregningPerioderGrunnlagRepository;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;
import no.nav.vedtak.felles.testutilities.cdi.UnitTestLookupInstanceImpl;
import no.nav.vedtak.konfig.Tid;
import no.nav.vedtak.util.Tuple;

@RunWith(CdiRunner.class)
public class ForeslåBehandlingsresultatTjenesteTest {
    private static final LocalDate FOM = LocalDate.now();
    private static final LocalDate TOM = FOM.plusWeeks(6);

    private static final LocalDate SKJÆRINGSTIDSPUNKT = FOM;

    @Rule
    public final UnittestRepositoryRule repoRule = new UnittestRepositoryRule();

    @Inject
    private UttakInMemoryTjeneste uttakTjeneste;

    private BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(repoRule.getEntityManager());

    @Inject
    private BehandlingVedtakRepository behandlingVedtakRepository;

    @Inject
    private BehandlingRepository behandlingRepository;

    private KalkulusApiTjeneste kalkulusApiTjeneste = new KalkulusInMermoryTjeneste();

    private BeregningPerioderGrunnlagRepository grunnlagRepository = new BeregningPerioderGrunnlagRepository(repoRule.getEntityManager(), repositoryProvider.getVilkårResultatRepository());
    private UnitTestLookupInstanceImpl<KalkulusApiTjeneste> kalkulusTjenester = new UnitTestLookupInstanceImpl<>(kalkulusApiTjeneste);
    private BeregningTjeneste kalkulusInMermoryTjeneste = new BeregningsgrunnlagTjeneste(kalkulusTjenester, behandlingRepository, repositoryProvider.getVilkårResultatRepository(), grunnlagRepository);

    private RevurderingBehandlingsresultatutleder revurderingBehandlingsresultatutleder;
    private ForeslåBehandlingsresultatTjeneste tjeneste;

    private MedlemTjeneste medlemTjeneste = mock(MedlemTjeneste.class);
    private UttakRepository uttakRepository = mock(UttakRepository.class);
    private VedtakVarselRepository vedtakVarselRepository = mock(VedtakVarselRepository.class);

    @Before
    public void setup() {
        when(uttakRepository.hentOppgittSøknadsperioder(anyLong())).thenReturn(new Søknadsperioder(Set.of(new Søknadsperiode(DatoIntervallEntitet.fraOgMedTilOgMed(FOM, TOM)))));
        when(uttakRepository.hentOppgittUttak(anyLong())).thenReturn(new UttakAktivitet(Set.of(new UttakAktivitetPeriode(FOM, TOM, UttakArbeidType.ARBEIDSTAKER, Duration.ofHours(10), BigDecimal.valueOf(100L)))));

        when(medlemTjeneste.utledVilkårUtfall(any())).thenReturn(new Tuple<>(Utfall.OPPFYLT, Avslagsårsak.UDEFINERT));
        revurderingBehandlingsresultatutleder = Mockito.spy(new DefaultRevurderingBehandlingsresultatutleder());
        tjeneste = new UttakForeslåBehandlingsresultatTjeneste(repositoryProvider,
            vedtakVarselRepository,
            uttakRepository,
            revurderingBehandlingsresultatutleder);
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
                .medSkjæringstidspunktOpptjening(SKJÆRINGSTIDSPUNKT)
                .medSkjæringstidspunktBeregning(SKJÆRINGSTIDSPUNKT)
                .medUtledetSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT)
                .medFørsteUttaksdato(SKJÆRINGSTIDSPUNKT.plusDays(1))
                .build());
        tjeneste.foreslåVedtakVarsel(ref, lagKontekst(behandling));
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
        verify(revurderingBehandlingsresultatutleder).bestemBehandlingsresultatForRevurdering(Mockito.any(), any(), anyBoolean());
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
        verify(revurderingBehandlingsresultatutleder).bestemBehandlingsresultatForRevurdering(any(), any(), anyBoolean());
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
        verify(revurderingBehandlingsresultatutleder).bestemBehandlingsresultatForRevurdering(any(), any(), anyBoolean());
    }

    private Behandling lagRevurdering(Behandling originalBehandling) {
        Behandling revurdering = Behandling.fraTidligereBehandling(originalBehandling, BehandlingType.REVURDERING)
            .medBehandlingÅrsak(
                BehandlingÅrsak.builder(BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER)
                    .medManueltOpprettet(true)
                    .medOriginalBehandling(originalBehandling))
            .build();
        behandlingRepository.lagre(revurdering, behandlingRepository.taSkriveLås(revurdering));
        return revurdering;
    }

    private void inngangsvilkårOgUttak(Behandling behandling, Utfall utfall) {
        BehandlingLås lås = behandlingRepository.taSkriveLås(behandling);

        var vilkårsresultatBuilder = Vilkårene.builder();
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
        var periode = new Periode(FOM, TOM);
        var uttaksplan = new Uttaksplan(Map.of(periode, new InnvilgetUttaksplanperiode(100, List.of())));

        uttakTjeneste.lagreUttakResultatPerioder(behandling.getFagsak().getSaksnummer(), behandling.getUuid(), uttaksplan);
    }

    private void lagBehandlingsresultat(Behandling behandling) {
        behandling.setBehandlingResultatType(BehandlingResultatType.AVSLÅTT);
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));

        BehandlingVedtak behandlingVedtak = BehandlingVedtak.builder(behandling.getId())
            .medVedtakResultatType(VedtakResultatType.AVSLAG)
            .medAnsvarligSaksbehandler("asdf").build();
        behandlingVedtakRepository.lagre(behandlingVedtak, behandlingRepository.taSkriveLås(behandling));

        final var vilkårBuilder = new VilkårBuilder().medType(VilkårType.MEDLEMSKAPSVILKÅRET);
        final var vilkårResultat = Vilkårene.builder()
            .leggTil(vilkårBuilder.leggTil(vilkårBuilder.hentBuilderFor(Tid.TIDENES_BEGYNNELSE, Tid.TIDENES_ENDE).medUtfall(Utfall.IKKE_OPPFYLT)))
            .build();
        repositoryProvider.getVilkårResultatRepository().lagre(behandling.getId(), vilkårResultat);
    }

}
