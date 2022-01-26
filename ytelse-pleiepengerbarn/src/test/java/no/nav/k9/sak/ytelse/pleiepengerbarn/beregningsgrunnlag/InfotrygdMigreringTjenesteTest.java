package no.nav.k9.sak.ytelse.pleiepengerbarn.beregningsgrunnlag;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.k9.kodeverk.Fagsystem;
import no.nav.k9.kodeverk.behandling.BehandlingStatus;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.behandlingslager.fagsak.SakInfotrygdMigrering;
import no.nav.k9.sak.db.util.JpaExtension;
import no.nav.k9.sak.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.k9.sak.domene.iay.modell.VersjonType;
import no.nav.k9.sak.domene.iay.modell.YtelseBuilder;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.k9.sak.ytelse.pleiepengerbarn.infotrygdovergang.InfotrygdMigreringTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.infotrygdovergang.IntervallMedBehandlingstema;
import no.nav.k9.sak.ytelse.pleiepengerbarn.infotrygdovergang.infotrygd.InfotrygdService;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
class InfotrygdMigreringTjenesteTest {

    public static final LocalDate STP = LocalDate.of(2022, 2,1);
    @Inject
    private EntityManager entityManager;

    private FagsakRepository fagsakRepository;
    private BehandlingRepository behandlingRepository;
    private InntektArbeidYtelseTjeneste iayTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();
    private VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste = mock(VilkårsPerioderTilVurderingTjeneste.class);
    private Behandling behandling;
    private Fagsak fagsak;
    private InfotrygdService infotrygdService = mock(InfotrygdService.class);

    private InfotrygdMigreringTjeneste tjeneste;


    @BeforeEach
    public void setup() {

        fagsakRepository = new FagsakRepository(entityManager);
        behandlingRepository = new BehandlingRepository(entityManager);

        fagsak = Fagsak.opprettNy(FagsakYtelseType.PSB, new AktørId(123L), new Saksnummer("987"), STP, STP.plusDays(10));
        fagsakRepository.opprettNy(fagsak);
        behandling = Behandling.forFørstegangssøknad(fagsak).medBehandlingStatus(BehandlingStatus.UTREDES).build();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
        when(perioderTilVurderingTjeneste.utled(behandling.getId(), VilkårType.BEREGNINGSGRUNNLAGVILKÅR))
            .thenReturn(new TreeSet<>((Set.of(DatoIntervallEntitet.fraOgMedTilOgMed(STP, STP.plusDays(10))))));
        when(perioderTilVurderingTjeneste.utledFullstendigePerioder(behandling.getId()))
            .thenReturn(new TreeSet<>((Set.of(DatoIntervallEntitet.fraOgMedTilOgMed(STP, STP.plusDays(10))))));
        when(infotrygdService.finnGrunnlagsperioderForAndreAktører(any(), any(), any(), any())).thenReturn(Collections.emptyMap());
        tjeneste = new InfotrygdMigreringTjeneste(iayTjeneste, perioderTilVurderingTjeneste, fagsakRepository, behandlingRepository, infotrygdService);
    }

    @Test
    void skal_opprette_ved_overlapp() {
        lagInfotrygdPsbYtelse(DatoIntervallEntitet.fraOgMedTilOgMed(STP.minusMonths(1), STP));

        tjeneste.finnOgOpprettMigrertePerioder(behandling.getId(), behandling.getAktørId(), behandling.getFagsakId());

        var sakInfotrygdMigrering = fagsakRepository.hentSakInfotrygdMigreringer(fagsak.getId());
        assertThat(sakInfotrygdMigrering.size()).isEqualTo(1);
        assertThat(sakInfotrygdMigrering.get(0).getSkjæringstidspunkt()).isEqualTo(STP);
    }

    @Test
    void skal_opprette_migrering_ved_overlapp_der_sakinfotrygdmigrering_finnes_fra_før() {
        lagInfotrygdPsbYtelse(DatoIntervallEntitet.fraOgMedTilOgMed(STP.minusMonths(1), STP));
        fagsakRepository.lagreOgFlush(new SakInfotrygdMigrering(fagsak.getId(), STP));

         tjeneste.finnOgOpprettMigrertePerioder(behandling.getId(), behandling.getAktørId(), behandling.getFagsakId());

        var sakInfotrygdMigrering = fagsakRepository.hentSakInfotrygdMigreringer(fagsak.getId());
        assertThat(sakInfotrygdMigrering.size()).isEqualTo(1);
        assertThat(sakInfotrygdMigrering.get(0).getSkjæringstidspunkt()).isEqualTo(STP);
    }

