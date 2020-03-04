package no.nav.foreldrepenger.behandling.steg.vedtak;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.InternalManipulerBehandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.TestScenarioBuilder;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakResultatPerioderEntitet;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.uttak.OpphørUttakTjeneste;
import no.nav.foreldrepenger.domene.uttak.UttakRepositoryProvider;
import no.nav.foreldrepenger.domene.vedtak.impl.BehandlingVedtakEventPubliserer;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.k9.kodeverk.behandling.BehandlingResultatType;
import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.kodeverk.uttak.IkkeOppfyltÅrsak;
import no.nav.k9.kodeverk.uttak.InnvilgetÅrsak;
import no.nav.k9.kodeverk.uttak.PeriodeResultatType;
import no.nav.k9.kodeverk.vedtak.VedtakResultatType;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.vedtak.konfig.Tid;

public class BehandlingVedtakTjenesteTest {

    private static final LocalDate SKJÆRINGSTIDSPUNKT = LocalDate.now();

    @Rule
    public final UnittestRepositoryRule repoRule = new UnittestRepositoryRule();
    private final BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(repoRule.getEntityManager());
    private final InternalManipulerBehandling manipulerBehandling = new InternalManipulerBehandling();
    private BehandlingVedtakTjeneste behandlingVedtakTjeneste;
    private BehandlingRepository behandlingRepository = repositoryProvider.getBehandlingRepository();
    private UttakRepository uttakRepository = repositoryProvider.getUttakRepository();
    private BehandlingVedtakRepository behandlingVedtakRepository = repositoryProvider.getBehandlingVedtakRepository();

