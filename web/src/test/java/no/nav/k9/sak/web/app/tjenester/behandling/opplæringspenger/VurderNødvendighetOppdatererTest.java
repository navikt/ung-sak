package no.nav.k9.sak.web.app.tjenester.behandling.opplæringspenger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.util.List;
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
import no.nav.k9.sak.kontrakt.opplæringspenger.VurderNødvendighetDto;
import no.nav.k9.sak.kontrakt.opplæringspenger.VurderNødvendighetPeriodeDto;
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
        final VurderNødvendighetPeriodeDto periodeDto = new VurderNødvendighetPeriodeDto(true, now.minusMonths(2), now, "test", "livets skole");
        final VurderNødvendighetDto dto = new VurderNødvendighetDto(List.of(periodeDto));
        dto.setBegrunnelse("fordi");

        OppdateringResultat resultat = lagreGrunnlag(dto);
        assertThat(resultat).isNotNull();

        Optional<VurdertOpplæringGrunnlag> grunnlag = vurdertOpplæringRepository.hentAktivtGrunnlagForBehandling(behandling.getId());
        assertThat(grunnlag).isPresent();
        assertThat(grunnlag.get().getVurdertInstitusjonHolder()).isNull();
        assertThat(grunnlag.get().getVurdertOpplæringHolder().getVurdertOpplæring()).hasSize(1);
        VurdertOpplæring vurdertOpplæring = grunnlag.get().getVurdertOpplæringHolder().getVurdertOpplæring().get(0);
        assertThat(vurdertOpplæring.getNødvendigOpplæring()).isEqualTo(periodeDto.isNødvendigOpplæring());
        assertThat(vurdertOpplæring.getPeriode().getFomDato()).isEqualTo(now.minusMonths(2));
        assertThat(vurdertOpplæring.getPeriode().getTomDato()).isEqualTo(now);
        assertThat(vurdertOpplæring.getInstitusjon()).isEqualTo(periodeDto.getInstitusjon());
        assertThat(vurdertOpplæring.getBegrunnelse()).isEqualTo(periodeDto.getBegrunnelse());
    }

    @Test
    public void skalOppdatereVurdertOpplæringGrunnlag() {
        final VurderNødvendighetPeriodeDto periodeDto1 = new VurderNødvendighetPeriodeDto(false, now.minusMonths(2), now, "ikke nødvendig", "Fyrstikkalleen barnehage");
        final VurderNødvendighetDto dto1 = new VurderNødvendighetDto(List.of(periodeDto1));
        lagreGrunnlag(dto1);

        final VurderNødvendighetPeriodeDto periodeDto2 = new VurderNødvendighetPeriodeDto(true, now.minusMonths(1), now, "nødvendig", "Kampen Pub");
        final VurderNødvendighetDto dto2 = new VurderNødvendighetDto(List.of(periodeDto2));
        lagreGrunnlag(dto2);

        Optional<VurdertOpplæringGrunnlag> grunnlag = vurdertOpplæringRepository.hentAktivtGrunnlagForBehandling(behandling.getId());
        assertThat(grunnlag).isPresent();
        assertThat(grunnlag.get().getVurdertOpplæringHolder().getVurdertOpplæring()).hasSize(2);
        var vurdertOpplæring1 = grunnlag.get().getVurdertOpplæringHolder().getVurdertOpplæring().stream().filter(it -> it.getInstitusjon().equals(periodeDto1.getInstitusjon())).findFirst();
        assertThat(vurdertOpplæring1).isPresent();
        assertThat(vurdertOpplæring1.get().getNødvendigOpplæring()).isEqualTo(periodeDto1.isNødvendigOpplæring());
        assertThat(vurdertOpplæring1.get().getPeriode().getFomDato()).isEqualTo(now.minusMonths(2));
        assertThat(vurdertOpplæring1.get().getPeriode().getTomDato()).isEqualTo(now.minusMonths(1).minusDays(1));
        assertThat(vurdertOpplæring1.get().getInstitusjon()).isEqualTo(periodeDto1.getInstitusjon());
        assertThat(vurdertOpplæring1.get().getBegrunnelse()).isEqualTo(periodeDto1.getBegrunnelse());
        var vurdertOpplæring2 = grunnlag.get().getVurdertOpplæringHolder().getVurdertOpplæring().stream().filter(it -> it.getInstitusjon().equals(periodeDto2.getInstitusjon())).findFirst();
        assertThat(vurdertOpplæring2).isPresent();
        assertThat(vurdertOpplæring2.get().getNødvendigOpplæring()).isEqualTo(periodeDto2.isNødvendigOpplæring());
        assertThat(vurdertOpplæring2.get().getPeriode().getFomDato()).isEqualTo(now.minusMonths(1));
        assertThat(vurdertOpplæring2.get().getPeriode().getTomDato()).isEqualTo(now);
        assertThat(vurdertOpplæring2.get().getInstitusjon()).isEqualTo(periodeDto2.getInstitusjon());
        assertThat(vurdertOpplæring2.get().getBegrunnelse()).isEqualTo(periodeDto2.getBegrunnelse());
    }

    @Test
    public void oppdaterGrunnlagSkalKopiereVurdertInstitusjonHolderFraAktivtGrunnlag() {
        final VurderNødvendighetPeriodeDto periodeDto1 = new VurderNødvendighetPeriodeDto(false, now.minusMonths(2), now, "ikke nødvendig", "institusjon");
        final VurderNødvendighetDto dto1 = new VurderNødvendighetDto(List.of(periodeDto1));
        lagreGrunnlag(dto1);
        Optional<VurdertOpplæringGrunnlag> grunnlag1 = vurdertOpplæringRepository.hentAktivtGrunnlagForBehandling(behandling.getId());
        lagreGrunnlag(dto1);
        Optional<VurdertOpplæringGrunnlag> grunnlag2 = vurdertOpplæringRepository.hentAktivtGrunnlagForBehandling(behandling.getId());
        assertThat(grunnlag1).isPresent();
        assertThat(grunnlag2).isPresent();
        assertThat(grunnlag1.get().getVurdertInstitusjonHolder()).isEqualTo(grunnlag2.get().getVurdertInstitusjonHolder());
    }

    @Test
    public void skalIkkeLagreVurdertOpplæringGrunnlagMedOverlappendePerioder() {
        final VurderNødvendighetPeriodeDto periodeDto1 = new VurderNødvendighetPeriodeDto(true, now.minusMonths(2), now, "test", "her");
        final VurderNødvendighetPeriodeDto periodeDto2 = new VurderNødvendighetPeriodeDto(false, now.minusMonths(1), now, "test", "her");
        final VurderNødvendighetDto dto = new VurderNødvendighetDto(List.of(periodeDto1, periodeDto2));

        assertThrows(IllegalArgumentException.class, () ->
            lagreGrunnlag(dto)
        );
        Optional<VurdertOpplæringGrunnlag> grunnlag = vurdertOpplæringRepository.hentAktivtGrunnlagForBehandling(behandling.getId());
        assertThat(grunnlag).isEmpty();
    }

    @Test
    public void skalIkkeLagreVurdertOpplæringGrunnlagMedDuplisertePerioder() {
        final VurderNødvendighetPeriodeDto periodeDto1 = new VurderNødvendighetPeriodeDto(true, now.minusMonths(2), now, "test", "der");
        final VurderNødvendighetPeriodeDto periodeDto2 = new VurderNødvendighetPeriodeDto(false, now.minusMonths(2), now, "test", "der");
        final VurderNødvendighetDto dto = new VurderNødvendighetDto(List.of(periodeDto1, periodeDto2));

        assertThrows(IllegalArgumentException.class, () ->
            lagreGrunnlag(dto)
        );
        Optional<VurdertOpplæringGrunnlag> grunnlag = vurdertOpplæringRepository.hentAktivtGrunnlagForBehandling(behandling.getId());
        assertThat(grunnlag).isEmpty();
    }

    private OppdateringResultat lagreGrunnlag(VurderNødvendighetDto dto) {
        Optional<Aksjonspunkt> aksjonspunkt = behandling.getAksjonspunktFor(dto.getKode());
        AksjonspunktOppdaterParameter param = new AksjonspunktOppdaterParameter(behandling, aksjonspunkt, dto);

        return vurderNødvendighetOppdaterer.oppdater(dto, param);
    }
}
