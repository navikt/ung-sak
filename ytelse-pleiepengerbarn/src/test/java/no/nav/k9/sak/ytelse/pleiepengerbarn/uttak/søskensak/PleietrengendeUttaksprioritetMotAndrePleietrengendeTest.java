package no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.søskensak;

import static no.nav.k9.sak.domene.typer.tid.AbstractLocalDateInterval.TIDENES_ENDE;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.folketrygdloven.beregningsgrunnlag.BgRef;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.KalkulusTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.Beregningsgrunnlag;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagGrunnlagBuilder;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.k9.kodeverk.behandling.BehandlingResultatType;
import no.nav.k9.kodeverk.behandling.BehandlingStatus;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.beregningsgrunnlag.BeregningsgrunnlagTilstand;
import no.nav.k9.kodeverk.vedtak.IverksettingStatus;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.k9.sak.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatBuilder;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriodeBuilder;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.db.util.JpaExtension;
import no.nav.k9.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.k9.sak.ytelse.beregning.grunnlag.BeregningPerioderGrunnlagRepository;
import no.nav.k9.sak.ytelse.beregning.grunnlag.BeregningsgrunnlagPeriode;

@ExtendWith(JpaExtension.class)
@ExtendWith(CdiAwareExtension.class)
class PleietrengendeUttaksprioritetMotAndrePleietrengendeTest {

    @Inject
    private EntityManager entityManager;

    @Inject
    private BehandlingRepository behandlingRepository;
    @Inject
    private FagsakRepository fagsakRepository;
    @Inject
    private VilkårResultatRepository vilkårResultatRepository;
    @Inject
    private BehandlingVedtakRepository behandlingVedtakRepository;

    private KalkulusTjeneste kalkulusTjeneste = mock(KalkulusTjeneste.class);

    private PleietrengendeUttaksprioritetMotAndrePleietrengende uttaksprioritet;

    private Behandling behandlingBarn1;
    private Behandling behandlingBarn2;
    private BeregningPerioderGrunnlagRepository beregningPerioderGrunnlagRepository;
    private Fagsak fagsakBarn2;
    private Fagsak fagsakBarn1;

    @BeforeEach
    void setUp() {
        beregningPerioderGrunnlagRepository = new BeregningPerioderGrunnlagRepository(entityManager, new VilkårResultatRepository(entityManager));
        uttaksprioritet = new PleietrengendeUttaksprioritetMotAndrePleietrengende(fagsakRepository, behandlingRepository, kalkulusTjeneste,
            beregningPerioderGrunnlagRepository, true, vilkårResultatRepository);

        var brukerAktørId = new AktørId(123L);
        var barn1AktørId = new AktørId(567L);
        fagsakBarn1 = Fagsak.opprettNy(FagsakYtelseType.PLEIEPENGER_SYKT_BARN, brukerAktørId, barn1AktørId, null, new Saksnummer("BARN1"), LocalDate.now().minusMonths(1), LocalDate.now());

        fagsakRepository.opprettNy(fagsakBarn1);
        behandlingBarn1 = Behandling.forFørstegangssøknad(fagsakBarn1).medBehandlingStatus(BehandlingStatus.AVSLUTTET).medAvsluttetDato(LocalDateTime.now()).build();
        behandlingRepository.lagre(behandlingBarn1, behandlingRepository.taSkriveLås(behandlingBarn1.getId()));
        BehandlingLås lås = behandlingRepository.taSkriveLås(behandlingBarn1);
        behandlingVedtakRepository.lagre(opprettBehandlingVedtak(behandlingBarn1), lås);

        var barn2AktørId = new AktørId(897L);
        fagsakBarn2 = Fagsak.opprettNy(FagsakYtelseType.PLEIEPENGER_SYKT_BARN, brukerAktørId, barn2AktørId, null, new Saksnummer("BARN2"), LocalDate.now().minusMonths(1), LocalDate.now());
        fagsakRepository.opprettNy(fagsakBarn2);
        behandlingBarn2 = Behandling.forFørstegangssøknad(fagsakBarn2).medBehandlingStatus(BehandlingStatus.UTREDES).build();
        behandlingRepository.lagre(behandlingBarn2, behandlingRepository.taSkriveLås(behandlingBarn2.getId()));
    }


