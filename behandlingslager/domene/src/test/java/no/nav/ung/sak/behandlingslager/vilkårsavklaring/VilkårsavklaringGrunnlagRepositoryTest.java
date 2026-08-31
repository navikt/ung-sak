package no.nav.ung.sak.behandlingslager.vilkårsavklaring;

import jakarta.inject.Inject;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.vilkår.Avklaringtype;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.ung.sak.db.util.CdiDbAwareTest;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.Saksnummer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@CdiDbAwareTest
class VilkårsavklaringGrunnlagRepositoryTest {

    private static final LocalDate FOM = LocalDate.of(2026, 1, 1);
    private static final LocalDate TOM = LocalDate.of(2026, 1, 31);
    private static final String VURDERT_AV = "saksbehandler1";
    private static final LocalDateTime VURDERT_TIDSPUNKT = LocalDateTime.of(2026, 1, 15, 10, 0);
    private static final VilkårType VILKÅR_TYPE = VilkårType.BOSTEDSVILKÅR;
    private static final VilkårType ANNEN_VILKÅR_TYPE = VilkårType.ALDERSVILKÅR;

    @Inject
    private FagsakRepository fagsakRepository;

    @Inject
    private BehandlingRepository behandlingRepository;

    @Inject
    private VilkårsavklaringGrunnlagRepository repository;

    private Behandling behandling;

    @BeforeEach
    void setUp() {
        Fagsak fagsak = Fagsak.opprettNy(FagsakYtelseType.AKTIVITETSPENGER, new AktørId("1"), new Saksnummer("SAK1"), FOM, TOM);
        fagsakRepository.opprettNy(fagsak);
        behandling = Behandling.nyBehandlingFor(fagsak, BehandlingType.FØRSTEGANGSSØKNAD).build();
        behandlingRepository.lagre(behandling, new BehandlingLås(null));
    }

    @Test
    void skal_opprette_grunnlag_ved_forste_lagring_av_foreslatte_avklaringer() {
        assertThat(repository.hentGrunnlagHvisEksisterer(behandling.getId(), VILKÅR_TYPE)).isEmpty();

        var avklaring = lagAvklaring(FOM, TOM);
        repository.lagreForeslåtteAvklaringer(behandling.getId(), VILKÅR_TYPE, Set.of(avklaring));

        var grunnlag = repository.hentGrunnlagHvisEksisterer(behandling.getId(), VILKÅR_TYPE).orElseThrow();
        assertThat(grunnlag.getForeslåtteAvklaringer())
            .extracting(VilkårPeriodeAvklaring::getReferanse)
            .containsExactly(avklaring.getReferanse());
    }

    @Test
    void skal_erstatte_avklaringer_under_arbeid_nar_nye_avklaringer_lagres() {
        var første = lagAvklaring(FOM, LocalDate.of(2026, 1, 15));
        repository.lagreForeslåtteAvklaringer(behandling.getId(), VILKÅR_TYPE, Set.of(første));

        var andre = lagAvklaring(LocalDate.of(2026, 1, 16), TOM);
        repository.lagreForeslåtteAvklaringer(behandling.getId(), VILKÅR_TYPE, Set.of(andre));

        var avklaringer = hentSorterteAvklaringer();
        assertThat(avklaringer).hasSize(1);
        assertThat(avklaringer.getFirst().getReferanse()).isEqualTo(andre.getReferanse());
    }

    @Test
    void skal_beholde_avklaringen_og_referanse_ved_ny_lagring_hvis_innhold_er_uendret() {
        var opprinneligAvklaring = lagAvklaring(FOM, TOM);
        repository.lagreForeslåtteAvklaringer(behandling.getId(), VILKÅR_TYPE, Set.of(opprinneligAvklaring));

        // Lagre på nytt med ny instans av samme innhold (ny referanse før lagring)
        repository.lagreForeslåtteAvklaringer(behandling.getId(), VILKÅR_TYPE, Set.of(lagAvklaring(FOM, TOM)));

        var avklaringer = hentSorterteAvklaringer();
        assertThat(avklaringer).hasSize(1);
        assertThat(avklaringer.getFirst().getReferanse()).isEqualTo(opprinneligAvklaring.getReferanse());

        var endretAvklaring = lagAvklaring(FOM, TOM, "ny begrunnelse");
        repository.lagreForeslåtteAvklaringer(behandling.getId(), VILKÅR_TYPE, Set.of(endretAvklaring));
        var avklaringerEtterEndring = hentSorterteAvklaringer();
        assertThat(avklaringerEtterEndring).hasSize(1);
        assertThat(avklaringerEtterEndring.getFirst().getReferanse()).isEqualTo(endretAvklaring.getReferanse());
    }

