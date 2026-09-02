package no.nav.ung.sak.web.app.tjenester.behandling.aktivitetspenger;

import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.k9.felles.testutilities.sikkerhet.StaticSubjectHandler;
import no.nav.k9.felles.testutilities.sikkerhet.SubjectHandlerUtils;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.varsel.EtterlysningType;
import no.nav.ung.kodeverk.vilkår.BistandsvilkårIkkeOppfyltÅrsak;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.startdato.StartdatoRepository;
import no.nav.ung.sak.behandlingslager.behandling.startdato.Startdatoer;
import no.nav.ung.sak.behandlingslager.behandling.startdato.SøktStartdato;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårResultatBuilder;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.ung.sak.behandlingslager.etterlysning.Etterlysning;
import no.nav.ung.sak.behandlingslager.etterlysning.EtterlysningRepository;
import no.nav.ung.sak.behandlingslager.inngangsvilkår.AktivitetspengerInngangsvilkårResultatGrunnlag;
import no.nav.ung.sak.behandlingslager.inngangsvilkår.BistandsvilkårResultatPeriode;
import no.nav.ung.sak.behandlingslager.inngangsvilkår.InngangsvilkårVurderingRepository;
import no.nav.ung.sak.behandlingslager.vilkårsavklaring.VilkårPeriodeAvklaring;
import no.nav.ung.sak.behandlingslager.vilkårsavklaring.VilkårsavklaringGrunnlagRepository;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.etterlysning.VilkårsavklaringEtterlysningTjeneste;
import no.nav.ung.sak.kontrakt.aktivitetspenger.ÅpenPeriode;
import no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår.bistand.BistandFaktaavklaringPeriodeDto;
import no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår.bistand.BistandVurderingIkkeOppfyltDto;
import no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår.bistand.VurderFaktaOmBistandDto;
import no.nav.ung.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.ung.sak.test.util.behandling.ungdomsprogramytelse.TestScenarioBuilder;
import no.nav.ung.sak.trigger.ProsessTriggereRepository;
import no.nav.ung.sak.trigger.Trigger;
import no.nav.ung.sak.typer.JournalpostId;
import no.nav.ung.sak.typer.Periode;
import no.nav.ung.ytelse.aktivitetspenger.del1.InngangsvilkårVurderingTjeneste;
import no.nav.ung.ytelse.aktivitetspenger.del1.steg.bistandsvilkår.BistandAvklaringTjeneste;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(JpaExtension.class)
@ExtendWith(CdiAwareExtension.class)
class VurderFaktaOmBistandOppdatererTest {

    private static final LocalDate FOM = LocalDate.of(2026, 1, 1);
    private static final LocalDate TOM = LocalDate.of(2026, 12, 31);

