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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.k9.kodeverk.Fagsystem;
import no.nav.k9.kodeverk.arbeidsforhold.Arbeidskategori;
import no.nav.k9.kodeverk.behandling.BehandlingStatus;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.PåTversAvHelgErKantIKantVurderer;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatBuilder;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
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

    public static final LocalDate STP = LocalDate.of(2022, 2, 1);
    @Inject
    private EntityManager entityManager;

    private FagsakRepository fagsakRepository;
    private VilkårResultatRepository vilkårResultatRepository;
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
        vilkårResultatRepository = new VilkårResultatRepository(entityManager);
        fagsak = Fagsak.opprettNy(FagsakYtelseType.PSB, new AktørId(123L), new Saksnummer("987"), STP, STP.plusDays(10));
        fagsakRepository.opprettNy(fagsak);
        behandling = Behandling.forFørstegangssøknad(fagsak).medBehandlingStatus(BehandlingStatus.UTREDES).build();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
        when(perioderTilVurderingTjeneste.utled(behandling.getId(), VilkårType.BEREGNINGSGRUNNLAGVILKÅR))
            .thenReturn(new TreeSet<>((Set.of(DatoIntervallEntitet.fraOgMedTilOgMed(STP, STP.plusDays(10))))));
        when(perioderTilVurderingTjeneste.utledFullstendigePerioder(behandling.getId()))
            .thenReturn(new TreeSet<>((Set.of(DatoIntervallEntitet.fraOgMedTilOgMed(STP, STP.plusDays(10))))));
        when(infotrygdService.finnGrunnlagsperioderForAndreAktører(any(), any(), any(), any())).thenReturn(Collections.emptyMap());
        when(perioderTilVurderingTjeneste.getKantIKantVurderer())
            .thenReturn(new PåTversAvHelgErKantIKantVurderer());

        lagreVilkårPeriode(List.of(DatoIntervallEntitet.fraOgMedTilOgMed(STP, STP.plusDays(10))));
        tjeneste = new InfotrygdMigreringTjeneste(iayTjeneste, perioderTilVurderingTjeneste, vilkårResultatRepository, fagsakRepository);
    }

    private void lagreVilkårPeriode(List<DatoIntervallEntitet> perioder) {
        var vilkårene = new VilkårResultatBuilder().leggTilIkkeVurderteVilkår(perioder, VilkårType.BEREGNINGSGRUNNLAGVILKÅR)
            .build();
        vilkårResultatRepository.lagre(behandling.getId(), vilkårene);
    }

    @Test
    void skal_opprette_ved_overlapp() {
        lagInfotrygdPsbYtelse(DatoIntervallEntitet.fraOgMedTilOgMed(STP.minusMonths(1), STP), behandling.getId(), fagsak.getAktørId());

        tjeneste.finnOgOpprettMigrertePerioder(behandling.getId(), behandling.getAktørId(), behandling.getFagsakId());

        var sakInfotrygdMigrering = fagsakRepository.hentSakInfotrygdMigreringer(fagsak.getId());
        assertThat(sakInfotrygdMigrering.size()).isEqualTo(1);
        assertThat(sakInfotrygdMigrering.get(0).getSkjæringstidspunkt()).isEqualTo(STP);
    }

    @Test
    void skal_deaktivere_der_periode_er_slått_sammen_med_periode_i_forkant() {
        lagreVilkårPeriode(List.of(DatoIntervallEntitet.fraOgMedTilOgMed(STP, STP.plusDays(10))));
        lagInfotrygdPsbYtelse(DatoIntervallEntitet.fraOgMedTilOgMed(STP.minusMonths(1), STP), behandling.getId(), fagsak.getAktørId());
        fagsakRepository.opprettInfotrygdmigrering(fagsak.getId(), STP.plusDays(5));

        tjeneste.finnOgOpprettMigrertePerioder(behandling.getId(), behandling.getAktørId(), behandling.getFagsakId());

        var sakInfotrygdMigrering = fagsakRepository.hentSakInfotrygdMigreringer(fagsak.getId());
        assertThat(sakInfotrygdMigrering.size()).isEqualTo(1);
        assertThat(sakInfotrygdMigrering.get(0).getSkjæringstidspunkt()).isEqualTo(STP);
    }

    @Test
    void skal_opprette_migrering_ved_overlapp_der_sakinfotrygdmigrering_finnes_fra_før() {
        lagInfotrygdPsbYtelse(DatoIntervallEntitet.fraOgMedTilOgMed(STP.minusMonths(1), STP), behandling.getId(), fagsak.getAktørId());
        fagsakRepository.opprettInfotrygdmigrering(fagsak.getId(), STP);

        tjeneste.finnOgOpprettMigrertePerioder(behandling.getId(), behandling.getAktørId(), behandling.getFagsakId());

        var sakInfotrygdMigrering = fagsakRepository.hentSakInfotrygdMigreringer(fagsak.getId());
        assertThat(sakInfotrygdMigrering.size()).isEqualTo(1);
        assertThat(sakInfotrygdMigrering.get(0).getSkjæringstidspunkt()).isEqualTo(STP);
    }

    @Test
    void skal_oppdatere_eksisterende_sakinfotrygdmigrering() {
        lagInfotrygdPsbYtelse(DatoIntervallEntitet.fraOgMedTilOgMed(STP.minusMonths(1), STP.plusDays(2)), behandling.getId(), fagsak.getAktørId());
        fagsakRepository.opprettInfotrygdmigrering(fagsak.getId(), STP.plusDays(2));

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
        lagreVilkårPeriode(List.of(DatoIntervallEntitet.fraOgMedTilOgMed(STP, STP.plusDays(10)),
            DatoIntervallEntitet.fraOgMedTilOgMed(STP.plusDays(20), STP.plusDays(30))));

        lagInfotrygdPsbYtelse(DatoIntervallEntitet.fraOgMedTilOgMed(STP.minusMonths(1), STP.plusDays(30)), behandling.getId(), fagsak.getAktørId());
        fagsakRepository.opprettInfotrygdmigrering(fagsak.getId(), STP.plusDays(2));
        fagsakRepository.opprettInfotrygdmigrering(fagsak.getId(), STP.plusDays(20));

        tjeneste.finnOgOpprettMigrertePerioder(behandling.getId(), behandling.getAktørId(), behandling.getFagsakId());

        var sakInfotrygdMigrering = fagsakRepository.hentSakInfotrygdMigreringer(fagsak.getId());
        assertThat(sakInfotrygdMigrering.size()).isEqualTo(2);
        assertThat(sakInfotrygdMigrering.stream().anyMatch(s -> s.getSkjæringstidspunkt().equals(STP))).isTrue();
        assertThat(sakInfotrygdMigrering.stream().anyMatch(s -> s.getSkjæringstidspunkt().equals(STP.plusDays(20)))).isTrue();
    }

    @Test
    void skal_rydde_infotrygdmigrering_dersom_den_ikke_sammenfaller_med_eksisterende_skjæringstidspunkt() {
        when(perioderTilVurderingTjeneste.utledFullstendigePerioder(behandling.getId()))
            .thenReturn(new TreeSet<>((Set.of(
                DatoIntervallEntitet.fraOgMedTilOgMed(STP, STP.plusDays(10)),
                DatoIntervallEntitet.fraOgMedTilOgMed(STP.minusMonths(1), STP.minusDays(10))
            ))));
        lagreVilkårPeriode(List.of(
            DatoIntervallEntitet.fraOgMedTilOgMed(STP, STP.plusDays(10)),
            DatoIntervallEntitet.fraOgMedTilOgMed(STP.minusMonths(1), STP.minusDays(10))
        ));
        lagInfotrygdPsbYtelse(DatoIntervallEntitet.fraOgMedTilOgMed(STP.minusMonths(1), STP.minusDays(10)), behandling.getId(), fagsak.getAktørId());
        fagsakRepository.opprettInfotrygdmigrering(fagsak.getId(), STP.minusDays(10));

        tjeneste.finnOgOpprettMigrertePerioder(behandling.getId(), behandling.getAktørId(), behandling.getFagsakId());

        var sakInfotrygdMigrering = fagsakRepository.hentSakInfotrygdMigreringer(fagsak.getId());
        assertThat(sakInfotrygdMigrering.size()).isEqualTo(0);
    }

    @Test
    void skal_rydde_infotrygdmigrering_for_periode_som_vurderes() {
        lagInfotrygdPsbYtelse(DatoIntervallEntitet.fraOgMedTilOgMed(STP.minusMonths(1), STP.plusDays(10)), behandling.getId(), fagsak.getAktørId());
        fagsakRepository.opprettInfotrygdmigrering(fagsak.getId(), STP.plusDays(2));

        tjeneste.finnOgOpprettMigrertePerioder(behandling.getId(), behandling.getAktørId(), behandling.getFagsakId());

        var sakInfotrygdMigrering = fagsakRepository.hentSakInfotrygdMigreringer(fagsak.getId());
        assertThat(sakInfotrygdMigrering.size()).isEqualTo(1);
        assertThat(sakInfotrygdMigrering.get(0).getSkjæringstidspunkt()).isEqualTo(STP);
    }

    @Test
    void skal_rydde_infotrygdmigrering_for_to_perioder_som_vurderes() {
        when(perioderTilVurderingTjeneste.utled(behandling.getId(), VilkårType.BEREGNINGSGRUNNLAGVILKÅR))
            .thenReturn(new TreeSet<>((Set.of(DatoIntervallEntitet.fraOgMedTilOgMed(STP, STP.plusDays(10)),
                DatoIntervallEntitet.fraOgMedTilOgMed(STP.minusMonths(1), STP.minusMonths(1).plusDays(10))))));
        when(perioderTilVurderingTjeneste.utledFullstendigePerioder(behandling.getId()))
            .thenReturn(new TreeSet<>((Set.of(DatoIntervallEntitet.fraOgMedTilOgMed(STP, STP.plusDays(10)),
                DatoIntervallEntitet.fraOgMedTilOgMed(STP.minusMonths(1), STP.minusMonths(1).plusDays(10))))));
        lagreVilkårPeriode(List.of(DatoIntervallEntitet.fraOgMedTilOgMed(STP, STP.plusDays(10)),
            DatoIntervallEntitet.fraOgMedTilOgMed(STP.minusMonths(1), STP.minusMonths(1).plusDays(10))));
        lagInfotrygdPsbYtelsePerioder(List.of(
            DatoIntervallEntitet.fraOgMedTilOgMed(STP, STP.plusDays(10)),
            DatoIntervallEntitet.fraOgMedTilOgMed(STP.minusMonths(1), STP.minusMonths(1).plusDays(10))));
        fagsakRepository.opprettInfotrygdmigrering(fagsak.getId(), STP.plusDays(2));

        tjeneste.finnOgOpprettMigrertePerioder(behandling.getId(), behandling.getAktørId(), behandling.getFagsakId());

        var sakInfotrygdMigrering = fagsakRepository.hentSakInfotrygdMigreringer(fagsak.getId());
        assertThat(sakInfotrygdMigrering.size()).isEqualTo(2);
        assertThat(sakInfotrygdMigrering.get(0).getSkjæringstidspunkt()).isEqualTo(STP.minusMonths(1));
        assertThat(sakInfotrygdMigrering.get(1).getSkjæringstidspunkt()).isEqualTo(STP);

    }


    @Test
    void skal_feile_ved_trukket_søknad_dersom_periode_er_fjernet_infotrygd() {
        when(perioderTilVurderingTjeneste.utledFullstendigePerioder(behandling.getId()))
            .thenReturn(new TreeSet<>((Set.of(
                DatoIntervallEntitet.fraOgMedTilOgMed(STP, STP.plusDays(10))
            ))));
        lagreVilkårPeriode(List.of(
            DatoIntervallEntitet.fraOgMedTilOgMed(STP, STP.plusDays(10))
        ));
        fagsakRepository.opprettInfotrygdmigrering(fagsak.getId(), STP.minusMonths(1));
        lagUtenInfotrygdPsbYtelse();

        assertThrows(IllegalStateException.class, () -> tjeneste.finnOgOpprettMigrertePerioder(behandling.getId(), behandling.getAktørId(), behandling.getFagsakId()));

    }

    @Test
    void skal_ikke_feile_ved_trukket_søknad_dersom_periode_ikke_er_fjernet_infotrygd() {
        when(perioderTilVurderingTjeneste.utledFullstendigePerioder(behandling.getId()))
            .thenReturn(new TreeSet<>((Set.of(
                DatoIntervallEntitet.fraOgMedTilOgMed(STP, STP.plusDays(10))
            ))));
        lagreVilkårPeriode(List.of(
            DatoIntervallEntitet.fraOgMedTilOgMed(STP, STP.plusDays(10))
        ));
        lagInfotrygdPsbYtelse(DatoIntervallEntitet.fraOgMedTilOgMed(STP.minusMonths(1), STP.minusDays(10)), behandling.getId(), fagsak.getAktørId());
        fagsakRepository.opprettInfotrygdmigrering(fagsak.getId(), STP.minusMonths(1));

        tjeneste.finnOgOpprettMigrertePerioder(behandling.getId(), behandling.getAktørId(), behandling.getFagsakId());

        var migreringer = fagsakRepository.hentSakInfotrygdMigreringer(fagsak.getId());
        assertThat(migreringer.size()).isEqualTo(0);
    }

    @Test
    void skal_ikke_feile_ved_trukket_søknad_dersom_periode_fra_infotrygd_ligger_kant_i_kant() {
        when(perioderTilVurderingTjeneste.utledFullstendigePerioder(behandling.getId()))
            .thenReturn(new TreeSet<>((Set.of(
                DatoIntervallEntitet.fraOgMedTilOgMed(STP, STP.plusDays(10))
            ))));
        lagreVilkårPeriode(List.of(
            DatoIntervallEntitet.fraOgMedTilOgMed(STP, STP.plusDays(10))
        ));
        lagInfotrygdPsbYtelse(DatoIntervallEntitet.fraOgMedTilOgMed(STP.minusMonths(1).minusDays(1), STP.minusMonths(1).minusDays(1)), behandling.getId(), fagsak.getAktørId());
        fagsakRepository.opprettInfotrygdmigrering(fagsak.getId(), STP.minusMonths(1));

        tjeneste.finnOgOpprettMigrertePerioder(behandling.getId(), behandling.getAktørId(), behandling.getFagsakId());

        var migreringer = fagsakRepository.hentSakInfotrygdMigreringer(fagsak.getId());
        assertThat(migreringer.size()).isEqualTo(0);
    }

    @Test
    void skal_ikkje_opprette_uten_overlapp() {
        lagInfotrygdPsbYtelse(DatoIntervallEntitet.fraOgMedTilOgMed(STP.minusMonths(1), STP.minusDays(10)), behandling.getId(), fagsak.getAktørId());

        tjeneste.finnOgOpprettMigrertePerioder(behandling.getId(), behandling.getAktørId(), behandling.getFagsakId());

        var sakInfotrygdMigrering = fagsakRepository.hentSakInfotrygdMigreringer(fagsak.getId());
        assertThat(sakInfotrygdMigrering.size()).isZero();
    }

    @Test
    void skal_opprette_for_kant_i_kant() {
        lagInfotrygdPsbYtelse(DatoIntervallEntitet.fraOgMedTilOgMed(STP.minusMonths(1), STP.minusDays(1)), behandling.getId(), fagsak.getAktørId());

        tjeneste.finnOgOpprettMigrertePerioder(behandling.getId(), behandling.getAktørId(), behandling.getFagsakId());

        var sakInfotrygdMigrering = fagsakRepository.hentSakInfotrygdMigreringer(fagsak.getId());
        assertThat(sakInfotrygdMigrering.size()).isEqualTo(1);
        assertThat(sakInfotrygdMigrering.get(0).getSkjæringstidspunkt()).isEqualTo(STP);
    }

    @Test
    void skal_opprette_ved_overlapp_eksisterende_overlapp_og_søknad_for_annen_periode() {
        lagInfotrygdPsbYtelsePerioder(List.of(DatoIntervallEntitet.fraOgMedTilOgMed(STP, STP),
            DatoIntervallEntitet.fraOgMedTilOgMed(STP.minusMonths(1), STP.minusMonths(1))));


        tjeneste.finnOgOpprettMigrertePerioder(behandling.getId(), behandling.getAktørId(), behandling.getFagsakId());

        var sakInfotrygdMigrering = fagsakRepository.hentSakInfotrygdMigreringer(fagsak.getId());
        assertThat(sakInfotrygdMigrering.size()).isEqualTo(1);
        assertThat(sakInfotrygdMigrering.get(0).getSkjæringstidspunkt()).isEqualTo(STP);


        when(perioderTilVurderingTjeneste.utled(behandling.getId(), VilkårType.BEREGNINGSGRUNNLAGVILKÅR))
            .thenReturn(new TreeSet<>((Set.of(
                DatoIntervallEntitet.fraOgMedTilOgMed(STP.minusMonths(1), STP.minusMonths(1)),
                DatoIntervallEntitet.fraOgMedTilOgMed(STP, STP.plusDays(10))))));

        tjeneste.finnOgOpprettMigrertePerioder(behandling.getId(), behandling.getAktørId(), behandling.getFagsakId());

        sakInfotrygdMigrering = fagsakRepository.hentSakInfotrygdMigreringer(fagsak.getId());
        assertThat(sakInfotrygdMigrering.size()).isEqualTo(2);
        assertThat(sakInfotrygdMigrering.get(0).getSkjæringstidspunkt()).isEqualTo(STP.minusMonths(1));
        assertThat(sakInfotrygdMigrering.get(1).getSkjæringstidspunkt()).isEqualTo(STP);

    }


    @Test
    void skal_fjerne_migrering_dersom_overlapp_forsvinner_for_eksisterende_migrering() {
        fagsakRepository.opprettInfotrygdmigrering(fagsak.getId(), STP);
        lagUtenInfotrygdPsbYtelse();
        tjeneste.finnOgOpprettMigrertePerioder(behandling.getId(), behandling.getAktørId(), behandling.getFagsakId());

        var sakInfotrygdMigrering = fagsakRepository.hentSakInfotrygdMigreringer(fagsak.getId());
        assertThat(sakInfotrygdMigrering.size()).isEqualTo(1);
        assertThat(sakInfotrygdMigrering.get(0).getSkjæringstidspunkt()).isEqualTo(STP);

    }


    private void lagUtenInfotrygdPsbYtelse() {
        var iayBuilder = InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.empty(), VersjonType.REGISTER);
        var aktørYtelseBuilder = InntektArbeidYtelseAggregatBuilder.AktørYtelseBuilder.oppdatere(Optional.empty());
        iayBuilder.leggTilAktørYtelse(aktørYtelseBuilder);
        iayTjeneste.lagreIayAggregat(behandling.getId(), iayBuilder);
    }


    private void lagInfotrygdPsbYtelse(DatoIntervallEntitet periode, Long behandlingId, AktørId aktørId) {
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
            .medAktørId(aktørId);
        iayBuilder.leggTilAktørYtelse(aktørYtelseBuilder);
        iayTjeneste.lagreIayAggregat(behandlingId, iayBuilder);
    }


    private void lagInfotrygdPsbYtelsePerioder(List<DatoIntervallEntitet> perioder) {
        var iayBuilder = InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.empty(), VersjonType.REGISTER);
        var aktørYtelseBuilder = InntektArbeidYtelseAggregatBuilder.AktørYtelseBuilder.oppdatere(Optional.empty());


        perioder.forEach(periode -> {
            var ytelseBuilder = YtelseBuilder.oppdatere(Optional.empty());
            ytelseBuilder.medPeriode(periode)
                .medYtelseGrunnlag(ytelseBuilder.getGrunnlagBuilder().medArbeidskategori(Arbeidskategori.ARBEIDSTAKER).build())
                .medKilde(Fagsystem.INFOTRYGD)
                .medYtelseType(FagsakYtelseType.PSB);
            var ytelseAnvist = ytelseBuilder.getAnvistBuilder().medAnvistPeriode(periode)
                .medBeløp(BigDecimal.TEN)
                .medDagsats(BigDecimal.TEN)
                .medUtbetalingsgradProsent(BigDecimal.valueOf(100))
                .build();
            ytelseBuilder.medYtelseAnvist(ytelseAnvist);
            aktørYtelseBuilder.leggTilYtelse(ytelseBuilder);
        });

        aktørYtelseBuilder.medAktørId(fagsak.getAktørId());
        iayBuilder.leggTilAktørYtelse(aktørYtelseBuilder);
        iayTjeneste.lagreIayAggregat(behandling.getId(), iayBuilder);
    }

}
