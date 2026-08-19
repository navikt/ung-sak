package no.nav.ung.sak.behandlingslager.bosatt;

import jakarta.inject.Inject;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.bosatt.Avklaringtype;
import no.nav.ung.kodeverk.vilkår.BostedsvilkårIkkeOppfyltÅrsak;
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
class BostedsGrunnlagRepositoryTest {

    private static final LocalDate FOM = LocalDate.of(2026, 1, 1);
    private static final LocalDate TOM = LocalDate.of(2026, 1, 31);
    private static final String VURDERT_AV = "saksbehandler1";
    private static final LocalDateTime VURDERT_TIDSPUNKT = LocalDateTime.of(2026, 1, 15, 10, 0);

    @Inject
    private FagsakRepository fagsakRepository;

    @Inject
    private BehandlingRepository behandlingRepository;

    @Inject
    private BostedsGrunnlagRepository repository;

    private Behandling behandling;

    @BeforeEach
    void setUp() {
        Fagsak fagsak = Fagsak.opprettNy(FagsakYtelseType.AKTIVITETSPENGER, new AktørId("1"), new Saksnummer("SAK1"), FOM, TOM);
        fagsakRepository.opprettNy(fagsak);
        behandling = Behandling.nyBehandlingFor(fagsak, BehandlingType.FØRSTEGANGSSØKNAD).build();
        behandlingRepository.lagre(behandling, new BehandlingLås(null));
        repository.lagreInformasjonFraSøknad(behandling.getId(), "jp-1", FOM, true);
    }

    @Test
    void skal_erstatte_avklaringer_under_arbeid_nar_nye_avklaringer_lagres() {
        var første = lagAvklaring(FOM, LocalDate.of(2026, 1, 15));
        repository.lagreForeslåtteAvklaringer(behandling.getId(), List.of(første));

        var andre = lagAvklaring(LocalDate.of(2026, 1, 16), TOM);
        repository.lagreForeslåtteAvklaringer(behandling.getId(), List.of(andre));

        var avklaringer = hentSorterteAvklaringer();
        assertThat(avklaringer).hasSize(1);
        assertThat(avklaringer.getFirst().getReferanse()).isEqualTo(andre.getReferanse());
    }

