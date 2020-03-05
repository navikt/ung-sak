package no.nav.foreldrepenger.behandling.steg.foreslåresultat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Set;

import javax.persistence.EntityManager;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

import no.nav.folketrygdloven.beregningsgrunnlag.HentBeregningsgrunnlagTjeneste;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandling.revurdering.ytelse.HarEtablertYtelseImpl;
import no.nav.foreldrepenger.behandling.revurdering.ytelse.RevurderingBehandlingsresultatutleder;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.fordeling.Fordeling;
import no.nav.foreldrepenger.behandlingslager.behandling.fordeling.FordelingPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.fordeling.FordelingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakVarselRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultatBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.TestScenarioBuilder;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakResultatPerioderEntitet;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.medlem.MedlemTjeneste;
import no.nav.foreldrepenger.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.kodeverk.behandling.BehandlingResultatType;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.kodeverk.uttak.PeriodeResultatType;
import no.nav.k9.kodeverk.uttak.PeriodeResultatÅrsak;
import no.nav.k9.kodeverk.vedtak.VedtakResultatType;
import no.nav.k9.kodeverk.vilkår.Avslagsårsak;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.vedtak.konfig.Tid;
import no.nav.vedtak.util.Tuple;

public class ForeslåBehandlingsresultatTjenesteTest {
    private static final LocalDate FOM = LocalDate.now();
    private static final LocalDate TOM = FOM.plusWeeks(6);

    private static final LocalDate SKJÆRINGSTIDSPUNKT = FOM;

    @Rule
    public final UnittestRepositoryRule repoRule = new UnittestRepositoryRule();
    private final EntityManager entityManager = repoRule.getEntityManager();
    private BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(entityManager);
    private final BehandlingVedtakRepository behandlingVedtakRepository = repositoryProvider.getBehandlingVedtakRepository();
    private HentBeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste = new HentBeregningsgrunnlagTjeneste(repoRule.getEntityManager());
    private BehandlingRepository behandlingRepository = repositoryProvider.getBehandlingRepository();
    private UttakRepository uttakRepository = repositoryProvider.getUttakRepository();

    private RevurderingBehandlingsresultatutleder revurderingBehandlingsresultatutleder;
    private ForeslåBehandlingsresultatTjeneste tjeneste;

    private MedlemTjeneste medlemTjeneste = mock(MedlemTjeneste.class);
    private FordelingRepository fordelingRepository = mock(FordelingRepository.class);
    private VedtakVarselRepository vedtakVarselRepository = mock(VedtakVarselRepository.class);

    @Before
    public void setup() {
        when(fordelingRepository.hent(any())).thenReturn(new Fordeling(Set.of(new FordelingPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(FOM, TOM)))));

        when(medlemTjeneste.utledVilkårUtfall(any())).thenReturn(new Tuple<>(Utfall.OPPFYLT, Avslagsårsak.UDEFINERT));
        revurderingBehandlingsresultatutleder = Mockito.spy(new RevurderingBehandlingsresultatutleder(repositoryProvider,
            vedtakVarselRepository,
            beregningsgrunnlagTjeneste,
            new HarEtablertYtelseImpl(vedtakVarselRepository),
            medlemTjeneste));
        tjeneste = new ForeslåBehandlingsresultatTjeneste(repositoryProvider,
            vedtakVarselRepository,
            fordelingRepository,
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
        verify(revurderingBehandlingsresultatutleder).bestemBehandlingsresultatForRevurdering(Mockito.any(), anyBoolean());
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
        verify(revurderingBehandlingsresultatutleder).bestemBehandlingsresultatForRevurdering(any(), anyBoolean());
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
        verify(revurderingBehandlingsresultatutleder).bestemBehandlingsresultatForRevurdering(any(), anyBoolean());
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
        UttakResultatPerioderEntitet uttakResultatPerioder = new UttakResultatPerioderEntitet();
        UttakResultatPeriodeEntitet uttakResultatPeriode = new UttakResultatPeriodeEntitet.Builder(FOM, TOM)
            .medPeriodeResultat(PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.UKJENT)
            .build();
        uttakResultatPerioder.leggTilPeriode(uttakResultatPeriode);
        uttakRepository.lagreOpprinneligUttakResultatPerioder(behandling.getId(), uttakResultatPerioder);
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
