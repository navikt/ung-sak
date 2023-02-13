package no.nav.k9.sak.web.app.tjenester.behandling.opplæringspenger.visning.gjennomgått;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
import no.nav.k9.sak.kontrakt.opplæringspenger.dokument.OpplæringDokumentType;
import no.nav.k9.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.dokument.OpplæringDokument;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.dokument.OpplæringDokumentRepository;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.vurdering.VurdertOpplæringPeriode;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.vurdering.VurdertOpplæringPerioderHolder;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.vurdering.VurdertOpplæringRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.KursPeriode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.PerioderFraSøknad;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.UttakPeriode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.UttakPerioderGrunnlagRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.UttakPerioderHolder;

@CdiDbAwareTest
class GjennomgåttOpplæringRestTjenesteTest {

    private GjennomgåttOpplæringRestTjeneste restTjeneste;
    @Inject
    private BehandlingRepositoryProvider repositoryProvider;
    @Inject
    private BehandlingRepository behandlingRepository;
    @Inject
    private VurdertOpplæringRepository vurdertOpplæringRepository;
    @Inject
    private UttakPerioderGrunnlagRepository uttakPerioderGrunnlagRepository;
    @Inject
    private OpplæringDokumentRepository opplæringDokumentRepository;

    private Behandling behandling;
    private final Periode periode1 = new Periode(LocalDate.now().minusWeeks(1), LocalDate.now());
    private final Periode periode2 = new Periode(LocalDate.now().plusDays(1), LocalDate.now().plusWeeks(1));
    private OpplæringDokument dokument;

    @BeforeEach
    void setup() {
        restTjeneste = new GjennomgåttOpplæringRestTjeneste(behandlingRepository, vurdertOpplæringRepository, uttakPerioderGrunnlagRepository);

        TestScenarioBuilder scenario = TestScenarioBuilder.builderMedSøknad(FagsakYtelseType.OPPLÆRINGSPENGER);
        behandling = scenario.lagre(repositoryProvider);

        scenario.getFagsak().setPleietrengende(AktørId.dummy());
        dokument = new OpplæringDokument(new JournalpostId("456"),null, OpplæringDokumentType.DOKUMENTASJON_AV_OPPLÆRING, behandling.getUuid(), behandling.getFagsak().getSaksnummer(), null, LocalDate.now(), LocalDateTime.now());
        opplæringDokumentRepository.lagre(dokument);
    }

    private KursPeriode lagKursperiode(Periode periode) {
        return new KursPeriode(periode.getFom(), periode.getTom(), null, null, null, null, null);
    }

    private PerioderFraSøknad lagPerioderFraSøknad(List<KursPeriode> kursperioder) {
        PerioderFraSøknad perioderFraSøknad = new PerioderFraSøknad(new JournalpostId("1098"),
            kursperioder.stream().map(kursperiode -> new UttakPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(kursperiode.getPeriode().getFomDato(), kursperiode.getPeriode().getTomDato()), Duration.ofHours(7).plusMinutes(30))).toList(),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            kursperioder);

