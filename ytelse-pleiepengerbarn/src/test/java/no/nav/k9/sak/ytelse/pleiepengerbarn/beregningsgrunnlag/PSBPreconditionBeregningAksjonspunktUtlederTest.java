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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.abakus.iaygrunnlag.kodeverk.VirksomhetType;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.OpptjeningAktiviteter;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.k9.kodeverk.Fagsystem;
import no.nav.k9.kodeverk.arbeidsforhold.Arbeidskategori;
import no.nav.k9.kodeverk.behandling.BehandlingStatus;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktUtlederInput;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.BehandlingÅrsak;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.db.util.JpaExtension;
import no.nav.k9.sak.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag.BeregningsgrunnlagVilkårTjeneste;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.k9.sak.domene.iay.modell.OppgittOpptjening;
import no.nav.k9.sak.domene.iay.modell.OppgittOpptjeningBuilder;
import no.nav.k9.sak.domene.iay.modell.VersjonType;
import no.nav.k9.sak.domene.iay.modell.YtelseBuilder;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.k9.sak.vilkår.VilkårPeriodeFilter;
import no.nav.k9.sak.vilkår.VilkårPeriodeFilterProvider;
import no.nav.k9.sak.ytelse.beregning.grunnlag.BeregningPerioderGrunnlagRepository;
import no.nav.k9.sak.ytelse.beregning.grunnlag.InputOverstyringPeriode;
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
    private BeregningsgrunnlagVilkårTjeneste perioderTilVurderingTjeneste = mock(BeregningsgrunnlagVilkårTjeneste.class);
    private Behandling behandling;
    private Behandling revurderingIkkeManueltOpprettet;
    private Behandling behandlingManueltOpprettet;
    private Fagsak fagsak;
    private PSBOpptjeningForBeregningTjeneste opptjeningForBeregningTjeneste;
    private PSBOppgittOpptjeningFilter oppgittOpptjeningFilter;
    private VilkårPeriodeFilterProvider filterProvider = mock(VilkårPeriodeFilterProvider.class);
    private PSBPreconditionBeregningAksjonspunktUtleder utleder;
    private VilkårPeriodeFilter filter = mock(VilkårPeriodeFilter.class);
    private BeregningPerioderGrunnlagRepository beregningPerioderGrunnlagRepository;


    @BeforeEach
    public void setup() {

        fagsakRepository = new FagsakRepository(entityManager);
        behandlingRepository = new BehandlingRepository(entityManager);
        fagsak = Fagsak.opprettNy(FagsakYtelseType.DAGPENGER, new AktørId(123L), new Saksnummer("987"), STP, STP.plusDays(10));
        fagsakRepository.opprettNy(fagsak);
        behandling = Behandling.forFørstegangssøknad(fagsak).medBehandlingStatus(BehandlingStatus.UTREDES).build();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
        behandlingManueltOpprettet = Behandling.fraTidligereBehandling(behandling, BehandlingType.REVURDERING)
            .medBehandlingStatus(BehandlingStatus.UTREDES)
            .medBehandlingÅrsak(new BehandlingÅrsak.Builder(List.of(BehandlingÅrsakType.RE_OPPLYSNINGER_OM_INNTEKT)).medManueltOpprettet(true)).build();
        behandlingRepository.lagre(behandlingManueltOpprettet, behandlingRepository.taSkriveLås(behandlingManueltOpprettet));

        revurderingIkkeManueltOpprettet = Behandling.fraTidligereBehandling(behandling, BehandlingType.REVURDERING)
            .medBehandlingStatus(BehandlingStatus.UTREDES)
            .medBehandlingÅrsak(new BehandlingÅrsak.Builder(List.of(BehandlingÅrsakType.RE_OPPLYSNINGER_OM_INNTEKT))).build();
        behandlingRepository.lagre(revurderingIkkeManueltOpprettet, behandlingRepository.taSkriveLås(revurderingIkkeManueltOpprettet));


        var filter = mock(VilkårPeriodeFilter.class);
        when(filterProvider.getFilter(BehandlingReferanse.fra(behandling)))
            .thenReturn(filter);
        when(filterProvider.getFilter(BehandlingReferanse.fra(behandlingManueltOpprettet)))
            .thenReturn(filter);
        when(filterProvider.getFilter(BehandlingReferanse.fra(revurderingIkkeManueltOpprettet)))
            .thenReturn(filter);
        when(perioderTilVurderingTjeneste.utledPerioderTilVurdering(any(BehandlingReferanse.class), any(VilkårPeriodeFilter.class)))
            .thenReturn(new TreeSet<>(Set.of(DatoIntervallEntitet.fraOgMedTilOgMed(STP, STP.plusDays(10)))));

        opptjeningForBeregningTjeneste = mock(PSBOpptjeningForBeregningTjeneste.class);
        when(opptjeningForBeregningTjeneste.hentEksaktOpptjeningForBeregning(any(), any(), any(), true))
            .thenReturn(Optional.of(new OpptjeningAktiviteter(List.of(
                OpptjeningAktiviteter.nyPeriode(OpptjeningAktivitetType.PLEIEPENGER, new Periode(STP.minusMonths(1), STP),
                    null, null, null)))));

        beregningPerioderGrunnlagRepository = new BeregningPerioderGrunnlagRepository(entityManager, new VilkårResultatRepository(entityManager));

        oppgittOpptjeningFilter = mock(PSBOppgittOpptjeningFilter.class);
        when(oppgittOpptjeningFilter.hentOppgittOpptjening(any(), any(), any(LocalDate.class)))
            .thenReturn(Optional.empty());
        utleder = new PSBPreconditionBeregningAksjonspunktUtleder(iayTjeneste, opptjeningForBeregningTjeneste,
            fagsakRepository, oppgittOpptjeningFilter,
            perioderTilVurderingTjeneste,
            filterProvider, beregningPerioderGrunnlagRepository, behandlingRepository);
    }

    @Test
    void skal_ikke_returnere_aksjonspunkt_når_eksisterende_overstyring() {
        lagInfotrygdPsbYtelse(DatoIntervallEntitet.fraOgMedTilOgMed(STP, STP), Arbeidskategori.ARBEIDSTAKER, revurderingIkkeManueltOpprettet);
        fagsakRepository.opprettInfotrygdmigrering(revurderingIkkeManueltOpprettet.getFagsakId(), STP);
        beregningPerioderGrunnlagRepository.lagreInputOverstyringer(revurderingIkkeManueltOpprettet.getId(), List.of(new InputOverstyringPeriode(STP, List.of())));

        var aksjonspunkter = utleder.utledAksjonspunkterFor(new AksjonspunktUtlederInput(BehandlingReferanse.fra(revurderingIkkeManueltOpprettet, STP)));

        assertThat(aksjonspunkter.size()).isEqualTo(0);
    }

    @Test
    void skal_returnere_aksjonspunkt_når_eksisterende_overstyring_og_manuelt_opprettet_behandling() {
        lagInfotrygdPsbYtelse(DatoIntervallEntitet.fraOgMedTilOgMed(STP, STP), Arbeidskategori.ARBEIDSTAKER, behandlingManueltOpprettet);
        fagsakRepository.opprettInfotrygdmigrering(behandlingManueltOpprettet.getFagsakId(), STP);
        beregningPerioderGrunnlagRepository.lagreInputOverstyringer(behandlingManueltOpprettet.getId(), List.of(new InputOverstyringPeriode(STP, List.of())));

        var aksjonspunkter = utleder.utledAksjonspunkterFor(new AksjonspunktUtlederInput(BehandlingReferanse.fra(behandlingManueltOpprettet, STP)));

        assertThat(aksjonspunkter.size()).isEqualTo(1);
    }



    @Test
    void skal_returnere_aksjonspunkt_eksisterende_migrering() {
        lagInfotrygdPsbYtelse(DatoIntervallEntitet.fraOgMedTilOgMed(STP, STP), Arbeidskategori.ARBEIDSTAKER, behandling);
        fagsakRepository.opprettInfotrygdmigrering(behandling.getFagsakId(), STP);

        var aksjonspunkter = utleder.utledAksjonspunkterFor(new AksjonspunktUtlederInput(BehandlingReferanse.fra(behandling, STP)));

        assertThat(aksjonspunkter.size()).isEqualTo(1);
        assertThat(aksjonspunkter.get(0).getAksjonspunktDefinisjon()).isEqualTo(AksjonspunktDefinisjon.OVERSTYR_BEREGNING_INPUT);
    }

    @Test
    void skal_returnere_aksjonspunkt_ved_overlapp_med_flere_perioder() {
        when(perioderTilVurderingTjeneste.utledPerioderTilVurdering(any(BehandlingReferanse.class), any(VilkårPeriodeFilter.class)))
            .thenReturn(new TreeSet<>((Set.of(DatoIntervallEntitet.fraOgMedTilOgMed(STP, STP.plusDays(10)),
                DatoIntervallEntitet.fraOgMedTilOgMed(STP.plusDays(20), STP.plusDays(30))))));
        lagInfotrygdPsbYtelsePerioder(List.of(
            DatoIntervallEntitet.fraOgMedTilOgMed(STP, STP.plusDays(10)),
            DatoIntervallEntitet.fraOgMedTilOgMed(STP.plusDays(20), STP.plusDays(30))
        ), Arbeidskategori.ARBEIDSTAKER, behandling);

        fagsakRepository.opprettInfotrygdmigrering(fagsak.getId(), STP.plusDays(20));

        var aksjonspunkter = utleder.utledAksjonspunkterFor(new AksjonspunktUtlederInput(BehandlingReferanse.fra(behandling, STP)));

        assertThat(aksjonspunkter.size()).isEqualTo(1);
        assertThat(aksjonspunkter.get(0).getAksjonspunktDefinisjon()).isEqualTo(AksjonspunktDefinisjon.OVERSTYR_BEREGNING_INPUT);
    }

    @Test
    void skal_ikkje_returnere_aksjonspunkt_med_overlapp_for_periode_som_ikke_vurderes() {
        lagInfotrygdPsbYtelse(DatoIntervallEntitet.fraOgMedTilOgMed(STP.minusDays(10), STP.minusDays(3)), Arbeidskategori.ARBEIDSTAKER, behandling);
        fagsakRepository.opprettInfotrygdmigrering(fagsak.getId(), STP.minusDays(10));

        var aksjonspunkter = utleder.utledAksjonspunkterFor(new AksjonspunktUtlederInput(BehandlingReferanse.fra(behandling, STP)));

        assertThat(aksjonspunkter.size()).isEqualTo(0);
    }

    @Test
    void skal_ikkje_returnere_aksjonspunkt_uten_overlapp() {
        lagInfotrygdPsbYtelse(DatoIntervallEntitet.fraOgMedTilOgMed(STP.minusMonths(2), STP.minusMonths(2).plusDays(2)), Arbeidskategori.ARBEIDSTAKER, behandling);
        var aksjonspunkter = utleder.utledAksjonspunkterFor(new AksjonspunktUtlederInput(BehandlingReferanse.fra(behandling, STP)));

        assertThat(aksjonspunkter.size()).isEqualTo(0);
    }

    @Test
    void skal_ikke_returnere_ventepunkt_når_ingen_perioder_overlapper_med_infotrygd() {
        lagInfotrygdPsbYtelse(DatoIntervallEntitet.fraOgMedTilOgMed(STP.minusDays(20), STP.minusDays(10)), Arbeidskategori.ARBEIDSTAKER, behandling);
        fagsakRepository.opprettInfotrygdmigrering(fagsak.getId(), STP);

        var aksjonspunkter = utleder.utledAksjonspunkterFor(new AksjonspunktUtlederInput(BehandlingReferanse.fra(behandling, STP)));

        assertThat(aksjonspunkter.size()).isEqualTo(1);
        assertThat(aksjonspunkter.get(0).getAksjonspunktDefinisjon()).isEqualTo(AksjonspunktDefinisjon.OVERSTYR_BEREGNING_INPUT);
    }

    @Test
    void skal_ikke_returnere_aksjonspunt_ved_pleiepenger_av_dagpenger() {
        lagInfotrygdPsbYtelse(DatoIntervallEntitet.fraOgMedTilOgMed(STP, STP.plusDays(10)), Arbeidskategori.DAGPENGER, behandling);
        when(opptjeningForBeregningTjeneste.hentEksaktOpptjeningForBeregning(any(), any(), any(), true))
            .thenReturn(Optional.of(new OpptjeningAktiviteter(List.of(
                OpptjeningAktiviteter.nyPeriode(OpptjeningAktivitetType.PLEIEPENGER_AV_DAGPENGER, new Periode(STP, STP.plusDays(10)),
                    null, null, null)))));
        fagsakRepository.opprettInfotrygdmigrering(fagsak.getId(), STP);

        var aksjonspunkter = utleder.utledAksjonspunkterFor(new AksjonspunktUtlederInput(BehandlingReferanse.fra(behandling, STP)));

        assertThat(aksjonspunkter.size()).isEqualTo(0);
    }

    @Test
    void skal_sette_på_vent_ved_pleiepenger_av_næring_uten_søkt_om_næring() {
        lagInfotrygdPsbYtelse(DatoIntervallEntitet.fraOgMedTilOgMed(STP, STP.plusDays(10)), Arbeidskategori.SELVSTENDIG_NÆRINGSDRIVENDE, behandling);
        when(opptjeningForBeregningTjeneste.hentEksaktOpptjeningForBeregning(any(), any(), any(), true))
            .thenReturn(Optional.of(new OpptjeningAktiviteter(List.of(
                OpptjeningAktiviteter.nyPeriode(OpptjeningAktivitetType.NÆRING, new Periode(STP, STP.plusDays(10)),
                    null, null, null)))));
        fagsakRepository.opprettInfotrygdmigrering(fagsak.getId(), STP);

        var aksjonspunkter = utleder.utledAksjonspunkterFor(new AksjonspunktUtlederInput(BehandlingReferanse.fra(behandling, STP)));

        assertThat(aksjonspunkter.size()).isEqualTo(1);
        assertThat(aksjonspunkter.get(0).getAksjonspunktDefinisjon()).isEqualTo(AksjonspunktDefinisjon.AUTO_VENT_PÅ_KOMPLETT_SØKNAD_VED_OVERGANG_FRA_INFOTRYGD);
    }

    @Test
    void skal_returnere_aksjonspunkt_ved_pleiepenger_av_næring_med_søkt_om_næring() {
        lagInfotrygdPsbYtelse(DatoIntervallEntitet.fraOgMedTilOgMed(STP, STP.plusDays(10)), Arbeidskategori.SELVSTENDIG_NÆRINGSDRIVENDE, behandling);
        when(opptjeningForBeregningTjeneste.hentEksaktOpptjeningForBeregning(any(), any(), any(), true))
            .thenReturn(Optional.of(new OpptjeningAktiviteter(List.of(
                OpptjeningAktiviteter.nyPeriode(OpptjeningAktivitetType.NÆRING, new Periode(STP, STP.plusDays(10)),
                    null, null, null)))));
        when(oppgittOpptjeningFilter.hentOppgittOpptjening(any(), any(), any(LocalDate.class)))
            .thenReturn(Optional.of(byggOppgittOpptjeningMedNæring()));
        fagsakRepository.opprettInfotrygdmigrering(fagsak.getId(), STP);

        var aksjonspunkter = utleder.utledAksjonspunkterFor(new AksjonspunktUtlederInput(BehandlingReferanse.fra(behandling, STP)));

        assertThat(aksjonspunkter.size()).isEqualTo(1);
        assertThat(aksjonspunkter.get(0).getAksjonspunktDefinisjon()).isEqualTo(AksjonspunktDefinisjon.OVERSTYR_BEREGNING_INPUT);
    }


    @Test
    void skal_sette_på_vent_ved_pleiepenger_av_frilans_uten_søkt_om_frilans() {
        lagInfotrygdPsbYtelse(DatoIntervallEntitet.fraOgMedTilOgMed(STP, STP.plusDays(10)), Arbeidskategori.FRILANSER, behandling);
        when(opptjeningForBeregningTjeneste.hentEksaktOpptjeningForBeregning(any(), any(), any(), true))
            .thenReturn(Optional.of(new OpptjeningAktiviteter(List.of(
                OpptjeningAktiviteter.nyPeriode(OpptjeningAktivitetType.FRILANS, new Periode(STP, STP.plusDays(10)),
                    null, null, null)))));
        fagsakRepository.opprettInfotrygdmigrering(fagsak.getId(), STP);

        var aksjonspunkter = utleder.utledAksjonspunkterFor(new AksjonspunktUtlederInput(BehandlingReferanse.fra(behandling, STP)));

        assertThat(aksjonspunkter.size()).isEqualTo(1);
        assertThat(aksjonspunkter.get(0).getAksjonspunktDefinisjon()).isEqualTo(AksjonspunktDefinisjon.AUTO_VENT_PÅ_KOMPLETT_SØKNAD_VED_OVERGANG_FRA_INFOTRYGD);
    }


    @Test
    void skal_returnere_aksjonspunkt_ved_pleiepenger_av_frilans_med_søkt_om_frilans() {
        lagInfotrygdPsbYtelse(DatoIntervallEntitet.fraOgMedTilOgMed(STP, STP.plusDays(10)), Arbeidskategori.FRILANSER, behandling);
        when(opptjeningForBeregningTjeneste.hentEksaktOpptjeningForBeregning(any(), any(), any(), true))
            .thenReturn(Optional.of(new OpptjeningAktiviteter(List.of(
                OpptjeningAktiviteter.nyPeriode(OpptjeningAktivitetType.FRILANS, new Periode(STP, STP.plusDays(10)),
                    null, null, null)))));
        when(oppgittOpptjeningFilter.hentOppgittOpptjening(any(), any(), any(LocalDate.class)))
            .thenReturn(Optional.of(byggOppgittOpptjeningMedFrilans()));
        fagsakRepository.opprettInfotrygdmigrering(fagsak.getId(), STP);

        var aksjonspunkter = utleder.utledAksjonspunkterFor(new AksjonspunktUtlederInput(BehandlingReferanse.fra(behandling, STP)));

        assertThat(aksjonspunkter.size()).isEqualTo(1);
        assertThat(aksjonspunkter.get(0).getAksjonspunktDefinisjon()).isEqualTo(AksjonspunktDefinisjon.OVERSTYR_BEREGNING_INPUT);
    }


    private OppgittOpptjening byggOppgittOpptjeningMedNæring() {
        return OppgittOpptjeningBuilder.ny().leggTilEgneNæringer(List.of(OppgittOpptjeningBuilder.EgenNæringBuilder.ny()
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(STP.minusMonths(10), STP.plusMonths(1)))
            .medVirksomhetType(VirksomhetType.FISKE)
            .medVirksomhet("999999999"))).build();
    }

    private OppgittOpptjening byggOppgittOpptjeningMedFrilans() {
        return OppgittOpptjeningBuilder.ny().leggTilFrilansOpplysninger(OppgittOpptjeningBuilder.OppgittFrilansBuilder.ny()
            .medFrilansOppdrag(List.of(OppgittOpptjeningBuilder.OppgittFrilansOppdragBuilder.ny()
                .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(STP.minusMonths(10), STP.plusMonths(1)))
                .medOppdragsgiver("999999999")
                .medInntekt(BigDecimal.TEN).build())).build()).build();
    }


    private void lagInfotrygdPsbYtelse(DatoIntervallEntitet periode, Arbeidskategori arbeidskategori, Behandling behandling) {
        lagInfotrygdPsbYtelsePerioder(List.of(periode), arbeidskategori, behandling);
    }


    private void lagInfotrygdPsbYtelsePerioder(List<DatoIntervallEntitet> perioder, Arbeidskategori arbeidskategori, Behandling behandling) {
        var iayBuilder = InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.empty(), VersjonType.REGISTER);
        var aktørYtelseBuilder = InntektArbeidYtelseAggregatBuilder.AktørYtelseBuilder.oppdatere(Optional.empty());
        var ytelseBuilder = YtelseBuilder.oppdatere(Optional.empty());

        var fom = perioder.stream().map(DatoIntervallEntitet::getFomDato).min(Comparator.naturalOrder()).orElseThrow();
        var tom = perioder.stream().map(DatoIntervallEntitet::getTomDato).max(Comparator.naturalOrder()).orElseThrow();

        ytelseBuilder.medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom))
            .medYtelseGrunnlag(ytelseBuilder.getGrunnlagBuilder().medArbeidskategori(arbeidskategori).build())
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
