package no.nav.ung.ytelse.aktivitetspenger.perioder;

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
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.Periode;
import no.nav.ung.ytelse.aktivitetspenger.testdata.AktivitetspengerTestScenarioBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(JpaExtension.class)
@ExtendWith(CdiAwareExtension.class)
class AktivitetspengerPerioderTjenesteTest {

    private static final LocalDate FOM = LocalDate.of(2026, 1, 5);
    private static final LocalDate TOM = LocalDate.of(2026, 6, 30);
    private static final Periode HELE_ÅRET = new Periode(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));

    @Inject
    private EntityManager entityManager;
    @Inject
    private BehandlingRepository behandlingRepository;
    @Inject
    private AktivitetspengerPerioderTjeneste tjeneste;

    @Test
    void skal_gi_tom_liste_når_bruker_ikke_har_fagsak() {
        var resultat = tjeneste.hentInnvilgedePerioder(Set.of(AktørId.dummy()), HELE_ÅRET);

        assertThat(resultat).isEmpty();
    }

    @Test
    void skal_gi_innvilget_periode_fra_gjeldende_vedtak() {
        var aktørId = AktørId.dummy();
        opprettAvsluttetBehandling(aktørId, null, vilkår(FOM, TOM, Utfall.OPPFYLT), 2);

        var resultat = tjeneste.hentInnvilgedePerioder(Set.of(aktørId), HELE_ÅRET);

        assertThat(resultat).containsExactly(new Periode(FOM, TOM));
    }

    @Test
    void skal_ikke_gi_perioder_ved_avslag() {
        var aktørId = AktørId.dummy();
        opprettAvsluttetBehandling(aktørId, null, vilkår(FOM, TOM, Utfall.IKKE_OPPFYLT), 2);

        var resultat = tjeneste.hentInnvilgedePerioder(Set.of(aktørId), HELE_ÅRET);

        assertThat(resultat).isEmpty();
    }

    @Test
    void skal_gi_forkortet_periode_etter_opphør_i_revurdering() {
        var aktørId = AktørId.dummy();
        var opphørsdato = LocalDate.of(2026, 4, 1);
        var førstegang = opprettAvsluttetBehandling(aktørId, null, vilkår(FOM, TOM, Utfall.OPPFYLT), 3);
        opprettAvsluttetBehandling(aktørId, førstegang, List.of(
            new VilkårData(new Periode(FOM, opphørsdato.minusDays(1)), Utfall.OPPFYLT),
            new VilkårData(new Periode(opphørsdato, TOM), Utfall.IKKE_OPPFYLT)), 1);

        var resultat = tjeneste.hentInnvilgedePerioder(Set.of(aktørId), HELE_ÅRET);

        assertThat(resultat).containsExactly(new Periode(FOM, opphørsdato.minusDays(1)));
    }

    @Test
    void skal_gi_endret_fom_etter_revurdering() {
        var aktørId = AktørId.dummy();
        var nyFom = LocalDate.of(2026, 2, 2);
        var førstegang = opprettAvsluttetBehandling(aktørId, null, vilkår(FOM, TOM, Utfall.OPPFYLT), 3);
        opprettAvsluttetBehandling(aktørId, førstegang, List.of(
            new VilkårData(new Periode(FOM, nyFom.minusDays(1)), Utfall.IKKE_OPPFYLT),
            new VilkårData(new Periode(nyFom, TOM), Utfall.OPPFYLT)), 1);

        var resultat = tjeneste.hentInnvilgedePerioder(Set.of(aktørId), HELE_ÅRET);

        assertThat(resultat).containsExactly(new Periode(nyFom, TOM));
    }

    @Test
    void skal_ignorere_åpen_revurdering() {
        var aktørId = AktørId.dummy();
        var førstegang = opprettAvsluttetBehandling(aktørId, null, vilkår(FOM, TOM, Utfall.OPPFYLT), 2);
        var åpenRevurdering = AktivitetspengerTestScenarioBuilder.builderMedSøknad()
            .medBehandlingType(BehandlingType.REVURDERING)
            .medOriginalBehandling(førstegang, BehandlingÅrsakType.ENDRET_BOSTED);
        leggTilVilkår(åpenRevurdering, vilkår(FOM, TOM, Utfall.IKKE_OPPFYLT));
        åpenRevurdering.lagre(entityManager);

        var resultat = tjeneste.hentInnvilgedePerioder(Set.of(aktørId), HELE_ÅRET);

        assertThat(resultat).containsExactly(new Periode(FOM, TOM));
    }

    @Test
    void skal_returnere_hele_perioden_uten_å_kutte_ved_forespurt_fom_og_tom() {
        var aktørId = AktørId.dummy();
        opprettAvsluttetBehandling(aktørId, null, vilkår(FOM, TOM, Utfall.OPPFYLT), 2);

        var resultat = tjeneste.hentInnvilgedePerioder(Set.of(aktørId), new Periode(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31)));

        assertThat(resultat).containsExactly(new Periode(FOM, TOM));
    }

    @Test
    void skal_ikke_gi_perioder_som_ikke_overlapper_forespurt_periode() {
        var aktørId = AktørId.dummy();
        opprettAvsluttetBehandling(aktørId, null, vilkår(FOM, TOM, Utfall.OPPFYLT), 2);

        var resultat = tjeneste.hentInnvilgedePerioder(Set.of(aktørId), new Periode(TOM.plusDays(1), TOM.plusMonths(1)));

        assertThat(resultat).isEmpty();
    }

    @Test
    void skal_slå_sammen_perioder_fra_fagsaker_på_historiske_aktørIder() {
        var gjeldendeAktørId = AktørId.dummy();
        var historiskAktørId = AktørId.dummy();
        var senereFom = TOM.plusDays(1);
        var senereTom = LocalDate.of(2026, 9, 30);
        opprettAvsluttetBehandling(historiskAktørId, null, vilkår(FOM, TOM, Utfall.OPPFYLT), 2);
        opprettAvsluttetBehandling(gjeldendeAktørId, null, vilkår(senereFom, senereTom, Utfall.OPPFYLT), 1);

        var resultat = tjeneste.hentInnvilgedePerioder(Set.of(gjeldendeAktørId, historiskAktørId), HELE_ÅRET);

        assertThat(resultat).containsExactly(new Periode(FOM, senereTom));
    }

    private record VilkårData(Periode periode, Utfall utfall) {
    }

    private static List<VilkårData> vilkår(LocalDate fom, LocalDate tom, Utfall utfall) {
        return List.of(new VilkårData(new Periode(fom, tom), utfall));
    }

    private Behandling opprettAvsluttetBehandling(AktørId aktørId, Behandling originalBehandling, List<VilkårData> vilkår, int vedtakDagerSiden) {
        var scenario = originalBehandling == null
            ? AktivitetspengerTestScenarioBuilder.builderMedSøknad(aktørId)
            : AktivitetspengerTestScenarioBuilder.builderMedSøknad()
            .medBehandlingType(BehandlingType.REVURDERING)
            .medOriginalBehandling(originalBehandling, BehandlingÅrsakType.ENDRET_BOSTED);
        scenario.medBehandlingsresultat(BehandlingResultatType.INNVILGET);
        leggTilVilkår(scenario, vilkår);
        scenario.medBehandlingVedtak()
            .medVedtakstidspunkt(LocalDateTime.now().minusDays(vedtakDagerSiden))
            .medAnsvarligSaksbehandler("Z000000");
        var behandling = scenario.lagre(entityManager);

        behandling.avsluttBehandling();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
        return behandling;
    }

    private static void leggTilVilkår(AktivitetspengerTestScenarioBuilder scenario, List<VilkårData> vilkår) {
        vilkår.forEach(v -> scenario.leggTilVilkår(VilkårType.AKTIVITETSVILKÅR, v.utfall(), v.periode()));
    }
}