    private static final Periode PERIODE_1 = new Periode(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));
    private static final Periode PERIODE_2 = new Periode(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31));

    private static final String SAKSBEHANDLER = "saksbehandler1";
    private static final String BEGRUNNELSE_IKKE_VARSEL = "Begrunnelse for hvorfor det ikke varsles";

    @Inject
    private EntityManager entityManager;

    @Inject
    private @Any Instance<VilkårsPerioderTilVurderingTjeneste> vilkårsPerioderTilVurderingTjenester;

    private VilkårsavklaringGrunnlagRepository vilkårsavklaringGrunnlagRepository;
    private InngangsvilkårVurderingRepository inngangsvilkårVurderingRepository;
    private EtterlysningRepository etterlysningRepository;
    private ProsessTaskTjeneste prosessTaskTjeneste;
    private VurderFaktaOmBistandOppdaterer oppdaterer;
    private Behandling behandling;
    private VilkårResultatRepository vilkårResultatRepository;

    @BeforeAll
    static void beforeAll() {
        SubjectHandlerUtils.useSubjectHandler(StaticSubjectHandler.class);
        SubjectHandlerUtils.setInternBruker(SAKSBEHANDLER);
    }

    @AfterAll
    static void afterAll() {
        SubjectHandlerUtils.reset();
    }

    @BeforeEach
    void setUp() {
        var behandlingRepository = new BehandlingRepository(entityManager);
        var historikkinnslagRepository = new HistorikkinnslagRepository(entityManager);
        vilkårsavklaringGrunnlagRepository = new VilkårsavklaringGrunnlagRepository(entityManager);
        inngangsvilkårVurderingRepository = new InngangsvilkårVurderingRepository(entityManager);
        etterlysningRepository = new EtterlysningRepository(entityManager);
        prosessTaskTjeneste = mock(ProsessTaskTjeneste.class);
        vilkårResultatRepository = new VilkårResultatRepository(entityManager);
        var inngangsvilkårVurderingTjeneste = new InngangsvilkårVurderingTjeneste(inngangsvilkårVurderingRepository, behandlingRepository, vilkårResultatRepository);
        var bistandAvklaringTjeneste = new BistandAvklaringTjeneste(
            vilkårsavklaringGrunnlagRepository,
            new VilkårsavklaringEtterlysningTjeneste(etterlysningRepository, prosessTaskTjeneste),
            inngangsvilkårVurderingTjeneste);

        oppdaterer = new VurderFaktaOmBistandOppdaterer(
            behandlingRepository,
            historikkinnslagRepository,
            vilkårsPerioderTilVurderingTjenester,
            bistandAvklaringTjeneste
        );

        behandling = opprettBehandlingMedVilkårOgPeriode();
    }

    @Test
    void skal_lagre_foreslatt_avklaring_og_opprette_etterlysning() {
        oppdater(dtoMedVarsel(new ÅpenPeriode(FOM, TOM), BistandsvilkårIkkeOppfyltÅrsak.IKKE_14A_VEDTAK));

        var avklaringer = hentSorterteAvklaringer();
        assertThat(avklaringer).hasSize(1);
        assertThat(avklaringer.getFirst().getPeriode()).isEqualTo(DatoIntervallEntitet.fraOgMedTilOgMed(FOM, TOM));
        assertThat(avklaringer.getFirst().getIkkeOppfyltÅrsakKode()).isEqualTo(BistandsvilkårIkkeOppfyltÅrsak.IKKE_14A_VEDTAK.getKode());
        assertThat(avklaringer.getFirst().getVurdertAv()).isEqualTo(SAKSBEHANDLER);

        var etterlysninger = etterlysningRepository.hentOpprettetEtterlysninger(behandling.getId(), EtterlysningType.UTTALELSE_BISTAND);
        assertThat(etterlysninger).hasSize(1);
        assertThat(etterlysninger.getFirst().getGrunnlagsreferanse()).isEqualTo(avklaringer.getFirst().getReferanse());
        verify(prosessTaskTjeneste).lagre(any(ProsessTaskData.class));
    }

    @Test
    void skal_ikke_opprette_eller_avbryte_nar_avklaring_er_uendret() {
        var dto = dtoMedVarsel(new ÅpenPeriode(FOM, TOM), BistandsvilkårIkkeOppfyltÅrsak.IKKE_14A_VEDTAK);
        oppdater(dto);

        var referanseFørstegang = hentSorterteAvklaringer().getFirst().getReferanse();

        oppdater(dto);

        assertThat(etterlysningRepository.hentEtterlysningerSomSkalAvbrytes(behandling.getId())).isEmpty();
        assertThat(etterlysningRepository.hentOpprettetEtterlysninger(behandling.getId(), EtterlysningType.UTTALELSE_BISTAND))
            .extracting(Etterlysning::getGrunnlagsreferanse)
            .containsExactly(referanseFørstegang);
        verify(prosessTaskTjeneste, org.mockito.Mockito.times(1)).lagre(any(ProsessTaskData.class));
    }

    @Test
    void skal_lagre_avklaring_uten_varsel_uten_a_opprette_etterlysning() {
        oppdater(dtoUtenVarsel(new ÅpenPeriode(FOM, TOM), BistandsvilkårIkkeOppfyltÅrsak.IKKE_14A_VEDTAK));

        var avklaringer = hentSorterteAvklaringer();
        assertThat(avklaringer).hasSize(1);
        assertThat(avklaringer.getFirst().skalSendeVarsel()).isFalse();
        assertThat(avklaringer.getFirst().getBegrunnelseIkkeVarsel()).isEqualTo(BEGRUNNELSE_IKKE_VARSEL);

        assertThat(etterlysningRepository.hentEtterlysninger(behandling.getId())).isEmpty();
        verify(prosessTaskTjeneste, never()).lagre(any(ProsessTaskData.class));
    }

    @Test
    void avkortet_skal_avvises_ved_lagring() {
        var dto = dtoUtenVarsel(new ÅpenPeriode(FOM, TOM), BistandsvilkårIkkeOppfyltÅrsak.AVKORTET);

        assertThatThrownBy(() -> oppdater(dto)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void annet_uten_fritekst_skal_avvises_ved_lagring() {
        var vurdering = new BistandVurderingIkkeOppfyltDto(BistandsvilkårIkkeOppfyltÅrsak.ANNET, "begrunnelse", null, null);
        var dto = new VurderFaktaOmBistandDto(List.of(new BistandFaktaavklaringPeriodeDto(new ÅpenPeriode(FOM, TOM), vurdering, false)), "begrunnelse");

        assertThatThrownBy(() -> oppdater(dto)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void apen_tom_skal_lukkes_mot_maksdato_i_vilkarsperioden() {
        oppdater(dtoUtenVarsel(new ÅpenPeriode(FOM.plusMonths(1), null), BistandsvilkårIkkeOppfyltÅrsak.IKKE_14A_VEDTAK));

        var avklaringer = hentSorterteAvklaringer();
        assertThat(avklaringer).hasSize(1);
        assertThat(avklaringer.getFirst().getPeriode()).isEqualTo(DatoIntervallEntitet.fraOgMedTilOgMed(FOM.plusMonths(1), TOM));
    }

    @Test
    void skal_sette_vilkarsperiode_for_avklaringen_til_ikke_vurdert() {
        var dto = dtoUtenVarsel(new ÅpenPeriode(FOM, TOM), BistandsvilkårIkkeOppfyltÅrsak.IKKE_14A_VEDTAK);
        var param = new AksjonspunktOppdaterParameter(behandling, Optional.empty(), dto);

        oppdaterer.oppdater(dto, param);

        var bistandsvilkår = param.getVilkårResultatBuilder().build().getVilkår(VilkårType.BISTANDSVILKÅR).orElseThrow();
        assertThat(bistandsvilkår.getPerioder()).hasSize(1);
        assertThat(bistandsvilkår.getPerioder().getFirst().getPeriode()).isEqualTo(DatoIntervallEntitet.fraOgMedTilOgMed(FOM, TOM));
        assertThat(bistandsvilkår.getPerioder().getFirst().getUtfall()).isEqualTo(Utfall.IKKE_VURDERT);
    }

    @Test
    void endret_avklaring_som_ikke_dekker_hele_tidligere_avklaring_skal_gjenopprette_resten_fra_forrige_behandling() {
        var heleperioden = new Periode(FOM, TOM);
        var originalBehandling = opprettOriginalBehandling(vilkårsperiode(heleperioden, Utfall.OPPFYLT));
        lagreVilkårsvurderinger(originalBehandling, oppfyltVurdering(heleperioden, "original vilkårsvurdering"));

        var revurdering = opprettRevurderingMedGrunnlagKopiert(originalBehandling, vilkårsperiode(heleperioden, Utfall.OPPFYLT));

        var vilkårResultat1 = oppdater(revurdering, dtoUtenVarsel(new ÅpenPeriode(PERIODE_1.getFom(), PERIODE_1.getTom()), BistandsvilkårIkkeOppfyltÅrsak.IKKE_14A_VEDTAK));

        assertThat(hentAllePerioderMedIkkeVurdert(vilkårResultat1))
            .as("det skal finnes nøyaktig én periode med IKKE_VURDERT, og den skal være lik perioden for ny avklaring")
            .containsExactly(PERIODE_1);

        // Simulerer at saksbehandler har utført vilkårsvurderingen, men at behandlingen er retur fra beslutter.
        lagreVilkårsvurderinger(revurdering, ikkeOppfyltVurdering(PERIODE_1, BistandsvilkårIkkeOppfyltÅrsak.IKKE_14A_VEDTAK, "vilkårsvurdering etter avklaring1"));

        assertThat(hentVilkårsvurderingerForPeriode(revurdering, PERIODE_1).getFirst().getBegrunnelse())
            .isEqualTo("vilkårsvurdering etter avklaring1");

        var vilkårResultat = oppdater(revurdering, dtoUtenVarsel(new ÅpenPeriode(PERIODE_2.getFom(), PERIODE_2.getTom()), BistandsvilkårIkkeOppfyltÅrsak.IKKE_14A_VEDTAK));

        assertThat(hentVilkårsvurderingerForPeriode(revurdering, PERIODE_1).getFirst().getBegrunnelse())
            .as("vilkårsvurderingen for tidligere avklaring skal være erstattet av gjenopprettet vilkårsvurdering fra forrige behandling")
            .isEqualTo("original vilkårsvurdering");

        assertThat(hentVilkårsperiode(vilkårResultat, PERIODE_1).getGjeldendeUtfall())
            .as("perioden som ikke lenger er dekket av avklaringen skal gjenopprettes fra forrige behandling")
            .isEqualTo(Utfall.OPPFYLT);

        assertThat(hentAllePerioderMedIkkeVurdert(vilkårResultat)).containsExactly(PERIODE_2);
    }

    @Test
    void endret_avklaring_som_dekker_hele_tidligere_avklaring_skal_ikke_gjenopprette_noe() {
        var heleperioden = new Periode(FOM, TOM);
        var originalBehandling = opprettOriginalBehandling(vilkårsperiode(heleperioden, Utfall.OPPFYLT));
        lagreVilkårsvurderinger(originalBehandling, oppfyltVurdering(heleperioden, "original vurdering"));

        var revurdering = opprettRevurderingMedGrunnlagKopiert(originalBehandling, vilkårsperiode(heleperioden, Utfall.IKKE_VURDERT));

        oppdater(revurdering, dtoUtenVarsel(new ÅpenPeriode(FOM, TOM), BistandsvilkårIkkeOppfyltÅrsak.ANNET));
        var vilkårResultat = oppdater(revurdering, dtoUtenVarsel(new ÅpenPeriode(FOM, TOM), BistandsvilkårIkkeOppfyltÅrsak.IKKE_14A_VEDTAK));

        assertThat(hentVilkårsperiode(vilkårResultat, heleperioden).getGjeldendeUtfall())
            .as("hele perioden det avklares på nytt for skal vurderes på nytt")
            .isEqualTo(Utfall.IKKE_VURDERT);

        assertThat(hentVilkårsvurderinger(revurdering))
            .as("ingenting skal gjenopprettes når ny avklaring dekker hele forrige avklaring")
            .extracting(BistandsvilkårResultatPeriode::getPeriode, BistandsvilkårResultatPeriode::isGodkjent, BistandsvilkårResultatPeriode::getBegrunnelse)
            .containsExactly(tuple(tilDatoIntervallEntitet(heleperioden), true, "original vurdering"));
    }

    private void oppdater(VurderFaktaOmBistandDto dto) {
        oppdaterer.oppdater(dto, new AksjonspunktOppdaterParameter(behandling, Optional.empty(), dto));
    }

    private LocalDateTimeline<VilkårPeriode> oppdater(Behandling behandling, VurderFaktaOmBistandDto dto) {
        VilkårResultatBuilder vilkårResultatBuilder = Vilkårene.builderFraEksisterende(vilkårResultatRepository.hent(behandling.getId()));
        var param = new AksjonspunktOppdaterParameter(behandling, Optional.empty(), vilkårResultatBuilder, dto);
        oppdaterer.oppdater(dto, param);
        var vilkårResultat = param.getVilkårResultatBuilder().build();
        vilkårResultatRepository.lagre(behandling.getId(), vilkårResultat);
        return vilkårResultat.getVilkårTimeline(VilkårType.BISTANDSVILKÅR);
    }

    private static VilkårPeriode hentVilkårsperiode(LocalDateTimeline<VilkårPeriode> tidslinje, Periode periode) {
        return tidslinje.intersection(new LocalDateInterval(periode.getFom(), periode.getTom()))
            .segmenter().stream()
            .map(LocalDateSegment::getValue)
            .findFirst()
            .orElseThrow(() -> new AssertionError("Fant ikke vilkårsperiode " + periode + " i " + tidslinje));
    }

    private static List<Periode> hentAllePerioderMedIkkeVurdert(LocalDateTimeline<VilkårPeriode> tidslinje) {
        return tidslinje.segmenter().stream()
            .filter(s -> s.getValue().getGjeldendeUtfall() == Utfall.IKKE_VURDERT)
            .map(s -> new Periode(s.getFom(), s.getTom()))
            .toList();
    }

    private List<BistandsvilkårResultatPeriode> hentVilkårsvurderinger(Behandling behandling) {
        return inngangsvilkårVurderingRepository.hentEksisterendeGrunnlag(behandling.getId())
            .map(AktivitetspengerInngangsvilkårResultatGrunnlag::hentBistandsvilkårResultatPerioder)
            .orElseThrow();
    }

    private List<BistandsvilkårResultatPeriode> hentVilkårsvurderingerForPeriode(Behandling behandling, Periode periode) {
        return hentVilkårsvurderinger(behandling).stream()
            .filter(it -> !it.getPeriode().getFomDato().isAfter(periode.getTom()) && !it.getPeriode().getTomDato().isBefore(periode.getFom()))
            .toList();
    }

    private Behandling opprettOriginalBehandling(VilkårsperiodeData... vilkårsperioder) {
        var builder = TestScenarioBuilder.builderMedSøknad(FagsakYtelseType.AKTIVITETSPENGER);
        leggTilVilkårsperioder(builder, vilkårsperioder);
        var original = builder.lagre(entityManager);

        inngangsvilkårVurderingRepository.lagreBistandsVurderinger(original.getId(), List.of());
        new ProsessTriggereRepository(entityManager).leggTil(original.getId(), Set.of(
            new Trigger(BehandlingÅrsakType.NY_SØKT_PERIODE, DatoIntervallEntitet.fraOgMedTilOgMed(FOM, TOM))));
        return original;
    }

    private Behandling opprettRevurderingMedGrunnlagKopiert(Behandling originalBehandling, VilkårsperiodeData... vilkårsperioder) {
        var builder = TestScenarioBuilder.builderMedSøknad(FagsakYtelseType.AKTIVITETSPENGER)
            .medBehandlingType(BehandlingType.REVURDERING)
            .medOriginalBehandling(originalBehandling, BehandlingÅrsakType.ENDRET_BISTANDSBEHOV);
        leggTilVilkårsperioder(builder, vilkårsperioder);
        var revurdering = builder.lagre(entityManager);

        vilkårsavklaringGrunnlagRepository.kopierGrunnlagFraEksisterendeBehandling(originalBehandling.getId(), revurdering.getId());
        inngangsvilkårVurderingRepository.kopier(originalBehandling.getId(), revurdering.getId());
        new ProsessTriggereRepository(entityManager).leggTil(revurdering.getId(), Set.of(
            new Trigger(BehandlingÅrsakType.ENDRET_BISTANDSBEHOV, DatoIntervallEntitet.fraOgMedTilOgMed(FOM, TOM))));
        return revurdering;
    }

    private static void leggTilVilkårsperioder(TestScenarioBuilder builder, VilkårsperiodeData... vilkårsperioder) {
        for (var v : vilkårsperioder) {
            builder.leggTilVilkår(VilkårType.BISTANDSVILKÅR, v.utfall(), v.periode());
        }
    }

    private void lagreVilkårsvurderinger(Behandling behandling, BistandVurderingData... vurderinger) {
        var perioder = Arrays.stream(vurderinger)
            .map(v -> new BistandsvilkårResultatPeriode(
                tilDatoIntervallEntitet(v.periode()),
                v.godkjent(),
                v.ikkeOppfyltÅrsak(),
                true,
                v.begrunnelse(),
                null,
                SAKSBEHANDLER,
                LocalDateTime.now()))
            .toList();
        inngangsvilkårVurderingRepository.lagreBistandsVurderinger(behandling.getId(), perioder);
    }

    private static VilkårsperiodeData vilkårsperiode(Periode periode, Utfall utfall) {
        return new VilkårsperiodeData(periode, utfall);
    }

    private record VilkårsperiodeData(Periode periode, Utfall utfall) {
    }

    private static BistandVurderingData oppfyltVurdering(Periode periode, String begrunnelse) {
        return new BistandVurderingData(periode, true, null, begrunnelse);
    }

    private static BistandVurderingData ikkeOppfyltVurdering(Periode periode, BistandsvilkårIkkeOppfyltÅrsak årsak, String begrunnelse) {
        return new BistandVurderingData(periode, false, årsak, begrunnelse);
    }

    private record BistandVurderingData(Periode periode, boolean godkjent, BistandsvilkårIkkeOppfyltÅrsak ikkeOppfyltÅrsak, String begrunnelse) {
    }

    private static DatoIntervallEntitet tilDatoIntervallEntitet(Periode periode) {
        return DatoIntervallEntitet.fraOgMedTilOgMed(periode.getFom(), periode.getTom());
    }

    private Behandling opprettBehandlingMedVilkårOgPeriode() {
        var behandling = TestScenarioBuilder.builderMedSøknad(FagsakYtelseType.AKTIVITETSPENGER)
            .leggTilVilkår(VilkårType.BISTANDSVILKÅR, Utfall.IKKE_VURDERT, new Periode(FOM, TOM))
            .lagre(entityManager);

        var søktStartdato = new SøktStartdato(FOM, new JournalpostId("jp-søknad-1"));
        var startdatoRepository = new StartdatoRepository(entityManager);
        startdatoRepository.lagre(behandling.getId(), List.of(søktStartdato));
        startdatoRepository.lagreRelevanteSøknader(behandling.getId(), new Startdatoer(List.of(søktStartdato)));

        new ProsessTriggereRepository(entityManager).leggTil(behandling.getId(), Set.of(
            new Trigger(BehandlingÅrsakType.NY_SØKT_PERIODE, DatoIntervallEntitet.fraOgMedTilOgMed(FOM, TOM))));
        return behandling;
    }

    private List<VilkårPeriodeAvklaring> hentSorterteAvklaringer() {
        return vilkårsavklaringGrunnlagRepository.hentGrunnlagHvisEksisterer(behandling.getId(), VilkårType.BISTANDSVILKÅR)
            .orElseThrow()
            .getForeslåtteAvklaringer()
            .stream()
            .sorted(Comparator.comparing(a -> a.getPeriode().getFomDato()))
            .toList();
    }

    private static VurderFaktaOmBistandDto dtoUtenVarsel(ÅpenPeriode periode, BistandsvilkårIkkeOppfyltÅrsak årsak) {
        var vurdering = new BistandVurderingIkkeOppfyltDto(årsak, "begrunnelse", null, BEGRUNNELSE_IKKE_VARSEL);
        return new VurderFaktaOmBistandDto(List.of(new BistandFaktaavklaringPeriodeDto(periode, vurdering, true)), "begrunnelse");
    }

    private static VurderFaktaOmBistandDto dtoMedVarsel(ÅpenPeriode periode, BistandsvilkårIkkeOppfyltÅrsak årsak) {
        var vurdering = new BistandVurderingIkkeOppfyltDto(årsak, "begrunnelse", "Fritekst til varsel", null);
        return new VurderFaktaOmBistandDto(List.of(new BistandFaktaavklaringPeriodeDto(periode, vurdering, false)), "begrunnelse");
    }
}
