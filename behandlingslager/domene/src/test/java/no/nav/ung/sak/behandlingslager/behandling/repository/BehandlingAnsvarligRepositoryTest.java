package no.nav.ung.sak.behandlingslager.behandling.repository;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.ung.kodeverk.behandling.BehandlingDel;
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
    void skal_lagre_og_hente_ansvarligSaksbehandlere_og_besluttere() {
        var behandling = behandlingBuilder.opprettOgLagreFørstegangssøknad(fagsak, BehandlingStatus.UTREDES);
        behandlingAnsvarligRepository.setAnsvarligSaksbehandler(behandling.getId(), BehandlingDel.LOKAL, "Z000000");
        behandlingAnsvarligRepository.setAnsvarligSaksbehandler(behandling.getId(), BehandlingDel.SENTRAL, "Z000001");
        behandlingAnsvarligRepository.setAnsvarligBeslutter(behandling.getId(), BehandlingDel.LOKAL, "B000000");
        behandlingAnsvarligRepository.setAnsvarligBeslutter(behandling.getId(), BehandlingDel.SENTRAL, "B000001");
        Assertions.assertThat(behandlingAnsvarligRepository.hentAnsvarligSaksbehandler(behandling.getId(), BehandlingDel.LOKAL)).isEqualTo("Z000000");
        Assertions.assertThat(behandlingAnsvarligRepository.hentAnsvarligSaksbehandler(behandling.getId(), BehandlingDel.SENTRAL)).isEqualTo("Z000001");
        Assertions.assertThat(behandlingAnsvarligRepository.hentBehandlingAnsvarlig(behandling.getId(), BehandlingDel.LOKAL).get().getAnsvarligBeslutter()).isEqualTo("B000000");
        Assertions.assertThat(behandlingAnsvarligRepository.hentBehandlingAnsvarlig(behandling.getId(), BehandlingDel.SENTRAL).get().getAnsvarligBeslutter()).isEqualTo("B000001");

    }
}
