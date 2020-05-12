package no.nav.k9.sak.domene.behandling.steg.foreslåvedtak;


import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.k9.kodeverk.arbeidsforhold.AktivitetStatus;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.BeregningTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BGAndelArbeidsforhold;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.Beregningsgrunnlag;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPeriode;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.k9.kodeverk.arbeidsforhold.AktivitetStatus;
import no.nav.k9.kodeverk.behandling.BehandlingResultatType;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRevurderingRepository;
import no.nav.k9.sak.behandlingslager.behandling.vedtak.VedtakVarsel;
import no.nav.k9.sak.behandlingslager.behandling.vedtak.VedtakVarselRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatBuilder;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.db.util.UnittestRepositoryRule;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.k9.sak.typer.Arbeidsgiver;

public class ForeslåVedtakRevurderingStegImplTest {

    @Rule
    public final UnittestRepositoryRule repoRule = new UnittestRepositoryRule();
    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule().silent();
    @Mock
    private ForeslåVedtakTjeneste foreslåVedtakTjeneste;
    @Mock
    private BehandleStegResultat behandleStegResultat;
    @Mock
    private BehandlingRevurderingRepository behandlingRevurderingRepository;
    @Mock
    private VedtakVarselRepository vedtakVarselRepository;
    @Mock
    private BehandlingRepository behandlingRepository;
    @Mock
    private BeregningTjeneste beregningsgrunnlagTjeneste;
    @Mock
    private VilkårResultatRepository vilkårResultatRepository;
    @Mock
    private VedtakVarsel behandlingsresultat;
    @Mock
    private VedtakVarsel orginalBehandlingsresultat;

    private BehandlingRepositoryProvider repositoryProvider = mock(BehandlingRepositoryProvider.class);
    private ForeslåVedtakRevurderingStegImpl foreslåVedtakRevurderingStegForeldrepenger;

    private Behandling orginalBehandling;
    private Behandling revurdering;
    private BehandlingskontrollKontekst kontekstRevurdering;

    @Before
    public void before() {
        when(repositoryProvider.getBehandlingRepository()).thenReturn(behandlingRepository);
        when(repositoryProvider.getBehandlingRevurderingRepository()).thenReturn(behandlingRevurderingRepository);
        when(repositoryProvider.getVilkårResultatRepository()).thenReturn(vilkårResultatRepository);

        var vilkårResultatBuilder = new VilkårResultatBuilder();
        vilkårResultatBuilder.leggTilIkkeVurderteVilkår(List.of(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now(), LocalDate.now().plusDays(10))), VilkårType.BEREGNINGSGRUNNLAGVILKÅR);
        when(vilkårResultatRepository.hent(any())).thenReturn(vilkårResultatBuilder.build());

        orginalBehandling = TestScenarioBuilder.builderMedSøknad().lagMocked();
        orginalBehandling.setBehandlingResultatType(BehandlingResultatType.INNVILGET);
        orginalBehandling.avsluttBehandling();
        revurdering = TestScenarioBuilder.builderMedSøknad()
            .medBehandlingType(BehandlingType.REVURDERING)
            .medOriginalBehandling(orginalBehandling, BehandlingÅrsakType.BERØRT_BEHANDLING)
            .lagMocked();


        kontekstRevurdering = mock(BehandlingskontrollKontekst.class);
        BehandlingLås behandlingLås = mock(BehandlingLås.class);
        when(kontekstRevurdering.getBehandlingId()).thenReturn(revurdering.getId());
        when(kontekstRevurdering.getSkriveLås()).thenReturn(behandlingLås);
        when(behandlingRepository.hentBehandling(kontekstRevurdering.getBehandlingId())).thenReturn(revurdering);

