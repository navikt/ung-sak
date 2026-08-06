package no.nav.ung.ytelse.aktivitetspenger.del1;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.vilkår.Avslagsårsak;
import no.nav.ung.kodeverk.vilkår.BostedsvilkårIkkeOppfyltÅrsak;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.ung.sak.behandlingslager.inngangsvilkår.AktivitetspengerInngangsvilkårResultatGrunnlag;
import no.nav.ung.sak.behandlingslager.inngangsvilkår.BostedsvilkårResultatHolder;
import no.nav.ung.sak.behandlingslager.inngangsvilkår.BostedsvilkårResultatPeriode;
import no.nav.ung.sak.behandlingslager.inngangsvilkår.InngangsvilkårVurderingRepository;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.typer.Periode;
import no.nav.ung.ytelse.aktivitetspenger.testdata.AktivitetspengerTestScenarioBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(JpaExtension.class)
@ExtendWith(CdiAwareExtension.class)
class InngangsvilkårVurderingTjenesteTest {

    private static final Periode PERIODE_1 = new Periode(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));
    private static final Periode PERIODE_2 = new Periode(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28));
    private static final Periode PERIODE_3 = new Periode(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31));

    private static final String VURDERT_AV = "Z999999";
    private static final LocalDateTime VURDERT_TIDSPUNKT = LocalDateTime.of(2026, 2, 1, 10, 0);

    @Inject
    private EntityManager entityManager;

    private BehandlingRepository behandlingRepository;
    private VilkårResultatRepository vilkårResultatRepository;
    private InngangsvilkårVurderingRepository inngangsvilkårVurderingRepository;
    private InngangsvilkårVurderingTjeneste tjeneste;

    @BeforeEach
    void setUp() {
        var repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        behandlingRepository = repositoryProvider.getBehandlingRepository();
        vilkårResultatRepository = repositoryProvider.getVilkårResultatRepository();
        inngangsvilkårVurderingRepository = new InngangsvilkårVurderingRepository(entityManager);
        tjeneste = new InngangsvilkårVurderingTjeneste(inngangsvilkårVurderingRepository, behandlingRepository, vilkårResultatRepository);
    }

    @Test
    void skal_lagre_bostedvurdering_og_sette_tilsvarende_vilkårsresultat() {
        var behandling = opprettFørstegangsbehandling(
            vilkår(PERIODE_1, Utfall.IKKE_VURDERT),
            vilkår(PERIODE_2, Utfall.IKKE_VURDERT));

        inngangsvilkårVurderingRepository.lagreBostedVurderinger(behandling.getId(), List.of(
            oppfyltBostedvurdering(PERIODE_1),
            ikkeOppfyltBostedvurdering(PERIODE_2, BostedsvilkårIkkeOppfyltÅrsak.IKKE_BOSATTADRESSE_I_TRONDHEIM)));

        tjeneste.oppdaterBostedsvilkårResultatFraVurdering(behandling.getId());

        var lagredeVurderinger = inngangsvilkårVurderingRepository.hentGrunnlag(behandling.getId())
            .flatMap(AktivitetspengerInngangsvilkårResultatGrunnlag::getBostedsvilkårResultatHolder)
            .map(BostedsvilkårResultatHolder::getVurderinger)
            .orElseThrow();
        assertThat(lagredeVurderinger).hasSize(2);
        var lagretPeriode2 = lagredeVurderinger.stream().filter(v -> v.getPeriode().equals(tilIntervall(PERIODE_2))).findFirst().orElseThrow();
        assertThat(lagretPeriode2.isGodkjent()).isFalse();
        assertThat(lagretPeriode2.getIkkeOppfyltÅrsak()).isEqualTo(BostedsvilkårIkkeOppfyltÅrsak.IKKE_BOSATTADRESSE_I_TRONDHEIM);
        assertThat(lagretPeriode2.isManuellVurdering()).isTrue();
        assertThat(lagretPeriode2.getVurdertAv()).isEqualTo(VURDERT_AV);
        assertThat(lagretPeriode2.getVurdertTidspunkt()).isEqualTo(VURDERT_TIDSPUNKT);

        var vilkårsperioder = hentBostedsvilkårTidslinje(behandling.getId());
        var vilkårPeriode1 = hentVilkårPeriode(vilkårsperioder, PERIODE_1);
        assertThat(vilkårPeriode1.getGjeldendeUtfall()).isEqualTo(Utfall.OPPFYLT);
        assertThat(vilkårPeriode1.getErManueltVurdert()).isTrue();
        assertThat(vilkårPeriode1.getBegrunnelse()).isEqualTo("Begrunnelse " + PERIODE_1.getFom());

        var vilkårPeriode2 = hentVilkårPeriode(vilkårsperioder, PERIODE_2);
        assertThat(vilkårPeriode2.getGjeldendeUtfall()).isEqualTo(Utfall.IKKE_OPPFYLT);
        assertThat(vilkårPeriode2.getErManueltVurdert()).isTrue();
        assertThat(vilkårPeriode2.getAvslagsårsak()).isEqualTo(Avslagsårsak.YTELSE_IKKE_TILGJENGELIG_PÅ_FOLKEREGISTRERT_ELLER_BOSTEDSADRESSE);
    }

    @Test
    void skal_sette_automatisk_utfall_når_vurderingen_ikke_er_manuell() {
        var behandling = opprettFørstegangsbehandling(vilkår(PERIODE_1, Utfall.IKKE_VURDERT));

        inngangsvilkårVurderingRepository.lagreBostedVurderinger(behandling.getId(), List.of(
            new BostedsvilkårResultatPeriode(tilIntervall(PERIODE_1), false, BostedsvilkårIkkeOppfyltÅrsak.STUDIE_ELLER_ARBEIDSSTED_UTENFOR_TRONDHEIM,
                false, "Automatisk avslag", null, null, null)));

        tjeneste.oppdaterBostedsvilkårResultatFraVurdering(behandling.getId());

        var vilkårPeriode = hentVilkårPeriode(hentBostedsvilkårTidslinje(behandling.getId()), PERIODE_1);
        assertThat(vilkårPeriode.getGjeldendeUtfall()).isEqualTo(Utfall.IKKE_OPPFYLT);
        assertThat(vilkårPeriode.getErManueltVurdert()).isFalse();
        assertThat(vilkårPeriode.getAvslagsårsak()).isEqualTo(Avslagsårsak.YTELSE_IKKE_TILGJENGELIG_PÅ_FOLKEREGISTRERT_ELLER_BOSTEDSADRESSE);
    }

    @Test
    void skal_fylle_inn_ikke_vurderte_perioder_fra_forrige_behandling_uten_å_overskrive_vurdering_i_denne_behandlingen() {
        var originalBehandling = opprettFørstegangsbehandling(
            vilkår(PERIODE_1, Utfall.IKKE_OPPFYLT, Avslagsårsak.YTELSE_IKKE_TILGJENGELIG_PÅ_FOLKEREGISTRERT_ELLER_BOSTEDSADRESSE, "fra original periode 1"),
            vilkår(PERIODE_2, Utfall.IKKE_OPPFYLT, Avslagsårsak.YTELSE_IKKE_TILGJENGELIG_PÅ_FOLKEREGISTRERT_ELLER_BOSTEDSADRESSE, "fra original periode 2"),
            vilkår(PERIODE_3, Utfall.OPPFYLT, null, "fra original periode 3"));

        // Periode 1 er vurdert på nytt i revurderingen, periode 2 og 3 er ikke vurdert
        var revurdering = opprettRevurdering(originalBehandling,
            vilkår(PERIODE_1, Utfall.OPPFYLT, null, "vurdert i denne behandlingen"),
            vilkår(PERIODE_2, Utfall.IKKE_VURDERT),
            vilkår(PERIODE_3, Utfall.IKKE_VURDERT));

        var vilkårResultatBuilder = Vilkårene.builderFraEksisterende(vilkårResultatRepository.hent(revurdering.getId()));
        tjeneste.gjenopprettForrigeVurderingForPerioderIkkeVurdert(revurdering.getId(), vilkårResultatBuilder, VilkårType.BOSTEDSVILKÅR);
        var resultat = vilkårResultatBuilder.build().getVilkårTimeline(VilkårType.BOSTEDSVILKÅR);

        var periode1 = hentVilkårPeriode(resultat, PERIODE_1);
        assertThat(periode1.getGjeldendeUtfall()).as("vurdering gjort i denne behandlingen skal ikke overskrives").isEqualTo(Utfall.OPPFYLT);
        assertThat(periode1.getFritekstVurderingBrev()).isEqualTo("vurdert i denne behandlingen");

        var periode2 = hentVilkårPeriode(resultat, PERIODE_2);
        assertThat(periode2.getGjeldendeUtfall()).isEqualTo(Utfall.IKKE_OPPFYLT);
        assertThat(periode2.getAvslagsårsak()).isEqualTo(Avslagsårsak.YTELSE_IKKE_TILGJENGELIG_PÅ_FOLKEREGISTRERT_ELLER_BOSTEDSADRESSE);
        assertThat(periode2.getFritekstVurderingBrev()).isEqualTo("fra original periode 2");

        var periode3 = hentVilkårPeriode(resultat, PERIODE_3);
        assertThat(periode3.getGjeldendeUtfall()).isEqualTo(Utfall.OPPFYLT);
        assertThat(periode3.getFritekstVurderingBrev()).isEqualTo("fra original periode 3");

        assertThat(resultat.filterValue(v -> Utfall.IKKE_VURDERT.equals(v.getGjeldendeUtfall())).isEmpty())
            .as("ingen perioder skal stå igjen som ikke vurdert")
            .isTrue();
    }

    @Test
    void skal_ikke_gjenopprette_perioder_som_ikke_var_vurdert_i_forrige_behandling() {
        var originalBehandling = opprettFørstegangsbehandling(
            vilkår(PERIODE_1, Utfall.OPPFYLT, null, "fra original periode 1"));

        var revurdering = opprettRevurdering(originalBehandling,
            vilkår(PERIODE_1, Utfall.IKKE_VURDERT),
            vilkår(PERIODE_2, Utfall.IKKE_VURDERT));

        var vilkårResultatBuilder = Vilkårene.builderFraEksisterende(vilkårResultatRepository.hent(revurdering.getId()));
        tjeneste.gjenopprettForrigeVurderingForPerioderIkkeVurdert(revurdering.getId(), vilkårResultatBuilder, VilkårType.BOSTEDSVILKÅR);
        var resultat = vilkårResultatBuilder.build().getVilkårTimeline(VilkårType.BOSTEDSVILKÅR);

        assertThat(hentVilkårPeriode(resultat, PERIODE_1).getGjeldendeUtfall()).isEqualTo(Utfall.OPPFYLT);
        assertThat(hentVilkårPeriode(resultat, PERIODE_2).getGjeldendeUtfall())
            .as("periode uten vurdering i forrige behandling skal fortsatt være til vurdering")
            .isEqualTo(Utfall.IKKE_VURDERT);
    }

    @Test
    void skal_ikke_gjenopprette_noe_når_behandlingen_ikke_har_original_behandling() {
        var behandling = opprettFørstegangsbehandling(vilkår(PERIODE_1, Utfall.IKKE_VURDERT));

        var vilkårResultatBuilder = Vilkårene.builderFraEksisterende(vilkårResultatRepository.hent(behandling.getId()));
        tjeneste.gjenopprettForrigeVurderingForPerioderIkkeVurdert(behandling.getId(), vilkårResultatBuilder, VilkårType.BOSTEDSVILKÅR);
        var resultat = vilkårResultatBuilder.build().getVilkårTimeline(VilkårType.BOSTEDSVILKÅR);

        assertThat(hentVilkårPeriode(resultat, PERIODE_1).getGjeldendeUtfall()).isEqualTo(Utfall.IKKE_VURDERT);
    }

    @Test
    void skal_fjerne_vurdering_og_sette_vilkår_til_ikke_vurdert_for_angitte_perioder() {
        var behandling = opprettFørstegangsbehandling(
            vilkår(PERIODE_1, Utfall.IKKE_VURDERT),
            vilkår(PERIODE_2, Utfall.IKKE_VURDERT));

        inngangsvilkårVurderingRepository.lagreBostedVurderinger(behandling.getId(), List.of(
            oppfyltBostedvurdering(PERIODE_1),
            oppfyltBostedvurdering(PERIODE_2)));
        tjeneste.oppdaterBostedsvilkårResultatFraVurdering(behandling.getId());

        var vilkårResultatBuilder = Vilkårene.builderFraEksisterende(vilkårResultatRepository.hent(behandling.getId()));
        tjeneste.fjernVilkårVurderingOgSettVilkårResultatIkkeVurdertForPeriode(behandling.getId(), vilkårResultatBuilder,
            VilkårType.BOSTEDSVILKÅR, List.of(tilIntervall(PERIODE_2)));
        var resultat = vilkårResultatBuilder.build().getVilkårTimeline(VilkårType.BOSTEDSVILKÅR);

        assertThat(hentVilkårPeriode(resultat, PERIODE_1).getGjeldendeUtfall()).isEqualTo(Utfall.OPPFYLT);
        assertThat(hentVilkårPeriode(resultat, PERIODE_2).getGjeldendeUtfall()).isEqualTo(Utfall.IKKE_VURDERT);

        var gjenværendeVurderinger = inngangsvilkårVurderingRepository.hentGrunnlag(behandling.getId())
            .flatMap(AktivitetspengerInngangsvilkårResultatGrunnlag::getBostedsvilkårResultatHolder)
            .map(BostedsvilkårResultatHolder::getVurderinger)
            .orElseThrow();
        assertThat(gjenværendeVurderinger).hasSize(1);
        assertThat(gjenværendeVurderinger.getFirst().getPeriode()).isEqualTo(tilIntervall(PERIODE_1));
    }

    private LocalDateTimeline<VilkårPeriode> hentBostedsvilkårTidslinje(Long behandlingId) {
        return vilkårResultatRepository.hent(behandlingId).getVilkårTimeline(VilkårType.BOSTEDSVILKÅR);
    }

    private static VilkårPeriode hentVilkårPeriode(LocalDateTimeline<VilkårPeriode> tidslinje, Periode periode) {
        return tidslinje.toSegments().stream()
            .filter(s -> s.getFom().equals(periode.getFom()) && s.getTom().equals(periode.getTom()))
            .map(s -> s.getValue())
            .findFirst()
            .orElseThrow(() -> new AssertionError("Fant ikke vilkårsperiode " + periode + " i " + tidslinje));
    }

    private static DatoIntervallEntitet tilIntervall(Periode periode) {
        return DatoIntervallEntitet.fraOgMedTilOgMed(periode.getFom(), periode.getTom());
    }

    private static BostedsvilkårResultatPeriode oppfyltBostedvurdering(Periode periode) {
        return new BostedsvilkårResultatPeriode(tilIntervall(periode), true, null, true,
            "Begrunnelse " + periode.getFom(), null, VURDERT_AV, VURDERT_TIDSPUNKT);
    }

    private static BostedsvilkårResultatPeriode ikkeOppfyltBostedvurdering(Periode periode, BostedsvilkårIkkeOppfyltÅrsak årsak) {
        return new BostedsvilkårResultatPeriode(tilIntervall(periode), false, årsak, true,
            "Begrunnelse " + periode.getFom(), null, VURDERT_AV, VURDERT_TIDSPUNKT);
    }

    private Behandling opprettFørstegangsbehandling(VilkårsperiodeData... vilkårsperioder) {
        var scenario = AktivitetspengerTestScenarioBuilder.builderMedSøknad();
        leggTilVilkår(scenario, vilkårsperioder);
        return scenario.lagre(entityManager);
    }

    private Behandling opprettRevurdering(Behandling originalBehandling, VilkårsperiodeData... vilkårsperioder) {
        var scenario = AktivitetspengerTestScenarioBuilder.builderMedSøknad()
            .medBehandlingType(BehandlingType.REVURDERING)
            .medOriginalBehandling(originalBehandling, BehandlingÅrsakType.ENDRET_BOSTED);
        leggTilVilkår(scenario, vilkårsperioder);
        return scenario.lagre(entityManager);
    }

    private static void leggTilVilkår(AktivitetspengerTestScenarioBuilder scenario, VilkårsperiodeData... vilkårsperioder) {
        for (var v : vilkårsperioder) {
            scenario.leggTilVilkår(VilkårType.BOSTEDSVILKÅR, v.utfall(), v.periode(), v.avslagsårsak(), v.fritekstBrev());
            scenario.leggTilVilkår(VilkårType.ALDERSVILKÅR, Utfall.OPPFYLT, v.periode());
        }
    }

    private static VilkårsperiodeData vilkår(Periode periode, Utfall utfall) {
        return new VilkårsperiodeData(periode, utfall, null, null);
    }

    private static VilkårsperiodeData vilkår(Periode periode, Utfall utfall, Avslagsårsak avslagsårsak, String fritekstBrev) {
        return new VilkårsperiodeData(periode, utfall, avslagsårsak, fritekstBrev);
    }

    private record VilkårsperiodeData(Periode periode, Utfall utfall, Avslagsårsak avslagsårsak, String fritekstBrev) {
    }
}
