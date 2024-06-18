package no.nav.folketrygdloven.beregningsgrunnlag.kalkulus;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import jakarta.inject.Inject;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.k9.felles.testutilities.cdi.UnitTestLookupInstanceImpl;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.db.util.JpaExtension;
import no.nav.k9.sak.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.k9.sak.domene.iay.modell.OppgittOpptjeningBuilder;
import no.nav.k9.sak.domene.iay.modell.VersjonType;
import no.nav.k9.sak.domene.opptjening.OppgittOpptjeningFilter;
import no.nav.k9.sak.domene.opptjening.OppgittOpptjeningFilterProvider;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.trigger.ProsessTriggereRepository;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.k9.sak.vilkår.VilkårTjeneste;
import no.nav.k9.sak.ytelse.beregning.grunnlag.BeregningPerioderGrunnlagRepository;
import no.nav.k9.sak.ytelse.beregning.grunnlag.BeregningsgrunnlagPeriode;
import no.nav.k9.sak.ytelse.beregning.grunnlag.PGIPeriode;

;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
class FastsettPGIPeriodeTjenesteTest {


    public static final LocalDate STP = LocalDate.now();
    public static final LocalDate STP2 = LocalDate.now().plusDays(3);

    public static final AktørId AKTØR_ID = AktørId.dummy();
    @Inject
    private BeregningPerioderGrunnlagRepository beregningPerioderGrunnlagRepository;
    @Inject
    private VilkårResultatRepository vilkårResultatRepository;
    @Inject
    private VilkårTjeneste vilkårTjeneste;
    @Inject
    private BehandlingRepository behandlingRepository;
    @Inject
    private ProsessTriggereRepository prosessTriggereRepository;
    @Inject
    private FagsakRepository fagsakRepository;

    private FastsettPGIPeriodeTjeneste fastsettPGIPeriodeTjeneste;
    private Behandling behandling;
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();
    private OppgittOpptjeningFilter oppgittOpptjeningFilter = mock(OppgittOpptjeningFilter.class);

