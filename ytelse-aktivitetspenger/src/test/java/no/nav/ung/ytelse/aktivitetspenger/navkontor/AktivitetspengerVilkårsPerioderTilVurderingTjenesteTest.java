package no.nav.ung.ytelse.aktivitetspenger.navkontor;

import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.vilkår.Avklaringtype;
import no.nav.ung.kodeverk.vilkår.BostedsavklaringKildeType;
import no.nav.ung.kodeverk.vilkår.BostedsvilkårIkkeOppfyltÅrsak;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.startdato.StartdatoRepository;
import no.nav.ung.sak.behandlingslager.behandling.startdato.Startdatoer;
import no.nav.ung.sak.behandlingslager.behandling.startdato.SøktStartdato;
import no.nav.ung.sak.behandlingslager.vilkårsavklaring.VilkårPeriodeAvklaringForeslått;
import no.nav.ung.sak.behandlingslager.vilkårsavklaring.VilkårsavklaringGrunnlagRepository;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.ung.sak.trigger.ProsessTriggereRepository;
import no.nav.ung.sak.trigger.Trigger;
import no.nav.ung.sak.typer.JournalpostId;
import no.nav.ung.sak.typer.Periode;
import no.nav.ung.ytelse.aktivitetspenger.perioder.AktivitetspengerSøknadsperiodeTjeneste;
import no.nav.ung.ytelse.aktivitetspenger.testdata.AktivitetspengerTestScenarioBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(JpaExtension.class)
@ExtendWith(CdiAwareExtension.class)
class AktivitetspengerVilkårsPerioderTilVurderingTjenesteTest {

