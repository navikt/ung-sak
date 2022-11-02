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
import no.nav.k9.sak.kontrakt.dokument.JournalpostIdDto;
import no.nav.k9.sak.kontrakt.opplæringspenger.VurderNødvendighetDto;
import no.nav.k9.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertOpplæring;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertOpplæringGrunnlag;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertOpplæringRepository;

@CdiDbAwareTest
public class VurderNødvendighetOppdatererTest {

    @Inject
    private VurdertOpplæringRepository vurdertOpplæringRepository;

    @Inject
    public EntityManager entityManager;

    private VurderNødvendighetOppdaterer vurderNødvendighetOppdaterer;
    private LocalDate now;
    private Behandling behandling;

    @BeforeEach
    public void setup() {
        BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        vurderNødvendighetOppdaterer = new VurderNødvendighetOppdaterer(vurdertOpplæringRepository);
        now = LocalDate.now();
        TestScenarioBuilder scenario = TestScenarioBuilder.builderMedSøknad();
        scenario.medSøknad().medSøknadsdato(now);
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.VURDER_NØDVENDIGHET, BehandlingStegType.VURDER_NØDVENDIGHETS_VILKÅR);
        behandling = scenario.lagre(repositoryProvider);
    }

    @Test
    public void skalLagreNyttVurdertOpplæringGrunnlag() {
        final JournalpostIdDto journalpostIdDto = new JournalpostIdDto("1337");
        final VurderNødvendighetDto dto = new VurderNødvendighetDto(journalpostIdDto, true);
        dto.setBegrunnelse("fordi");

        OppdateringResultat resultat = lagreGrunnlag(dto);
        assertThat(resultat).isNotNull();

        Optional<VurdertOpplæringGrunnlag> grunnlag = vurdertOpplæringRepository.hentAktivtGrunnlagForBehandling(behandling.getId());
        assertThat(grunnlag).isPresent();
        assertThat(grunnlag.get().getVurdertInstitusjonHolder()).isNull();
        assertThat(grunnlag.get().getVurdertOpplæringHolder().getVurdertOpplæring()).hasSize(1);
        VurdertOpplæring vurdertOpplæring = grunnlag.get().getVurdertOpplæringHolder().getVurdertOpplæring().get(0);
        assertThat(vurdertOpplæring.getNødvendigOpplæring()).isEqualTo(dto.isNødvendigOpplæring());
        assertThat(vurdertOpplæring.getBegrunnelse()).isEqualTo(dto.getBegrunnelse());
    }

    @Test
    public void skalOppdatereVurdertOpplæringGrunnlag() {
        final JournalpostIdDto journalpostIdDto = new JournalpostIdDto("1338");
        final VurderNødvendighetDto dto1 = new VurderNødvendighetDto(journalpostIdDto, false);
        lagreGrunnlag(dto1);

        final VurderNødvendighetDto dto2 = new VurderNødvendighetDto(journalpostIdDto, true);
        lagreGrunnlag(dto2);

        Optional<VurdertOpplæringGrunnlag> grunnlag = vurdertOpplæringRepository.hentAktivtGrunnlagForBehandling(behandling.getId());
        assertThat(grunnlag).isPresent();
        assertThat(grunnlag.get().getVurdertOpplæringHolder().getVurdertOpplæring()).hasSize(1);
        var vurdertOpplæring = grunnlag.get().getVurdertOpplæringHolder().getVurdertOpplæring().get(0);
        assertThat(vurdertOpplæring.getNødvendigOpplæring()).isEqualTo(dto2.isNødvendigOpplæring());
    }

    @Test
    public void skalLagreGrunnlagOgKopiereFraAktivtForAnnenJornalpostId() {
        final JournalpostIdDto journalpostIdDto1 = new JournalpostIdDto("1339");
        final VurderNødvendighetDto dto1 = new VurderNødvendighetDto(journalpostIdDto1, true);
        lagreGrunnlag(dto1);

        final JournalpostIdDto journalpostIdDto2 = new JournalpostIdDto("1340");
        final VurderNødvendighetDto dto2 = new VurderNødvendighetDto(journalpostIdDto2, true);
        lagreGrunnlag(dto2);

        Optional<VurdertOpplæringGrunnlag> grunnlag = vurdertOpplæringRepository.hentAktivtGrunnlagForBehandling(behandling.getId());
        assertThat(grunnlag).isPresent();
        assertThat(grunnlag.get().getVurdertOpplæringHolder().getVurdertOpplæring()).hasSize(2);
        var vurdertOpplæring1 = grunnlag.get().getVurdertOpplæringHolder().getVurdertOpplæring().stream().filter(vurdertOpplæring -> vurdertOpplæring.getJournalpostId().equals(journalpostIdDto1.getJournalpostId())).findFirst();
        assertThat(vurdertOpplæring1).isPresent();
        var vurdertOpplæring2 = grunnlag.get().getVurdertOpplæringHolder().getVurdertOpplæring().stream().filter(vurdertOpplæring -> vurdertOpplæring.getJournalpostId().equals(journalpostIdDto2.getJournalpostId())).findFirst();
        assertThat(vurdertOpplæring2).isPresent();
    }

    private OppdateringResultat lagreGrunnlag(VurderNødvendighetDto dto) {
        Optional<Aksjonspunkt> aksjonspunkt = behandling.getAksjonspunktFor(dto.getKode());
        AksjonspunktOppdaterParameter param = new AksjonspunktOppdaterParameter(behandling, aksjonspunkt, dto);

        return vurderNødvendighetOppdaterer.oppdater(dto, param);
    }
}
