package no.nav.ung.ytelse.aktivitetspenger.mottak;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.ung.kodeverk.behandling.BehandlingResultatType;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.typer.Periode;
import no.nav.ung.ytelse.aktivitetspenger.testdata.AktivitetspengerTestScenarioBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(JpaExtension.class)
@ExtendWith(CdiAwareExtension.class)
class VirkingstidspunktUtlederTest {

    private static final LocalDate UKE1_TORSDAG = LocalDate.of(2026, 1, 1);
    private static final LocalDate UKE1_FREDAG = LocalDate.of(2026, 1, 2);
    private static final LocalDate UKE1_LØRDAG = LocalDate.of(2026, 1, 3);
    private static final LocalDate UKE1_SØNDAG = LocalDate.of(2026, 1, 4);
    private static final LocalDate UKE2_MANDAG = LocalDate.of(2026, 1, 5);
    private static final LocalDate UKE2_TIRSDAG = LocalDate.of(2026, 1, 6);

    @Inject
    private EntityManager entityManager;
    @Inject
    private BehandlingRepository behandlingRepository;
    @Inject
    private VirkningstidspunktUtleder utleder;

    @Test
    void skal_bruke_søknad_fom_når_ingen_tidligere_innvilget_dato() {
        var resultat = VirkningstidspunktUtleder.utledVirkningstidspunkt(UKE2_MANDAG, (LocalDate) null);

        assertThat(resultat).isEqualTo(UKE2_MANDAG);
    }

    @Test
    void skal_bruke_søknad_fom_når_siste_innvilgede_dato_er_før_søknad_fom() {
        var resultat = VirkningstidspunktUtleder.utledVirkningstidspunkt(UKE2_TIRSDAG, UKE1_FREDAG);

        assertThat(resultat).isEqualTo(UKE2_TIRSDAG);
    }

    @Test
    void skal_bruke_dagen_etter_siste_innvilgede_dato_når_søker_på_siste_dag_i_innvilget_periode() {
        var resultat = VirkningstidspunktUtleder.utledVirkningstidspunkt(UKE2_MANDAG, UKE2_MANDAG);

        assertThat(resultat).isEqualTo(UKE2_TIRSDAG);
    }

    @Test
    void skal_bruke_dagen_etter_siste_innvilgede_dato_når_denne_er_etter_søknad_fom() {
        var resultat = VirkningstidspunktUtleder.utledVirkningstidspunkt(UKE1_TORSDAG, UKE2_MANDAG);

        assertThat(resultat).isEqualTo(UKE2_TIRSDAG);
    }

    @Test
    void skal_flytte_søknad_fom_fra_lørdag_til_mandag() {
        var resultat = VirkningstidspunktUtleder.utledVirkningstidspunkt(UKE1_LØRDAG, (LocalDate) null);

        assertThat(resultat).isEqualTo(UKE2_MANDAG);
    }

    @Test
    void skal_flytte_søknad_fom_fra_søndag_til_mandag() {
        var resultat = VirkningstidspunktUtleder.utledVirkningstidspunkt(UKE1_SØNDAG, (LocalDate) null);

        assertThat(resultat).isEqualTo(UKE2_MANDAG);
    }

    @Test
    void skal_flytte_dagen_etter_siste_innvilgede_dato_over_helg() {
        // siste innvilgede = fredag => dagen etter = lørdag => flyttes til mandag
        var resultat = VirkningstidspunktUtleder.utledVirkningstidspunkt(UKE1_TORSDAG, UKE1_FREDAG);

        assertThat(resultat).isEqualTo(UKE2_MANDAG);
    }

    @Test
    void skal_bruke_søknad_fom_når_det_ikke_finnes_avsluttet_behandling() {
        var behandling = opprettFørstegangsbehandling();

        var resultat = utleder.utledVirkningstidspunkt(UKE2_MANDAG, behandling);

        assertThat(resultat).isEqualTo(UKE2_MANDAG);
    }

    @Test
    void skal_bruke_dagen_etter_siste_innvilgede_dato_fra_forrige_vedtak() {
        var sisteInnvilgedeDato = LocalDate.of(2026, 3, 31);
        var forventetVirkningstidspunkt = LocalDate.of(2026, 4, 1); // onsdag
        var avsluttet = opprettAvsluttetInnvilgetBehandling(new Periode(LocalDate.of(2026, 3, 2), sisteInnvilgedeDato));
        var revurdering = opprettRevurdering(avsluttet);

        var resultat = utleder.utledVirkningstidspunkt(UKE2_MANDAG, revurdering);

        assertThat(resultat).isEqualTo(forventetVirkningstidspunkt);
    }

    @Test
    void skal_bruke_søknad_fom_når_den_er_etter_siste_innvilgede_dato_fra_forrige_vedtak() {
        var søknadFom = LocalDate.of(2026, 2, 2); // mandag, etter forrige innvilgede periode
        var avsluttet = opprettAvsluttetInnvilgetBehandling(new Periode(UKE1_TORSDAG, UKE2_MANDAG));
        var revurdering = opprettRevurdering(avsluttet);

        var resultat = utleder.utledVirkningstidspunkt(søknadFom, revurdering);

        assertThat(resultat).isEqualTo(søknadFom);
    }

    private Behandling opprettFørstegangsbehandling() {
        return AktivitetspengerTestScenarioBuilder.builderMedSøknad()
            .lagre(entityManager);
    }

    private Behandling opprettAvsluttetInnvilgetBehandling(Periode oppfyltPeriode) {
        var scenario = AktivitetspengerTestScenarioBuilder.builderMedSøknad()
            .medBehandlingsresultat(BehandlingResultatType.INNVILGET)
            .leggTilVilkår(VilkårType.ALDERSVILKÅR, Utfall.OPPFYLT, oppfyltPeriode)
            .leggTilVilkår(VilkårType.BOSTEDSVILKÅR, Utfall.OPPFYLT, oppfyltPeriode)
            .leggTilVilkår(VilkårType.ANDRE_LIVSOPPHOLDSYTELSER_VILKÅR, Utfall.OPPFYLT, oppfyltPeriode)
            .leggTilVilkår(VilkårType.BISTANDSVILKÅR, Utfall.OPPFYLT, oppfyltPeriode)
            .leggTilVilkår(VilkårType.AKTIVITETSVILKÅR, Utfall.OPPFYLT, oppfyltPeriode)
            ;
        scenario.medBehandlingVedtak()
            .medVedtakstidspunkt(LocalDateTime.now().minusDays(1))
            .medAnsvarligSaksbehandler("Z000000");
        var behandling = scenario.lagre(entityManager);

        behandling.avsluttBehandling();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
        return behandling;
    }

    private Behandling opprettRevurdering(Behandling originalBehandling) {
        return AktivitetspengerTestScenarioBuilder.builderMedSøknad()
            .medBehandlingType(BehandlingType.REVURDERING)
            .medOriginalBehandling(originalBehandling, BehandlingÅrsakType.ENDRET_BOSTED)
            .lagre(entityManager);
    }
}



