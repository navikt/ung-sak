package no.nav.ung.sak.behandlingslager.behandling.medlemskap;

import jakarta.inject.Inject;
import no.nav.k9.felles.jpa.TomtResultatException;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.ung.sak.db.util.CdiDbAwareTest;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.JournalpostId;
import no.nav.ung.sak.typer.Saksnummer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@CdiDbAwareTest
class OppgittForutgåendeMedlemskapRepositoryTest {

    private static final JournalpostId JP1 = new JournalpostId("JP1");
    private static final JournalpostId JP2 = new JournalpostId("JP2");

    @Inject
    private FagsakRepository fagsakRepository;

    @Inject
    private BehandlingRepository behandlingRepository;

    @Inject
    private OppgittForutgåendeMedlemskapRepository repository;

    private Behandling behandling;

    @BeforeEach
    void setUp() {
        Fagsak fagsak = Fagsak.opprettNy(FagsakYtelseType.AKTIVITETSPENGER, new AktørId("1"), new Saksnummer("SAK1"), LocalDate.now(), LocalDate.now().plusYears(1).minusDays(1));
        fagsakRepository.opprettNy(fagsak);
        behandling = Behandling.nyBehandlingFor(fagsak, BehandlingType.FØRSTEGANGSSØKNAD).build();
        behandlingRepository.lagre(behandling, new BehandlingLås(null));
    }

    @Test
    void skal_lagre_og_hente_grunnlag_med_utenlandsopphold() {
        LocalDate fom1 = LocalDate.of(2019, 7, 1);
        LocalDate tom1 = LocalDate.of(2022, 12, 31);
        LocalDate fom2 = LocalDate.of(2023, 1, 1);
        LocalDate tom2 = LocalDate.of(2024, 6, 30);
        var utenlandsopphold = Set.of(
            new OppgittUtenlandsopphold(fom1, tom1, "SWE", true, "ABC123"),
            new OppgittUtenlandsopphold(fom2, tom2, "DEU", true, null)
        );

        repository.leggTilOppgittPeriode(behandling.getId(), nyPeriode(JP1, fom1, tom2, utenlandsopphold));

        var grunnlag = repository.hentGrunnlag(behandling.getId());

        assertThat(grunnlag.getOppgittePerioder()).hasSize(1);
        assertThat(grunnlag.isAktiv()).isTrue();
        var periode = grunnlag.getOppgittePerioder().iterator().next();
        assertThat(periode.getPeriode().getFomDato()).isEqualTo(fom1);
        assertThat(periode.getPeriode().getTomDato()).isEqualTo(tom2);
        assertThat(periode.harBoddINorge()).isTrue();
        assertThat(periode.harJobbetINorge()).isNull();
        assertThat(periode.harJobbetUtenforNorge()).isTrue();
        assertThat(periode.getUtenlandsopphold()).hasSize(2);
        assertThat(periode.getJournalpostId()).isEqualTo(JP1);
        assertThat(periode.getUtenlandsopphold()).extracting(OppgittUtenlandsopphold::getPeriode)
            .containsExactlyInAnyOrder(DatoIntervallEntitet.fra(fom1, tom1),
                DatoIntervallEntitet.fra(fom2, tom2));
        assertThat(periode.getUtenlandsopphold()).extracting(OppgittUtenlandsopphold::harJobbetIPerioden)
            .containsExactlyInAnyOrder(true, true);

        assertThat(periode.getUtenlandsopphold()).extracting(OppgittUtenlandsopphold::getUtenlandskNasjonalId)
            .containsExactlyInAnyOrder(null, "ABC123");
    }

    @Test
    void skal_lagre_grunnlag_uten_utenlandsopphold() {
        var fom = LocalDate.of(2019, 7, 1);
        var tom = LocalDate.of(2024, 6, 30);

        repository.leggTilOppgittPeriode(behandling.getId(), nyPeriode(JP1, fom, tom, Set.of()));

        var grunnlag = repository.hentGrunnlag(behandling.getId());

        var periode = grunnlag.getOppgittePerioder().iterator().next();
        assertThat(periode.getUtenlandsopphold()).isEmpty();
        assertThat(periode.harBoddINorge()).isTrue();
        assertThat(periode.harJobbetINorge()).isNull();
        assertThat(periode.harJobbetUtenforNorge()).isFalse();
    }

    @Test
    void skal_legge_til_perioder_ved_ny_søknad_på_samme_behandling() {
        var fom1 = LocalDate.of(2019, 7, 1);
        var tom1 = LocalDate.of(2024, 6, 30);
        repository.leggTilOppgittPeriode(behandling.getId(), nyPeriode(JP1, fom1, tom1, Set.of(new OppgittUtenlandsopphold(fom1, tom1, "SWE", false, null ))));

        var fom2 = LocalDate.of(2020, 1, 1);
        var tom2 = LocalDate.of(2025, 1, 1);
        repository.leggTilOppgittPeriode(behandling.getId(), nyPeriode(JP2, fom2, tom2, Set.of(new OppgittUtenlandsopphold(fom2, tom2, "FIN", false, null ))));

        var grunnlag = repository.hentGrunnlag(behandling.getId());

        assertThat(grunnlag.getOppgittePerioder()).hasSize(2);
    }