    @Test
    void skal_returnere_en_sak_dersom_annen_fagsak_ikke_har_beregningsgrunnlag() {
        var stp = LocalDate.now();
        var vilkårPeriodeTom = stp.plusDays(7);
        var eksternReferanse = UUID.randomUUID();
        initVilkår(stp, vilkårPeriodeTom, behandlingBarn2);
        opprettBG(eksternReferanse, stp, BigDecimal.TEN, behandlingBarn2);

        var prio = uttaksprioritet.vurderUttakprioritetEgneSaker(fagsakBarn2.getId(), false);


        var segmenter = prio.toSegments();
        assertThat(segmenter.size()).isEqualTo(1);
        assertThat(segmenter.getFirst().getValue().size()).isEqualTo(1);
        assertThat(segmenter.getFirst().getValue().getFirst().getAktuellBehandlingUuid()).isEqualTo(behandlingBarn2.getUuid());
        assertThat(segmenter.getFirst().getFom()).isEqualTo(stp);
        assertThat(segmenter.getFirst().getTom()).isEqualTo(vilkårPeriodeTom);
    }

    @Test
    void skal_returnere_to_saker_med_prio_til_den_andre_fagsaken() {
        var stp = LocalDate.now();
        var vilkårPeriodeTom = stp.plusDays(7);
        var eksternReferanse = UUID.randomUUID();
        var eksternReferanse2 = UUID.randomUUID();

        initVilkår(stp, vilkårPeriodeTom, behandlingBarn2);
        opprettBG(eksternReferanse, stp, BigDecimal.TEN, behandlingBarn2);

        initVilkår(stp, vilkårPeriodeTom, behandlingBarn1);
        opprettBG(eksternReferanse2, stp, BigDecimal.valueOf(11), behandlingBarn1);

        var prio = uttaksprioritet.vurderUttakprioritetEgneSaker(fagsakBarn2.getId(), false);

        var segmenter = prio.toSegments();
        assertThat(segmenter.size()).isEqualTo(1);
        assertThat(segmenter.getFirst().getValue().size()).isEqualTo(2);
        assertThat(segmenter.getFirst().getValue().getFirst().getAktuellBehandlingUuid()).isEqualTo(behandlingBarn1.getUuid());
        assertThat(segmenter.getFirst().getValue().get(1).getAktuellBehandlingUuid()).isEqualTo(behandlingBarn2.getUuid());
        assertThat(segmenter.getFirst().getFom()).isEqualTo(stp);
        assertThat(segmenter.getFirst().getTom()).isEqualTo(vilkårPeriodeTom);
    }


    @Test
    void skal_returnere_to_saker_med_prio_til_denne_fagsaken() {
        var stp = LocalDate.now();
        var vilkårPeriodeTom = stp.plusDays(7);
        var eksternReferanse = UUID.randomUUID();
        var eksternReferanse2 = UUID.randomUUID();

        initVilkår(stp, vilkårPeriodeTom, behandlingBarn2);
        opprettBG(eksternReferanse, stp, BigDecimal.TEN, behandlingBarn2);

        initVilkår(stp, vilkårPeriodeTom, behandlingBarn1);
        opprettBG(eksternReferanse2, stp, BigDecimal.valueOf(9), behandlingBarn1);

        var prio = uttaksprioritet.vurderUttakprioritetEgneSaker(fagsakBarn2.getId(), false);

        var segmenter = prio.toSegments();
        assertThat(segmenter.size()).isEqualTo(1);
        assertThat(segmenter.getFirst().getValue().size()).isEqualTo(2);
        assertThat(segmenter.getFirst().getValue().getFirst().getAktuellBehandlingUuid()).isEqualTo(behandlingBarn2.getUuid());
        assertThat(segmenter.getFirst().getValue().get(1).getAktuellBehandlingUuid()).isEqualTo(behandlingBarn1.getUuid());
        assertThat(segmenter.getFirst().getFom()).isEqualTo(stp);
        assertThat(segmenter.getFirst().getTom()).isEqualTo(vilkårPeriodeTom);
    }