    @BeforeEach
    void setUp() {
        fastsettPGIPeriodeTjeneste = new FastsettPGIPeriodeTjeneste(beregningPerioderGrunnlagRepository, vilkårTjeneste, inntektArbeidYtelseTjeneste, new OppgittOpptjeningFilterProvider(
            new UnitTestLookupInstanceImpl<>(oppgittOpptjeningFilter), behandlingRepository), prosessTriggereRepository
        );
        opprettBehandling();
        var registerBuilder = InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.empty(), VersjonType.REGISTER);
        inntektArbeidYtelseTjeneste.lagreIayAggregat(behandling.getId(), registerBuilder);
    }

    @Test
    void skal_ikke_gi_endring_ved_ingen_eksisterende_pgi_periode_og_ingen_oppgitt_næring() {
        oppgittOpptjeningUtenNæring(STP);
        fastsettPGIPeriodeTjeneste.fjernPGIDersomIkkeRelevant(behandling.getId());

        var beregningsgrunnlagPerioderGrunnlag = beregningPerioderGrunnlagRepository.hentGrunnlag(behandling.getId());

        assertThat(beregningsgrunnlagPerioderGrunnlag.isEmpty()).isTrue();
    }

    @Test
    void skal_ikke_gi_endring_ved_ingen_eksisterende_pgi_periode_men_eksisterende_grunnlag_og_ingen_oppgitt_næring() {
        oppgittOpptjeningUtenNæring(STP);
        var intiellPeriode = new BeregningsgrunnlagPeriode(UUID.randomUUID(), STP);
        beregningPerioderGrunnlagRepository.lagre(behandling.getId(), intiellPeriode);
        fastsettPGIPeriodeTjeneste.fjernPGIDersomIkkeRelevant(behandling.getId());

        var beregningsgrunnlagPerioderGrunnlag = beregningPerioderGrunnlagRepository.hentGrunnlag(behandling.getId());
        var grunnlagPerioder = beregningsgrunnlagPerioderGrunnlag.get().getGrunnlagPerioder();
        assertThat(grunnlagPerioder.size()).isEqualTo(1);
        assertThat(grunnlagPerioder.get(0)).isEqualTo(intiellPeriode);
    }

    @Test
    void skal_fjerne_eksisterende_pgi_periode_når_ingen_oppgitt_næring() {
        oppgittOpptjeningUtenNæring(STP);
        beregningPerioderGrunnlagRepository.lagreOgDeaktiverPGIPerioder(behandling.getId(), List.of(new PGIPeriode(UUID.randomUUID(), STP)), List.of());

        fastsettPGIPeriodeTjeneste.fjernPGIDersomIkkeRelevant(behandling.getId());

        var beregningsgrunnlagPerioderGrunnlag = beregningPerioderGrunnlagRepository.hentGrunnlag(behandling.getId());
        var pgiPerioder = beregningsgrunnlagPerioderGrunnlag.get().getPGIPerioder();
        assertThat(pgiPerioder.isEmpty()).isTrue();
    }


    @Test
    void skal_ikke_fjerne_eksisterende_pgi_periode_når_oppgitt_næring() {
        oppgittOpptjeningMedNæring(STP);
        var initiellPeriode = new PGIPeriode(UUID.randomUUID(), STP);
        beregningPerioderGrunnlagRepository.lagreOgDeaktiverPGIPerioder(behandling.getId(), List.of(initiellPeriode), List.of());

        fastsettPGIPeriodeTjeneste.fjernPGIDersomIkkeRelevant(behandling.getId());

        var beregningsgrunnlagPerioderGrunnlag = beregningPerioderGrunnlagRepository.hentGrunnlag(behandling.getId());
        var pgiPerioder = beregningsgrunnlagPerioderGrunnlag.get().getPGIPerioder();
        assertThat(pgiPerioder.size()).isEqualTo(1);
        assertThat(pgiPerioder.get(0)).isEqualTo(initiellPeriode);
    }

    @Test
    void skal_gjenopprette_initell_pgi_periode_når_oppgitt_næring() {
        oppgittOpptjeningMedNæring(STP);
        var initiellPeriode = new PGIPeriode(UUID.randomUUID(), STP);
        beregningPerioderGrunnlagRepository.lagreOgDeaktiverPGIPerioder(behandling.getId(), List.of(initiellPeriode), List.of());
        beregningPerioderGrunnlagRepository.lagreOgDeaktiverPGIPerioder(behandling.getId(), List.of(), List.of(initiellPeriode));

        fastsettPGIPeriodeTjeneste.fjernPGIDersomIkkeRelevant(behandling.getId());

        var beregningsgrunnlagPerioderGrunnlag = beregningPerioderGrunnlagRepository.hentGrunnlag(behandling.getId());
        var pgiPerioder = beregningsgrunnlagPerioderGrunnlag.get().getPGIPerioder();
        assertThat(pgiPerioder.size()).isEqualTo(1);
        assertThat(pgiPerioder.get(0)).isEqualTo(initiellPeriode);
        assertThat(pgiPerioder.get(0).getId().equals(initiellPeriode.getId())).isFalse();
    }


    @Test
    void skal_gjenopprette_initell_pgi_periode_når_oppgitt_næring_og_fjerne_perioder_for_ikke_oppgitt_næring() {
        oppgittOpptjeningMedNæring(STP);
        oppgittOpptjeningUtenNæring(STP);
        var initiellPeriode = new PGIPeriode(UUID.randomUUID(), STP);
        var initiellPeriode2 = new PGIPeriode(UUID.randomUUID(), STP2);
        beregningPerioderGrunnlagRepository.lagreOgDeaktiverPGIPerioder(behandling.getId(), List.of(initiellPeriode, initiellPeriode2), List.of());
        beregningPerioderGrunnlagRepository.lagreOgDeaktiverPGIPerioder(behandling.getId(), List.of(), List.of(initiellPeriode));

        fastsettPGIPeriodeTjeneste.fjernPGIDersomIkkeRelevant(behandling.getId());

        var beregningsgrunnlagPerioderGrunnlag = beregningPerioderGrunnlagRepository.hentGrunnlag(behandling.getId());
        var pgiPerioder = beregningsgrunnlagPerioderGrunnlag.get().getPGIPerioder();
        assertThat(pgiPerioder.size()).isEqualTo(1);
        assertThat(pgiPerioder.get(0)).isEqualTo(initiellPeriode);
        assertThat(pgiPerioder.get(0).getId().equals(initiellPeriode.getId())).isFalse();
    }


    private void oppgittOpptjeningMedNæring(LocalDate stp) {
        var inntektArbeidYtelseGrunnlag = inntektArbeidYtelseTjeneste.hentGrunnlag(behandling.getId());
        var oppgittOpptjeningBuilder = OppgittOpptjeningBuilder.ny();
        oppgittOpptjeningBuilder.leggTilEgneNæringer(List.of(OppgittOpptjeningBuilder.EgenNæringBuilder.ny().medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(stp.minusDays(28), stp.plusDays(1)))));
        when(oppgittOpptjeningFilter.hentOppgittOpptjening(behandling.getId(), inntektArbeidYtelseGrunnlag, stp)).thenReturn(Optional.of(oppgittOpptjeningBuilder.build()));
    }

    private void oppgittOpptjeningUtenNæring(LocalDate stp) {
        var inntektArbeidYtelseGrunnlag = inntektArbeidYtelseTjeneste.hentGrunnlag(behandling.getId());
        var oppgittOpptjeningBuilder = OppgittOpptjeningBuilder.ny();
        when(oppgittOpptjeningFilter.hentOppgittOpptjening(behandling.getId(), inntektArbeidYtelseGrunnlag, stp)).thenReturn(Optional.of(oppgittOpptjeningBuilder.build()));
    }

    private Behandling opprettBehandling() {
        Fagsak fagsak = Fagsak.opprettNy(FagsakYtelseType.PLEIEPENGER_SYKT_BARN, AKTØR_ID, new Saksnummer("SAKEN"), STP, STP);
        fagsakRepository.opprettNy(fagsak);
        var builder = Behandling.forFørstegangssøknad(fagsak);
        behandling = builder.build();
        var vilkårResultatBuilder = Vilkårene.builder();
        var bgVilkårBuilder = vilkårResultatBuilder.hentBuilderFor(VilkårType.BEREGNINGSGRUNNLAGVILKÅR);
        var vilkårPeriodeBuilder = bgVilkårBuilder.hentBuilderFor(STP, STP);
        var vilkårPeriodeBuilder2 = bgVilkårBuilder.hentBuilderFor(STP2, STP2);
        bgVilkårBuilder.leggTil(vilkårPeriodeBuilder);
        bgVilkårBuilder.leggTil(vilkårPeriodeBuilder2);
        vilkårResultatBuilder.leggTil(bgVilkårBuilder);
        Vilkårene nyttResultat = vilkårResultatBuilder.build();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
        vilkårResultatRepository.lagre(behandling.getId(), nyttResultat);
        return behandling;
    }
}
