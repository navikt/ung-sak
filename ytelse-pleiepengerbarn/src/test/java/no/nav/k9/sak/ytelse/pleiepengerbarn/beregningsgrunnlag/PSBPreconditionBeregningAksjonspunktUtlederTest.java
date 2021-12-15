package no.nav.k9.sak.ytelse.pleiepengerbarn.beregningsgrunnlag;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.OpptjeningAktiviteter;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.OpptjeningForBeregningTjeneste;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.k9.felles.testutilities.cdi.UnitTestLookupInstanceImpl;
import no.nav.k9.kodeverk.Fagsystem;
import no.nav.k9.kodeverk.behandling.BehandlingStatus;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktUtlederInput;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.opptjening.OpptjeningRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.behandlingslager.fagsak.SakInfotrygdMigrering;
import no.nav.k9.sak.db.util.JpaExtension;
import no.nav.k9.sak.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.k9.sak.domene.iay.modell.VersjonType;
import no.nav.k9.sak.domene.iay.modell.YtelseBuilder;
import no.nav.k9.sak.domene.opptjening.OppgittOpptjeningFilterProvider;
import no.nav.k9.sak.domene.opptjening.aksjonspunkt.OpptjeningsperioderTjeneste;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.k9.sak.ytelse.pleiepengerbarn.opptjening.PSBOppgittOpptjeningFilter;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
class PSBPreconditionBeregningAksjonspunktUtlederTest {

    public static final LocalDate STP = LocalDate.of(2022, 2, 1);
    @Inject
    private EntityManager entityManager;

    private FagsakRepository fagsakRepository;
    private BehandlingRepository behandlingRepository;
    private InntektArbeidYtelseTjeneste iayTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();
    private VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste = mock(VilkårsPerioderTilVurderingTjeneste.class);
    private Behandling behandling;
    private Fagsak fagsak;
    private PSBOpptjeningForBeregningTjeneste opptjeningForBeregningTjeneste;

    private PSBPreconditionBeregningAksjonspunktUtleder utleder;


    @BeforeEach
    public void setup() {

        fagsakRepository = new FagsakRepository(entityManager);
        behandlingRepository = new BehandlingRepository(entityManager);
        fagsak = Fagsak.opprettNy(FagsakYtelseType.DAGPENGER, new AktørId(123L), new Saksnummer("987"), STP, STP.plusDays(10));
        fagsakRepository.opprettNy(fagsak);
        behandling = Behandling.forFørstegangssøknad(fagsak).medBehandlingStatus(BehandlingStatus.UTREDES).build();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
        when(perioderTilVurderingTjeneste.utled(behandling.getId(), VilkårType.BEREGNINGSGRUNNLAGVILKÅR))
            .thenReturn(new TreeSet<>((Set.of(DatoIntervallEntitet.fraOgMedTilOgMed(STP, STP.plusDays(10))))));
        when(perioderTilVurderingTjeneste.utledFullstendigePerioder(behandling.getId()))
            .thenReturn(new TreeSet<>((Set.of(DatoIntervallEntitet.fraOgMedTilOgMed(STP, STP.plusDays(10))))));

        opptjeningForBeregningTjeneste = mock(PSBOpptjeningForBeregningTjeneste.class);
        when(opptjeningForBeregningTjeneste.hentEksaktOpptjeningForBeregning(any(), any(), any()))
            .thenReturn(Optional.of(new OpptjeningAktiviteter(List.of(
                OpptjeningAktiviteter.nyPeriode(OpptjeningAktivitetType.PLEIEPENGER, new Periode(STP.minusMonths(1), STP),
                    null, null, null)))));

        utleder = new PSBPreconditionBeregningAksjonspunktUtleder(iayTjeneste, opptjeningForBeregningTjeneste, perioderTilVurderingTjeneste,
            fagsakRepository, true);
    }

    @Test
    void skal_ikkje_returnere_aksjonspunkt_med_toggle_av() {
        utleder = new PSBPreconditionBeregningAksjonspunktUtleder(iayTjeneste, opptjeningForBeregningTjeneste, perioderTilVurderingTjeneste, fagsakRepository, false);
        var aksjonspunkter = utleder.utledAksjonspunkterFor(new AksjonspunktUtlederInput(BehandlingReferanse.fra(behandling, STP)));

        assertThat(aksjonspunkter.size()).isEqualTo(0);
    }

