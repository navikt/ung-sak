package no.nav.k9.sak.ytelse.pleiepengerbarn.beregningsgrunnlag;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.OpptjeningForBeregningTjeneste;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.k9.felles.testutilities.cdi.UnitTestLookupInstanceImpl;
import no.nav.k9.kodeverk.arbeidsforhold.InntektsKilde;
import no.nav.k9.kodeverk.arbeidsforhold.InntektspostType;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.db.util.JpaExtension;
import no.nav.k9.sak.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.domene.iay.modell.InntektBuilder;
import no.nav.k9.sak.domene.iay.modell.InntektspostBuilder;
import no.nav.k9.sak.domene.iay.modell.OppgittOpptjeningBuilder;
import no.nav.k9.sak.domene.iay.modell.VersjonType;
import no.nav.k9.sak.domene.opptjening.OppgittOpptjeningFilter;
import no.nav.k9.sak.domene.opptjening.OppgittOpptjeningFilterProvider;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Saksnummer;


@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
class FinnInntekterFraFørsteVedtaksdatoTest {

    public static final AktørId AKTØR_ID = AktørId.dummy();
    public static final String ORG_NR = "889640782";
    public static final LocalDate STP1 = LocalDate.now();
    public static final LocalDate STP2 = LocalDate.now().plusDays(10);

    @Inject
    private EntityManager entityManager;
    private FagsakRepository fagsakRepository;
    private FinnInntekterFraFørsteVedtaksdato tjeneste;
    private BehandlingRepository behandlingRepository;
    private AbakusInMemoryInntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;
    private VilkårsPerioderTilVurderingTjeneste vilkårsPerioderTilVurderingTjeneste = mock(VilkårsPerioderTilVurderingTjeneste.class);
    private OpptjeningForBeregningTjeneste opptjeningForBeregningTjeneste = mock(OpptjeningForBeregningTjeneste.class);
    private OppgittOpptjeningFilter defaultOpptjeningFilter = new OppgittOpptjeningFilter() {
    };

    @BeforeEach
    void setUp() {
        fagsakRepository = new FagsakRepository(entityManager);
        behandlingRepository = new BehandlingRepository(entityManager);
        inntektArbeidYtelseTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();
        OppgittOpptjeningFilterProvider mocKProvider = mock(OppgittOpptjeningFilterProvider.class);
        when(mocKProvider.finnOpptjeningFilter(anyLong())).thenReturn(defaultOpptjeningFilter);
        tjeneste = new FinnInntekterFraFørsteVedtaksdato(behandlingRepository,
            new UnitTestLookupInstanceImpl<>(new PSBOpptjeningForBeregningTjeneste(null, mocKProvider, null)),
            inntektArbeidYtelseTjeneste,
            new UnitTestLookupInstanceImpl<>(vilkårsPerioderTilVurderingTjeneste)
        );
    }

    @Test
    void skal_finne_fra_denne_behandlingen_for_førstegangsbehandling() {
        // Arrange
        var årsbeløpSigrun = BigDecimal.TEN;
        var behandling = lagFørstegangsbehandling();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
        lagIAYMedSigruninntekt(behandling.getId(), STP1, årsbeløpSigrun);
        inntektArbeidYtelseTjeneste.lagreOppgittOpptjening(behandling.getId(), lagEgenNæringOpptjening(STP1));

        InntektArbeidYtelseGrunnlag iayGrunnlag = inntektArbeidYtelseTjeneste.hentGrunnlag(behandling.getId());


        // Act
        var inntekter = tjeneste.finnInntekter(BehandlingReferanse.fra(behandling), iayGrunnlag, STP1);

        // Assert
        assertThat(inntekter.size()).isEqualTo(1);
        assertThat(inntekter.get(0).getAlleInntektsposter().iterator().next().getBeløp().getVerdi()).isEqualTo(årsbeløpSigrun);
    }