    @Test
    void ferdigstilling_skal_gi_ingen_nye_rader_i_vilkaar_periode_avklaring_ved_lagring_av_forslag() {
        var avklaring = lagAvklaring(FOM, TOM);
        repository.lagreForeslåtteAvklaringer(behandling.getId(), VILKÅR_TYPE, Set.of(avklaring));
        repository.ferdigstillForeslåtteAvklaringer(behandling.getId(), VILKÅR_TYPE);

        // Lagrer et nytt (uendret innholdsmessig identisk) forslag - skal ikke berøre ferdigstilte rader
        repository.lagreForeslåtteAvklaringer(behandling.getId(), VILKÅR_TYPE, Set.of(lagAvklaring(FOM, TOM)));

        var grunnlag = repository.hentGrunnlagHvisEksisterer(behandling.getId(), VILKÅR_TYPE).orElseThrow();
        assertThat(grunnlag.getFerdigstilteAvklaringer())
            .extracting(VilkårPeriodeAvklaring::getReferanse)
            .containsExactly(avklaring.getReferanse());
    }

    @Test
    void ferdigstilling_skal_beholde_foreslaatte_avklaringer_og_vaere_idempotent() {
        var foreslått = lagAvklaring(FOM, TOM);
        repository.lagreForeslåtteAvklaringer(behandling.getId(), VILKÅR_TYPE, Set.of(foreslått));

        repository.ferdigstillForeslåtteAvklaringer(behandling.getId(), VILKÅR_TYPE);
        repository.ferdigstillForeslåtteAvklaringer(behandling.getId(), VILKÅR_TYPE);

        var grunnlag = repository.hentGrunnlagHvisEksisterer(behandling.getId(), VILKÅR_TYPE).orElseThrow();
        assertThat(grunnlag.getForeslåtteAvklaringer())
            .extracting(VilkårPeriodeAvklaring::getReferanse)
            .containsExactly(foreslått.getReferanse());
        assertThat(grunnlag.getFerdigstilteAvklaringer())
            .extracting(VilkårPeriodeAvklaring::getReferanse)
            .containsExactly(foreslått.getReferanse());
    }

    @Test
    void to_vilkarstyper_pa_samme_behandling_skal_ikke_pavirke_hverandre() {
        var avklaringBosted = lagAvklaring(FOM, TOM);
        var avklaringAlder = lagAvklaring(FOM, TOM);

        repository.lagreForeslåtteAvklaringer(behandling.getId(), VILKÅR_TYPE, Set.of(avklaringBosted));
        repository.lagreForeslåtteAvklaringer(behandling.getId(), ANNEN_VILKÅR_TYPE, Set.of(avklaringAlder));

        repository.ferdigstillForeslåtteAvklaringer(behandling.getId(), VILKÅR_TYPE);

        var bostedGrunnlag = repository.hentGrunnlagHvisEksisterer(behandling.getId(), VILKÅR_TYPE).orElseThrow();
        var alderGrunnlag = repository.hentGrunnlagHvisEksisterer(behandling.getId(), ANNEN_VILKÅR_TYPE).orElseThrow();

        assertThat(bostedGrunnlag.getFerdigstilteAvklaringer())
            .extracting(VilkårPeriodeAvklaring::getReferanse)
            .containsExactly(avklaringBosted.getReferanse());
        // Ferdigstilling av bosted skal ikke ferdigstille alder-avklaringen
        assertThat(alderGrunnlag.getFerdigstilteAvklaringer()).isEmpty();
        assertThat(alderGrunnlag.getForeslåtteAvklaringer())
            .extracting(VilkårPeriodeAvklaring::getReferanse)
            .containsExactly(avklaringAlder.getReferanse());
    }

    @Test
    void skal_opprette_nytt_grunnlag_med_ny_avklaring_uten_a_mutere_avklaringer_pa_tidligere_behandling() {
        var opprinnelig = lagAvklaring(FOM, TOM);
        repository.lagreForeslåtteAvklaringer(behandling.getId(), VILKÅR_TYPE, Set.of(opprinnelig));

        Behandling nyBehandling = Behandling.nyBehandlingFor(behandling.getFagsak(), BehandlingType.REVURDERING).build();
        behandlingRepository.lagre(nyBehandling, new BehandlingLås(null));

        repository.kopierGrunnlagFraEksisterendeBehandling(behandling.getId(), nyBehandling.getId());
        var ny = lagAvklaring(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28));
        repository.lagreForeslåtteAvklaringer(nyBehandling.getId(), VILKÅR_TYPE, Set.of(opprinnelig, ny));

        var avklaringerPåNyBehandling = repository.hentGrunnlagHvisEksisterer(nyBehandling.getId(), VILKÅR_TYPE)
            .orElseThrow()
            .getForeslåtteAvklaringer();

        assertThat(avklaringerPåNyBehandling)
            .extracting(VilkårPeriodeAvklaring::getReferanse)
            .containsExactlyInAnyOrder(opprinnelig.getReferanse(), ny.getReferanse());

        var avklaringerPåGammelBehandling = repository.hentGrunnlagHvisEksisterer(behandling.getId(), VILKÅR_TYPE)
            .orElseThrow()
            .getForeslåtteAvklaringer();

