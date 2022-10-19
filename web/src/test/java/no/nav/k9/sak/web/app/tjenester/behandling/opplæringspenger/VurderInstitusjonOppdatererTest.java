package no.nav.k9.sak.web.app.tjenester.behandling.opplæringspenger;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.k9.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.db.util.CdiDbAwareTest;
import no.nav.k9.sak.kontrakt.opplæringspenger.VurderInstitusjonDto;
import no.nav.k9.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertInstitusjon;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertOpplæringGrunnlag;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertOpplæringRepository;

@CdiDbAwareTest
public class VurderInstitusjonOppdatererTest {

    @Inject
    private VurdertOpplæringRepository vurdertOpplæringRepository;

    @Inject
    public EntityManager entityManager;

    private VurderInstitusjonOppdaterer vurderInstitusjonOppdaterer;
    private Behandling behandling;

    @BeforeEach
    public void setup() {
        BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        vurderInstitusjonOppdaterer = new VurderInstitusjonOppdaterer(vurdertOpplæringRepository);
        TestScenarioBuilder scenario = TestScenarioBuilder.builderMedSøknad();
        scenario.medSøknad().medSøknadsdato(LocalDate.now());
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.VURDER_INSTITUSJON, BehandlingStegType.VURDER_INSTITUSJON_VILKÅR);
        behandling = scenario.lagre(repositoryProvider);
    }

    @Test
    public void skalLagreNyttVurdertInstitusjonGrunnlag() {
        final VurderInstitusjonDto dto = new VurderInstitusjonDto("Livets skole", true, "fordi");

        OppdateringResultat resultat = lagreGrunnlag(dto);
        assertThat(resultat).isNotNull();

        Optional<VurdertOpplæringGrunnlag> grunnlag = vurdertOpplæringRepository.hentAktivtGrunnlagForBehandling(behandling.getId());
        assertThat(grunnlag).isPresent();
        assertThat(grunnlag.get().getVurdertInstitusjonHolder().getVurdertInstitusjon()).hasSize(1);
        assertThat(grunnlag.get().getVurdertOpplæringHolder().getVurdertOpplæring()).hasSize(0);
        VurdertInstitusjon vurdertInstitusjon = grunnlag.get().getVurdertInstitusjonHolder().getVurdertInstitusjon().get(0);
        assertThat(vurdertInstitusjon.getGodkjent()).isEqualTo(dto.isGodkjent());
        assertThat(vurdertInstitusjon.getInstitusjon()).isEqualTo(dto.getInstitusjon());
        assertThat(vurdertInstitusjon.getBegrunnelse()).isEqualTo(dto.getBegrunnelse());
        assertThat(grunnlag.get().getBegrunnelse()).isEqualTo(dto.getBegrunnelse());
    }

    @Test
    public void skalOppdatereVurdertInstitusjonGrunnlag() {
        final VurderInstitusjonDto dto1 = new VurderInstitusjonDto("Fyrstikkalleen barnehage", true, "fordi");
        lagreGrunnlag(dto1);

        final VurderInstitusjonDto dto2 = new VurderInstitusjonDto("Kampen Pub", true, "fordi noe annet");
        lagreGrunnlag(dto2);

        final VurderInstitusjonDto dto3 = new VurderInstitusjonDto("Kampen Pub", false, "nei");
        lagreGrunnlag(dto3);

        Optional<VurdertOpplæringGrunnlag> grunnlag = vurdertOpplæringRepository.hentAktivtGrunnlagForBehandling(behandling.getId());
        assertThat(grunnlag).isPresent();
        assertThat(grunnlag.get().getVurdertInstitusjonHolder().getVurdertInstitusjon()).hasSize(2);
        VurdertInstitusjon vurdertInstitusjon1 = grunnlag.get().getVurdertInstitusjonHolder().getVurdertInstitusjon().get(0);
        assertThat(vurdertInstitusjon1.getGodkjent()).isEqualTo(dto1.isGodkjent());
        assertThat(vurdertInstitusjon1.getInstitusjon()).isEqualTo(dto1.getInstitusjon());
        assertThat(vurdertInstitusjon1.getBegrunnelse()).isEqualTo(dto1.getBegrunnelse());
        VurdertInstitusjon vurdertInstitusjon2 = grunnlag.get().getVurdertInstitusjonHolder().getVurdertInstitusjon().get(1);
        assertThat(vurdertInstitusjon2.getGodkjent()).isEqualTo(dto3.isGodkjent());
        assertThat(vurdertInstitusjon2.getInstitusjon()).isEqualTo(dto3.getInstitusjon());
        assertThat(vurdertInstitusjon2.getBegrunnelse()).isEqualTo(dto3.getBegrunnelse());
        assertThat(grunnlag.get().getBegrunnelse()).isEqualTo(dto3.getBegrunnelse());
    }

    private OppdateringResultat lagreGrunnlag(VurderInstitusjonDto dto) {
        Optional<Aksjonspunkt> aksjonspunkt = behandling.getAksjonspunktFor(dto.getKode());
        AksjonspunktOppdaterParameter param = new AksjonspunktOppdaterParameter(behandling, aksjonspunkt, dto);

        return vurderInstitusjonOppdaterer.oppdater(dto, param);
    }
}