    @Test
    void skal_ikke_ta_hensyn_til_ikke_avsluttede_behandlinger() {
        var revurderingBarn1 = Behandling.fraTidligereBehandling(behandlingBarn1, BehandlingType.REVURDERING).build();
        behandlingRepository.lagre(revurderingBarn1, behandlingRepository.taSkriveLås(revurderingBarn1.getId()));

        var stp = LocalDate.now();
        var vilkårPeriodeTom = stp.plusDays(7);
        var eksternReferanse = UUID.randomUUID();
        var eksternReferanse2 = UUID.randomUUID();
        var eksternReferanse3 = UUID.randomUUID();

        initVilkår(stp, vilkårPeriodeTom, behandlingBarn2);
        opprettBG(eksternReferanse, stp, BigDecimal.TEN, behandlingBarn2);

        initVilkår(stp, vilkårPeriodeTom, behandlingBarn1);
        opprettBG(eksternReferanse2, stp, BigDecimal.valueOf(9), behandlingBarn1);

        initVilkår(stp, vilkårPeriodeTom, revurderingBarn1);
        opprettBG(eksternReferanse3, stp, BigDecimal.valueOf(11), revurderingBarn1);

        var prio = uttaksprioritet.vurderUttakprioritetEgneSaker(fagsakBarn2.getId(), false);

        var segmenter = prio.toSegments();
        assertThat(segmenter.size()).isEqualTo(1);
        assertThat(segmenter.getFirst().getValue().size()).isEqualTo(2);
        assertThat(segmenter.getFirst().getValue().getFirst().getAktuellBehandlingUuid()).isEqualTo(behandlingBarn2.getUuid());
        assertThat(segmenter.getFirst().getValue().get(1).getAktuellBehandlingUuid()).isEqualTo(behandlingBarn1.getUuid());
        assertThat(segmenter.getFirst().getFom()).isEqualTo(stp);
        assertThat(segmenter.getFirst().getTom()).isEqualTo(vilkårPeriodeTom);
    }

    @Test
    void skal_ta_hensyn_til_ikke_avsluttede_behandlinger() {
        var revurderingBarn1 = Behandling.fraTidligereBehandling(behandlingBarn1, BehandlingType.REVURDERING).build();
        behandlingRepository.lagre(revurderingBarn1, behandlingRepository.taSkriveLås(revurderingBarn1.getId()));

        var stp = LocalDate.now();
        var vilkårPeriodeTom = stp.plusDays(7);
        var eksternReferanse = UUID.randomUUID();
        var eksternReferanse2 = UUID.randomUUID();
        var eksternReferanse3 = UUID.randomUUID();

        initVilkår(stp, vilkårPeriodeTom, behandlingBarn2);
        opprettBG(eksternReferanse, stp, BigDecimal.TEN, behandlingBarn2);

        initVilkår(stp, vilkårPeriodeTom, behandlingBarn1);
        opprettBG(eksternReferanse2, stp, BigDecimal.valueOf(9), behandlingBarn1);

        initVilkår(stp, vilkårPeriodeTom, revurderingBarn1);
        opprettBG(eksternReferanse3, stp, BigDecimal.valueOf(11), revurderingBarn1);

        var prio = uttaksprioritet.vurderUttakprioritetEgneSaker(fagsakBarn2.getId(), true);

        var segmenter = prio.toSegments();
        assertThat(segmenter.size()).isEqualTo(1);
        assertThat(segmenter.getFirst().getValue().size()).isEqualTo(2);
        assertThat(segmenter.getFirst().getValue().getFirst().getAktuellBehandlingUuid()).isEqualTo(revurderingBarn1.getUuid());
        assertThat(segmenter.getFirst().getValue().get(1).getAktuellBehandlingUuid()).isEqualTo(behandlingBarn2.getUuid());
        assertThat(segmenter.getFirst().getFom()).isEqualTo(stp);
        assertThat(segmenter.getFirst().getTom()).isEqualTo(vilkårPeriodeTom);
    }

