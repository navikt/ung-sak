package no.nav.ung.ytelse.aktivitetspenger.del1.steg.bosatt;

import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.kodeverk.varsel.EtterlysningType;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.ung.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.ung.sak.behandlingslager.bosatt.BostedsGrunnlagRepository;
import no.nav.ung.sak.behandlingslager.etterlysning.Etterlysning;
import no.nav.ung.sak.behandlingslager.etterlysning.EtterlysningRepository;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.etterlysning.EtterlysningTjeneste;
import no.nav.ung.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.ung.sak.trigger.ProsessTriggereRepository;
import no.nav.ung.sak.trigger.Trigger;
import no.nav.ung.sak.typer.Periode;
import no.nav.ung.sak.vilkår.ManuelleVilkårRekkefølgeTjeneste;
import no.nav.ung.sak.vilkår.VilkårTjeneste;
import no.nav.ung.ytelse.aktivitetspenger.testdata.AktivitetspengerTestScenarioBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(JpaExtension.class)
@ExtendWith(CdiAwareExtension.class)
class VurderBosattStegTest {

    private static final LocalDate FOM = LocalDate.of(2026, 12, 2);
    private static final LocalDate TOM = LocalDate.of(2027, 12, 1);
    private static final Periode VILKÅR_PERIODE = new Periode(FOM, TOM);

    @Inject
    private EntityManager entityManager;

    @Inject
    @Any
    private Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjenester;

    @Inject
    private ManuelleVilkårRekkefølgeTjeneste manuelleVilkårRekkefølgeTjeneste;

    @Inject
    private ProsessTriggereRepository prosessTriggereRepository;

    @Inject
    private EtterlysningRepository etterlysningRepository;

    @Inject
    private EtterlysningTjeneste etterlysningTjeneste;

    @Inject
    private BostedsGrunnlagRepository bostedsGrunnlagRepository;

    private BehandlingRepository behandlingRepository;
    private VilkårResultatRepository vilkårResultatRepository;
    private VurderBosattSteg steg;

    @BeforeEach
    void setUp() {
        var repoProvider = new BehandlingRepositoryProvider(entityManager);
        behandlingRepository = repoProvider.getBehandlingRepository();
        vilkårResultatRepository = repoProvider.getVilkårResultatRepository();
        var vilkårTjeneste = new VilkårTjeneste(behandlingRepository, null, vilkårResultatRepository);
        steg = new VurderBosattSteg(
            manuelleVilkårRekkefølgeTjeneste,
            vilkårResultatRepository,
            vilkårTjeneste,
            behandlingRepository,
            bostedsGrunnlagRepository,
            perioderTilVurderingTjenester,
            etterlysningTjeneste
        );
    }

    @Test
    void skal_returnere_aksjonspunkt_VURDER_BOSTED_når_det_finnes_relevante_perioder() {
        var behandling = AktivitetspengerTestScenarioBuilder.builderMedSøknad()
            .leggTilVilkår(VilkårType.BOSTEDSVILKÅR, Utfall.IKKE_VURDERT, VILKÅR_PERIODE)
            .leggTilVilkår(VilkårType.ALDERSVILKÅR, Utfall.OPPFYLT, VILKÅR_PERIODE)
            .leggTilVilkår(VilkårType.SØKNADSFRIST, Utfall.OPPFYLT, VILKÅR_PERIODE)
            .lagre(entityManager);
        prosessTriggereRepository.leggTil(behandling.getId(), Set.of(new Trigger(BehandlingÅrsakType.NY_SØKT_PERIODE, DatoIntervallEntitet.fra(VILKÅR_PERIODE))));

        var resultat = utførSteg(behandling);

        assertThat(resultat.getAksjonspunktListe()).containsExactly(AksjonspunktDefinisjon.VURDER_BOSTED);
    }

