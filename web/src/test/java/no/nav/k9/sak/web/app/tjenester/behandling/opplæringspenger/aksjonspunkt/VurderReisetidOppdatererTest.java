package no.nav.k9.sak.web.app.tjenester.behandling.opplæringspenger.aksjonspunkt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.sikkerhet.StaticSubjectHandler;
import no.nav.k9.felles.testutilities.sikkerhet.SubjectHandlerUtils;
import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.k9.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.db.util.CdiDbAwareTest;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.kontrakt.opplæringspenger.vurdering.VurderReisetidPeriodeDto;
import no.nav.k9.sak.kontrakt.opplæringspenger.vurdering.VurderReisetidDto;
import no.nav.k9.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.vurdering.VurdertOpplæringRepository;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.vurdering.VurdertReisetid;

@CdiDbAwareTest
class VurderReisetidOppdatererTest {

    @Inject
    private VurdertOpplæringRepository vurdertOpplæringRepository;
    @Inject
    public EntityManager entityManager;

    private VurderReisetidOppdaterer vurderReisetidOppdaterer;
    private Behandling behandling;
    private final LocalDate idag = LocalDate.now();
    private static final String brukerId = "bruker1";

    @BeforeAll
    static void subjectHandlerSetup() {
        SubjectHandlerUtils.useSubjectHandler(StaticSubjectHandler.class);
        SubjectHandlerUtils.setInternBruker(brukerId);
    }

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
        Optional<Aksjonspunkt> aksjonspunkt = behandling.getAksjonspunktForHvisFinnes(dto.getKode());
        AksjonspunktOppdaterParameter param = new AksjonspunktOppdaterParameter(behandling, aksjonspunkt, dto);