    @Test
    void skal_returnere_aksjonspunkt_eksisterende_migrering() {
        lagInfotrygdPsbYtelse(DatoIntervallEntitet.fraOgMedTilOgMed(STP, STP));
        fagsakRepository.lagreOgFlush(new SakInfotrygdMigrering(behandling.getFagsakId(), STP));

        var aksjonspunkter = utleder.utledAksjonspunkterFor(new AksjonspunktUtlederInput(BehandlingReferanse.fra(behandling, STP)));

        assertThat(aksjonspunkter.size()).isEqualTo(1);
        assertThat(aksjonspunkter.get(0).getAksjonspunktDefinisjon()).isEqualTo(AksjonspunktDefinisjon.OVERSTYR_BEREGNING_INPUT);
    }

    @Test
    void skal_returnere_aksjonspunkt_ved_overlapp_med_flere_perioder() {
        when(perioderTilVurderingTjeneste.utled(behandling.getId(), VilkårType.BEREGNINGSGRUNNLAGVILKÅR))
            .thenReturn(new TreeSet<>((Set.of(DatoIntervallEntitet.fraOgMedTilOgMed(STP, STP.plusDays(10)),
                DatoIntervallEntitet.fraOgMedTilOgMed(STP.plusDays(20), STP.plusDays(30))))));
        when(perioderTilVurderingTjeneste.utledFullstendigePerioder(behandling.getId()))
            .thenReturn(new TreeSet<>((Set.of(DatoIntervallEntitet.fraOgMedTilOgMed(STP, STP.plusDays(10)),
                DatoIntervallEntitet.fraOgMedTilOgMed(STP.plusDays(20), STP.plusDays(30))))));
        lagInfotrygdPsbYtelsePerioder(List.of(
            DatoIntervallEntitet.fraOgMedTilOgMed(STP, STP.plusDays(10)),
            DatoIntervallEntitet.fraOgMedTilOgMed(STP.plusDays(20), STP.plusDays(30))
            ));

        fagsakRepository.lagreOgFlush(new SakInfotrygdMigrering(fagsak.getId(), STP));
        fagsakRepository.lagreOgFlush(new SakInfotrygdMigrering(fagsak.getId(), STP.plusDays(20)));

        var aksjonspunkter = utleder.utledAksjonspunkterFor(new AksjonspunktUtlederInput(BehandlingReferanse.fra(behandling, STP)));

        assertThat(aksjonspunkter.size()).isEqualTo(1);
        assertThat(aksjonspunkter.get(0).getAksjonspunktDefinisjon()).isEqualTo(AksjonspunktDefinisjon.OVERSTYR_BEREGNING_INPUT);
    }

    @Test
    void skal_ikkje_returnere_aksjonspunkt_med_overlapp_for_periode_som_ikke_vurderes() {
        lagInfotrygdPsbYtelse(DatoIntervallEntitet.fraOgMedTilOgMed(STP.minusDays(10), STP.minusDays(3)));
        when(perioderTilVurderingTjeneste.utledFullstendigePerioder(behandling.getId()))
            .thenReturn(new TreeSet<>((Set.of(
                DatoIntervallEntitet.fraOgMedTilOgMed(STP.minusDays(10), STP.minusDays(3)),
                DatoIntervallEntitet.fraOgMedTilOgMed(STP, STP.plusDays(10))))));
        fagsakRepository.lagreOgFlush(new SakInfotrygdMigrering(fagsak.getId(), STP.minusDays(10)));

        var aksjonspunkter = utleder.utledAksjonspunkterFor(new AksjonspunktUtlederInput(BehandlingReferanse.fra(behandling, STP)));

        assertThat(aksjonspunkter.size()).isEqualTo(0);
    }

    @Test
    void skal_ikkje_returnere_aksjonspunkt_uten_overlapp() {
        lagInfotrygdPsbYtelse(DatoIntervallEntitet.fraOgMedTilOgMed(STP.minusMonths(2), STP.minusMonths(2).plusDays(2)));
        var aksjonspunkter = utleder.utledAksjonspunkterFor(new AksjonspunktUtlederInput(BehandlingReferanse.fra(behandling, STP)));

        assertThat(aksjonspunkter.size()).isEqualTo(0);
    }