    @Test
    void skal_ikke_returnere_aksjonspunkt_når_ingen_relevante_perioder_for_bostedsvilkåret() {
        var behandling = AktivitetspengerTestScenarioBuilder.builderMedSøknad()
            .leggTilVilkår(VilkårType.BOSTEDSVILKÅR, Utfall.IKKE_VURDERT, VILKÅR_PERIODE)
            .leggTilVilkår(VilkårType.ALDERSVILKÅR, Utfall.IKKE_OPPFYLT, VILKÅR_PERIODE)
            .leggTilVilkår(VilkårType.SØKNADSFRIST, Utfall.OPPFYLT, VILKÅR_PERIODE)
            .lagre(entityManager);
        prosessTriggereRepository.leggTil(behandling.getId(), Set.of(new Trigger(BehandlingÅrsakType.NY_SØKT_PERIODE, DatoIntervallEntitet.fra(VILKÅR_PERIODE))));

        var resultat = utførSteg(behandling);

        assertThat(resultat.getAksjonspunktListe()).isEmpty();
        var vilkår = vilkårResultatRepository.hent(behandling.getId())
            .getVilkår(VilkårType.BOSTEDSVILKÅR)
            .orElseThrow();
        assertThat(vilkår.getPerioder()).allSatisfy(p ->
            assertThat(p.getGjeldendeUtfall()).isEqualTo(Utfall.IKKE_RELEVANT)
        );
    }

    @Test
    void skal_sette_avslått_periode_til_ikke_relevant_og_returnere_aksjonspunkt_for_resterende() {
        var periode1 = new Periode(FOM, LocalDate.of(2027, 7, 15));
        var periode2 = new Periode(LocalDate.of(2027, 7, 16), TOM);
        var behandling = AktivitetspengerTestScenarioBuilder.builderMedSøknad()
            .leggTilVilkår(VilkårType.SØKNADSFRIST, Utfall.OPPFYLT, VILKÅR_PERIODE)
            .leggTilVilkår(VilkårType.ALDERSVILKÅR, Utfall.IKKE_OPPFYLT, periode1)
            .leggTilVilkår(VilkårType.ALDERSVILKÅR, Utfall.OPPFYLT, periode2)
            .leggTilVilkår(VilkårType.BOSTEDSVILKÅR, Utfall.IKKE_VURDERT, VILKÅR_PERIODE)
            .lagre(entityManager);
        prosessTriggereRepository.leggTil(behandling.getId(), Set.of(new Trigger(BehandlingÅrsakType.NY_SØKT_PERIODE, DatoIntervallEntitet.fra(VILKÅR_PERIODE))));

        var resultat = utførSteg(behandling);

        assertThat(resultat.getAksjonspunktListe()).containsExactly(AksjonspunktDefinisjon.VURDER_BOSTED);
        var perioder = vilkårResultatRepository.hent(behandling.getId())
            .getVilkår(VilkårType.BOSTEDSVILKÅR)
            .orElseThrow()
            .getPerioder()
            .stream().sorted(Comparator.comparing(VilkårPeriode::getFom))
            .toList();
        assertThat(perioder).hasSize(2);
        assertThat(perioder.get(0).getGjeldendeUtfall()).isEqualTo(Utfall.IKKE_RELEVANT);
        assertThat(perioder.get(1).getGjeldendeUtfall()).isEqualTo(Utfall.IKKE_VURDERT);
    }

    @Test
    void skal_sette_behandling_på_vent_når_etterlysning_venter() {
        var behandling = AktivitetspengerTestScenarioBuilder.builderMedSøknad()
            .leggTilVilkår(VilkårType.BOSTEDSVILKÅR, Utfall.IKKE_VURDERT, VILKÅR_PERIODE)
            .leggTilVilkår(VilkårType.ALDERSVILKÅR, Utfall.OPPFYLT, VILKÅR_PERIODE)
            .leggTilVilkår(VilkårType.SØKNADSFRIST, Utfall.OPPFYLT, VILKÅR_PERIODE)
            .lagre(entityManager);
        prosessTriggereRepository.leggTil(behandling.getId(), Set.of(new Trigger(BehandlingÅrsakType.NY_SØKT_PERIODE, DatoIntervallEntitet.fra(VILKÅR_PERIODE))));

        // Lagre foreslått vurdering og oppretter en ventende etterlysning
        bostedsGrunnlagRepository.lagreAvklaringer(behandling.getId(), java.util.Map.of(FOM, true));
        var grunnlagsreferanse = bostedsGrunnlagRepository.hentGrunnlagHvisEksisterer(behandling.getId())
            .orElseThrow().getGrunnlagsreferanse();
        var etterlysning = Etterlysning.opprettForType(
            behandling.getId(), grunnlagsreferanse, UUID.randomUUID(),
            DatoIntervallEntitet.fraOgMedTilOgMed(FOM, TOM), EtterlysningType.UTTALELSE_BOSTED);
        etterlysning.vent(java.time.LocalDateTime.now().plusDays(14));
        etterlysningRepository.lagre(etterlysning);
        entityManager.flush();

        var resultat = utførSteg(behandling);

        assertThat(resultat.getAksjonspunktListe()).hasSize(1);
        assertThat(resultat.getAksjonspunktListe().get(0))
            .isEqualTo(EtterlysningType.UTTALELSE_BOSTED.tilAutopunktDefinisjon());
    }

