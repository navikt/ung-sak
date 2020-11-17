package no.nav.k9.sak.behandlingslager.behandling.repository;

import java.time.LocalDateTime;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.assertj.core.api.Assertions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.k9.kodeverk.behandling.BehandlingStatus;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.Venteårsak;
import no.nav.k9.sak.behandlingslager.behandling.BasicBehandlingBuilder;
import no.nav.k9.sak.behandlingslager.behandling.aksjonspunkt.AksjonspunktTestSupport;
import no.nav.k9.sak.db.util.JpaExtension;
import no.nav.vedtak.felles.testutilities.cdi.CdiAwareExtension;

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