    @Test
    void skal_returnere_ventepunkt_når_periode_i_samme_år_som_ikke_er_søkt_for() {
        lagInfotrygdPsbYtelse(DatoIntervallEntitet.fraOgMedTilOgMed(STP.minusMonths(1), STP.minusMonths(1).plusDays(10)));

        var aksjonspunkter = utleder.utledAksjonspunkterFor(new AksjonspunktUtlederInput(BehandlingReferanse.fra(behandling, STP)));

        assertThat(aksjonspunkter.size()).isEqualTo(1);
        assertThat(aksjonspunkter.get(0).getAksjonspunktDefinisjon()).isEqualTo(AksjonspunktDefinisjon.AUTO_VENT_PÅ_SØKNAD_FOR_PERIODE);
    }

    @Test
    void skal_returnere_ventepunkt_når_periode_etter_migrering_som_ikke_er_søkt_for() {
        lagInfotrygdPsbYtelse(DatoIntervallEntitet.fraOgMedTilOgMed(STP.plusMonths(1), STP.plusMonths(1).plusDays(10)));
        fagsakRepository.lagreOgFlush(new SakInfotrygdMigrering(fagsak.getId(), STP));

        var aksjonspunkter = utleder.utledAksjonspunkterFor(new AksjonspunktUtlederInput(BehandlingReferanse.fra(behandling, STP)));

        assertThat(aksjonspunkter.size()).isEqualTo(1);
        assertThat(aksjonspunkter.get(0).getAksjonspunktDefinisjon()).isEqualTo(AksjonspunktDefinisjon.AUTO_VENT_PÅ_SØKNAD_FOR_PERIODE);
    }

    @Test
    void skal_ikke_returnere_aksjonspunt_ved_dagpenger_av_pleiepenger() {
        lagInfotrygdPsbYtelse(DatoIntervallEntitet.fraOgMedTilOgMed(STP, STP.plusDays(10)));
        when(opptjeningForBeregningTjeneste.hentEksaktOpptjeningForBeregning(any(), any(), any()))
            .thenReturn(Optional.of(new OpptjeningAktiviteter(List.of(
                OpptjeningAktiviteter.nyPeriode(OpptjeningAktivitetType.PLEIEPENGER_AV_DAGPENGER, new Periode(STP, STP.plusDays(10)),
                    null, null, null)))));
        fagsakRepository.lagreOgFlush(new SakInfotrygdMigrering(fagsak.getId(), STP));

        var aksjonspunkter = utleder.utledAksjonspunkterFor(new AksjonspunktUtlederInput(BehandlingReferanse.fra(behandling, STP)));

        assertThat(aksjonspunkter.size()).isEqualTo(0);
    }



    private void lagInfotrygdPsbYtelse(DatoIntervallEntitet periode) {
        lagInfotrygdPsbYtelsePerioder(List.of(periode));
    }


    private void lagInfotrygdPsbYtelsePerioder(List<DatoIntervallEntitet> perioder) {
        var iayBuilder = InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.empty(), VersjonType.REGISTER);
        var aktørYtelseBuilder = InntektArbeidYtelseAggregatBuilder.AktørYtelseBuilder.oppdatere(Optional.empty());
        var ytelseBuilder = YtelseBuilder.oppdatere(Optional.empty());

        var fom = perioder.stream().map(DatoIntervallEntitet::getFomDato).min(Comparator.naturalOrder()).orElseThrow();
        var tom = perioder.stream().map(DatoIntervallEntitet::getTomDato).max(Comparator.naturalOrder()).orElseThrow();

        ytelseBuilder.medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom))
            .medKilde(Fagsystem.INFOTRYGD)
            .medYtelseType(FagsakYtelseType.PSB);

        perioder.stream().map(periode -> ytelseBuilder.getAnvistBuilder().medAnvistPeriode(periode)
                .medBeløp(BigDecimal.TEN)
                .medDagsats(BigDecimal.TEN)
                .medUtbetalingsgradProsent(BigDecimal.valueOf(100))
                .build())
            .forEach(ytelseBuilder::medYtelseAnvist);

        aktørYtelseBuilder.leggTilYtelse(ytelseBuilder)
            .medAktørId(fagsak.getAktørId());
        iayBuilder.leggTilAktørYtelse(aktørYtelseBuilder);
        iayTjeneste.lagreIayAggregat(behandling.getId(), iayBuilder);
    }


}
