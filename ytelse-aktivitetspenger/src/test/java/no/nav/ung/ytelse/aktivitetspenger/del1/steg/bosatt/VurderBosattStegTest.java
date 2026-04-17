package no.nav.ung.ytelse.aktivitetspenger.del1.steg.bosatt;

import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.ung.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.ung.sak.typer.Periode;
import no.nav.ung.sak.vilkår.ManuelleVilkårRekkefølgeTjeneste;
import no.nav.ung.sak.vilkår.VilkårTjeneste;
import no.nav.ung.ytelse.aktivitetspenger.testdata.AktivitetspengerTestScenarioBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDate;
import java.util.Comparator;

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
            perioderTilVurderingTjenester
        );
    }

    @Test
    void skal_returnere_aksjonspunkt_VURDER_BOSTED_når_det_finnes_relevante_perioder() {
        var behandling = AktivitetspengerTestScenarioBuilder.builderMedSøknad()
            .leggTilVilkår(VilkårType.BOSTEDSVILKÅR, Utfall.IKKE_VURDERT, VILKÅR_PERIODE)
            .leggTilVilkår(VilkårType.ALDERSVILKÅR, Utfall.OPPFYLT, VILKÅR_PERIODE)
            .leggTilVilkår(VilkårType.SØKNADSFRIST, Utfall.OPPFYLT, VILKÅR_PERIODE)
            .lagre(entityManager);

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

    private BehandleStegResultat utførSteg(Behandling behandling) {
        var kontekst = new BehandlingskontrollKontekst(
            behandling.getFagsakId(),
            behandling.getAktørId(),
            behandlingRepository.taSkriveLås(behandling.getId()));
        return steg.utførSteg(kontekst);
    }
}