    @Test
    void skal_oppdatere_eksisterende_sakinfotrygdmigrering() {
        lagInfotrygdPsbYtelse(DatoIntervallEntitet.fraOgMedTilOgMed(STP.minusMonths(1), STP.plusDays(2)));
        fagsakRepository.lagreOgFlush(new SakInfotrygdMigrering(fagsak.getId(), STP.plusDays(2)));

        tjeneste.finnOgOpprettMigrertePerioder(behandling.getId(), behandling.getAktørId(), behandling.getFagsakId());

        var sakInfotrygdMigrering = fagsakRepository.hentSakInfotrygdMigreringer(fagsak.getId());
        assertThat(sakInfotrygdMigrering.size()).isEqualTo(1);
        assertThat(sakInfotrygdMigrering.get(0).getSkjæringstidspunkt()).isEqualTo(STP);
    }

    @Test
    void skal_opprette_ved_overlapp_med_flere_perioder() {
        when(perioderTilVurderingTjeneste.utled(behandling.getId(), VilkårType.BEREGNINGSGRUNNLAGVILKÅR))
            .thenReturn(new TreeSet<>((Set.of(DatoIntervallEntitet.fraOgMedTilOgMed(STP, STP.plusDays(10)),
                DatoIntervallEntitet.fraOgMedTilOgMed(STP.plusDays(20), STP.plusDays(30))))));
        when(perioderTilVurderingTjeneste.utledFullstendigePerioder(behandling.getId()))
            .thenReturn(new TreeSet<>((Set.of(DatoIntervallEntitet.fraOgMedTilOgMed(STP, STP.plusDays(10)),
                DatoIntervallEntitet.fraOgMedTilOgMed(STP.plusDays(20), STP.plusDays(30))))));
        lagInfotrygdPsbYtelse(DatoIntervallEntitet.fraOgMedTilOgMed(STP.minusMonths(1), STP.plusDays(30)));
        fagsakRepository.lagreOgFlush(new SakInfotrygdMigrering(fagsak.getId(), STP.plusDays(2)));
        fagsakRepository.lagreOgFlush(new SakInfotrygdMigrering(fagsak.getId(), STP.plusDays(20)));

        tjeneste.finnOgOpprettMigrertePerioder(behandling.getId(), behandling.getAktørId(), behandling.getFagsakId());

        var sakInfotrygdMigrering = fagsakRepository.hentSakInfotrygdMigreringer(fagsak.getId());
        assertThat(sakInfotrygdMigrering.size()).isEqualTo(2);
        assertThat(sakInfotrygdMigrering.stream().anyMatch(s -> s.getSkjæringstidspunkt().equals(STP))).isTrue();
        assertThat(sakInfotrygdMigrering.stream().anyMatch(s -> s.getSkjæringstidspunkt().equals(STP.plusDays(20)))).isTrue();
    }

    @Test
    void skal_opprette_med_overlapp_for_periode_som_ikke_vurderes() {
        when(perioderTilVurderingTjeneste.utledFullstendigePerioder(behandling.getId()))
            .thenReturn(new TreeSet<>((Set.of(
                DatoIntervallEntitet.fraOgMedTilOgMed(STP, STP.plusDays(10)),
                DatoIntervallEntitet.fraOgMedTilOgMed(STP.minusMonths(1), STP.minusDays(10))
                ))));
        lagInfotrygdPsbYtelse(DatoIntervallEntitet.fraOgMedTilOgMed(STP.minusMonths(1), STP.minusDays(10)));
        fagsakRepository.lagreOgFlush(new SakInfotrygdMigrering(fagsak.getId(), STP.minusDays(10)));

        tjeneste.finnOgOpprettMigrertePerioder(behandling.getId(), behandling.getAktørId(), behandling.getFagsakId());

        var sakInfotrygdMigrering = fagsakRepository.hentSakInfotrygdMigreringer(fagsak.getId());
        assertThat(sakInfotrygdMigrering.size()).isEqualTo(1);
        assertThat(sakInfotrygdMigrering.get(0).getSkjæringstidspunkt()).isEqualTo(STP.minusDays(10));
    }