    @Test
    void skal_feile_når_ingen_grunnlag_eksisterer() {
        assertThatThrownBy(() -> repository.hentGrunnlag(behandling.getId()))
            .isInstanceOf(TomtResultatException.class);
    }

    @Test
    void skal_kopiere_grunnlag_til_ny_behandling_og_dele_holder() {
        var fom = LocalDate.of(2019, 7, 1);
        var tom = LocalDate.of(2024, 6, 30);
        repository.leggTilOppgittPeriode(behandling.getId(), nyPeriode(JP1, fom, tom,
            Set.of(new OppgittUtenlandsopphold(LocalDate.of(2020, 1, 1),
                LocalDate.of(2024, 6, 30), "DEU", false, null ))));

        Behandling nyBehandling = Behandling.nyBehandlingFor(behandling.getFagsak(), BehandlingType.REVURDERING).build();
        behandlingRepository.lagre(nyBehandling, new BehandlingLås(null));

        repository.kopierGrunnlagFraEksisterendeBehandling(behandling.getId(), nyBehandling.getId());

        var kopiert = repository.hentGrunnlag(nyBehandling.getId());

        assertThat(kopiert.getOppgittePerioder()).hasSize(1);
        var periode = kopiert.getOppgittePerioder().iterator().next();
        assertThat(periode.getPeriode().getFomDato()).isEqualTo(fom);
        assertThat(periode.getUtenlandsopphold()).hasSize(1);

        var original = repository.hentGrunnlag(behandling.getId());
        assertThat(kopiert.getHolder().getId())
            .as("Kopiert grunnlag skal dele samme holder")
            .isEqualTo(original.getHolder().getId());
    }

    @Test
    void skal_kopiere_holder_ved_ny_søknad_på_revurdering_med_delt_holder() {
        repository.leggTilOppgittPeriode(behandling.getId(), nyPeriode(JP1,
            LocalDate.of(2019, 7, 1), LocalDate.of(2024, 6, 30),
            Set.of(new OppgittUtenlandsopphold(
                LocalDate.of(2020, 1, 1),
                LocalDate.of(2024, 6, 30),
                "DEU", false, null ))));

        Behandling revurdering = Behandling.nyBehandlingFor(behandling.getFagsak(), BehandlingType.REVURDERING).build();
        behandlingRepository.lagre(revurdering, new BehandlingLås(null));
        repository.kopierGrunnlagFraEksisterendeBehandling(behandling.getId(), revurdering.getId());

        var holderIdFørNySøknad = repository.hentGrunnlag(revurdering.getId()).getHolder().getId();

        repository.leggTilOppgittPeriode(revurdering.getId(), nyPeriode(JP2, LocalDate.of(2020, 1, 1), LocalDate.of(2025, 1, 1),
            Set.of(new OppgittUtenlandsopphold(LocalDate.of(2020, 1, 1), LocalDate.of(2025, 1, 1), "FIN", false, null ))));

        var revGrunnlag = repository.hentGrunnlag(revurdering.getId());
        assertThat(revGrunnlag.getOppgittePerioder()).hasSize(2);
        assertThat(revGrunnlag.getHolder().getId())
            .as("Ny søknad gir alltid ny holder")
            .isNotEqualTo(holderIdFørNySøknad);

        var origGrunnlag = repository.hentGrunnlag(behandling.getId());
        assertThat(origGrunnlag.getOppgittePerioder()).hasSize(1)
            .as("Original behandling skal ikke påvirkes");
    }

    @Test
    void skal_ikke_kopiere_når_ingen_grunnlag_eksisterer() {
        Behandling nyBehandling = Behandling.nyBehandlingFor(behandling.getFagsak(), BehandlingType.REVURDERING).build();
        behandlingRepository.lagre(nyBehandling, new BehandlingLås(null));

        repository.kopierGrunnlagFraEksisterendeBehandling(behandling.getId(), nyBehandling.getId());

        assertThat(repository.hentGrunnlagHvisEksisterer(nyBehandling.getId())).isEmpty();
    }

    private static OppgittForutgåendeMedlemskapPeriode nyPeriode(
        JournalpostId journalpostId,
        LocalDate fom,
        LocalDate tom,
        Set<OppgittUtenlandsopphold> utenlandsopphold) {
        return OppgittForutgåendeMedlemskapPeriode.builder()
            .medJournalpostId(journalpostId)
            .medFom(fom)
            .medTom(tom)
            .medUtenlandsopphold(utenlandsopphold)
            .medHarBoddINorge(true)
            .medHarJobbetINorge(null)
            .medHarJobbetUtenforNorge(!utenlandsopphold.isEmpty())
            .build();
    }
}