    private static final DatoIntervallEntitet PERIODE_1 = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));
    private static final DatoIntervallEntitet PERIODE_2 = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31));

    @Inject
    private EntityManager entityManager;

    @Inject
    private @Any Instance<VilkårsPerioderTilVurderingTjeneste> vilkårsPerioderTilVurderingTjenester;

    private VilkårsPerioderTilVurderingTjeneste tjeneste;
    private ProsessTriggereRepository prosessTriggereRepository;
    private VilkårsavklaringGrunnlagRepository vilkårsavklaringGrunnlagRepository;
    private StartdatoRepository startdatoRepository;

    @BeforeEach
    void setUp() {
        tjeneste = VilkårsPerioderTilVurderingTjeneste.finnTjeneste(vilkårsPerioderTilVurderingTjenester, FagsakYtelseType.AKTIVITETSPENGER, BehandlingType.FØRSTEGANGSSØKNAD);
        prosessTriggereRepository = new ProsessTriggereRepository(entityManager);
        vilkårsavklaringGrunnlagRepository = new VilkårsavklaringGrunnlagRepository(entityManager);
        startdatoRepository = new StartdatoRepository(entityManager);
    }

    @Test
    void skal_vurdere_ny_søknadsperiode() {
        var behandling = opprettBehandlingMedVilkårsperioder(VilkårType.BOSTEDSVILKÅR, PERIODE_1, PERIODE_2);
        leggTilTriggere(behandling, new Trigger(BehandlingÅrsakType.NY_SØKT_PERIODE, PERIODE_1));

        assertThat(tjeneste.utled(behandling.getId(), VilkårType.BOSTEDSVILKÅR)).containsExactly(PERIODE_1);
    }

    @Test
    void skal_kun_vurdere_avklart_del_av_triggerperioden_for_avklaringsårsak() {
        var behandling = opprettBehandlingMedVilkårsperioder(VilkårType.BOSTEDSVILKÅR, PERIODE_1, PERIODE_2);
        leggTilTriggere(behandling,
            new Trigger(BehandlingÅrsakType.ENDRET_BOSTED, PERIODE_1),
            new Trigger(BehandlingÅrsakType.ENDRET_BOSTED, PERIODE_2));
        var avklartPeriode = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2026, 1, 15), LocalDate.of(2026, 1, 31));
        lagreAvklaring(behandling, avklartPeriode);

        assertThat(tjeneste.utled(behandling.getId(), VilkårType.BOSTEDSVILKÅR)).containsExactly(avklartPeriode);
    }

    @Test
    void skal_ikke_vurdere_noe_for_avklaringsårsak_uten_avklaring() {
        var behandling = opprettBehandlingMedVilkårsperioder(VilkårType.BOSTEDSVILKÅR, PERIODE_1, PERIODE_2);
        leggTilTriggere(behandling, new Trigger(BehandlingÅrsakType.ENDRET_BOSTED, PERIODE_1));

        assertThat(tjeneste.utled(behandling.getId(), VilkårType.BOSTEDSVILKÅR)).isEmpty();
    }

    @Test
    void skal_vurdere_ny_søknadsperiode_med_avklaringsårsak_når_avklaring_finnes_et_annet_sted() {
        var behandling = opprettBehandlingMedVilkårsperioder(VilkårType.BOSTEDSVILKÅR, PERIODE_1, PERIODE_2);
        leggTilTriggere(behandling,
            new Trigger(BehandlingÅrsakType.NY_SØKT_PERIODE, PERIODE_1),
            new Trigger(BehandlingÅrsakType.ENDRET_BOSTED, PERIODE_1));
        lagreAvklaring(behandling, PERIODE_2);

        assertThat(tjeneste.utled(behandling.getId(), VilkårType.BOSTEDSVILKÅR)).containsExactly(PERIODE_1, PERIODE_2);
    }

    @Test
    void skal_utlede_perioder_per_trigger() {
        var behandling = opprettBehandlingMedVilkårsperioder(VilkårType.BOSTEDSVILKÅR, PERIODE_1, PERIODE_2);
        leggTilTriggere(behandling,
            new Trigger(BehandlingÅrsakType.NY_SØKT_PERIODE, PERIODE_1),
            new Trigger(BehandlingÅrsakType.ENDRET_BOSTED, PERIODE_2));
        var avklartPeriode = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2026, 1, 15), LocalDate.of(2026, 1, 31));
        lagreAvklaring(behandling, avklartPeriode);

        var perioderPerTrigger = ((AktivitetspengerVilkårsPerioderTilVurderingTjeneste) tjeneste).utledPerTrigger(behandling.getId(), VilkårType.BOSTEDSVILKÅR);

        assertThat(perioderFor(perioderPerTrigger, BehandlingÅrsakType.NY_SØKT_PERIODE))
            .containsExactly(PERIODE_1.toLocalDateInterval());
        assertThat(perioderFor(perioderPerTrigger, BehandlingÅrsakType.ENDRET_BOSTED))
            .containsExactly(avklartPeriode.toLocalDateInterval());
        assertThat(tjeneste.utled(behandling.getId(), VilkårType.BOSTEDSVILKÅR)).containsExactly(PERIODE_1);
    }

    @Test
    void skal_utvide_ny_søknadsperiode_med_avklart_periode_uten_trigger() {
        var behandling = opprettBehandlingMedVilkårsperioder(VilkårType.BOSTEDSVILKÅR, PERIODE_1, PERIODE_2);
        leggTilTriggere(behandling, new Trigger(BehandlingÅrsakType.NY_SØKT_PERIODE, PERIODE_1));
        lagreAvklaring(behandling, PERIODE_2);

        assertThat(tjeneste.utled(behandling.getId(), VilkårType.BOSTEDSVILKÅR)).containsExactly(PERIODE_1, PERIODE_2);
    }

    @Test
    void skal_gi_avklaringsårsak_per_trigger_uten_trigger_for_avklaringsårsaken() {
        var behandling = opprettBehandlingMedVilkårsperioder(VilkårType.BOSTEDSVILKÅR, PERIODE_1, PERIODE_2);
        leggTilTriggere(behandling, new Trigger(BehandlingÅrsakType.NY_SØKT_PERIODE, PERIODE_1));
        lagreAvklaring(behandling, PERIODE_2);

        var perioderPerTrigger = aktivitetspengerTjeneste().utledPerTrigger(behandling.getId(), VilkårType.BOSTEDSVILKÅR);

        assertThat(perioderFor(perioderPerTrigger, BehandlingÅrsakType.ENDRET_BOSTED))
            .containsExactly(PERIODE_2.toLocalDateInterval());
    }

    @Test
    void skal_ikke_gi_avklaringsårsak_per_trigger_når_avklaring_mangler() {
        var behandling = opprettBehandlingMedVilkårsperioder(VilkårType.BOSTEDSVILKÅR, PERIODE_1, PERIODE_2);
        leggTilTriggere(behandling, new Trigger(BehandlingÅrsakType.ENDRET_BOSTED, PERIODE_1));

        assertThat(aktivitetspengerTjeneste().utledPerTrigger(behandling.getId(), VilkårType.BOSTEDSVILKÅR))
            .doesNotContainKey(BehandlingÅrsakType.ENDRET_BOSTED);
    }

    @Test
    void skal_klippe_trigger_som_ikke_vurderer_vilkår_til_vilkårsperiodene() {
        var behandling = opprettBehandlingMedVilkårsperioder(VilkårType.BOSTEDSVILKÅR, PERIODE_1, PERIODE_2);
        leggTilTriggere(behandling, new Trigger(BehandlingÅrsakType.RE_RAPPORTERING_INNTEKT,
            DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2026, 1, 15), LocalDate.of(2026, 3, 10))));

        var perioderPerTrigger = aktivitetspengerTjeneste().utledPerTrigger(behandling.getId(), VilkårType.BOSTEDSVILKÅR);

        assertThat(perioderFor(perioderPerTrigger, BehandlingÅrsakType.RE_RAPPORTERING_INNTEKT)).containsExactly(
            new LocalDateInterval(LocalDate.of(2026, 1, 15), LocalDate.of(2026, 1, 31)),
            new LocalDateInterval(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 10)));
    }

    @Test
    void skal_ikke_vurdere_trigger_som_ikke_vurderer_vilkår() {
        var behandling = opprettBehandlingMedVilkårsperioder(VilkårType.BOSTEDSVILKÅR, PERIODE_1, PERIODE_2);
        leggTilTriggere(behandling, new Trigger(BehandlingÅrsakType.RE_RAPPORTERING_INNTEKT, PERIODE_1));

        assertThat(tjeneste.utled(behandling.getId(), VilkårType.BOSTEDSVILKÅR)).isEmpty();
    }

    @Test
    void skal_tåle_overlappende_foreslåtte_avklaringer() {
        var behandling = opprettBehandlingMedVilkårsperioder(VilkårType.BOSTEDSVILKÅR, PERIODE_1, PERIODE_2);
        lagreAvklaringer(behandling,
            DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2026, 1, 5), LocalDate.of(2026, 1, 20)),
            DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2026, 1, 10), LocalDate.of(2026, 1, 25)));

        assertThat(tjeneste.utled(behandling.getId(), VilkårType.BOSTEDSVILKÅR))
            .containsExactly(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2026, 1, 5), LocalDate.of(2026, 1, 25)));
    }

    private AktivitetspengerVilkårsPerioderTilVurderingTjeneste aktivitetspengerTjeneste() {
        return (AktivitetspengerVilkårsPerioderTilVurderingTjeneste) tjeneste;
    }

    private static List<LocalDateInterval> perioderFor(Map<BehandlingÅrsakType, LocalDateTimeline<Boolean>> perioderPerTrigger, BehandlingÅrsakType årsak) {
        return List.copyOf(perioderPerTrigger.get(årsak).getLocalDateIntervals());
    }

    @Test
    void skal_gi_søknadsperiodene_når_vilkåret_ikke_finnes() {
        var behandling = AktivitetspengerTestScenarioBuilder.builderMedSøknad().lagre(entityManager);

        var søktStartdato = new SøktStartdato(PERIODE_1.getFomDato(), new JournalpostId("jp-søknad"));
        startdatoRepository.lagre(behandling.getId(), List.of(søktStartdato));
        startdatoRepository.lagreRelevanteSøknader(behandling.getId(), new Startdatoer(List.of(søktStartdato)));

        var søknadsperiode = AktivitetspengerSøknadsperiodeTjeneste.tidslinjeFraVirkningstidspunkt(PERIODE_1.getFomDato());
        assertThat(tjeneste.utled(behandling.getId(), VilkårType.BOSTEDSVILKÅR))
            .containsExactly(DatoIntervallEntitet.fraOgMedTilOgMed(søknadsperiode.getMinLocalDate(), søknadsperiode.getMaxLocalDate()));
    }

    @Test
    void skal_vurdere_hele_søknadsperioden_når_avklaringen_dekker_deler_av_den() {
        var behandling = opprettBehandlingMedVilkårsperioder(VilkårType.BOSTEDSVILKÅR, PERIODE_1, PERIODE_2);
        leggTilTriggere(behandling, new Trigger(BehandlingÅrsakType.NY_SØKT_PERIODE, PERIODE_1));
        lagreAvklaring(behandling, DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2026, 1, 15), LocalDate.of(2026, 1, 31)));

        assertThat(tjeneste.utled(behandling.getId(), VilkårType.BOSTEDSVILKÅR)).containsExactly(PERIODE_1);
    }

    @Test
    void skal_ikke_utvide_annet_vilkår_med_avklaring_for_bosted() {
        var behandling = opprettBehandlingMedVilkårsperioder(VilkårType.ALDERSVILKÅR, PERIODE_1);
        leggTilTriggere(behandling, new Trigger(BehandlingÅrsakType.NY_SØKT_PERIODE, PERIODE_1));
        lagreAvklaring(behandling, PERIODE_2);

        assertThat(tjeneste.utled(behandling.getId(), VilkårType.ALDERSVILKÅR)).containsExactly(PERIODE_1);
    }

    private Behandling opprettBehandlingMedVilkårsperioder(VilkårType vilkårType, DatoIntervallEntitet... perioder) {
        var builder = AktivitetspengerTestScenarioBuilder.builderMedSøknad();
        for (var periode : perioder) {
            builder.leggTilVilkår(vilkårType, Utfall.IKKE_VURDERT, new Periode(periode.getFomDato(), periode.getTomDato()));
        }
        return builder.lagre(entityManager);
    }

    private void leggTilTriggere(Behandling behandling, Trigger... triggere) {
        prosessTriggereRepository.leggTil(behandling.getId(), Set.of(triggere));
    }

    private void lagreAvklaring(Behandling behandling, DatoIntervallEntitet periode) {
        lagreAvklaringer(behandling, periode);
    }

    private void lagreAvklaringer(Behandling behandling, DatoIntervallEntitet... perioder) {
        vilkårsavklaringGrunnlagRepository.lagreForeslåtteAvklaringer(behandling.getId(), VilkårType.BOSTEDSVILKÅR, Arrays.stream(perioder)
            .map(periode -> new VilkårPeriodeAvklaringForeslått(
                UUID.randomUUID(),
                periode,
                BostedsvilkårIkkeOppfyltÅrsak.IKKE_BOSATTADRESSE_I_TRONDHEIM.getKode(),
                "begrunnelse",
                true,
                "fritekst",
                null,
                BostedsavklaringKildeType.BRUKER,
                null,
                "A12345",
                LocalDateTime.now(),
                Avklaringtype.AVSLAG))
            .collect(Collectors.toSet()));
    }
}
