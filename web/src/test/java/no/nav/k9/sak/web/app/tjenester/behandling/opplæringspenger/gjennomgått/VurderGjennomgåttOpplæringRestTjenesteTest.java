package no.nav.k9.sak.web.app.tjenester.behandling.opplæringspenger.gjennomgått;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.db.util.CdiDbAwareTest;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.kontrakt.behandling.BehandlingUuidDto;
import no.nav.k9.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertOpplæringPeriode;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertOpplæringPerioderHolder;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertOpplæringRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.KursPeriode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.PerioderFraSøknad;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.UttakPeriode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.UttakPerioderGrunnlagRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.UttakPerioderHolder;

@CdiDbAwareTest
class VurderGjennomgåttOpplæringRestTjenesteTest {

    private VurderGjennomgåttOpplæringRestTjeneste restTjeneste;
    @Inject
    private BehandlingRepositoryProvider repositoryProvider;
    @Inject
    private BehandlingRepository behandlingRepository;
    @Inject
    private VurdertOpplæringRepository vurdertOpplæringRepository;
    @Inject
    private UttakPerioderGrunnlagRepository uttakPerioderGrunnlagRepository;

    private Behandling behandling;
    private final Periode kursperiode1 = new Periode(LocalDate.now().minusWeeks(1), LocalDate.now());
    private final Periode kursperiode2 = new Periode(LocalDate.now().plusDays(1), LocalDate.now().plusWeeks(1));

    @BeforeEach
    void setup() {
        restTjeneste = new VurderGjennomgåttOpplæringRestTjeneste(behandlingRepository, vurdertOpplæringRepository, uttakPerioderGrunnlagRepository);

        TestScenarioBuilder scenario = TestScenarioBuilder.builderMedSøknad(FagsakYtelseType.OPPLÆRINGSPENGER);
        behandling = scenario.lagre(repositoryProvider);
    }

    private PerioderFraSøknad lagPerioderFraSøknad(List<Periode> kursperioder) {
        PerioderFraSøknad perioderFraSøknad = new PerioderFraSøknad(new JournalpostId("1098"),
            kursperioder.stream().map(kursperiode -> new UttakPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(kursperiode.getFom(), kursperiode.getTom()), Duration.ofHours(7).plusMinutes(30))).toList(),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            kursperioder.stream().map(kursperiode -> new KursPeriode(kursperiode.getFom(), kursperiode.getTom(), null, null, "noe", null, "")).toList());

        uttakPerioderGrunnlagRepository.lagre(behandling.getId(), perioderFraSøknad);
        return perioderFraSøknad;
    }

    @Test
    void ingenVurdering() {
        var perioderFraSøknad = lagPerioderFraSøknad(List.of(kursperiode1));
        uttakPerioderGrunnlagRepository.lagreRelevantePerioder(behandling.getId(), new UttakPerioderHolder(Set.of(perioderFraSøknad)));

        Response response = restTjeneste.hentVurdertOpplæring(new BehandlingUuidDto(behandling.getUuid()));
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(200);

        GjennomgåttOpplæringDto result = (GjennomgåttOpplæringDto) response.getEntity();
        assertThat(result).isNotNull();

        assertThat(result.getPerioder()).hasSize(1);
        assertThat(result.getPerioder().get(0).getPeriode()).isEqualTo(kursperiode1);
        assertThat(result.getPerioder().get(0).getReisetid()).isNotNull();

        assertThat(result.getVurderinger()).hasSize(1);
        assertThat(result.getVurderinger().get(0).getPeriode()).isEqualTo(kursperiode1);
        assertThat(result.getVurderinger().get(0).getResultat()).isEqualTo(Resultat.MÅ_VURDERES);
        assertThat(result.getVurderinger().get(0).getReisetidVurdering()).isNull();
        assertThat(result.getVurderinger().get(0).getBegrunnelse()).isNull();
    }

    @Test
    void komplettVurdering() {
        var perioderFraSøknad = lagPerioderFraSøknad(List.of(kursperiode1));
        uttakPerioderGrunnlagRepository.lagreRelevantePerioder(behandling.getId(), new UttakPerioderHolder(Set.of(perioderFraSøknad)));

        var vurdertOpplæringperiode = new VurdertOpplæringPeriode(kursperiode1.getFom(), kursperiode1.getTom(), true, null, "derfor");
        vurdertOpplæringRepository.lagre(behandling.getId(), new VurdertOpplæringPerioderHolder(List.of(vurdertOpplæringperiode)));

        Response response = restTjeneste.hentVurdertOpplæring(new BehandlingUuidDto(behandling.getUuid()));
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(200);

        GjennomgåttOpplæringDto result = (GjennomgåttOpplæringDto) response.getEntity();
        assertThat(result).isNotNull();

        assertThat(result.getPerioder()).hasSize(1);
        assertThat(result.getPerioder().get(0).getPeriode()).isEqualTo(kursperiode1);

        assertThat(result.getVurderinger()).hasSize(1);
        assertThat(result.getVurderinger().get(0).getPeriode()).isEqualTo(kursperiode1);
        assertThat(result.getVurderinger().get(0).getResultat()).isEqualTo(Resultat.GODKJENT);
        assertThat(result.getVurderinger().get(0).getReisetidVurdering()).isNull();
        assertThat(result.getVurderinger().get(0).getBegrunnelse()).isEqualTo(vurdertOpplæringperiode.getBegrunnelse());
    }

    @Test
    void kombinertVurdering() {
        var perioderFraSøknad1 = lagPerioderFraSøknad(List.of(kursperiode1, kursperiode2));
        uttakPerioderGrunnlagRepository.lagreRelevantePerioder(behandling.getId(), new UttakPerioderHolder(Set.of(perioderFraSøknad1)));

        var vurdertOpplæringperiode = new VurdertOpplæringPeriode(kursperiode1.getFom(), kursperiode1.getTom(), false, null, "nei");
        vurdertOpplæringRepository.lagre(behandling.getId(), new VurdertOpplæringPerioderHolder(List.of(vurdertOpplæringperiode)));

        Response response = restTjeneste.hentVurdertOpplæring(new BehandlingUuidDto(behandling.getUuid()));
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(200);

        GjennomgåttOpplæringDto result = (GjennomgåttOpplæringDto) response.getEntity();
        assertThat(result).isNotNull();
        assertThat(result.getPerioder()).hasSize(2);

        assertThat(result.getVurderinger()).hasSize(2);
        var medVurdering = result.getVurderinger().stream().filter(v -> v.getPeriode().equals(kursperiode1)).findFirst().orElseThrow();
        assertThat(medVurdering.getResultat()).isEqualTo(Resultat.IKKE_GODKJENT);
        var utenVurdering = result.getVurderinger().stream().filter(v -> v.getPeriode().equals(kursperiode2)).findFirst().orElseThrow();
        assertThat(utenVurdering.getResultat()).isEqualTo(Resultat.MÅ_VURDERES);
    }

    //TODO: test reisetid
}
