package no.nav.k9.sak.behandlingslager.behandling.merknad;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.k9.kodeverk.behandling.BehandlingStatus;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandlingslager.behandling.BasicBehandlingBuilder;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.db.util.JpaExtension;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
class BehandlingMerknadRepositoryTest {

    @Inject
    private EntityManager entityManager;
    @Inject
    private BehandlingRepository behandlingRepository;
    @Inject
    private BehandlingMerknadRepository behandlingMerknadRepository;

    private BasicBehandlingBuilder behandlingBuilder;

    @BeforeEach
    public void setup() {
        behandlingBuilder = new BasicBehandlingBuilder(entityManager);
    }

    @Test
    void skal_kunne_lagre_og_hente_merknad_på_en_behandling() {
        var fagsak = behandlingBuilder.opprettFagsak(FagsakYtelseType.OMSORGSPENGER);
        var behandling = behandlingBuilder.opprettOgLagreFørstegangssøknad(fagsak, BehandlingStatus.UTREDES);
        BehandlingLås behandlingLås = behandlingRepository.taSkriveLås(behandling);
        behandlingRepository.lagre(behandling, behandlingLås);

        assertThat(behandlingMerknadRepository.hentBehandlingMerknad(behandling.getId())).isEmpty();

        behandlingMerknadRepository.registrerMerknadtyper(behandling.getId(), Set.of(BehandlingMerknadType.HASTESAK), "fritekst her");

        Optional<BehandlingMerknad> merknad = behandlingMerknadRepository.hentBehandlingMerknad(behandling.getId());
        assertThat(merknad).isPresent();
        assertThat(merknad.get().merknadTyper()).isEqualTo(Set.of(BehandlingMerknadType.HASTESAK));
        assertThat(merknad.get().fritekst()).isEqualTo("fritekst her");
    }

}