        return vurderReisetidOppdaterer.oppdater(dto, param);
    }

    @Test
    void skalLagreNyttGrunnlag() {
        var opplæringPeriode = new Periode(idag, idag.plusDays(4));
        var reiseperiode = new Periode(opplæringPeriode.getFom().minusDays(2), opplæringPeriode.getFom().minusDays(1));

        var dto = new VurderReisetidDto(List.of(
            new VurderReisetidPeriodeDto(reiseperiode, true, "ja")));

        var resultat = lagreGrunnlag(dto);
        assertThat(resultat).isNotNull();

        var grunnlag = vurdertOpplæringRepository.hentAktivtGrunnlagForBehandling(behandling.getId());
        assertThat(grunnlag).isPresent();
        assertThat(grunnlag.get().getVurdertReisetid()).isNotNull();
        assertThat(grunnlag.get().getVurdertReisetid().getReisetid()).hasSize(1);

        VurdertReisetid reisetidFraGrunnlag = grunnlag.get().getVurdertReisetid().getReisetid().get(0);
        assertThat(reisetidFraGrunnlag.getPeriode()).isEqualTo(DatoIntervallEntitet.fra(reiseperiode));
        assertThat(reisetidFraGrunnlag.getGodkjent()).isTrue();
        assertThat(reisetidFraGrunnlag.getBegrunnelse()).isEqualTo("ja");
        assertThat(reisetidFraGrunnlag.getVurdertAv()).isEqualTo(brukerId);
        assertThat(reisetidFraGrunnlag.getVurdertTidspunkt()).isNotNull();
    }

    @Test
    void skalOppdatereGrunnlag() {
        var opplæringPeriode = new Periode(idag, idag.plusDays(4));
        var reiseperiode = new Periode(opplæringPeriode.getFom().minusDays(2), opplæringPeriode.getFom().minusDays(1));

        var dto1 = new VurderReisetidDto(List.of(
            new VurderReisetidPeriodeDto(reiseperiode, true, "ok")));
        lagreGrunnlag(dto1);
        LocalDateTime vurdertTidspunkt1 = vurdertOpplæringRepository.hentAktivtGrunnlagForBehandling(behandling.getId()).orElseThrow().getVurdertReisetid().getReisetid().get(0).getVurdertTidspunkt();

        var reiseperiode1 = new Periode(reiseperiode.getFom(), reiseperiode.getFom());
        var reiseperiode2 = new Periode(reiseperiode.getTom(), reiseperiode.getTom());

        var dto2 = new VurderReisetidDto(List.of(
            new VurderReisetidPeriodeDto(reiseperiode1, false, "grunn 1"),
            new VurderReisetidPeriodeDto(reiseperiode2, true, "grunn 2")));
        lagreGrunnlag(dto2);

        var grunnlag = vurdertOpplæringRepository.hentAktivtGrunnlagForBehandling(behandling.getId());
        assertThat(grunnlag).isPresent();
        assertThat(grunnlag.get().getVurdertReisetid()).isNotNull();
        assertThat(grunnlag.get().getVurdertReisetid().getReisetid()).hasSize(2);

        Optional<VurdertReisetid> periode1 = grunnlag.get().getVurdertReisetid().getReisetid().stream()
            .filter(reisetid -> reisetid.getPeriode().equals(DatoIntervallEntitet.fra(reiseperiode1))).findFirst();
        assertThat(periode1).isPresent();
        assertThat(periode1.get().getGodkjent()).isFalse();
        assertThat(periode1.get().getBegrunnelse()).isEqualTo("grunn 1");
        assertThat(periode1.get().getVurdertTidspunkt()).isAfter(vurdertTidspunkt1);

        Optional<VurdertReisetid> periode2 = grunnlag.get().getVurdertReisetid().getReisetid().stream()
            .filter(reisetid -> reisetid.getPeriode().equals(DatoIntervallEntitet.fra(reiseperiode2))).findFirst();
        assertThat(periode2).isPresent();
        assertThat(periode2.get().getGodkjent()).isTrue();
        assertThat(periode2.get().getBegrunnelse()).isEqualTo("grunn 2");
        assertThat(periode2.get().getVurdertTidspunkt()).isAfter(vurdertTidspunkt1);
    }

    @Test
    void skalKopiereFraAktivtGrunnlag() {
        var opplæringPeriode1 = new Periode(idag, idag.plusDays(4));
        var reiseperiodeTil = new Periode(opplæringPeriode1.getFom().minusDays(2), opplæringPeriode1.getFom().minusDays(1));
        var reiseperiodeHjem = new Periode(opplæringPeriode1.getTom().plusDays(1), opplæringPeriode1.getTom().plusDays(2));

        var dto1 = new VurderReisetidDto(List.of(
            new VurderReisetidPeriodeDto(reiseperiodeTil, true, "ja")));
        lagreGrunnlag(dto1);

        var dto2 = new VurderReisetidDto(List.of(
            new VurderReisetidPeriodeDto(reiseperiodeHjem, false, "nei")));
        lagreGrunnlag(dto2);

        var grunnlag = vurdertOpplæringRepository.hentAktivtGrunnlagForBehandling(behandling.getId());
        assertThat(grunnlag).isPresent();
        assertThat(grunnlag.get().getVurdertReisetid()).isNotNull();
        assertThat(grunnlag.get().getVurdertReisetid().getReisetid()).hasSize(2);
        assertThat(grunnlag.get().getVurdertReisetid().getReisetid().get(0).getVurdertTidspunkt()).isNotEqualTo(grunnlag.get().getVurdertReisetid().getReisetid().get(1).getVurdertTidspunkt());
    }

    @Test
    void overlappendeReisetidSkalFeile() {
        var opplæringPeriode = new Periode(idag, idag.plusDays(4));
        var reiseperiodeTil1 = new Periode(opplæringPeriode.getFom().minusDays(2), opplæringPeriode.getFom().minusDays(1));
        var reiseperiodeTil2 = new Periode(opplæringPeriode.getFom().minusDays(1), opplæringPeriode.getFom().minusDays(1));

        var dto = new VurderReisetidDto(List.of(
            new VurderReisetidPeriodeDto(reiseperiodeTil1, true, "ok"),
            new VurderReisetidPeriodeDto(reiseperiodeTil2, true, "ok")));

        assertThrows(IllegalArgumentException.class, () -> lagreGrunnlag(dto));
    }
}
