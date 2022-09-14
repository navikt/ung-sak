package no.nav.k9.sak.web.app.tjenester.behandling.opplæringspenger;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Collections;
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
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.VURDER_INSTITUSJON_OG_NØDVENDIGHET, BehandlingStegType.VURDER_NØDVENDIGHETS_VILKÅR);
        behandling = scenario.lagre(repositoryProvider);
    }

    @Test
    public void skalLagreNyttVurdertOpplæringGrunnlag() {
        final VurderNødvendighetPeriodeDto periodeDto = new VurderNødvendighetPeriodeDto(true, now.minusMonths(2), now);
        final VurderNødvendighetDto dto = new VurderNødvendighetDto(true, Collections.singletonList(periodeDto));
        dto.setBegrunnelse("fordi");

        OppdateringResultat resultat = lagreGrunnlag(dto);
        assertThat(resultat).isNotNull();
        assertThat(resultat.getNesteSteg()).isEqualTo(BehandlingStegType.VURDER_NØDVENDIGHETS_VILKÅR);
        assertThat(resultat.getSkalRekjøreSteg()).isTrue();

        Optional<VurdertOpplæringGrunnlag> grunnlag = vurdertOpplæringRepository.hentAktivtGrunnlagForBehandling(behandling.getId());
        assertThat(grunnlag).isPresent();
        assertThat(grunnlag.get().getGodkjentInstitusjon()).isEqualTo(dto.isGodkjentInstitusjon());
        assertThat(grunnlag.get().getVurdertOpplæring()).hasSize(1);
        assertThat(grunnlag.get().getBegrunnelse()).isEqualTo(dto.getBegrunnelse());
        VurdertOpplæring vurdertOpplæring = grunnlag.get().getVurdertOpplæring().get(0);
        assertThat(vurdertOpplæring.getNødvendigOpplæring()).isEqualTo(periodeDto.isNødvendigOpplæring());
        assertThat(vurdertOpplæring.getPeriode().getFomDato()).isEqualTo(now.minusMonths(2));
        assertThat(vurdertOpplæring.getPeriode().getTomDato()).isEqualTo(now);
    }

    @Test
    public void skalOppdatereVurdertOpplæringGrunnlag() {
        final VurderNødvendighetPeriodeDto periodeDto1 = new VurderNødvendighetPeriodeDto(false, now.minusMonths(2), now);
        final VurderNødvendighetDto dto1 = new VurderNødvendighetDto(false, Collections.singletonList(periodeDto1));
        lagreGrunnlag(dto1);

        final VurderNødvendighetPeriodeDto periodeDto2 = new VurderNødvendighetPeriodeDto(true, now.minusMonths(1), now);
        final VurderNødvendighetDto dto2 = new VurderNødvendighetDto(true, Collections.singletonList(periodeDto2));
        lagreGrunnlag(dto2);

        Optional<VurdertOpplæringGrunnlag> grunnlag = vurdertOpplæringRepository.hentAktivtGrunnlagForBehandling(behandling.getId());
        assertThat(grunnlag).isPresent();
        assertThat(grunnlag.get().getGodkjentInstitusjon()).isEqualTo(dto2.isGodkjentInstitusjon());
        assertThat(grunnlag.get().getVurdertOpplæring()).hasSize(2);
        VurdertOpplæring vurdertOpplæring1 = grunnlag.get().getVurdertOpplæring().get(0);
        assertThat(vurdertOpplæring1.getNødvendigOpplæring()).isEqualTo(periodeDto1.isNødvendigOpplæring());
        assertThat(vurdertOpplæring1.getPeriode().getFomDato()).isEqualTo(now.minusMonths(2));
        assertThat(vurdertOpplæring1.getPeriode().getTomDato()).isEqualTo(now.minusMonths(1).minusDays(1));
        VurdertOpplæring vurdertOpplæring2 = grunnlag.get().getVurdertOpplæring().get(1);
        assertThat(vurdertOpplæring2.getNødvendigOpplæring()).isEqualTo(periodeDto2.isNødvendigOpplæring());
        assertThat(vurdertOpplæring2.getPeriode().getFomDato()).isEqualTo(now.minusMonths(1));
        assertThat(vurdertOpplæring2.getPeriode().getTomDato()).isEqualTo(now);
    }

    private OppdateringResultat lagreGrunnlag(VurderNødvendighetDto dto) {
        Optional<Aksjonspunkt> aksjonspunkt = behandling.getAksjonspunktFor(dto.getKode());
        AksjonspunktOppdaterParameter param = new AksjonspunktOppdaterParameter(behandling, aksjonspunkt, dto);

        return vurderNødvendighetOppdaterer.oppdater(dto, param);
    }
}
