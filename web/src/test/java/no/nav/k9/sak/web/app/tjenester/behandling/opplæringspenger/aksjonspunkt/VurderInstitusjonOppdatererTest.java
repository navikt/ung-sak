package no.nav.k9.sak.web.app.tjenester.behandling.opplæringspenger.aksjonspunkt;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
        final VurderInstitusjonDto dto = new VurderInstitusjonDto(new JournalpostIdDto("123"), true, "fordi");

        OppdateringResultat resultat = lagreGrunnlag(dto);
        assertThat(resultat).isNotNull();

        Optional<VurdertOpplæringGrunnlag> grunnlag = vurdertOpplæringRepository.hentAktivtGrunnlagForBehandling(behandling.getId());
        assertThat(grunnlag).isPresent();
        assertThat(grunnlag.get().getVurdertInstitusjonHolder().getVurdertInstitusjon()).hasSize(1);
        assertThat(grunnlag.get().getVurdertOpplæringHolder()).isNull();
        VurdertInstitusjon vurdertInstitusjon = grunnlag.get().getVurdertInstitusjonHolder().getVurdertInstitusjon().get(0);
        assertThat(vurdertInstitusjon.getGodkjent()).isEqualTo(dto.isGodkjent());
        assertThat(vurdertInstitusjon.getJournalpostId()).isEqualTo(dto.getJournalpostId().getJournalpostId());
        assertThat(vurdertInstitusjon.getBegrunnelse()).isEqualTo(dto.getBegrunnelse());
        assertThat(vurdertInstitusjon.getVurdertAv()).isEqualTo("VL");
        assertThat(vurdertInstitusjon.getVurdertTidspunkt()).isNotNull();
    }

    @Test
    public void skalOppdatereVurdertInstitusjonGrunnlag() {
        final VurderInstitusjonDto dto1 = new VurderInstitusjonDto(new JournalpostIdDto("123"), true, "fordi");
        lagreGrunnlag(dto1);

        final VurderInstitusjonDto dto2 = new VurderInstitusjonDto(new JournalpostIdDto("321"), true, "fordi noe annet");
        lagreGrunnlag(dto2);

        final VurderInstitusjonDto dto3 = new VurderInstitusjonDto(new JournalpostIdDto("321"), false, "nei");
        lagreGrunnlag(dto3);

        Optional<VurdertOpplæringGrunnlag> grunnlag = vurdertOpplæringRepository.hentAktivtGrunnlagForBehandling(behandling.getId());
        assertThat(grunnlag).isPresent();
        assertThat(grunnlag.get().getVurdertInstitusjonHolder().getVurdertInstitusjon()).hasSize(2);
        var vurdertInstitusjon1 = grunnlag.get().getVurdertInstitusjonHolder().getVurdertInstitusjon().stream().filter(it -> it.getJournalpostId().equals(dto1.getJournalpostId().getJournalpostId())).findFirst();
        assertThat(vurdertInstitusjon1).isPresent();
        assertThat(vurdertInstitusjon1.get().getGodkjent()).isEqualTo(dto1.isGodkjent());
        assertThat(vurdertInstitusjon1.get().getBegrunnelse()).isEqualTo(dto1.getBegrunnelse());
        var vurdertInstitusjon2 = grunnlag.get().getVurdertInstitusjonHolder().getVurdertInstitusjon().stream().filter(it -> it.getJournalpostId().equals(dto2.getJournalpostId().getJournalpostId())).findFirst();
        assertThat(vurdertInstitusjon2).isPresent();
        assertThat(vurdertInstitusjon2.get().getGodkjent()).isEqualTo(dto3.isGodkjent());
        assertThat(vurdertInstitusjon2.get().getJournalpostId()).isEqualTo(dto3.getJournalpostId().getJournalpostId());
        assertThat(vurdertInstitusjon2.get().getBegrunnelse()).isEqualTo(dto3.getBegrunnelse());
        assertThat(vurdertInstitusjon1.get().getVurdertTidspunkt()).isNotEqualTo(vurdertInstitusjon2.get().getVurdertTidspunkt());
    }

    private OppdateringResultat lagreGrunnlag(VurderInstitusjonDto dto) {
        Optional<Aksjonspunkt> aksjonspunkt = behandling.getAksjonspunktFor(dto.getKode());
        AksjonspunktOppdaterParameter param = new AksjonspunktOppdaterParameter(behandling, aksjonspunkt, dto);

        return vurderInstitusjonOppdaterer.oppdater(dto, param);
    }
}
