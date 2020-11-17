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

import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.junit.Rule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

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
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatBuilder;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.db.util.JpaExtension;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.vedtak.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.vedtak.felles.testutilities.cdi.UnitTestLookupInstanceImpl;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ForeslåVedtakRevurderingStegImplTest {

    @Inject
    private EntityManager entityManager;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule().silent();
    @Mock
    private ForeslåVedtakTjeneste foreslåVedtakTjeneste;
    @Mock
    private BehandleStegResultat behandleStegResultat;
    @Mock
    private BehandlingRevurderingRepository behandlingRevurderingRepository;
    @Mock
    private BehandlingRepository behandlingRepository;
    @Mock
    private BeregningTjeneste beregningsgrunnlagTjeneste;
    @Mock
    private VilkårResultatRepository vilkårResultatRepository;

    private DefaultErEndringIBeregningTjeneste endringIBeregningTjeneste;

    private BehandlingRepositoryProvider repositoryProvider = mock(BehandlingRepositoryProvider.class);
    private ForeslåVedtakRevurderingStegImpl foreslåVedtakRevurderingSteg;

    private Behandling orginalBehandling;
    private Behandling revurdering;
    private BehandlingskontrollKontekst kontekstRevurdering;

    @BeforeEach
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
        when(behandlingRepository.hentBehandling(orginalBehandling.getId())).thenReturn(orginalBehandling);

        endringIBeregningTjeneste = new DefaultErEndringIBeregningTjeneste(beregningsgrunnlagTjeneste);

        foreslåVedtakRevurderingSteg =
            new ForeslåVedtakRevurderingStegImpl(foreslåVedtakTjeneste, repositoryProvider, new UnitTestLookupInstanceImpl<>(endringIBeregningTjeneste));
        when(foreslåVedtakTjeneste.foreslåVedtak(revurdering, kontekstRevurdering)).thenReturn(behandleStegResultat);
        when(behandleStegResultat.getAksjonspunktResultater()).thenReturn(Collections.emptyList());
    }

    @Test
    public void skal_ikke_opprette_aksjonspunkt_når_samme_beregningsgrunnlag() {
        when(beregningsgrunnlagTjeneste.hentEksaktFastsatt(eq(BehandlingReferanse.fra(orginalBehandling)), any())).thenReturn(List.of(buildBeregningsgrunnlag(1000L)));
        when(beregningsgrunnlagTjeneste.hentEksaktFastsatt(eq(BehandlingReferanse.fra(revurdering)), any())).thenReturn(List.of(buildBeregningsgrunnlag(1000L)));

        BehandleStegResultat behandleStegResultat = foreslåVedtakRevurderingSteg.utførSteg(kontekstRevurdering);
        assertThat(behandleStegResultat.getAksjonspunktListe()).isEmpty();
    }

    @Test
    public void skal_opprette_aksjonspunkt_når_revurdering_har_mindre_beregningsgrunnlag() {
        when(beregningsgrunnlagTjeneste.hentEksaktFastsatt(eq(BehandlingReferanse.fra(orginalBehandling)), any())).thenReturn(List.of(buildBeregningsgrunnlag(1000L)));
        when(beregningsgrunnlagTjeneste.hentEksaktFastsatt(eq(BehandlingReferanse.fra(revurdering)), any())).thenReturn(List.of(buildBeregningsgrunnlag(100L)));

        BehandleStegResultat behandleStegResultat = foreslåVedtakRevurderingSteg.utførSteg(kontekstRevurdering);
        assertThat(behandleStegResultat.getAksjonspunktListe().get(0)).isEqualTo(AksjonspunktDefinisjon.KONTROLLER_REVURDERINGSBEHANDLING_VARSEL_VED_UGUNST);
    }

    @Test
    public void testTilbakehopp() {
        // Arrange

        // Act
        foreslåVedtakRevurderingSteg.vedHoppOverBakover(kontekstRevurdering, null, null, null);

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