    @Test
    void skal_fjerne_overlapp_for_periode_som_ikke_er_søkt_for() {
        when(perioderTilVurderingTjeneste.utledFullstendigePerioder(behandling.getId()))
            .thenReturn(new TreeSet<>((Set.of(
                DatoIntervallEntitet.fraOgMedTilOgMed(STP, STP.plusDays(10))
            ))));
        lagInfotrygdPsbYtelse(DatoIntervallEntitet.fraOgMedTilOgMed(STP.minusMonths(1), STP.minusDays(10)));
        fagsakRepository.lagreOgFlush(new SakInfotrygdMigrering(fagsak.getId(), STP.minusMonths(1)));

        tjeneste.finnOgOpprettMigrertePerioder(behandling.getId(), behandling.getAktørId(), behandling.getFagsakId());

        var sakInfotrygdMigrering = fagsakRepository.hentSakInfotrygdMigreringer(fagsak.getId());
        assertThat(sakInfotrygdMigrering.size()).isEqualTo(0);
    }

    @Test
    void skal_ikkje_opprette_uten_overlapp() {
        lagInfotrygdPsbYtelse(DatoIntervallEntitet.fraOgMedTilOgMed(STP.minusMonths(1), STP.minusDays(10)));

        tjeneste.finnOgOpprettMigrertePerioder(behandling.getId(), behandling.getAktørId(), behandling.getFagsakId());

        var sakInfotrygdMigrering = fagsakRepository.hentSakInfotrygdMigreringer(fagsak.getId());
        assertThat(sakInfotrygdMigrering.size()).isZero();
    }

    @Test
    void skal_opprette_for_kant_i_kant() {
        lagInfotrygdPsbYtelse(DatoIntervallEntitet.fraOgMedTilOgMed(STP.minusMonths(1), STP.minusDays(1)));

        tjeneste.finnOgOpprettMigrertePerioder(behandling.getId(), behandling.getAktørId(), behandling.getFagsakId());

        var sakInfotrygdMigrering = fagsakRepository.hentSakInfotrygdMigreringer(fagsak.getId());
        assertThat(sakInfotrygdMigrering.size()).isEqualTo(1);
        assertThat(sakInfotrygdMigrering.get(0).getSkjæringstidspunkt()).isEqualTo(STP);
    }


    @Test
    void skal_returnere_aksjonspunkt_når_periode_i_samme_år_som_ikke_er_søkt_for() {
        lagInfotrygdPsbYtelse(DatoIntervallEntitet.fraOgMedTilOgMed(STP.minusMonths(1), STP.minusMonths(1).plusDays(10)));
        fagsakRepository.lagreOgFlush(new SakInfotrygdMigrering(fagsak.getId(), STP));

        var aksjonspunkter = tjeneste.utledAksjonspunkter(BehandlingReferanse.fra(behandling, STP));

        assertThat(aksjonspunkter.size()).isEqualTo(1);
        assertThat(aksjonspunkter.get(0).getAksjonspunktDefinisjon()).isEqualTo(AksjonspunktDefinisjon.TRENGER_SØKNAD_FOR_INFOTRYGD_PERIODE);
    }