    @Before
    public void setUp() {
        BehandlingVedtakEventPubliserer behandlingVedtakEventPubliserer = mock(BehandlingVedtakEventPubliserer.class);
        SkjæringstidspunktTjeneste skjæringstidspunktTjeneste = mock(SkjæringstidspunktTjeneste.class);
        Skjæringstidspunkt skjæringstidspunkt = Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT).build();
        when(skjæringstidspunktTjeneste.getSkjæringstidspunkter(Mockito.any())).thenReturn(skjæringstidspunkt);
        OpphørUttakTjeneste opphørUttakTjeneste = new OpphørUttakTjeneste(new UttakRepositoryProvider(repositoryProvider.getEntityManager()));
        behandlingVedtakTjeneste = new BehandlingVedtakTjeneste(behandlingVedtakEventPubliserer, repositoryProvider, opphørUttakTjeneste, skjæringstidspunktTjeneste);
    }

    // Tester behandlingsresultattype OPPHØR med opphør etter skjæringstidspunkt
    @Test
    public void skal_opprette_behandlingsvedtak_for_revurdering_med_opphør_etter_skjæringstidspunkt() {
        // Arrange
        Behandling originalBehandling = lagInnvilgetOriginalBehandling();
        Behandling revurdering = Behandling.fraTidligereBehandling(originalBehandling, BehandlingType.REVURDERING)
            .medBehandlingÅrsak(BehandlingÅrsak.builder(BehandlingÅrsakType.RE_ANNET)
                .medOriginalBehandling(originalBehandling))
            .build();
        manipulerBehandling.forceOppdaterBehandlingSteg(revurdering, BehandlingStegType.FATTE_VEDTAK);
        var behandlingLås = lagreBehandling(revurdering);
        var fagsak = revurdering.getFagsak();
        var revurderingKontekst = new BehandlingskontrollKontekst(fagsak.getId(), fagsak.getAktørId(), behandlingLås);
        
        revurdering = oppdaterMedBehandlingsresultat(revurderingKontekst, BehandlingResultatType.OPPHØR);
        lagUttaksresultatOpphørEtterSkjæringstidspunkt(revurdering);

        // Act
        behandlingVedtakTjeneste.opprettBehandlingVedtak(revurderingKontekst, revurdering);

        // Assert
        Optional<BehandlingVedtak> vedtak = behandlingVedtakRepository.hentBehandlingVedtakForBehandlingId(revurdering.getId());
        assertThat(vedtak).isPresent();
        assertThat(vedtak.get().getVedtakResultatType()).isEqualTo(VedtakResultatType.INNVILGET);
    }

    // Tester behandlingsresultattype opphør for opphør på skjæringstidspunkt
    @Test
    public void skal_opprette_behandlingsvedtak_for_revurdering_med_opphør_på_skjæringstidspunkt() {
        // Arrange
        Behandling originalBehandling = lagInnvilgetOriginalBehandling();
        Behandling revurdering = Behandling.fraTidligereBehandling(originalBehandling, BehandlingType.REVURDERING)
            .medBehandlingÅrsak(BehandlingÅrsak.builder(BehandlingÅrsakType.RE_MANGLER_FØDSEL).medOriginalBehandling(originalBehandling)).build();
        manipulerBehandling.forceOppdaterBehandlingSteg(revurdering, BehandlingStegType.FATTE_VEDTAK);
        BehandlingLås behandlingLås = lagreBehandling(revurdering);
        Fagsak fagsak = revurdering.getFagsak();
        BehandlingskontrollKontekst revurderingKontekst = new BehandlingskontrollKontekst(fagsak.getId(), fagsak.getAktørId(), behandlingLås);
        revurdering = oppdaterMedBehandlingsresultat(revurderingKontekst, BehandlingResultatType.OPPHØR);
        lagUttaksresultatOpphørPåSkjæringstidspunkt(revurdering);

        // Act
        behandlingVedtakTjeneste.opprettBehandlingVedtak(revurderingKontekst, revurdering);

        // Assert
        Optional<BehandlingVedtak> vedtak = behandlingVedtakRepository.hentBehandlingVedtakForBehandlingId(revurdering.getId());
        assertThat(vedtak).isPresent();
        assertThat(vedtak.get().getVedtakResultatType()).isEqualTo(VedtakResultatType.AVSLAG);
    }

    private Behandling lagInnvilgetOriginalBehandling() {
        BehandlingskontrollKontekst kontekst = byggBehandlingsgrunnlag(BehandlingStegType.FATTE_VEDTAK);
        return oppdaterMedBehandlingsresultat(kontekst, BehandlingResultatType.INNVILGET);
    }

    private void lagUttaksresultatOpphørEtterSkjæringstidspunkt(Behandling revurdering) {
        UttakResultatPerioderEntitet uttakResultatPerioderEntitet = new UttakResultatPerioderEntitet();
        uttakResultatPerioderEntitet.leggTilPeriode(lagInnvilgetUttakPeriode(SKJÆRINGSTIDSPUNKT, SKJÆRINGSTIDSPUNKT.plusMonths(1)));
        uttakResultatPerioderEntitet.leggTilPeriode(lagOpphørtPeriode(SKJÆRINGSTIDSPUNKT.plusMonths(1).plusDays(1), SKJÆRINGSTIDSPUNKT.plusMonths(6)));
        uttakRepository.lagreOpprinneligUttakResultatPerioder(revurdering.getId(), uttakResultatPerioderEntitet);
    }

    private void lagUttaksresultatOpphørPåSkjæringstidspunkt(Behandling revurdering) {
        UttakResultatPerioderEntitet uttakResultatPerioderEntitet = new UttakResultatPerioderEntitet();
        uttakResultatPerioderEntitet.leggTilPeriode(lagOpphørtPeriode(SKJÆRINGSTIDSPUNKT, SKJÆRINGSTIDSPUNKT.plusMonths(6)));
        uttakRepository.lagreOpprinneligUttakResultatPerioder(revurdering.getId(), uttakResultatPerioderEntitet);
    }

    private UttakResultatPeriodeEntitet lagInnvilgetUttakPeriode(LocalDate fom, LocalDate tom) {
        return new UttakResultatPeriodeEntitet.Builder(fom, tom)
            .medPeriodeResultat(PeriodeResultatType.INNVILGET, InnvilgetÅrsak.UTTAK_OPPFYLT).build();
    }

    private UttakResultatPeriodeEntitet lagOpphørtPeriode(LocalDate fom, LocalDate tom) {
        return new UttakResultatPeriodeEntitet.Builder(fom, tom)
            .medPeriodeResultat(PeriodeResultatType.AVSLÅTT, IkkeOppfyltÅrsak.opphørsAvslagÅrsaker().iterator().next()).build();
    }

    private BehandlingLås lagreBehandling(Behandling behandling) {
        BehandlingLås behandlingLås = behandlingRepository.taSkriveLås(behandling);
        behandlingRepository.lagre(behandling, behandlingLås);
        return behandlingLås;
    }

    private Behandling oppdaterMedBehandlingsresultat(BehandlingskontrollKontekst kontekst, BehandlingResultatType behandlingResultatType) {
        Long behandlingId = kontekst.getBehandlingId();
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        behandling.setBehandlingResultatType(behandlingResultatType);
        BehandlingLås lås = kontekst.getSkriveLås();
        behandlingRepository.lagre(behandling, lås);

        boolean ikkeAvslått = !behandlingResultatType.equals(BehandlingResultatType.AVSLÅTT);
        final var vilkårResultatBuilder = Vilkårene.builder();
        final var vilkårBuilder = vilkårResultatBuilder.hentBuilderFor(VilkårType.MEDLEMSKAPSVILKÅRET);
        vilkårBuilder.leggTil(vilkårBuilder.hentBuilderFor(Tid.TIDENES_BEGYNNELSE, Tid.TIDENES_ENDE).medUtfall(ikkeAvslått ? Utfall.OPPFYLT : Utfall.IKKE_OPPFYLT));
        vilkårResultatBuilder.leggTil(vilkårBuilder);
        Vilkårene vilkårene = vilkårResultatBuilder.build();

        repositoryProvider.getVilkårResultatRepository().lagre(behandling.getId(), vilkårene);
        return behandlingRepository.hentBehandling(behandlingId);
    }

    private BehandlingskontrollKontekst byggBehandlingsgrunnlag(BehandlingStegType behandlingStegType) {
        var scenario = TestScenarioBuilder.builderMedSøknad();

        Behandling behandling = scenario
            .medBehandlingStegStart(behandlingStegType)
            .medBehandlendeEnhet("Stord")
            .medBehandlingsresultat(BehandlingResultatType.INNVILGET)    
            .lagre(repositoryProvider);

        Fagsak fagsak = behandling.getFagsak();
        return new BehandlingskontrollKontekst(fagsak.getId(), fagsak.getAktørId(), behandlingRepository.taSkriveLås(behandling));
    }

}