        foreslåVedtakRevurderingStegForeldrepenger =
            new ForeslåVedtakRevurderingStegImpl(foreslåVedtakTjeneste, beregningsgrunnlagTjeneste, repositoryProvider);
        when(foreslåVedtakTjeneste.foreslåVedtak(revurdering, kontekstRevurdering)).thenReturn(behandleStegResultat);
        when(behandleStegResultat.getAksjonspunktResultater()).thenReturn(Collections.emptyList());
    }

    @Test
    public void skal_ikke_opprette_aksjonspunkt_når_samme_beregningsgrunnlag() {
        when(beregningsgrunnlagTjeneste.hentFastsatt(eq(BehandlingReferanse.fra(orginalBehandling)), any())).thenReturn(Optional.of(buildBeregningsgrunnlag(1000L)));
        when(beregningsgrunnlagTjeneste.hentFastsatt(eq(BehandlingReferanse.fra(revurdering)), any())).thenReturn(Optional.of(buildBeregningsgrunnlag(1000L)));

        BehandleStegResultat behandleStegResultat = foreslåVedtakRevurderingStegForeldrepenger.utførSteg(kontekstRevurdering);
        assertThat(behandleStegResultat.getAksjonspunktListe()).isEmpty();
    }

    @Test
    public void skal_opprette_aksjonspunkt_når_revurdering_har_mindre_beregningsgrunnlag() {
        when(beregningsgrunnlagTjeneste.hentFastsatt(eq(BehandlingReferanse.fra(orginalBehandling)), any())).thenReturn(Optional.of(buildBeregningsgrunnlag(1000L)));
        when(beregningsgrunnlagTjeneste.hentFastsatt(eq(BehandlingReferanse.fra(revurdering)), any())).thenReturn(Optional.of(buildBeregningsgrunnlag(900L)));

        BehandleStegResultat behandleStegResultat = foreslåVedtakRevurderingStegForeldrepenger.utførSteg(kontekstRevurdering);
        assertThat(behandleStegResultat.getAksjonspunktListe().get(0)).isEqualTo(AksjonspunktDefinisjon.KONTROLLER_REVURDERINGSBEHANDLING_VARSEL_VED_UGUNST);
    }

    @Test
    public void testTilbakehopp() {
        // Arrange

        // Act
        foreslåVedtakRevurderingStegForeldrepenger.vedHoppOverBakover(kontekstRevurdering, null, null, null);

        // Assert
        revurdering = behandlingRepository.hentBehandling(revurdering.getId());
        assertThat(revurdering.getBehandlingResultatType()).isEqualTo(BehandlingResultatType.IKKE_FASTSATT);
    }

    private Beregningsgrunnlag buildBeregningsgrunnlag(Long bruttoPerÅr) {
        Beregningsgrunnlag beregningsgrunnlag = Beregningsgrunnlag.builder()
            .medSkjæringstidspunkt(LocalDate.now())
            .medGrunnbeløp(BigDecimal.valueOf(91425))
            .build();
        BeregningsgrunnlagPeriode periode = BeregningsgrunnlagPeriode.builder()
            .medBeregningsgrunnlagPeriode(LocalDate.now().minusDays(1), LocalDate.now().plusDays(1))
            .medBruttoPrÅr(BigDecimal.valueOf(bruttoPerÅr))
            .leggTilBeregningsgrunnlagPrStatusOgAndel(BeregningsgrunnlagPrStatusOgAndel.builder()
                .medBruttoPrÅr(BigDecimal.valueOf(bruttoPerÅr))
                .medRedusertBrukersAndelPrÅr(BigDecimal.valueOf(bruttoPerÅr))
                .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
                .medBGAndelArbeidsforhold(BGAndelArbeidsforhold.builder()
                    .medArbeidsgiver(Arbeidsgiver.virksomhet("000000000"))))
            .build(beregningsgrunnlag);
        BeregningsgrunnlagPrStatusOgAndel.builder().medAktivitetStatus(AktivitetStatus.FRILANSER)
            .medBeregnetPrÅr(BigDecimal.valueOf(bruttoPerÅr))
            .medRedusertBrukersAndelPrÅr(BigDecimal.valueOf(bruttoPerÅr))
            .build(periode);
        BeregningsgrunnlagPeriode.builder(periode).build(beregningsgrunnlag);
        return beregningsgrunnlag;
    }

}
