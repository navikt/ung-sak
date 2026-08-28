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
import no.nav.ung.kodeverk.vilkår.BostedsvilkårIkkeOppfyltÅrsak;
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
import no.nav.ung.sak.behandlingslager.bosatt.BostedsGrunnlagRepository;
import no.nav.ung.sak.behandlingslager.bosatt.BostedsPeriodeAvklaring;
import no.nav.ung.sak.behandlingslager.bosatt.BostedsPeriodeAvklaringForeslått;
import no.nav.ung.sak.behandlingslager.etterlysning.EtterlysningRepository;
import no.nav.ung.sak.behandlingslager.inngangsvilkår.AktivitetspengerInngangsvilkårResultatGrunnlag;
import no.nav.ung.sak.behandlingslager.inngangsvilkår.BostedsvilkårResultatPeriode;
import no.nav.ung.sak.behandlingslager.inngangsvilkår.InngangsvilkårVurderingRepository;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår.BostedFaktaavklaringPeriodeDto;
import no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår.BostedVurderingIkkeOppfyltDto;
import no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår.VurderFaktaOmBostedDto;
import no.nav.ung.sak.kontrakt.aktivitetspenger.ÅpenPeriode;
import no.nav.ung.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.ung.sak.test.util.behandling.ungdomsprogramytelse.TestScenarioBuilder;
import no.nav.ung.sak.trigger.ProsessTriggereRepository;
import no.nav.ung.sak.trigger.Trigger;
import no.nav.ung.sak.typer.JournalpostId;
import no.nav.ung.sak.typer.Periode;
import no.nav.ung.ytelse.aktivitetspenger.del1.InngangsvilkårVurderingTjeneste;
import no.nav.ung.ytelse.aktivitetspenger.del1.steg.bosatt.BostedAvklaringTjeneste;
import no.nav.ung.ytelse.aktivitetspenger.del1.steg.bosatt.BostedsAvklaringDataMapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(JpaExtension.class)
@ExtendWith(CdiAwareExtension.class)
class VurderFaktaOmBostedOppdatererTest {

    private static final LocalDate FOM = LocalDate.of(2026, 1, 1);
    private static final LocalDate TOM = LocalDate.of(2026, 12, 31);

