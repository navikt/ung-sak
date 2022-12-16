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
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.kontrakt.opplæringspenger.ReisetidDto;
import no.nav.k9.sak.kontrakt.opplæringspenger.ReisetidPeriodeDto;
import no.nav.k9.sak.kontrakt.opplæringspenger.VurderReisetidDto;
import no.nav.k9.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertOpplæringRepository;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertReisetid;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertReisetidPeriode;

@CdiDbAwareTest
class VurderReisetidOppdatererTest {

    @Inject
    private VurdertOpplæringRepository vurdertOpplæringRepository;
    @Inject
    public EntityManager entityManager;

    private VurderReisetidOppdaterer vurderReisetidOppdaterer;
    private Behandling behandling;
    private final LocalDate idag = LocalDate.now();

    @BeforeEach
    void setup() {
        BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        vurderReisetidOppdaterer = new VurderReisetidOppdaterer(vurdertOpplæringRepository);
        TestScenarioBuilder scenario = TestScenarioBuilder.builderMedSøknad();
        scenario.medSøknad().medSøknadsdato(idag);
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.VURDER_REISETID, BehandlingStegType.VURDER_GJENNOMGÅTT_OPPLÆRING);
        behandling = scenario.lagre(repositoryProvider);
    }

    private OppdateringResultat lagreGrunnlag(VurderReisetidDto dto) {
        Optional<Aksjonspunkt> aksjonspunkt = behandling.getAksjonspunktFor(dto.getKode());
        AksjonspunktOppdaterParameter param = new AksjonspunktOppdaterParameter(behandling, aksjonspunkt, dto);

        return vurderReisetidOppdaterer.oppdater(dto, param);
    }

    @Test
    void skalLagreNyttGrunnlag() {
        var opplæringPeriode = new Periode(idag, idag.plusDays(4));
        var reiseperiodeTil = new Periode(opplæringPeriode.getFom().minusDays(2), opplæringPeriode.getFom().minusDays(1));
        var reiseperiodeHjem = new Periode(opplæringPeriode.getTom().plusDays(1), opplæringPeriode.getTom().plusDays(2));

        var reisetidDto = new ReisetidDto(
            opplæringPeriode,
            List.of(new ReisetidPeriodeDto(reiseperiodeTil, true)),
            List.of(new ReisetidPeriodeDto(reiseperiodeHjem, false)),
            "reise");
        var dto = new VurderReisetidDto(List.of(reisetidDto));

        var resultat = lagreGrunnlag(dto);
        assertThat(resultat).isNotNull();

        var grunnlag = vurdertOpplæringRepository.hentAktivtGrunnlagForBehandling(behandling.getId());
        assertThat(grunnlag).isPresent();
        assertThat(grunnlag.get().getVurdertReisetid()).isNotNull();
        assertThat(grunnlag.get().getVurdertReisetid().getReisetid()).hasSize(1);

        VurdertReisetid reisetidFraGrunnlag = grunnlag.get().getVurdertReisetid().getReisetid().get(0);
        assertThat(reisetidFraGrunnlag.getOpplæringperiode()).isEqualTo(DatoIntervallEntitet.fra(opplæringPeriode));
        assertThat(reisetidFraGrunnlag.getBegrunnelse()).isEqualTo(reisetidDto.getBegrunnelse());
        assertThat(reisetidFraGrunnlag.getReiseperioder()).hasSize(2);

        Optional<VurdertReisetidPeriode> periode1 = reisetidFraGrunnlag.getReiseperioder().stream()
            .filter(reiseperiode -> reiseperiode.getPeriode().equals(DatoIntervallEntitet.fra(reiseperiodeTil))).findFirst();
        assertThat(periode1).isPresent();
        assertThat(periode1.get().getGodkjent()).isTrue();

        Optional<VurdertReisetidPeriode> periode2 = reisetidFraGrunnlag.getReiseperioder().stream()
            .filter(reiseperiode -> reiseperiode.getPeriode().equals(DatoIntervallEntitet.fra(reiseperiodeHjem))).findFirst();
        assertThat(periode2).isPresent();
        assertThat(periode2.get().getGodkjent()).isFalse();
    }

    @Test
    void skalOppdatereGrunnlag() {
        var opplæringPeriode = new Periode(idag, idag.plusDays(4));
        var reiseperiodeTil = new Periode(opplæringPeriode.getFom().minusDays(2), opplæringPeriode.getFom().minusDays(1));
        var reiseperiodeHjem = new Periode(opplæringPeriode.getTom().plusDays(1), opplæringPeriode.getTom().plusDays(2));

        var reisetidDto1 = new ReisetidDto(
            opplæringPeriode,
            List.of(new ReisetidPeriodeDto(reiseperiodeTil, true)),
            List.of(new ReisetidPeriodeDto(reiseperiodeHjem, true)),
            "ok");
        var dto1 = new VurderReisetidDto(List.of(reisetidDto1));
        lagreGrunnlag(dto1);

        var reiseperiode1 = new Periode(reiseperiodeTil.getFom(), reiseperiodeTil.getFom());
        var reiseperiode2 = new Periode(reiseperiodeTil.getTom(), reiseperiodeTil.getTom());
        var reiseperiode3 = new Periode(reiseperiodeHjem.getFom(), reiseperiodeHjem.getFom());
        var reiseperiode4 = new Periode(reiseperiodeHjem.getTom(), reiseperiodeHjem.getTom());

        var reisetidDto2 = new ReisetidDto(
            opplæringPeriode,
            List.of(new ReisetidPeriodeDto(reiseperiode1, false),
                new ReisetidPeriodeDto(reiseperiode2, true)),
            List.of(new ReisetidPeriodeDto(reiseperiode3, true),
                new ReisetidPeriodeDto(reiseperiode4, false)),
            "for lang");
        var dto2 = new VurderReisetidDto(List.of(reisetidDto2));
        lagreGrunnlag(dto2);

        var grunnlag = vurdertOpplæringRepository.hentAktivtGrunnlagForBehandling(behandling.getId());
        assertThat(grunnlag).isPresent();
        assertThat(grunnlag.get().getVurdertReisetid()).isNotNull();
        assertThat(grunnlag.get().getVurdertReisetid().getReisetid()).hasSize(1);

        VurdertReisetid reisetidFraGrunnlag = grunnlag.get().getVurdertReisetid().getReisetid().get(0);
        assertThat(reisetidFraGrunnlag.getOpplæringperiode()).isEqualTo(DatoIntervallEntitet.fra(opplæringPeriode));
        assertThat(reisetidFraGrunnlag.getBegrunnelse()).isEqualTo(reisetidDto2.getBegrunnelse());
        assertThat(reisetidFraGrunnlag.getReiseperioder()).hasSize(4);

        Optional<VurdertReisetidPeriode> periode1 = reisetidFraGrunnlag.getReiseperioder().stream()
            .filter(reiseperiode -> reiseperiode.getPeriode().equals(DatoIntervallEntitet.fra(reiseperiode1))).findFirst();
        assertThat(periode1).isPresent();
        assertThat(periode1.get().getGodkjent()).isFalse();

        Optional<VurdertReisetidPeriode> periode2 = reisetidFraGrunnlag.getReiseperioder().stream()
            .filter(reiseperiode -> reiseperiode.getPeriode().equals(DatoIntervallEntitet.fra(reiseperiode2))).findFirst();
        assertThat(periode2).isPresent();
        assertThat(periode2.get().getGodkjent()).isTrue();

        Optional<VurdertReisetidPeriode> periode3 = reisetidFraGrunnlag.getReiseperioder().stream()
            .filter(reiseperiode -> reiseperiode.getPeriode().equals(DatoIntervallEntitet.fra(reiseperiode3))).findFirst();
        assertThat(periode3).isPresent();
        assertThat(periode3.get().getGodkjent()).isTrue();

        Optional<VurdertReisetidPeriode> periode4 = reisetidFraGrunnlag.getReiseperioder().stream()
            .filter(reiseperiode -> reiseperiode.getPeriode().equals(DatoIntervallEntitet.fra(reiseperiode4))).findFirst();
        assertThat(periode4).isPresent();
        assertThat(periode4.get().getGodkjent()).isFalse();
    }

    @Test
    void skalKopiereFraAktivtGrunnlag() {
        var opplæringPeriode1 = new Periode(idag, idag.plusDays(4));
        var reiseperiodeTil1 = new Periode(opplæringPeriode1.getFom().minusDays(2), opplæringPeriode1.getFom().minusDays(1));
        var reiseperiodeHjem1 = new Periode(opplæringPeriode1.getTom().plusDays(1), opplæringPeriode1.getTom().plusDays(2));

        var reisetidDto1 = new ReisetidDto(
            opplæringPeriode1,
            List.of(new ReisetidPeriodeDto(reiseperiodeTil1, true)),
            List.of(new ReisetidPeriodeDto(reiseperiodeHjem1, true)),
            "ok");
        var dto1 = new VurderReisetidDto(List.of(reisetidDto1));
        lagreGrunnlag(dto1);

        var opplæringPeriode2 = new Periode(idag.plusWeeks(2), idag.plusWeeks(3));
        var reiseperiodeTil2 = new Periode(opplæringPeriode2.getFom().minusDays(2), opplæringPeriode2.getFom().minusDays(1));
        var reiseperiodeHjem2 = new Periode(opplæringPeriode2.getTom().plusDays(1), opplæringPeriode2.getTom().plusDays(2));

        var reisetidDto2 = new ReisetidDto(
            opplæringPeriode2,
            List.of(new ReisetidPeriodeDto(reiseperiodeTil2, true)),
            List.of(new ReisetidPeriodeDto(reiseperiodeHjem2, true)),
            "ok");
        var dto2 = new VurderReisetidDto(List.of(reisetidDto2));
        lagreGrunnlag(dto2);

        var grunnlag = vurdertOpplæringRepository.hentAktivtGrunnlagForBehandling(behandling.getId());
        assertThat(grunnlag).isPresent();
        assertThat(grunnlag.get().getVurdertReisetid()).isNotNull();
        assertThat(grunnlag.get().getVurdertReisetid().getReisetid()).hasSize(2);
    }

    @Test
    void overlappendeReisetidSkalFeile() {
        var opplæringPeriode = new Periode(idag, idag.plusDays(4));
        var reiseperiodeTil1 = new Periode(opplæringPeriode.getFom().minusDays(2), opplæringPeriode.getFom().minusDays(1));
        var reiseperiodeTil2 = new Periode(opplæringPeriode.getFom().minusDays(1), opplæringPeriode.getFom().minusDays(1));
        var reiseperiodeHjem = new Periode(opplæringPeriode.getTom().plusDays(1), opplæringPeriode.getTom().plusDays(2));

        var reisetidDto = new ReisetidDto(
            opplæringPeriode,
            List.of(new ReisetidPeriodeDto(reiseperiodeTil1, true), new ReisetidPeriodeDto(reiseperiodeTil2, true)),
            List.of(new ReisetidPeriodeDto(reiseperiodeHjem, true)),
            "ok");
        var dto = new VurderReisetidDto(List.of(reisetidDto));

        assertThrows(IllegalArgumentException.class, () -> lagreGrunnlag(dto));
    }

    @Test
    void overlappendeReisetidOgOpplæringSkalFeile() {
        var opplæringPeriode = new Periode(idag, idag.plusDays(4));
        var reiseperiodeTil = new Periode(opplæringPeriode.getFom().minusDays(2), opplæringPeriode.getFom());
        var reiseperiodeHjem = new Periode(opplæringPeriode.getTom().plusDays(1), opplæringPeriode.getTom().plusDays(2));

        var reisetidDto = new ReisetidDto(
            opplæringPeriode,
            List.of(new ReisetidPeriodeDto(reiseperiodeTil, true)),
            List.of(new ReisetidPeriodeDto(reiseperiodeHjem, true)),
            "ok");
        var dto = new VurderReisetidDto(List.of(reisetidDto));

        assertThrows(IllegalArgumentException.class, () -> lagreGrunnlag(dto));
    }
}
