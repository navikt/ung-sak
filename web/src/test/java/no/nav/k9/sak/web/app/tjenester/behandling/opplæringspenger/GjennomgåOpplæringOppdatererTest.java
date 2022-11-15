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
import no.nav.k9.sak.kontrakt.opplæringspenger.VurderGjennomgåttOpplæringDto;
import no.nav.k9.sak.kontrakt.opplæringspenger.VurderGjennomgåttOpplæringPeriodeDto;
import no.nav.k9.sak.kontrakt.opplæringspenger.VurderReisetidDto;
import no.nav.k9.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertOpplæringPeriode;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertOpplæringRepository;

@CdiDbAwareTest
class GjennomgåOpplæringOppdatererTest {

    @Inject
    private VurdertOpplæringRepository vurdertOpplæringRepository;

    @Inject
    public EntityManager entityManager;

    private GjennomgåOpplæringOppdaterer gjennomgåOpplæringOppdaterer;
    private Behandling behandling;
    private final LocalDate idag = LocalDate.now();

    @BeforeEach
    void setup() {
        BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        gjennomgåOpplæringOppdaterer = new GjennomgåOpplæringOppdaterer(vurdertOpplæringRepository);
        TestScenarioBuilder scenario = TestScenarioBuilder.builderMedSøknad();
        scenario.medSøknad().medSøknadsdato(idag);
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.VURDER_GJENNOMGÅTT_OPPLÆRING, BehandlingStegType.VURDER_GJENNOMGÅTT_OPPLÆRING);
        behandling = scenario.lagre(repositoryProvider);
    }

    @Test
    void skalLagreNyttGrunnlag() {
        var reisetidDto = new VurderReisetidDto(new Periode(idag.minusDays(1), idag.minusDays(1)), new Periode(idag.plusDays(1), idag.plusDays(1)), "reise");
        var periodeDto = new VurderGjennomgåttOpplæringPeriodeDto(idag, idag, true, reisetidDto, "test");
        var dto = new VurderGjennomgåttOpplæringDto(List.of(periodeDto));

        var resultat = lagreGrunnlag(dto);
        assertThat(resultat).isNotNull();

        var grunnlag = vurdertOpplæringRepository.hentAktivtGrunnlagForBehandling(behandling.getId());
        assertThat(grunnlag).isPresent();
        assertThat(grunnlag.get().getVurdertePerioder()).isNotNull();
        assertThat(grunnlag.get().getVurdertePerioder().getPerioder()).hasSize(1);
        VurdertOpplæringPeriode periodeFraGrunnlag = grunnlag.get().getVurdertePerioder().getPerioder().get(0);
        assertThat(periodeFraGrunnlag.getPeriode().getFomDato()).isEqualTo(periodeDto.getPeriode().getFom());
        assertThat(periodeFraGrunnlag.getPeriode().getTomDato()).isEqualTo(periodeDto.getPeriode().getTom());
        assertThat(periodeFraGrunnlag.getGjennomførtOpplæring()).isEqualTo(periodeDto.getGjennomførtOpplæring());
        assertThat(periodeFraGrunnlag.getBegrunnelse()).isEqualTo(periodeDto.getBegrunnelse());
        assertThat(periodeFraGrunnlag.getReisetid()).isNotNull();
        assertThat(periodeFraGrunnlag.getReisetid().getReiseperiodeTil().getFomDato()).isEqualTo(reisetidDto.getReisetidTil().getFom());
        assertThat(periodeFraGrunnlag.getReisetid().getReiseperiodeTil().getTomDato()).isEqualTo(reisetidDto.getReisetidTil().getTom());
        assertThat(periodeFraGrunnlag.getReisetid().getReiseperiodeHjem().getFomDato()).isEqualTo(reisetidDto.getReisetidHjem().getFom());
        assertThat(periodeFraGrunnlag.getReisetid().getReiseperiodeHjem().getTomDato()).isEqualTo(reisetidDto.getReisetidHjem().getTom());
        assertThat(periodeFraGrunnlag.getReisetid().getBegrunnelse()).isEqualTo(reisetidDto.getBegrunnelse());
    }

    @Test
    void skalOppdatereGrunnlag() {
        var periodeDto1 = new VurderGjennomgåttOpplæringPeriodeDto(idag, idag, false, null, "test1");
        var dto1 = new VurderGjennomgåttOpplæringDto(List.of(periodeDto1));
        lagreGrunnlag(dto1);
        var reisetidDto = new VurderReisetidDto(new Periode(idag.minusDays(1), idag.minusDays(1)), null, "reise");
        var periodeDto2 = new VurderGjennomgåttOpplæringPeriodeDto(idag, idag.plusDays(1), true, reisetidDto, "test2");
        var dto2 = new VurderGjennomgåttOpplæringDto(List.of(periodeDto2));
        lagreGrunnlag(dto2);

        var grunnlag = vurdertOpplæringRepository.hentAktivtGrunnlagForBehandling(behandling.getId());
        assertThat(grunnlag).isPresent();
        assertThat(grunnlag.get().getVurdertePerioder()).isNotNull();
        assertThat(grunnlag.get().getVurdertePerioder().getPerioder()).hasSize(1);
        VurdertOpplæringPeriode periodeFraGrunnlag = grunnlag.get().getVurdertePerioder().getPerioder().get(0);
        assertThat(periodeFraGrunnlag.getPeriode().getFomDato()).isEqualTo(periodeDto2.getPeriode().getFom());
        assertThat(periodeFraGrunnlag.getPeriode().getTomDato()).isEqualTo(periodeDto2.getPeriode().getTom());
        assertThat(periodeFraGrunnlag.getGjennomførtOpplæring()).isEqualTo(periodeDto2.getGjennomførtOpplæring());
        assertThat(periodeFraGrunnlag.getBegrunnelse()).isEqualTo(periodeDto2.getBegrunnelse());
        assertThat(periodeFraGrunnlag.getReisetid()).isNotNull();
    }

    @Test
    void skalKopiereFraAktivtGrunnlag() {
        var reisetidDto = new VurderReisetidDto(new Periode(idag.minusDays(1), idag.minusDays(1)), null, "reise");
        var periodeDto1 = new VurderGjennomgåttOpplæringPeriodeDto(idag, idag, false, reisetidDto, "test");
        var dto1 = new VurderGjennomgåttOpplæringDto(List.of(periodeDto1));
        lagreGrunnlag(dto1);
        var periodeDto2 = new VurderGjennomgåttOpplæringPeriodeDto(idag.plusDays(1), idag.plusDays(1), true, null, "test");
        var dto2 = new VurderGjennomgåttOpplæringDto(List.of(periodeDto2));
        lagreGrunnlag(dto2);

        var grunnlag = vurdertOpplæringRepository.hentAktivtGrunnlagForBehandling(behandling.getId());
        assertThat(grunnlag).isPresent();
        assertThat(grunnlag.get().getVurdertePerioder()).isNotNull();
        assertThat(grunnlag.get().getVurdertePerioder().getPerioder()).hasSize(2);
        var perioderFraGrunnlag1 = grunnlag.get().getVurdertePerioder().getPerioder().stream().filter(perioder -> perioder.getPeriode().getFomDato().equals(periodeDto1.getPeriode().getFom())).findFirst();
        var perioderFraGrunnlag2 = grunnlag.get().getVurdertePerioder().getPerioder().stream().filter(perioder -> perioder.getPeriode().getFomDato().equals(periodeDto2.getPeriode().getFom())).findFirst();
        assertThat(perioderFraGrunnlag1).isPresent();
        assertThat(perioderFraGrunnlag2).isPresent();
        assertThat(perioderFraGrunnlag1.get().getReisetid()).isNotNull();
        assertThat(perioderFraGrunnlag2.get().getReisetid()).isNull();
    }

    @Test
    void overlappendePerioderSkalFeile() {
        var periodeDto1 = new VurderGjennomgåttOpplæringPeriodeDto(idag, idag, true, null, "");
        var periodeDto2 = new VurderGjennomgåttOpplæringPeriodeDto(idag, idag.plusDays(1), true, null, "");
        var dto = new VurderGjennomgåttOpplæringDto(List.of(periodeDto1, periodeDto2));

        assertThrows(IllegalArgumentException.class, () -> lagreGrunnlag(dto));
    }

    @Test
    void overlappendeReisetidSkalFeile() {
        var reisetidDto = new VurderReisetidDto(new Periode(idag.minusDays(1), idag), null, "");
        var periodeDto1 = new VurderGjennomgåttOpplæringPeriodeDto(idag, idag, true, reisetidDto, "");
        var dto = new VurderGjennomgåttOpplæringDto(List.of(periodeDto1));

        assertThrows(IllegalArgumentException.class, () -> lagreGrunnlag(dto));
    }

    private OppdateringResultat lagreGrunnlag(VurderGjennomgåttOpplæringDto dto) {
        Optional<Aksjonspunkt> aksjonspunkt = behandling.getAksjonspunktFor(dto.getKode());
        AksjonspunktOppdaterParameter param = new AksjonspunktOppdaterParameter(behandling, aksjonspunkt, dto);

        return gjennomgåOpplæringOppdaterer.oppdater(dto, param);
    }
}
