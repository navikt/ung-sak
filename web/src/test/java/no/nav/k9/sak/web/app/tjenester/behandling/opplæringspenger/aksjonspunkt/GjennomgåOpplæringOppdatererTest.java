package no.nav.k9.sak.web.app.tjenester.behandling.opplæringspenger.aksjonspunkt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.db.util.CdiDbAwareTest;
import no.nav.k9.sak.kontrakt.opplæringspenger.VurderGjennomgåttOpplæringDto;
import no.nav.k9.sak.kontrakt.opplæringspenger.VurderGjennomgåttOpplæringPeriodeDto;
import no.nav.k9.sak.kontrakt.sykdom.dokument.SykdomDokumentType;
import no.nav.k9.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertOpplæringPeriode;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertOpplæringRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.pleietrengendesykdom.PleietrengendeSykdomDokument;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.pleietrengendesykdom.PleietrengendeSykdomDokumentInformasjon;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.pleietrengendesykdom.PleietrengendeSykdomDokumentRepository;

@CdiDbAwareTest
class GjennomgåOpplæringOppdatererTest {

    @Inject
    private VurdertOpplæringRepository vurdertOpplæringRepository;
    @Inject
    private EntityManager entityManager;
    @Inject
    private BehandlingRepository behandlingRepository;
    @Inject
    private PleietrengendeSykdomDokumentRepository pleietrengendeSykdomDokumentRepository;

    private GjennomgåOpplæringOppdaterer gjennomgåOpplæringOppdaterer;
    private Behandling behandling;
    private final LocalDate idag = LocalDate.now();
    private PleietrengendeSykdomDokument legeerklæring;

    @BeforeEach
    void setup() {
        BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        gjennomgåOpplæringOppdaterer = new GjennomgåOpplæringOppdaterer(vurdertOpplæringRepository, behandlingRepository, pleietrengendeSykdomDokumentRepository);
        TestScenarioBuilder scenario = TestScenarioBuilder.builderMedSøknad();
        scenario.medSøknad().medSøknadsdato(idag);
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.VURDER_GJENNOMGÅTT_OPPLÆRING, BehandlingStegType.VURDER_GJENNOMGÅTT_OPPLÆRING);
        behandling = scenario.lagre(repositoryProvider);
        scenario.getFagsak().setPleietrengende(AktørId.dummy());
        legeerklæring = lagreNyttSykdomDokument(SykdomDokumentType.LEGEERKLÆRING_MED_DOKUMENTASJON_AV_OPPLÆRING);
    }

    @Test
    void skalLagreNyttGrunnlag() {
        var periodeDto = new VurderGjennomgåttOpplæringPeriodeDto(idag, idag, true, "test", Set.of(legeerklæring.getId().toString()));
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
        assertThat(periodeFraGrunnlag.getDokumenter()).hasSize(1);
        assertThat(periodeFraGrunnlag.getDokumenter().get(0)).isEqualTo(legeerklæring);
    }

    @Test
    void skalOppdatereGrunnlag() {
        var periodeDto1 = new VurderGjennomgåttOpplæringPeriodeDto(idag, idag, false, "test1", Set.of(legeerklæring.getId().toString()));
        var dto1 = new VurderGjennomgåttOpplæringDto(List.of(periodeDto1));
        lagreGrunnlag(dto1);

        var kursbeskrivelse = lagreNyttSykdomDokument(SykdomDokumentType.DOKUMENTASJON_AV_OPPLÆRING);
        var periodeDto2 = new VurderGjennomgåttOpplæringPeriodeDto(idag, idag.plusDays(1), true, "test2", Set.of(kursbeskrivelse.getId().toString()));
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
        assertThat(periodeFraGrunnlag.getDokumenter()).hasSize(1);
        assertThat(periodeFraGrunnlag.getDokumenter().get(0)).isEqualTo(kursbeskrivelse);
    }

    @Test
    void skalKopiereFraAktivtGrunnlag() {
        var periodeDto1 = new VurderGjennomgåttOpplæringPeriodeDto(idag, idag, false, "test", Set.of());
        var dto1 = new VurderGjennomgåttOpplæringDto(List.of(periodeDto1));
        lagreGrunnlag(dto1);
        var periodeDto2 = new VurderGjennomgåttOpplæringPeriodeDto(idag.plusDays(1), idag.plusDays(1), true, "test", Set.of());
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
    }

    @Test
    void overlappendePerioderSkalFeile() {
        var periodeDto1 = new VurderGjennomgåttOpplæringPeriodeDto(idag, idag, true, "", Set.of());
        var periodeDto2 = new VurderGjennomgåttOpplæringPeriodeDto(idag, idag.plusDays(1), true, "", Set.of());
        var dto = new VurderGjennomgåttOpplæringDto(List.of(periodeDto1, periodeDto2));

        assertThrows(IllegalArgumentException.class, () -> lagreGrunnlag(dto));
    }

    private OppdateringResultat lagreGrunnlag(VurderGjennomgåttOpplæringDto dto) {
        Optional<Aksjonspunkt> aksjonspunkt = behandling.getAksjonspunktFor(dto.getKode());
        AksjonspunktOppdaterParameter param = new AksjonspunktOppdaterParameter(behandling, aksjonspunkt, dto);

        return gjennomgåOpplæringOppdaterer.oppdater(dto, param);
    }

    private PleietrengendeSykdomDokument lagreNyttSykdomDokument(SykdomDokumentType type) {
        PleietrengendeSykdomDokument dokument = new PleietrengendeSykdomDokument(new JournalpostId("456"), null,
            new PleietrengendeSykdomDokumentInformasjon(type, false, LocalDate.now(), LocalDateTime.now(), 1L, "meg", LocalDateTime.now()),
            null, null, null, "meg", LocalDateTime.now());
        pleietrengendeSykdomDokumentRepository.lagre(dokument, behandling.getFagsak().getPleietrengendeAktørId());
        return dokument;
    }
}