    @Test
    void skal_finne_fra_førstegangsbehandling_ved_revurdering() {
        // Arrange
        var årsbeløpSigrun = BigDecimal.TEN;
        var førstegangsbehandling = lagFørstegangsbehandling();
        behandlingRepository.lagre(førstegangsbehandling, behandlingRepository.taSkriveLås(førstegangsbehandling));
        settPeriodeTilVurdering(førstegangsbehandling, STP1);
        lagIAYMedSigruninntekt(førstegangsbehandling.getId(), STP1, årsbeløpSigrun);
        inntektArbeidYtelseTjeneste.lagreOppgittOpptjening(førstegangsbehandling.getId(), lagEgenNæringOpptjening(STP1));

        var årsbeløpSigrunRevurdering = BigDecimal.valueOf(6432);
        var revurdering = lagRevurdering(førstegangsbehandling);
        behandlingRepository.lagre(revurdering, behandlingRepository.taSkriveLås(revurdering));
        settPeriodeTilVurdering(revurdering, STP1);
        lagIAYMedSigruninntekt(revurdering.getId(), STP1, årsbeløpSigrunRevurdering);
        inntektArbeidYtelseTjeneste.lagreOppgittOpptjening(revurdering.getId(), lagEgenNæringOpptjening(STP1));

        InntektArbeidYtelseGrunnlag iayGrunnlagRevurdering = inntektArbeidYtelseTjeneste.hentGrunnlag(revurdering.getId());

            // Act
        var inntekter = tjeneste.finnInntekter(BehandlingReferanse.fra(revurdering), iayGrunnlagRevurdering, STP1);

        // Assert
        assertThat(inntekter.size()).isEqualTo(1);
        assertThat(inntekter.get(0).getAlleInntektsposter().iterator().next().getBeløp().getVerdi()).isEqualTo(årsbeløpSigrun);
    }

    @Test
    void skal_finne_fra_revurdering_ved_nytt_skjæringstidspunkt() {
        // Arrange
        var årsbeløpSigrun = BigDecimal.TEN;
        var førstegangsbehandling = lagFørstegangsbehandling();
        behandlingRepository.lagre(førstegangsbehandling, behandlingRepository.taSkriveLås(førstegangsbehandling));
        settPeriodeTilVurdering(førstegangsbehandling, STP1);
        lagIAYMedSigruninntekt(førstegangsbehandling.getId(), STP1, årsbeløpSigrun);
        inntektArbeidYtelseTjeneste.lagreOppgittOpptjening(førstegangsbehandling.getId(), lagEgenNæringOpptjening(STP1));

        var årsbeløpSigrunRevurdering = BigDecimal.valueOf(6432);
        var revurdering = lagRevurdering(førstegangsbehandling);
        behandlingRepository.lagre(revurdering, behandlingRepository.taSkriveLås(revurdering));
        settPeriodeTilVurdering(revurdering, STP2);
        lagIAYMedSigruninntekt(revurdering.getId(), STP2, årsbeløpSigrunRevurdering);
        inntektArbeidYtelseTjeneste.lagreOppgittOpptjening(revurdering.getId(), lagEgenNæringOpptjening(STP2));

        InntektArbeidYtelseGrunnlag iayGrunnlagRevurdering = inntektArbeidYtelseTjeneste.hentGrunnlag(revurdering.getId());

        // Act
        var inntekter = tjeneste.finnInntekter(BehandlingReferanse.fra(revurdering), iayGrunnlagRevurdering, STP2);

        // Assert
        assertThat(inntekter.size()).isEqualTo(1);
        assertThat(inntekter.get(0).getAlleInntektsposter().iterator().next().getBeløp().getVerdi()).isEqualTo(årsbeløpSigrunRevurdering);
    }

