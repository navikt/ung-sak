package no.nav.ung.sak.behandlingslager.bosatt;

import jakarta.inject.Inject;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.ung.sak.db.util.CdiDbAwareTest;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.Saksnummer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@CdiDbAwareTest
class BostedSøknadsfaktaGrunnlagRepositoryTest {

    private static final LocalDate FOM = LocalDate.of(2026, 1, 1);
    private static final LocalDate TOM = LocalDate.of(2026, 1, 31);

    @Inject
    private FagsakRepository fagsakRepository;

    @Inject
    private BehandlingRepository behandlingRepository;

    @Inject
    private BostedSøknadsfaktaGrunnlagRepository repository;

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
    void skal_erstatte_søknadsinformasjon_når_ny_søknad_kommer_for_samme_virkningsdato() {
        // setUp har allerede lagret jp-1 for FOM (virkningsdato) med erBosattITrondheim=true.
        // Ny søknad for samme virkningsdato med ny journalpostId og endret opplysning skal overskrive den forrige.
        repository.lagreInformasjonFraSøknad(behandling.getId(), "jp-2", FOM, false);

        var informasjon = repository.hentGrunnlagHvisEksisterer(behandling.getId())
            .orElseThrow()
            .getOppgittFraSøknad()
            .getInformasjon();

        assertThat(informasjon).hasSize(1);
        var eneste = informasjon.iterator().next();
        assertThat(eneste.getJournalpostId()).isEqualTo("jp-2");
        assertThat(eneste.getFomDato()).isEqualTo(FOM);
        assertThat(eneste.isErBosattITrondheim()).isFalse();
    }
}