    private static final Periode PERIODE_1 = new Periode(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));
    private static final Periode PERIODE_2 = new Periode(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31));
    private static final Periode PERIODE_3 = new Periode(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31));

    private static final String SAKSBEHANDLER = "saksbehandler1";
    private static final String BEGRUNNELSE_IKKE_VARSEL = "Begrunnelse for hvorfor det ikke varsles";

    @Inject
    private EntityManager entityManager;

    @Inject
    private @Any Instance<VilkårsPerioderTilVurderingTjeneste> vilkårsPerioderTilVurderingTjenester;

    private BostedsGrunnlagRepository bostedsGrunnlagRepository;
    private InngangsvilkårVurderingRepository inngangsvilkårVurderingRepository;
    private EtterlysningRepository etterlysningRepository;
    private ProsessTaskTjeneste prosessTaskTjeneste;
    private VurderFaktaOmBostedOppdaterer oppdaterer;
    private Behandling behandling;
    private BostedAvklaringTjeneste bostedAvklaringTjeneste;
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
        bostedsGrunnlagRepository = new BostedsGrunnlagRepository(entityManager);
        inngangsvilkårVurderingRepository = new InngangsvilkårVurderingRepository(entityManager);
        etterlysningRepository = new EtterlysningRepository(entityManager);
        prosessTaskTjeneste = mock(ProsessTaskTjeneste.class);
        vilkårResultatRepository = new VilkårResultatRepository(entityManager);
        var inngangsvilkårVurderingTjeneste = new InngangsvilkårVurderingTjeneste(inngangsvilkårVurderingRepository, behandlingRepository, vilkårResultatRepository);
        bostedAvklaringTjeneste = new BostedAvklaringTjeneste(bostedsGrunnlagRepository, inngangsvilkårVurderingTjeneste, etterlysningRepository, prosessTaskTjeneste);

        oppdaterer = new VurderFaktaOmBostedOppdaterer(
            behandlingRepository,
            historikkinnslagRepository,
            vilkårsPerioderTilVurderingTjenester,
            bostedAvklaringTjeneste
        );

        behandling = opprettBehandlingMedVilkårOgPeriode();
        bostedsGrunnlagRepository.lagreInformasjonFraSøknad(behandling.getId(), "jp-søknad-1", FOM, true);
        inngangsvilkårVurderingRepository.lagreBostedVurderinger(behandling.getId(), List.of());
    }

    @Test
    void skal_ikke_opprette_eller_avbryte_nar_avklaring_er_uendret() {
        var dto = dtoMedEnAvklaring(BostedsvilkårIkkeOppfyltÅrsak.IKKE_BOSATTADRESSE_I_TRONDHEIM, true);
        var bostedAvklaringPeriode = konverterTilBostedAvklaringPeriode(dto, behandling);

        bostedsGrunnlagRepository.lagreForeslåtteAvklaringer(behandling.getId(), Set.of(bostedAvklaringPeriode));

        oppdater(dto);

        assertThat(etterlysningRepository.hentEtterlysninger(behandling.getId())).isEmpty();
        verify(prosessTaskTjeneste, never()).lagre(any(ProsessTaskData.class));
    }

    private static BostedsPeriodeAvklaringForeslått konverterTilBostedAvklaringPeriode(VurderFaktaOmBostedDto dto, Behandling behandling) {
        return BostedsAvklaringDataMapper.mapTilBostedsPeriodeAvklaring(
            BostedsAvklaringDataMapper.mapTilBostedAvklaringInnhold(dto.getAvklaringer().getFirst(), TOM),
            UUID.randomUUID().toString(),
            LocalDateTime.now()
        );
    }

    @Test
    void skal_fjerne_vilkårsvurdering_og_vilkårresultat_for_periode_ved_lagring_av_forslag() {
        var dto = dtoMedEnAvklaring(BostedsvilkårIkkeOppfyltÅrsak.ANNET, false);
        var param = new AksjonspunktOppdaterParameter(behandling, Optional.empty(), dto);

        oppdaterer.oppdater(dto, param);

        var bostedsvilkår = param.getVilkårResultatBuilder().build().getVilkår(VilkårType.BOSTEDSVILKÅR).orElseThrow();
        assertThat(bostedsvilkår.getPerioder()).hasSize(1);
        assertThat(bostedsvilkår.getPerioder().getFirst().getPeriode())
            .isEqualTo(DatoIntervallEntitet.fraOgMedTilOgMed(FOM, TOM));
        assertThat(bostedsvilkår.getPerioder().getFirst().getUtfall()).isEqualTo(Utfall.IKKE_VURDERT);
    }

    @Test
    void skal_lagre_avklaring_uten_varsel_og_endre_avklaring_uten_varsel_ingen_etterlysninger_opprettet() {
        oppdater(dtoUtenVarsel(new ÅpenPeriode(FOM, TOM), BostedsvilkårIkkeOppfyltÅrsak.ANNET));

        var førsteLagring = hentSorterteAvklaringer();
        assertThat(førsteLagring).hasSize(1);
        assertThat(førsteLagring.getFirst().getPeriode()).isEqualTo(DatoIntervallEntitet.fraOgMedTilOgMed(FOM, TOM));
        assertThat(førsteLagring.getFirst().skalSendeVarsel()).isFalse();
        assertThat(førsteLagring.getFirst().getBegrunnelseIkkeVarsel()).isEqualTo(BEGRUNNELSE_IKKE_VARSEL);
        assertThat(førsteLagring.getFirst().getVurdertAv()).isEqualTo(SAKSBEHANDLER);

        oppdater(dtoUtenVarsel(new ÅpenPeriode(FOM.plusDays(1), null), BostedsvilkårIkkeOppfyltÅrsak.ANNET));

        var andreLagring = hentSorterteAvklaringer();
        assertThat(andreLagring).hasSize(1);
        assertThat(andreLagring.getLast().getPeriode()).isEqualTo(DatoIntervallEntitet.fraOgMedTilOgMed(FOM.plusDays(1), TOM));
        assertThat(andreLagring.getLast().skalSendeVarsel()).isFalse();

        assertThat(etterlysningRepository.hentEtterlysninger(behandling.getId())).isEmpty();
        verify(prosessTaskTjeneste, never()).lagre(any(ProsessTaskData.class));
    }

    @Test
    void endret_avklaring_som_ikke_dekker_hele_tidligere_avklaring_skal_gjenopprette_resten_fra_forrige_behandling() {
        var heleperioden = new Periode(FOM, TOM);
        var originalBehandling = opprettOriginalBehandling(vilkårsperiode(heleperioden, Utfall.OPPFYLT));
        lagreVilkårsvurderinger(originalBehandling,
            oppfyltVurdering(heleperioden, "original vilkårsvurdering"));

        var revurdering = opprettRevurderingMedGrunnlagKopiert(originalBehandling,
            vilkårsperiode(heleperioden, Utfall.OPPFYLT)
        );

        var avklaring1 = dtoUtenVarsel(PERIODE_1, BostedsvilkårIkkeOppfyltÅrsak.IKKE_BOSATTADRESSE_I_TRONDHEIM);
        var vilkårResultat1 = oppdater(revurdering, avklaring1);

        assertThat(hentAllePerioderMedIkkeVurdert(vilkårResultat1))
            .as("det skal finnes nøyaktig én periode med IKKE_VURDERT, og den skal være lik perioden for ny avklaring")
            .containsExactly(PERIODE_1);

        // Simulerer at saksbehandler har utført vilkårsvurderingen, men at behandlingen er retur fra beslutter.
        // Vilkårsperioden vil da være IKKE_VURDERT, men med den nye vilkårsvurderingen intakt.
        lagreVilkårsvurderinger(revurdering,
            ikkeOppfyltVurdering(PERIODE_1, BostedsvilkårIkkeOppfyltÅrsak.IKKE_BOSATTADRESSE_I_TRONDHEIM, "vilkårsvurdering etter avklaring1"));

        assertThat(hentVilkårsvurderingerForPeriode(revurdering, PERIODE_1).getFirst().getBegrunnelse())
            .as("Sjekker at perioden for tidligere har fått ny begrunnelse")
            .isEqualTo("vilkårsvurdering etter avklaring1");

        var vilkårResultat = oppdater(revurdering, dtoUtenVarsel(PERIODE_2, BostedsvilkårIkkeOppfyltÅrsak.IKKE_BOSATTADRESSE_I_TRONDHEIM));

        // Verifiserer periode for tidligere avklaring
        assertThat(hentVilkårsvurderingerForPeriode(revurdering, PERIODE_1).getFirst().getBegrunnelse())
            .as("vilkårsvurderingen for tidligere avklaring skal være erstattet av gjenopprettet vilkårsvurdering fra forrige behandling")
            .isEqualTo("original vilkårsvurdering");

        var vilkårsperiodeForTidligereAvklartPeriode = hentVilkårsperiode(vilkårResultat, PERIODE_1);
        assertThat(vilkårsperiodeForTidligereAvklartPeriode.getGjeldendeUtfall())
            .as("perioden som ikke lenger er dekket av avklaringen skal gjenopprettes fra forrige behandling")
            .isEqualTo(Utfall.OPPFYLT);

        // Verifiserer periode for ny avklaring
        assertThat(hentAllePerioderMedIkkeVurdert(vilkårResultat))
            .as("det skal finnes nøyaktig én periode med IKKE_VURDERT, og den skal være lik perioden for ny avklaring")
            .containsExactly(PERIODE_2);

        assertThat(hentVilkårsvurderingerForPeriode(revurdering, PERIODE_2).getFirst().getBegrunnelse())
            .as("vilkårsvurdering som overlapper med ny avklaring skal ikke påvirkes i dette aksjonspunktet")
            .isEqualTo("original vilkårsvurdering");
    }

    @Test
    void endret_avklaring_som_dekker_hele_tidligere_avklaring_skal_ikke_gjenopprette_noe() {
        var heleperioden = new Periode(FOM, TOM);
        var originalBehandling = opprettOriginalBehandling(vilkårsperiode(heleperioden, Utfall.OPPFYLT));
        lagreVilkårsvurderinger(originalBehandling, oppfyltVurdering(heleperioden, "original vurdering"));

        var revurdering = opprettRevurderingMedGrunnlagKopiert(originalBehandling, ikkeVurdertVilkårsperiode(heleperioden));

        // Første runde: saksbehandler avklarer hele perioden
        oppdater(revurdering, dtoUtenVarsel(new ÅpenPeriode(FOM, TOM), BostedsvilkårIkkeOppfyltÅrsak.ANNET));

        // Andre runde: saksbehandler avklarer på nytt for hele perioden
        var vilkårResultat = oppdater(revurdering, dtoUtenVarsel(new ÅpenPeriode(FOM, TOM), BostedsvilkårIkkeOppfyltÅrsak.IKKE_BOSATTADRESSE_I_TRONDHEIM));

        var vilkårHeleperioden = hentVilkårsperiode(vilkårResultat, heleperioden);
        assertThat(vilkårHeleperioden.getGjeldendeUtfall())
            .as("hele perioden det avklares på nytt for skal vurderes på nytt")
            .isEqualTo(Utfall.IKKE_VURDERT);

        assertThat(hentVilkårsvurderinger(revurdering))
            .as("ingenting skal gjenopprettes når ny avklaring dekker hele forrige avklaring")
            .extracting(BostedsvilkårResultatPeriode::getPeriode, BostedsvilkårResultatPeriode::isGodkjent, BostedsvilkårResultatPeriode::getBegrunnelse)
            .containsExactly(tuple(tilDatoIntervallEntitet(heleperioden), true, "original vurdering"));
    }

    private void oppdater(VurderFaktaOmBostedDto dto) {
        oppdaterer.oppdater(dto, new AksjonspunktOppdaterParameter(behandling, Optional.empty(), dto));
    }

    private LocalDateTimeline<VilkårPeriode> oppdater(Behandling behandling, VurderFaktaOmBostedDto dto) {
        var param = oppdaterParameter(behandling, dto);
        oppdaterer.oppdater(dto, param);
        var vilkårResultat = param.getVilkårResultatBuilder().build();
        vilkårResultatRepository.lagre(behandling.getId(), vilkårResultat);
        return vilkårResultat.getVilkårTimeline(VilkårType.BOSTEDSVILKÅR);
    }

    private AksjonspunktOppdaterParameter oppdaterParameter(Behandling behandling, VurderFaktaOmBostedDto dto) {
        VilkårResultatBuilder vilkårResultatBuilder = Vilkårene.builderFraEksisterende(vilkårResultatRepository.hent(behandling.getId()));
        return new AksjonspunktOppdaterParameter(behandling, Optional.empty(), vilkårResultatBuilder, dto);
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

    private List<BostedsvilkårResultatPeriode> hentVilkårsvurderinger(Behandling behandling) {
        return inngangsvilkårVurderingRepository.hentEksisterendeGrunnlag(behandling.getId())
            .map(AktivitetspengerInngangsvilkårResultatGrunnlag::hentBostedsvilkårResultatPerioder)
            .orElseThrow();
    }

    // Lagrede vurderingsperioder kan bli splittet/slått sammen med andre grenser enn den etterspurte perioden
    // (jf. InngangsvilkårVurderingRepository#kombinerBosted), så vi matcher på overlapp i stedet for eksakt periode.
    private List<BostedsvilkårResultatPeriode> hentVilkårsvurderingerForPeriode(Behandling behandling, Periode periode) {
        return hentVilkårsvurderinger(behandling).stream()
            .filter(it -> !it.getPeriode().getFomDato().isAfter(periode.getTom()) && !it.getPeriode().getTomDato().isBefore(periode.getFom()))
            .toList();
    }

    private Behandling opprettOriginalBehandling(VilkårsperiodeData... vilkårsperioder) {
        var builder = TestScenarioBuilder.builderMedSøknad(FagsakYtelseType.AKTIVITETSPENGER);
        leggTilVilkårsperioder(builder, vilkårsperioder);
        var original = builder.lagre(entityManager);

        bostedsGrunnlagRepository.lagreInformasjonFraSøknad(original.getId(), "jp-original", FOM, true);
        inngangsvilkårVurderingRepository.lagreBostedVurderinger(original.getId(), List.of());
        new ProsessTriggereRepository(entityManager).leggTil(original.getId(), Set.of(
            new Trigger(BehandlingÅrsakType.NY_SØKT_PERIODE, DatoIntervallEntitet.fraOgMedTilOgMed(FOM, TOM))));
        return original;
    }

    private Behandling opprettRevurderingMedGrunnlagKopiert(Behandling originalBehandling, VilkårsperiodeData... vilkårsperioder) {
        var builder = TestScenarioBuilder.builderMedSøknad(FagsakYtelseType.AKTIVITETSPENGER)
            .medBehandlingType(BehandlingType.REVURDERING)
            .medOriginalBehandling(originalBehandling, BehandlingÅrsakType.ENDRET_BOSTED);
        leggTilVilkårsperioder(builder, vilkårsperioder);
        var revurdering = builder.lagre(entityManager);

        bostedsGrunnlagRepository.kopierGrunnlagFraEksisterendeBehandling(originalBehandling.getId(), revurdering.getId());
        inngangsvilkårVurderingRepository.kopier(originalBehandling.getId(), revurdering.getId());
        new ProsessTriggereRepository(entityManager).leggTil(revurdering.getId(), Set.of(
            new Trigger(BehandlingÅrsakType.ENDRET_BOSTED, DatoIntervallEntitet.fraOgMedTilOgMed(FOM, TOM))));
        return revurdering;
    }

    private static void leggTilVilkårsperioder(TestScenarioBuilder builder, VilkårsperiodeData... vilkårsperioder) {
        for (var v : vilkårsperioder) {
            builder.leggTilVilkår(VilkårType.BOSTEDSVILKÅR, v.utfall(), v.periode());
        }
    }

    private void lagreVilkårsvurderinger(Behandling behandling, BostedVurderingData... vurderinger) {
        var perioder = Arrays.stream(vurderinger)
            .map(v -> new BostedsvilkårResultatPeriode(
                tilDatoIntervallEntitet(v.periode()),
                v.godkjent(),
                v.ikkeOppfyltÅrsak(),
                true,
                v.begrunnelse(),
                null,
                SAKSBEHANDLER,
                LocalDateTime.now()))
            .toList();
        inngangsvilkårVurderingRepository.lagreBostedVurderinger(behandling.getId(), perioder);
    }

    private static VilkårsperiodeData vilkårsperiode(Periode periode, Utfall utfall) {
        return new VilkårsperiodeData(periode, utfall);
    }

    private static VilkårsperiodeData ikkeVurdertVilkårsperiode(Periode periode) {
        return new VilkårsperiodeData(periode, Utfall.IKKE_VURDERT);
    }

    private record VilkårsperiodeData(Periode periode, Utfall utfall) {
    }

    private static BostedVurderingData oppfyltVurdering(Periode periode, String begrunnelse) {
        return new BostedVurderingData(periode, true, null, begrunnelse);
    }

    private static BostedVurderingData ikkeOppfyltVurdering(Periode periode, BostedsvilkårIkkeOppfyltÅrsak årsak, String begrunnelse) {
        return new BostedVurderingData(periode, false, årsak, begrunnelse);
    }

    /**
     * Representerer en tidligere lagret bostedsvurdering (jf. {@link BostedsvilkårResultatPeriode}) som skal
     * kunne gjenopprettes fra forrige behandling når en ny avklaring ikke lenger dekker hele den tidligere avklarte perioden.
     */
    private record BostedVurderingData(Periode periode, boolean godkjent, BostedsvilkårIkkeOppfyltÅrsak ikkeOppfyltÅrsak, String begrunnelse) {
    }

    private static DatoIntervallEntitet tilDatoIntervallEntitet(Periode periode) {
        return DatoIntervallEntitet.fraOgMedTilOgMed(periode.getFom(), periode.getTom());
    }

    private Behandling opprettBehandlingMedVilkårOgPeriode() {
        var behandling = TestScenarioBuilder.builderMedSøknad(FagsakYtelseType.AKTIVITETSPENGER)
            .leggTilVilkår(VilkårType.BOSTEDSVILKÅR, Utfall.IKKE_VURDERT, new Periode(FOM, TOM))
            .lagre(entityManager);

        var søktStartdato = new SøktStartdato(FOM, new JournalpostId("jp-søknad-1"));
        var startdatoRepository = new StartdatoRepository(entityManager);
        startdatoRepository.lagre(behandling.getId(), List.of(søktStartdato));
        startdatoRepository.lagreRelevanteSøknader(behandling.getId(), new Startdatoer(List.of(søktStartdato)));

        new ProsessTriggereRepository(entityManager).leggTil(behandling.getId(), Set.of(
            new Trigger(BehandlingÅrsakType.NY_SØKT_PERIODE, DatoIntervallEntitet.fraOgMedTilOgMed(FOM, TOM))));
        return behandling;
    }

    private List<BostedsPeriodeAvklaring> hentSorterteAvklaringer() {
        return bostedsGrunnlagRepository.hentGrunnlagHvisEksisterer(behandling.getId())
            .orElseThrow()
            .getForeslåtteAvklaringer()
            .stream()
            .sorted(Comparator.comparing(a -> a.getPeriode().getFomDato()))
            .toList();
    }

    private static VurderFaktaOmBostedDto dtoUtenVarsel(Periode periode, BostedsvilkårIkkeOppfyltÅrsak årsak) {
        var vurdering = new BostedVurderingIkkeOppfyltDto(
            årsak,
            "begrunnelse",
            (årsak == BostedsvilkårIkkeOppfyltÅrsak.ANNET) ? "Fritekstbegrunnelse til bruker" : null,
            BEGRUNNELSE_IKKE_VARSEL
        );
        var avklaring = new BostedFaktaavklaringPeriodeDto(new ÅpenPeriode(periode.getFom(), periode.getTom()), vurdering, true);
        return new VurderFaktaOmBostedDto(List.of(avklaring), "begrunnelse");
    }

    private static VurderFaktaOmBostedDto dtoMedEnAvklaring(BostedsvilkårIkkeOppfyltÅrsak årsak, boolean skalSendeVarsel) {
        var vurdering = new BostedVurderingIkkeOppfyltDto(
            årsak,
            "begrunnelse",
            (årsak == BostedsvilkårIkkeOppfyltÅrsak.ANNET) ? "Fritekstbegrunnelse til bruker" : null,
            skalSendeVarsel ? null : BEGRUNNELSE_IKKE_VARSEL
        );
        var avklaring = new BostedFaktaavklaringPeriodeDto(new ÅpenPeriode(FOM, TOM), vurdering, !skalSendeVarsel);
        return new VurderFaktaOmBostedDto(List.of(avklaring), "begrunnelse");
    }
}