    @Test
    void skal_gi_prio_til_det_første_skjæringstidspunktet_om_like_bg() {
        var stp1 = LocalDate.now().minusMonths(1);
        var vilkårPeriodeTom1 = stp1.plusMonths(2);
        var eksternReferanse = UUID.randomUUID();

        var stp2 = LocalDate.now();
        var vilkårPeriodeTom2 = vilkårPeriodeTom1;
        var eksternReferanse2 = UUID.randomUUID();


        initVilkår(stp1, vilkårPeriodeTom1, behandlingBarn2);
        opprettBG(eksternReferanse, stp1, BigDecimal.TEN, behandlingBarn2);

        initVilkår(stp2, vilkårPeriodeTom2, behandlingBarn1);
        opprettBG(eksternReferanse2, stp2, BigDecimal.TEN, behandlingBarn1);

        var prio = uttaksprioritet.vurderUttakprioritetEgneSaker(fagsakBarn2.getId(), false);

        var segmenter = prio.toSegments();
        var segmentIterator = segmenter.iterator();
        assertThat(segmenter.size()).isEqualTo(2);
        var segment1 = segmentIterator.next();
        assertThat(segment1.getValue().size()).isEqualTo(1);
        assertThat(segment1.getValue().getFirst().getAktuellBehandlingUuid()).isEqualTo(behandlingBarn2.getUuid());
        assertThat(segment1.getFom()).isEqualTo(stp1);
        assertThat(segment1.getTom()).isEqualTo(stp2.minusDays(1));


        var segment2 = segmentIterator.next();
        assertThat(segment2.getValue().size()).isEqualTo(2);
        assertThat(segment2.getValue().getFirst().getAktuellBehandlingUuid()).isEqualTo(behandlingBarn2.getUuid());
        assertThat(segment2.getValue().get(1).getAktuellBehandlingUuid()).isEqualTo(behandlingBarn1.getUuid());
        assertThat(segment2.getFom()).isEqualTo(stp2);
        assertThat(segment2.getTom()).isEqualTo(vilkårPeriodeTom2);
    }


    private void opprettBG(UUID eksternReferanse, LocalDate stp, BigDecimal bruttoBg, Behandling behandling) {
        beregningPerioderGrunnlagRepository.lagre(behandling.getId(), new BeregningsgrunnlagPeriode(eksternReferanse, stp));
        var beregningsgrunnlag = Beregningsgrunnlag.builder().medSkjæringstidspunkt(stp)
            .leggTilBeregningsgrunnlagPeriode(no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPeriode.builder()
                .medBeregningsgrunnlagPeriode(stp, TIDENES_ENDE)
                .medBruttoPrÅr(bruttoBg))
            .build();
        var beregningsgrunnlagGrunnlagBuilder = BeregningsgrunnlagGrunnlagBuilder.oppdatere(Optional.empty()).medBeregningsgrunnlag(
            beregningsgrunnlag
        );
        when(kalkulusTjeneste.hentGrunnlag(ArgumentMatchers.eq(BehandlingReferanse.fra(behandling)),
            ArgumentMatchers.argThat(a -> a.contains(new BgRef(eksternReferanse, stp))))).thenReturn(
            List.of(beregningsgrunnlagGrunnlagBuilder.build(BeregningsgrunnlagTilstand.VURDERT_VILKÅR))
        );
    }

    private void initVilkår(LocalDate stp, LocalDate vilkårPeriodeTom, Behandling behandling) {
        VilkårResultatBuilder vilkårResultatBuilder = Vilkårene.builder();
        vilkårResultatBuilder.leggTil(vilkårResultatBuilder.hentBuilderFor(VilkårType.BEREGNINGSGRUNNLAGVILKÅR)
            .leggTil(new VilkårPeriodeBuilder()
                .medUtfall(Utfall.OPPFYLT)
                .medPeriode(stp, vilkårPeriodeTom)));
        vilkårResultatRepository.lagre(behandling.getId(), vilkårResultatBuilder.build());
    }

    private BehandlingVedtak opprettBehandlingVedtak(Behandling behandling) {
        BehandlingVedtak behandlingVedtak = BehandlingVedtak.builder(behandling.getId())
            .medVedtakstidspunkt(LocalDateTime.now().minusDays(3))
            .medAnsvarligSaksbehandler("E2354345")
            .medIverksettingStatus(IverksettingStatus.IVERKSATT)
            .build();
        return behandlingVedtak;
    }

}