    @Test
    void skal_fastsette_og_auto_vurdere_periode_ved_utløpt_etterlysning() {
        var behandling = AktivitetspengerTestScenarioBuilder.builderMedSøknad()
            .leggTilVilkår(VilkårType.BOSTEDSVILKÅR, Utfall.IKKE_VURDERT, VILKÅR_PERIODE)
            .leggTilVilkår(VilkårType.ALDERSVILKÅR, Utfall.OPPFYLT, VILKÅR_PERIODE)
            .leggTilVilkår(VilkårType.SØKNADSFRIST, Utfall.OPPFYLT, VILKÅR_PERIODE)
            .lagre(entityManager);
        prosessTriggereRepository.leggTil(behandling.getId(), Set.of(new Trigger(BehandlingÅrsakType.NY_SØKT_PERIODE, DatoIntervallEntitet.fra(VILKÅR_PERIODE))));

        // Lagre foreslått vurdering (bosatt i Trondheim = true)
        bostedsGrunnlagRepository.lagreAvklaringer(behandling.getId(), java.util.Map.of(FOM, true));
        var grunnlagsreferanse = bostedsGrunnlagRepository.hentGrunnlagHvisEksisterer(behandling.getId())
            .orElseThrow().getGrunnlagsreferanse();

        // Opprett en utløpt etterlysning for perioden
        var etterlysning = Etterlysning.opprettForType(
            behandling.getId(), grunnlagsreferanse, UUID.randomUUID(),
            DatoIntervallEntitet.fraOgMedTilOgMed(FOM, TOM), EtterlysningType.UTTALELSE_BOSTED);
        etterlysning.vent(java.time.LocalDateTime.now().minusDays(1));
        etterlysning.utløpt();
        etterlysningRepository.lagre(etterlysning);
        entityManager.flush();

        var resultat = utførSteg(behandling);

        // Ingen aksjonspunkt – auto-vurdert
        assertThat(resultat.getAksjonspunktListe()).isEmpty();

        // Vilkåret skal nå være OPPFYLT (bosatt i Trondheim = true)
        var vilkår = vilkårResultatRepository.hent(behandling.getId())
            .getVilkår(VilkårType.BOSTEDSVILKÅR).orElseThrow();
        assertThat(vilkår.getPerioder()).allSatisfy(p ->
            assertThat(p.getGjeldendeUtfall()).isEqualTo(Utfall.OPPFYLT)
        );

        // Fastsatt holder skal nå være satt
        var grunnlag = bostedsGrunnlagRepository.hentGrunnlagHvisEksisterer(behandling.getId()).orElseThrow();
        assertThat(grunnlag.getFastsattHolder()).isNotNull();
        assertThat(grunnlag.getFastsattHolder().getAvklaringer()).hasSize(1);
        assertThat(grunnlag.getFastsattHolder().getAvklaringer().iterator().next().erBosattITrondheim()).isTrue();
    }

    private BehandleStegResultat utførSteg(Behandling behandling) {
        var kontekst = new BehandlingskontrollKontekst(
            behandling.getFagsakId(),
            behandling.getAktørId(),
            behandlingRepository.taSkriveLås(behandling.getId()));
        return steg.utførSteg(kontekst);
    }
}