    @Test
    void skal_ikke_returnere_aksjonspunkt_når_periode_i_forrige_år_som_ikke_er_søkt_for() {
        lagInfotrygdPsbYtelse(DatoIntervallEntitet.fraOgMedTilOgMed(STP.minusMonths(3), STP.minusMonths(1).plusDays(10)));
        when(perioderTilVurderingTjeneste.utledFullstendigePerioder(behandling.getId()))
            .thenReturn(new TreeSet<>((Set.of(
                DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2022, 1, 3), STP.minusMonths(1).plusDays(10)),
                DatoIntervallEntitet.fraOgMedTilOgMed(STP, STP.plusDays(10))
            ))));
        fagsakRepository.lagreOgFlush(new SakInfotrygdMigrering(fagsak.getId(), STP));

        var aksjonspunkter = tjeneste.utledAksjonspunkter(BehandlingReferanse.fra(behandling, STP));

        assertThat(aksjonspunkter.size()).isEqualTo(0);
    }


    @Test
    void skal_returnere_aksjonspunkt_når_annen_part_har_overlappende_periode_i_infotrygd() {
        lagInfotrygdPsbYtelse(DatoIntervallEntitet.fraOgMedTilOgMed(STP, STP.plusDays(10)));
        fagsakRepository.lagreOgFlush(new SakInfotrygdMigrering(fagsak.getId(), STP));

        when(infotrygdService.finnGrunnlagsperioderForAndreAktører(any(), any(), any(), any()))
            .thenReturn(Map.of(AktørId.dummy(),
                List.of(new IntervallMedBehandlingstema(DatoIntervallEntitet.fraOgMedTilOgMed(STP, STP.plusDays(10)), "PN"))));

        var aksjonspunkter = tjeneste.utledAksjonspunkter(BehandlingReferanse.fra(behandling, STP));

        assertThat(aksjonspunkter.size()).isEqualTo(1);
        assertThat(aksjonspunkter.get(0).getAksjonspunktDefinisjon()).isEqualTo(AksjonspunktDefinisjon.TRENGER_SØKNAD_FOR_INFOTRYGD_PERIODE_ANNEN_PART);
    }

    @Test
    void skal_kaste_feil_når_annen_part_har_overlappende_periode_i_infotrygd_på_gammel_ordning() {
        lagInfotrygdPsbYtelse(DatoIntervallEntitet.fraOgMedTilOgMed(STP, STP.plusDays(10)));
        fagsakRepository.lagreOgFlush(new SakInfotrygdMigrering(fagsak.getId(), STP));

        when(infotrygdService.finnGrunnlagsperioderForAndreAktører(any(), any(), any(), any()))
            .thenReturn(Map.of(AktørId.dummy(),
                List.of(new IntervallMedBehandlingstema(DatoIntervallEntitet.fraOgMedTilOgMed(STP, STP.plusDays(10)), "PB"))));

        assertThrows(IllegalStateException.class, () ->  tjeneste.utledAksjonspunkter(BehandlingReferanse.fra(behandling, STP)));
    }

    @Test
    void skal_ikke_gi_aksjonspunt_når_annen_part_har_overlappende_periode_i_infotrygd_som_er_søkt_om() {
        lagUtenInfotrygdPsbYtelse();
        var annenPartAktørId = new AktørId(345L);
        when(infotrygdService.finnGrunnlagsperioderForAndreAktører(any(), any(), any(), any()))
            .thenReturn(Map.of(annenPartAktørId,
                List.of(new IntervallMedBehandlingstema(DatoIntervallEntitet.fraOgMedTilOgMed(STP, STP.plusDays(10)), "PN"))));
        var annenPartfagsak = Fagsak.opprettNy(FagsakYtelseType.PSB, annenPartAktørId, new Saksnummer("456"), STP, STP.plusDays(10));
        fagsakRepository.opprettNy(annenPartfagsak);
        var annenPartbehandling = Behandling.forFørstegangssøknad(annenPartfagsak).medBehandlingStatus(BehandlingStatus.UTREDES).build();
        behandlingRepository.lagre(annenPartbehandling, behandlingRepository.taSkriveLås(annenPartbehandling));
        when(perioderTilVurderingTjeneste.utledFullstendigePerioder(annenPartbehandling.getId()))
            .thenReturn(new TreeSet<>((Set.of(DatoIntervallEntitet.fraOgMedTilOgMed(STP, STP.plusDays(10))))));


        var aksjonspunkter = tjeneste.utledAksjonspunkter(BehandlingReferanse.fra(behandling, STP));
        assertThat(aksjonspunkter.size()).isEqualTo(0);
    }



    private void lagUtenInfotrygdPsbYtelse() {
        var iayBuilder = InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.empty(), VersjonType.REGISTER);
        var aktørYtelseBuilder = InntektArbeidYtelseAggregatBuilder.AktørYtelseBuilder.oppdatere(Optional.empty());
        iayBuilder.leggTilAktørYtelse(aktørYtelseBuilder);
        iayTjeneste.lagreIayAggregat(behandling.getId(), iayBuilder);
    }


    private void lagInfotrygdPsbYtelse(DatoIntervallEntitet periode) {
        var iayBuilder = InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.empty(), VersjonType.REGISTER);
        var aktørYtelseBuilder = InntektArbeidYtelseAggregatBuilder.AktørYtelseBuilder.oppdatere(Optional.empty());
        var ytelseBuilder = YtelseBuilder.oppdatere(Optional.empty());

        var anvistBuilder = ytelseBuilder.getAnvistBuilder();
        var ytelseAnvist = anvistBuilder.medAnvistPeriode(periode)
            .medBeløp(BigDecimal.TEN)
            .medDagsats(BigDecimal.TEN)
            .medUtbetalingsgradProsent(BigDecimal.valueOf(100))
            .build();
        ytelseBuilder.medYtelseAnvist(ytelseAnvist)
            .medPeriode(periode)
            .medKilde(Fagsystem.INFOTRYGD)
            .medYtelseType(FagsakYtelseType.PSB);

        aktørYtelseBuilder.leggTilYtelse(ytelseBuilder)
            .medAktørId(fagsak.getAktørId());
        iayBuilder.leggTilAktørYtelse(aktørYtelseBuilder);
        iayTjeneste.lagreIayAggregat(behandling.getId(), iayBuilder);
    }

}