    @Test
    void skal_erstatte_hele_den_eksisterende_avklaringen_nar_ny_avklaring_overlapper() {
        var eksisterende = lagAvklaring(FOM, TOM);
        repository.lagreForeslåtteAvklaringer(behandling.getId(), List.of(eksisterende));

        var overlappende = lagAvklaring(LocalDate.of(2026, 1, 10), LocalDate.of(2026, 1, 20));
        repository.lagreForeslåtteAvklaringer(behandling.getId(), List.of(overlappende));

        var avklaringer = hentSorterteAvklaringer();

        // Den overlappede avklaringen splittes ikke — da ville referansen dekket to segmenter,
        // og varselet til bruker kunne ikke knyttes entydig til ett segment.
        assertThat(avklaringer).hasSize(1);
        assertThat(avklaringer.getFirst().getPeriode())
            .isEqualTo(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2026, 1, 10), LocalDate.of(2026, 1, 20)));
        assertThat(avklaringer.getFirst().getReferanse()).isEqualTo(overlappende.getReferanse());
    }

    @Test
    void skal_beholde_referansen_nar_avklaringen_lagres_uendret_slik_at_varselet_fortsatt_treffer() {
        var opprinnelig = lagAvklaring(FOM, TOM);
        repository.lagreForeslåtteAvklaringer(behandling.getId(), List.of(opprinnelig));

        // Samme innhold, men ny instans med ny referanse — grunnlaget skal ikke versjoneres
        repository.lagreForeslåtteAvklaringer(behandling.getId(), List.of(lagAvklaring(FOM, TOM)));

        var avklaringer = hentSorterteAvklaringer();
        assertThat(avklaringer).hasSize(1);
        assertThat(avklaringer.getFirst().getReferanse()).isEqualTo(opprinnelig.getReferanse());
    }

    @Test
    void skal_returnere_referanse_per_periodestart_for_avklaringene_under_arbeid() {
        var første = lagAvklaring(FOM, LocalDate.of(2026, 1, 15));
        var andre = lagAvklaring(LocalDate.of(2026, 1, 16), TOM);

        Set<BostedsPeriodeAvklaring> lagredeAvklaringer = repository.lagreForeslåtteAvklaringer(behandling.getId(), List.of(første, andre));

        assertThat(lagredeAvklaringer)
            .contains(første)
            .contains(andre)
            .hasSize(2);
    }

    @Test
    void skal_opprette_nytt_grunnlag_med_ny_soeknad_uten_å_mutere_grunnlaget_pa_tidligere_behandling() {
        Behandling nyBehandling = Behandling.nyBehandlingFor(behandling.getFagsak(), BehandlingType.REVURDERING).build();
        behandlingRepository.lagre(nyBehandling, new BehandlingLås(null));

        repository.kopierGrunnlagFraEksisterendeBehandling(behandling.getId(), nyBehandling.getId());
        repository.lagreInformasjonFraSøknad(nyBehandling.getId(), "jp-2", LocalDate.of(2026, 2, 1), false);

        var informasjonPåNyBehandling = repository.hentGrunnlagHvisEksisterer(nyBehandling.getId())
            .orElseThrow()
            .getOppgittFraSøknad()
            .getInformasjon();

        assertThat(informasjonPåNyBehandling).hasSize(2);
        assertThat(informasjonPåNyBehandling)
            .extracting(BostedsinformasjonFraSøknad::getJournalpostId)
            .containsExactlyInAnyOrder("jp-1", "jp-2");

        var informasjonPåGammelBehandling = repository.hentGrunnlagHvisEksisterer(behandling.getId())
            .orElseThrow()
            .getOppgittFraSøknad()
            .getInformasjon();

        assertThat(informasjonPåGammelBehandling).hasSize(1);
        assertThat(informasjonPåGammelBehandling.iterator().next().getJournalpostId()).isEqualTo("jp-1");
    }

    @Test
    void skal_opprette_nytt_grunnlag_med_ny_avklaring_uten_å_mutere_avklaringer_pa_tidligere_behandling() {
        var opprinnelig = lagAvklaring(FOM, TOM);
        repository.lagreForeslåtteAvklaringer(behandling.getId(), List.of(opprinnelig));

        Behandling nyBehandling = Behandling.nyBehandlingFor(behandling.getFagsak(), BehandlingType.REVURDERING).build();
        behandlingRepository.lagre(nyBehandling, new BehandlingLås(null));

        repository.kopierGrunnlagFraEksisterendeBehandling(behandling.getId(), nyBehandling.getId());
        var ny = lagAvklaring(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28));
        repository.lagreForeslåtteAvklaringer(nyBehandling.getId(), List.of(opprinnelig, ny));

        var avklaringerPåNyBehandling = repository.hentGrunnlagHvisEksisterer(nyBehandling.getId())
            .orElseThrow()
            .getForeslått()
            .getPeriodeAvklaringer();

        assertThat(avklaringerPåNyBehandling)
            .extracting(BostedsPeriodeAvklaring::getReferanse)
            .containsExactlyInAnyOrder(opprinnelig.getReferanse(), ny.getReferanse());

        var avklaringerPåGammelBehandling = repository.hentGrunnlagHvisEksisterer(behandling.getId())
            .orElseThrow()
            .getForeslått()
            .getPeriodeAvklaringer();

        assertThat(avklaringerPåGammelBehandling).hasSize(1);
        assertThat(avklaringerPåGammelBehandling.iterator().next().getReferanse()).isEqualTo(opprinnelig.getReferanse());
    }

    private List<BostedsPeriodeAvklaring> hentSorterteAvklaringer() {
        return repository.hentGrunnlagHvisEksisterer(behandling.getId())
            .orElseThrow()
            .getForeslått()
            .getPeriodeAvklaringer()
            .stream()
            .sorted(Comparator.comparing(a -> a.getPeriode().getFomDato()))
            .toList();
    }

    private static BostedsPeriodeAvklaring lagAvklaring(LocalDate fom, LocalDate tom) {
        return new BostedsPeriodeAvklaring(
            DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom),
            BostedsvilkårIkkeOppfyltÅrsak.IKKE_BOSATTADRESSE_I_TRONDHEIM,
            "begrunnelse",
            false,
            null,
            "begrunnelse for hvorfor det ikke varsles",
            VURDERT_AV,
            VURDERT_TIDSPUNKT,
            tom == null ? Avklaringtype.OPPHØR : Avklaringtype.AVSLAG);
    }
}