        assertThat(avklaringerPåGammelBehandling).hasSize(1);
        assertThat(avklaringerPåGammelBehandling.iterator().next().getReferanse()).isEqualTo(opprinnelig.getReferanse());
    }

    @Test
    void skal_ikke_kopiere_foreslaatte_avklaringer_til_ny_behandling() {
        var foreslått = lagAvklaring(FOM, TOM);
        repository.lagreForeslåtteAvklaringer(behandling.getId(), VILKÅR_TYPE, Set.of(foreslått));

        Behandling nyBehandling = Behandling.nyBehandlingFor(behandling.getFagsak(), BehandlingType.REVURDERING).build();
        behandlingRepository.lagre(nyBehandling, new BehandlingLås(null));
        repository.kopierGrunnlagFraEksisterendeBehandling(behandling.getId(), nyBehandling.getId());

        var nyttGrunnlag = repository.hentGrunnlagHvisEksisterer(nyBehandling.getId(), VILKÅR_TYPE).orElseThrow();
        assertThat(nyttGrunnlag.getForeslåtteAvklaringer()).isEmpty();
        assertThat(nyttGrunnlag.getFerdigstilteAvklaringer()).isEmpty();

        // Forslaget skal fortsatt ligge på den opprinnelige behandlingen
        assertThat(repository.hentGrunnlagHvisEksisterer(behandling.getId(), VILKÅR_TYPE).orElseThrow().getForeslåtteAvklaringer())
            .extracting(VilkårPeriodeAvklaring::getReferanse)
            .containsExactly(foreslått.getReferanse());
    }

    @Test
    void skal_kopiere_ferdigstilte_avklaringer_til_ny_behandling() {
        var foreslått = lagAvklaring(FOM, TOM);
        repository.lagreForeslåtteAvklaringer(behandling.getId(), VILKÅR_TYPE, Set.of(foreslått));
        repository.ferdigstillForeslåtteAvklaringer(behandling.getId(), VILKÅR_TYPE);

        Behandling nyBehandling = Behandling.nyBehandlingFor(behandling.getFagsak(), BehandlingType.REVURDERING).build();
        behandlingRepository.lagre(nyBehandling, new BehandlingLås(null));
        repository.kopierGrunnlagFraEksisterendeBehandling(behandling.getId(), nyBehandling.getId());

        var nyttGrunnlag = repository.hentGrunnlagHvisEksisterer(nyBehandling.getId(), VILKÅR_TYPE).orElseThrow();
        assertThat(nyttGrunnlag.getForeslåtteAvklaringer()).isEmpty();
        assertThat(nyttGrunnlag.getFerdigstilteAvklaringer())
            .extracting(VilkårPeriodeAvklaring::getReferanse)
            .containsExactly(foreslått.getReferanse());
    }

    @Test
    void holder_skal_deles_by_reference_nar_ferdigstilte_er_uendret_ogsa_nar_forrige_behandling_hadde_foreslatte() {
        var foreslått = lagAvklaring(FOM, TOM);
        repository.lagreForeslåtteAvklaringer(behandling.getId(), VILKÅR_TYPE, Set.of(foreslått));
        repository.ferdigstillForeslåtteAvklaringer(behandling.getId(), VILKÅR_TYPE);

        // Forrige behandling har fortsatt en foreslått avklaring i tillegg til den ferdigstilte
        var nyForeslått = lagAvklaring(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28));
        repository.lagreForeslåtteAvklaringer(behandling.getId(), VILKÅR_TYPE, Set.of(nyForeslått));

        Behandling nyBehandling = Behandling.nyBehandlingFor(behandling.getFagsak(), BehandlingType.REVURDERING).build();
        behandlingRepository.lagre(nyBehandling, new BehandlingLås(null));
        repository.kopierGrunnlagFraEksisterendeBehandling(behandling.getId(), nyBehandling.getId());

        var nyttGrunnlag = repository.hentGrunnlagHvisEksisterer(nyBehandling.getId(), VILKÅR_TYPE).orElseThrow();
        assertThat(nyttGrunnlag.getForeslåtteAvklaringer()).isEmpty();
        assertThat(nyttGrunnlag.getFerdigstilteAvklaringer())
            .extracting(VilkårPeriodeAvklaring::getReferanse)
            .containsExactly(foreslått.getReferanse());
    }

    private List<VilkårPeriodeAvklaring> hentSorterteAvklaringer() {
        return repository.hentGrunnlagHvisEksisterer(behandling.getId(), VILKÅR_TYPE)
            .orElseThrow()
            .getForeslåtteAvklaringer()
            .stream()
            .sorted(Comparator.comparing(a -> a.getPeriode().getFomDato()))
            .toList();
    }

    private VilkårPeriodeAvklaringForeslått lagAvklaring(LocalDate fom, LocalDate tom) {
        return lagAvklaring(fom, tom, "begrunnelse for hvorfor det ikke varsles");
    }

    private VilkårPeriodeAvklaringForeslått lagAvklaring(LocalDate fom, LocalDate tom, String begrunnelse) {
        return new VilkårPeriodeAvklaringForeslått(
            DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom),
            "IKKE_OPPFYLT_KODE",
            begrunnelse,
            false,
            null,
            "begrunnelse for hvorfor det ikke varsles",
            VURDERT_AV,
            VURDERT_TIDSPUNKT,
            Avklaringtype.AVSLAG
        );
    }
}
