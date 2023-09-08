package no.nav.k9.sak.behandlingslager.behandling.uttak;

import java.time.LocalDate;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.db.util.CdiDbAwareTest;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Saksnummer;

@CdiDbAwareTest
class UttakNyeReglerRepositoryTest {

    public static final LocalDate IDAG = LocalDate.now();
    public static final LocalDate IGÅR = IDAG.minusDays(1);
    @Inject
    private FagsakRepository fagsakRepository;
    @Inject
    private BehandlingRepository behandlingRepository;
    @Inject
    private UttakNyeReglerRepository uttakNyeReglerRepository;
    @Inject
    private EntityManager entityManager;

    @Test
    void skal_lagre_og_hente_opp_resultat() {
        Fagsak fagsak = lagFagsak();
        Long behandlingId = lagBehandling(fagsak);

        Assertions.assertThat(uttakNyeReglerRepository.finnDatoForNyeRegler(behandlingId)).isEmpty();
        uttakNyeReglerRepository.lagreDatoForNyeRegler(behandlingId, IDAG);
        entityManager.flush();
        entityManager.clear();

        Assertions.assertThat(uttakNyeReglerRepository.finnDatoForNyeRegler(behandlingId)).isPresent();
        Assertions.assertThat(uttakNyeReglerRepository.finnDatoForNyeRegler(behandlingId)).get().isEqualTo(IDAG);
    }

    @Test
    void skal_oppdatere_dato() {
        Fagsak fagsak = lagFagsak();
        Long behandlingId = lagBehandling(fagsak);

        uttakNyeReglerRepository.lagreDatoForNyeRegler(behandlingId, IDAG);
        entityManager.flush();
        entityManager.clear();

        uttakNyeReglerRepository.lagreDatoForNyeRegler(behandlingId, IGÅR);
        entityManager.flush();
        entityManager.clear();

        Assertions.assertThat(uttakNyeReglerRepository.finnDatoForNyeRegler(behandlingId)).isPresent();
        Assertions.assertThat(uttakNyeReglerRepository.finnDatoForNyeRegler(behandlingId)).get().isEqualTo(IGÅR);
    }

    @Test
    void skal_kopiere_fra_en_behandling_til_en_annen() {
        Fagsak fagsak = lagFagsak();
        Long behandlingId = lagBehandling(fagsak);
        uttakNyeReglerRepository.lagreDatoForNyeRegler(behandlingId, IGÅR);
        entityManager.flush();
        entityManager.clear();

        Long revurderingId = lagRevurderingBehandling(behandlingRepository.hentBehandling(behandlingId));

        uttakNyeReglerRepository.kopierGrunnlagFraEksisterendeBehandling(behandlingId, revurderingId);
        entityManager.flush();
        entityManager.clear();

        Assertions.assertThat(uttakNyeReglerRepository.finnDatoForNyeRegler(revurderingId)).isPresent();
        Assertions.assertThat(uttakNyeReglerRepository.finnDatoForNyeRegler(revurderingId)).get().isEqualTo(IGÅR);
    }

    private Long lagBehandling(Fagsak fagsak) {
        Behandling.Builder builder = Behandling.forFørstegangssøknad(fagsak);
        Behandling behandling = builder.build();
        return behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
    }

    private Long lagRevurderingBehandling(Behandling forrigeBehandling) {
        Behandling.Builder builder = Behandling.fraTidligereBehandling(forrigeBehandling, BehandlingType.REVURDERING);
        Behandling behandling = builder.build();
        return behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
    }

    private Fagsak lagFagsak() {
        Fagsak fagsak = Fagsak.opprettNy(FagsakYtelseType.PLEIEPENGER_SYKT_BARN, AktørId.dummy(), new Saksnummer("AAAAA"), IDAG, IDAG);
        fagsakRepository.opprettNy(fagsak);
        return fagsak;
    }

}
