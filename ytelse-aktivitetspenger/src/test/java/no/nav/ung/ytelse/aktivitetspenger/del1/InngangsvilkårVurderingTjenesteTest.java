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
import no.nav.ung.sak.behandlingslager.inngangsvilkår.BostedsvilkårResultatPeriode;
import no.nav.ung.sak.behandlingslager.inngangsvilkår.InngangsvilkårVurderingRepository;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.domene.typer.tid.TidslinjeUtil;
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
    private static final LocalDateTimeline<Boolean> UBEGRENSET_AVGRENSNING = TidslinjeUtil.tilTidslinje(List.of(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2020, 1, 1), LocalDate.of(2030, 12, 31))));

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
    void skal_sette_automatisk_utfall_når_vurderingen_ikke_er_manuell() {
        var behandling = opprettFørstegangsbehandling(vilkår(PERIODE_1, Utfall.IKKE_VURDERT));

        inngangsvilkårVurderingRepository.lagreBostedVurderinger(behandling.getId(), List.of(
            new BostedsvilkårResultatPeriode(tilIntervall(PERIODE_1), false, BostedsvilkårIkkeOppfyltÅrsak.STUDIE_ELLER_ARBEIDSSTED_UTENFOR_TRONDHEIM,
                false, "Automatisk avslag", null, "A111111", LocalDateTime.now())));

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

        leggTilBostedVurderinger(originalBehandling.getId(),
            bostedVurdering(PERIODE_1, false, "fra original periode 1"),
            bostedVurdering(PERIODE_2, false, "fra original periode 2"),
            bostedVurdering(PERIODE_3, true, "fra original periode 3"));

        var PERIODE_1_AVKORTET = new Periode(PERIODE_1.getFom(), PERIODE_1.getTom().minusDays(15));
        var PERIODE_2_3_FLYTTET = new Periode(PERIODE_2.getFom().minusDays(15), PERIODE_3.getTom().minusDays(15));

        var revurdering = opprettRevurdering(originalBehandling,
            vilkår(PERIODE_1_AVKORTET, Utfall.OPPFYLT, null, "vurdert i denne behandlingen"),
            vilkår(PERIODE_2_3_FLYTTET, Utfall.IKKE_VURDERT)
        );

        var vilkårResultatBuilder = Vilkårene.builderFraEksisterende(vilkårResultatRepository.hent(revurdering.getId()));
        tjeneste.gjenopprettForrigeVurderingForPerioderIkkeVurdert(revurdering.getId(), vilkårResultatBuilder, VilkårType.BOSTEDSVILKÅR, UBEGRENSET_AVGRENSNING);
        tjeneste.oppdaterBostedsvilkårResultatFraVurdering(revurdering.getId());
        var resultat = vilkårResultatRepository.hent(revurdering.getId()).getVilkårTimeline(VilkårType.BOSTEDSVILKÅR);

        var vurdertPeriode = hentVilkårPeriode(resultat, PERIODE_1_AVKORTET);
        assertThat(vurdertPeriode.getGjeldendeUtfall()).as("vurdering gjort i denne behandlingen skal ikke overskrives").isEqualTo(Utfall.OPPFYLT);
        assertThat(vurdertPeriode.getFritekstVurderingBrev()).isEqualTo("vurdert i denne behandlingen");

        var periode1 = hentVilkårPeriode(resultat, new Periode(PERIODE_1_AVKORTET.getTom().plusDays(1), PERIODE_1.getTom()));
        assertThat(periode1.getGjeldendeUtfall()).isEqualTo(Utfall.IKKE_OPPFYLT);
        assertThat(periode1.getAvslagsårsak()).isEqualTo(Avslagsårsak.YTELSE_IKKE_TILGJENGELIG_PÅ_FOLKEREGISTRERT_ELLER_BOSTEDSADRESSE);
        assertThat(periode1.getFritekstVurderingBrev()).isEqualTo("fra original periode 1");

        var periode2 = hentVilkårPeriode(resultat, PERIODE_2);
        assertThat(periode2.getGjeldendeUtfall()).isEqualTo(Utfall.IKKE_OPPFYLT);
        assertThat(periode2.getAvslagsårsak()).isEqualTo(Avslagsårsak.YTELSE_IKKE_TILGJENGELIG_PÅ_FOLKEREGISTRERT_ELLER_BOSTEDSADRESSE);
        assertThat(periode2.getFritekstVurderingBrev()).isEqualTo("fra original periode 2");

        var periode3 = hentVilkårPeriode(resultat, new Periode(PERIODE_3.getFom(), PERIODE_2_3_FLYTTET.getTom()));
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

        leggTilBostedVurderinger(originalBehandling.getId(),
            bostedVurdering(PERIODE_1, true, "fra original periode 1"));

        var revurdering = opprettRevurdering(originalBehandling,
            vilkår(PERIODE_1, Utfall.IKKE_VURDERT),
            vilkår(PERIODE_2, Utfall.IKKE_VURDERT));

        var vilkårResultatBuilder = Vilkårene.builderFraEksisterende(vilkårResultatRepository.hent(revurdering.getId()));
        tjeneste.gjenopprettForrigeVurderingForPerioderIkkeVurdert(revurdering.getId(), vilkårResultatBuilder, VilkårType.BOSTEDSVILKÅR, UBEGRENSET_AVGRENSNING);
        tjeneste.oppdaterBostedsvilkårResultatFraVurdering(revurdering.getId());
        var resultat = vilkårResultatRepository.hent(revurdering.getId()).getVilkårTimeline(VilkårType.BOSTEDSVILKÅR);

        assertThat(hentVilkårPeriode(resultat, PERIODE_1).getGjeldendeUtfall()).isEqualTo(Utfall.OPPFYLT);
        assertThat(hentVilkårPeriode(resultat, PERIODE_2).getGjeldendeUtfall())
            .as("periode uten vurdering i forrige behandling skal fortsatt være til vurdering")
            .isEqualTo(Utfall.IKKE_VURDERT);
    }

    @Test
    void skal_ikke_gjenopprette_noe_når_behandlingen_ikke_har_original_behandling() {
        var behandling = opprettFørstegangsbehandling(vilkår(PERIODE_1, Utfall.IKKE_VURDERT));

        var vilkårResultatBuilder = Vilkårene.builderFraEksisterende(vilkårResultatRepository.hent(behandling.getId()));
        tjeneste.gjenopprettForrigeVurderingForPerioderIkkeVurdert(behandling.getId(), vilkårResultatBuilder, VilkårType.BOSTEDSVILKÅR, UBEGRENSET_AVGRENSNING);
        var resultat = vilkårResultatBuilder.build().getVilkårTimeline(VilkårType.BOSTEDSVILKÅR);

        assertThat(hentVilkårPeriode(resultat, PERIODE_1).getGjeldendeUtfall()).isEqualTo(Utfall.IKKE_VURDERT);
    }

    @Test
    void skal_fjerne_vurdering_og_sette_vilkår_til_ikke_vurdert_for_angitte_perioder() {
        var behandling = opprettFørstegangsbehandling(
            vilkår(PERIODE_1, Utfall.OPPFYLT),
            vilkår(PERIODE_2, Utfall.IKKE_OPPFYLT));

        var vilkårResultatBuilder = Vilkårene.builderFraEksisterende(vilkårResultatRepository.hent(behandling.getId()));
        tjeneste.settVilkårResultatIkkeVurdertForPeriode(vilkårResultatBuilder, VilkårType.BOSTEDSVILKÅR, List.of(tilIntervall(PERIODE_2)));
        var resultat = vilkårResultatBuilder.build().getVilkårTimeline(VilkårType.BOSTEDSVILKÅR);

        assertThat(hentVilkårPeriode(resultat, PERIODE_1).getGjeldendeUtfall()).isEqualTo(Utfall.OPPFYLT);
        assertThat(hentVilkårPeriode(resultat, PERIODE_2).getGjeldendeUtfall()).isEqualTo(Utfall.IKKE_VURDERT);
    }

    private LocalDateTimeline<VilkårPeriode> hentBostedsvilkårTidslinje(Long behandlingId) {
        return vilkårResultatRepository.hent(behandlingId).getVilkårTimeline(VilkårType.BOSTEDSVILKÅR);
    }

    private static VilkårPeriode hentVilkårPeriode(LocalDateTimeline<VilkårPeriode> tidslinje, Periode periode) {
        return tidslinje.segmenter().stream()
            .filter(s -> s.getFom().equals(periode.getFom()) && s.getTom().equals(periode.getTom()))
            .map(s -> s.getValue())
            .findFirst()
            .orElseThrow(() -> new AssertionError("Fant ikke vilkårsperiode " + periode + " i " + tidslinje));
    }

    private static DatoIntervallEntitet tilIntervall(Periode periode) {
        return DatoIntervallEntitet.fraOgMedTilOgMed(periode.getFom(), periode.getTom());
    }

    private void leggTilBostedVurderinger(Long behandlingId, BostedVurderingData... vurderinger) {
        var perioder = List.of(vurderinger).stream()
            .map(v -> new BostedsvilkårResultatPeriode(tilIntervall(v.periode()), v.godkjent(), v.ikkeOppfyltÅrsak(), true, "begrunnelse", v.fritekstVurderingBrev(), "A111111", LocalDateTime.now()))
            .toList();
        inngangsvilkårVurderingRepository.lagreBostedVurderinger(behandlingId, perioder);
    }

    private static BostedVurderingData bostedVurdering(Periode periode, boolean godkjent, String fritekstVurderingBrev) {
        var ikkeOppfyltÅrsak = godkjent ? null : BostedsvilkårIkkeOppfyltÅrsak.STUDIE_ELLER_ARBEIDSSTED_UTENFOR_TRONDHEIM;
        return new BostedVurderingData(periode, godkjent, ikkeOppfyltÅrsak, fritekstVurderingBrev);
    }

    private record BostedVurderingData(Periode periode, boolean godkjent, BostedsvilkårIkkeOppfyltÅrsak ikkeOppfyltÅrsak, String fritekstVurderingBrev) {
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