    @Test
    void skal_finne_fra_førstegangsbehandling_ved_andre_revurdering_dersom_første_ikkje_vurderte_skjæringstidspunkt() {
        // Arrange
        var årsbeløpSigrun = BigDecimal.TEN;
        var førstegangsbehandling = lagFørstegangsbehandling();
        behandlingRepository.lagre(førstegangsbehandling, behandlingRepository.taSkriveLås(førstegangsbehandling));
        settPeriodeTilVurdering(førstegangsbehandling, STP1);
        lagIAYMedSigruninntekt(førstegangsbehandling.getId(), STP1, årsbeløpSigrun);
        inntektArbeidYtelseTjeneste.lagreOppgittOpptjening(førstegangsbehandling.getId(), lagEgenNæringOpptjening(STP1));

        var årsbeløpSigrunRevurdering = BigDecimal.valueOf(6432);
        var revurdering = lagRevurdering(førstegangsbehandling);
        behandlingRepository.lagre(revurdering, behandlingRepository.taSkriveLås(revurdering));
        settPeriodeTilVurdering(revurdering, STP2);
        lagIAYMedSigruninntekt(revurdering.getId(), STP2, årsbeløpSigrunRevurdering);
        inntektArbeidYtelseTjeneste.lagreOppgittOpptjening(revurdering.getId(), lagEgenNæringOpptjening(STP2));


        var årsbeløpSigrunRevurdering2 = BigDecimal.valueOf(2535);
        var revurdering2 = lagRevurdering(revurdering);
        behandlingRepository.lagre(revurdering2, behandlingRepository.taSkriveLås(revurdering2));
        settPeriodeTilVurdering(revurdering2, STP1);
        lagIAYMedSigruninntekt(revurdering2.getId(), STP1, årsbeløpSigrunRevurdering2);
        inntektArbeidYtelseTjeneste.lagreOppgittOpptjening(revurdering2.getId(), lagEgenNæringOpptjening(STP1));

        InntektArbeidYtelseGrunnlag iayGrunnlagRevurdering2 = inntektArbeidYtelseTjeneste.hentGrunnlag(revurdering2.getId());

        // Act
        var inntekter = tjeneste.finnInntekter(BehandlingReferanse.fra(revurdering2), iayGrunnlagRevurdering2, STP1);

        // Assert
        assertThat(inntekter.size()).isEqualTo(1);
        assertThat(inntekter.get(0).getAlleInntektsposter().iterator().next().getBeløp().getVerdi()).isEqualTo(årsbeløpSigrun);
    }


    private void settPeriodeTilVurdering(Behandling førstegangsbehandling, LocalDate stp) {
        var perioder = new TreeSet<DatoIntervallEntitet>();
        perioder.add(DatoIntervallEntitet.fraOgMedTilOgMed(stp, stp.plusDays(3)));
        when(vilkårsPerioderTilVurderingTjeneste.utled(førstegangsbehandling.getId(), VilkårType.BEREGNINGSGRUNNLAGVILKÅR))
            .thenReturn(perioder);
    }


    private void lagIAYMedSigruninntekt(Long behandlingId, LocalDate stp, BigDecimal årsbeløpSigrun) {
        var iayBuilder = InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.empty(), VersjonType.REGISTER);

        var aktørInntektBuilder = iayBuilder.getAktørInntektBuilder(AKTØR_ID);
        var sigrunInntekt = InntektBuilder.oppdatere(Optional.empty())
            .medInntektsKilde(InntektsKilde.SIGRUN)
            .leggTilInntektspost(InntektspostBuilder.ny().medInntektspostType(InntektspostType.SELVSTENDIG_NÆRINGSDRIVENDE)
                .medPeriode(stp.minusYears(1).withDayOfYear(1), stp.withDayOfYear(1).minusDays(1))
                .medBeløp(årsbeløpSigrun));
        aktørInntektBuilder.leggTilInntekt(sigrunInntekt);
        iayBuilder.leggTilAktørInntekt(aktørInntektBuilder);
        inntektArbeidYtelseTjeneste.lagreIayAggregat(behandlingId, iayBuilder);
    }


    private Behandling lagFørstegangsbehandling() {
        Fagsak fagsak = Fagsak.opprettNy(FagsakYtelseType.PLEIEPENGER_SYKT_BARN, AKTØR_ID, new Saksnummer("123456"), LocalDate.now(), LocalDate.now().plusYears(1));
        fagsakRepository.opprettNy(fagsak);
        return Behandling.nyBehandlingFor(fagsak, BehandlingType.FØRSTEGANGSSØKNAD).build();
    }

    private Behandling lagRevurdering(Behandling førstegangsbehandling) {
        return Behandling.fraTidligereBehandling(førstegangsbehandling, BehandlingType.REVURDERING).build();
    }


    private OppgittOpptjeningBuilder lagEgenNæringOpptjening(LocalDate stp) {
        var ny = OppgittOpptjeningBuilder.ny();
        var egenNæringBuilder = OppgittOpptjeningBuilder.EgenNæringBuilder.ny();
        egenNæringBuilder.medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(stp.minusMonths(12), stp.plusMonths(1)));
        egenNæringBuilder.medVirksomhet(ORG_NR);
        ny.leggTilEgneNæringer(List.of(egenNæringBuilder));
        return ny;
    }
}
