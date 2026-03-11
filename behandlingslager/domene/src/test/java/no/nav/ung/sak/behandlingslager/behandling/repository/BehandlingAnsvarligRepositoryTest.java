package no.nav.ung.sak.behandlingslager.behandling.repository;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.ung.kodeverk.behandling.BehandlingStatus;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandlingslager.behandling.BasicBehandlingBuilder;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.ung.sak.db.util.CdiDbAwareTest;
import no.nav.ung.sak.typer.AktørId;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@CdiDbAwareTest
class BehandlingAnsvarligRepositoryTest {

    @Inject
    private EntityManager entityManager;
    @Inject
    private FagsakRepository fagsakRepository;
    @Inject
    private BehandlingRepository behandlingRepository;
    @Inject
    private BehandlingAnsvarligRepository behandlingAnsvarligRepository;

    private BasicBehandlingBuilder behandlingBuilder;

    private AktørId aktørId = AktørId.dummy();
    private Fagsak fagsak;

    @BeforeEach
    void setUp() {
        behandlingBuilder = new BasicBehandlingBuilder(entityManager);
        fagsak = behandlingBuilder.opprettFagsak(FagsakYtelseType.UNGDOMSYTELSE, aktørId);
    }

    @Test
    void skal_lagre_og_hente_ansvarligSaksbehandler() {
        var behandling = behandlingBuilder.opprettOgLagreFørstegangssøknad(fagsak, BehandlingStatus.UTREDES);
        behandlingAnsvarligRepository.setAnsvarligSaksbehandler(behandling.getId(), "Z000000");

        Assertions.assertThat(behandlingAnsvarligRepository.hentAnsvarligSaksbehandler(behandling.getId())).isEqualTo("Z000000");
    }
}