        uttakPerioderGrunnlagRepository.lagre(behandling.getId(), perioderFraSøknad);
        return perioderFraSøknad;
    }

    @Test
    void ingenVurdering() {
        var kursperiode = lagKursperiode(periode1);
        var perioderFraSøknad = lagPerioderFraSøknad(List.of(kursperiode));
        uttakPerioderGrunnlagRepository.lagreRelevantePerioder(behandling.getId(), new UttakPerioderHolder(Set.of(perioderFraSøknad)));

        Response response = restTjeneste.hentVurdertOpplæring(new BehandlingUuidDto(behandling.getUuid()));
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(200);

        GjennomgåttOpplæringDto result = (GjennomgåttOpplæringDto) response.getEntity();
        assertThat(result).isNotNull();

        assertThat(result.getPerioder()).hasSize(1);
        assertThat(result.getPerioder().get(0)).isEqualTo(periode1);

        assertThat(result.getVurderinger()).hasSize(1);
        assertThat(result.getVurderinger().get(0).getPeriode()).isEqualTo(periode1);
        assertThat(result.getVurderinger().get(0).getResultat()).isEqualTo(Resultat.MÅ_VURDERES);
        assertThat(result.getVurderinger().get(0).getBegrunnelse()).isNull();
        assertThat(result.getVurderinger().get(0).getVurdertAv()).isNull();
        assertThat(result.getVurderinger().get(0).getVurdertTidspunkt()).isNull();
        assertThat(result.getVurderinger().get(0).getTilknyttedeDokumenter()).isEmpty();
    }

    @Test
    void komplettVurdering() {
        var kursperiode = lagKursperiode(periode1);
        var perioderFraSøknad = lagPerioderFraSøknad(List.of(kursperiode));
        uttakPerioderGrunnlagRepository.lagreRelevantePerioder(behandling.getId(), new UttakPerioderHolder(Set.of(perioderFraSøknad)));

        LocalDateTime nå = LocalDateTime.now();
        var vurdertOpplæringperiode = new VurdertOpplæringPeriode(periode1.getFom(), periode1.getTom(), true, "derfor", "meg", nå, List.of(dokument));
        vurdertOpplæringRepository.lagre(behandling.getId(), new VurdertOpplæringPerioderHolder(List.of(vurdertOpplæringperiode)));

        Response response = restTjeneste.hentVurdertOpplæring(new BehandlingUuidDto(behandling.getUuid()));
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(200);

        GjennomgåttOpplæringDto result = (GjennomgåttOpplæringDto) response.getEntity();
        assertThat(result).isNotNull();

        assertThat(result.getPerioder()).hasSize(1);
        assertThat(result.getPerioder().get(0)).isEqualTo(periode1);

        assertThat(result.getVurderinger()).hasSize(1);
        assertThat(result.getVurderinger().get(0).getPeriode()).isEqualTo(periode1);
        assertThat(result.getVurderinger().get(0).getResultat()).isEqualTo(Resultat.GODKJENT);
        assertThat(result.getVurderinger().get(0).getBegrunnelse()).isEqualTo(vurdertOpplæringperiode.getBegrunnelse());
        assertThat(result.getVurderinger().get(0).getVurdertAv()).isEqualTo("meg");
        assertThat(result.getVurderinger().get(0).getVurdertTidspunkt()).isEqualTo(nå);
        assertThat(result.getVurderinger().get(0).getTilknyttedeDokumenter()).hasSize(1);
        assertThat(result.getVurderinger().get(0).getTilknyttedeDokumenter().get(0)).isEqualTo(dokument.getId().toString());
    }

    @Test
    void kombinertVurdering() {
        var kursperiode1 = lagKursperiode(periode1);
        var kursperiode2 = lagKursperiode(periode2);
        var perioderFraSøknad1 = lagPerioderFraSøknad(List.of(kursperiode1, kursperiode2));
        uttakPerioderGrunnlagRepository.lagreRelevantePerioder(behandling.getId(), new UttakPerioderHolder(Set.of(perioderFraSøknad1)));

        var vurdertOpplæringperiode = new VurdertOpplæringPeriode(periode1.getFom(), periode1.getTom(), false, "nei", "", LocalDateTime.now(), List.of());
        vurdertOpplæringRepository.lagre(behandling.getId(), new VurdertOpplæringPerioderHolder(List.of(vurdertOpplæringperiode)));

        Response response = restTjeneste.hentVurdertOpplæring(new BehandlingUuidDto(behandling.getUuid()));
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(200);

        GjennomgåttOpplæringDto result = (GjennomgåttOpplæringDto) response.getEntity();
        assertThat(result).isNotNull();
        assertThat(result.getPerioder()).hasSize(2);

        assertThat(result.getVurderinger()).hasSize(2);
        var medVurdering = result.getVurderinger().stream().filter(v -> v.getPeriode().equals(periode1)).findFirst().orElseThrow();
        assertThat(medVurdering.getResultat()).isEqualTo(Resultat.IKKE_GODKJENT);
        var utenVurdering = result.getVurderinger().stream().filter(v -> v.getPeriode().equals(periode2)).findFirst().orElseThrow();
        assertThat(utenVurdering.getResultat()).isEqualTo(Resultat.MÅ_VURDERES);
    }
}
