package no.nav.ung.sak.behandlingslager.behandling.repository;

import java.time.LocalDateTime;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import org.assertj.core.api.Assertions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.ung.kodeverk.behandling.BehandlingStatus;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.Venteårsak;
import no.nav.ung.sak.behandlingslager.behandling.BasicBehandlingBuilder;
import no.nav.ung.sak.behandlingslager.behandling.aksjonspunkt.AksjonspunktTestSupport;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
public class BehandlingKandidaterRepositoryTest {

    @Inject
    private EntityManager entityManager;

    @Inject
    private BehandlingKandidaterRepository sutRepo;

    private AksjonspunktTestSupport aksjonspunktTestSupport = new AksjonspunktTestSupport();

    @Inject
    private BehandlingRepository behandlingRepository;

    private BasicBehandlingBuilder behandlingBuilder;

    @BeforeEach
    public void setup(){
        behandlingBuilder = new BasicBehandlingBuilder(entityManager);
    }

    @Test
    public void skal_finne_en_kandidat_for_automatisk_gjenopptagelse() throws Exception {
        // Arrange
        var behandling = behandlingBuilder.opprettOgLagreFørstegangssøknad(FagsakYtelseType.FORELDREPENGER, BehandlingStatus.UTREDES);
        var aksjonspunkt = aksjonspunktTestSupport.leggTilAksjonspunkt(behandling, AksjonspunktDefinisjon.AUTO_MANUELT_SATT_PÅ_VENT);
        aksjonspunktTestSupport.setFrist(aksjonspunkt, LocalDateTime.now().minusMinutes(1), Venteårsak.FOR_TIDLIG_SOKNAD, "Altfortidlig");
        behandlingRepository.lagre(behandling);

        // Act
        var behandlinger = sutRepo.finnBehandlingerForAutomatiskGjenopptagelse();

        // Assert
        Assertions.assertThat(behandlinger).containsOnly(behandling);

    }
}
