package no.nav.k9.sak.web.app.tjenester.behandling.opplæringspenger.nødvendighet;

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
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertOpplæring;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertOpplæringHolder;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertOpplæringRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.KursPeriode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.PerioderFraSøknad;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.UttakPeriode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.UttakPerioderGrunnlagRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.UttakPerioderHolder;

@CdiDbAwareTest
class VurderNødvendigOpplæringRestTjenesteTest {

    private VurderNødvendigOpplæringRestTjeneste restTjeneste;
    @Inject
    private BehandlingRepositoryProvider repositoryProvider;
    @Inject
    private BehandlingRepository behandlingRepository;
    @Inject
    private VurdertOpplæringRepository vurdertOpplæringRepository;
    @Inject
    private UttakPerioderGrunnlagRepository uttakPerioderGrunnlagRepository;

    private Behandling behandling;
    private final JournalpostId journalpostId1 = new JournalpostId("1776");
    private final JournalpostId journalpostId2 = new JournalpostId("1789");
    private final Periode kursperiode1 = new Periode(LocalDate.now().minusWeeks(1), LocalDate.now());
    private final Periode kursperiode2 = new Periode(LocalDate.now().plusDays(1), LocalDate.now().plusWeeks(1));

    @BeforeEach
    void setup() {
        restTjeneste = new VurderNødvendigOpplæringRestTjeneste(behandlingRepository, vurdertOpplæringRepository, uttakPerioderGrunnlagRepository);

        TestScenarioBuilder scenario = TestScenarioBuilder.builderMedSøknad(FagsakYtelseType.OPPLÆRINGSPENGER);
        behandling = scenario.lagre(repositoryProvider);
    }

    private PerioderFraSøknad lagPerioderFraSøknad(JournalpostId journalpostId, Periode kursperiode) {
        no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.PerioderFraSøknad perioderFraSøknad = new PerioderFraSøknad(journalpostId,
            List.of(new UttakPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(kursperiode.getFom(), kursperiode.getTom()), Duration.ofHours(7).plusMinutes(30))),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            List.of(new KursPeriode(kursperiode.getFom(), kursperiode.getTom(), null, null, "institusjon", null, "")));

        uttakPerioderGrunnlagRepository.lagre(behandling.getId(), perioderFraSøknad);
        return perioderFraSøknad;
    }

    @Test
    void ingenVurdering() {
        var perioderFraSøknad = lagPerioderFraSøknad(journalpostId1, kursperiode1);
        uttakPerioderGrunnlagRepository.lagreRelevantePerioder(behandling.getId(), new UttakPerioderHolder(Set.of(perioderFraSøknad)));

        Response response = restTjeneste.hentVurdertNødvendigOpplæring(new BehandlingUuidDto(behandling.getUuid()));
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(200);

        NødvendigOpplæringDto result = (NødvendigOpplæringDto) response.getEntity();
        assertThat(result).isNotNull();

        assertThat(result.getPerioder()).hasSize(1);
        assertThat(result.getPerioder().get(0).getPeriode()).isEqualTo(kursperiode1);
        assertThat(result.getPerioder().get(0).getJournalpostId().getJournalpostId()).isEqualTo(journalpostId1);

        assertThat(result.getVurderinger()).hasSize(1);
        assertThat(result.getVurderinger().get(0).getPerioder()).hasSize(1);
        assertThat(result.getVurderinger().get(0).getPerioder().get(0)).isEqualTo(kursperiode1);
        assertThat(result.getVurderinger().get(0).getResultat()).isEqualTo(Resultat.MÅ_VURDERES);
        assertThat(result.getVurderinger().get(0).getJournalpostId().getJournalpostId()).isEqualTo(journalpostId1);
        assertThat(result.getVurderinger().get(0).getBegrunnelse()).isNull();
    }

    @Test
    void komplettVurdering() {
        var perioderFraSøknad = lagPerioderFraSøknad(journalpostId1, kursperiode1);
        uttakPerioderGrunnlagRepository.lagreRelevantePerioder(behandling.getId(), new UttakPerioderHolder(Set.of(perioderFraSøknad)));

        var vurdertOpplæring = new VurdertOpplæring(journalpostId1, true, "fordi");
        vurdertOpplæringRepository.lagre(behandling.getId(), new VurdertOpplæringHolder(List.of(vurdertOpplæring)));

        Response response = restTjeneste.hentVurdertNødvendigOpplæring(new BehandlingUuidDto(behandling.getUuid()));
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(200);

        NødvendigOpplæringDto result = (NødvendigOpplæringDto) response.getEntity();
        assertThat(result).isNotNull();

        assertThat(result.getPerioder()).hasSize(1);
        assertThat(result.getPerioder().get(0).getPeriode()).isEqualTo(kursperiode1);
        assertThat(result.getPerioder().get(0).getJournalpostId().getJournalpostId()).isEqualTo(journalpostId1);

        assertThat(result.getVurderinger()).hasSize(1);
        assertThat(result.getVurderinger().get(0).getPerioder()).hasSize(1);
        assertThat(result.getVurderinger().get(0).getPerioder().get(0)).isEqualTo(kursperiode1);
        assertThat(result.getVurderinger().get(0).getResultat()).isEqualTo(Resultat.GODKJENT);
        assertThat(result.getVurderinger().get(0).getJournalpostId().getJournalpostId()).isEqualTo(journalpostId1);
        assertThat(result.getVurderinger().get(0).getBegrunnelse()).isEqualTo("fordi");
    }

    @Test
    void kombinertVurdering() {
        var perioderFraSøknad1 = lagPerioderFraSøknad(journalpostId1, kursperiode1);
        var perioderFraSøknad2 = lagPerioderFraSøknad(journalpostId2, kursperiode2);
        uttakPerioderGrunnlagRepository.lagreRelevantePerioder(behandling.getId(), new UttakPerioderHolder(Set.of(perioderFraSøknad1, perioderFraSøknad2)));

        var vurdertOpplæring = new VurdertOpplæring(journalpostId1, false, "fordi");
        vurdertOpplæringRepository.lagre(behandling.getId(), new VurdertOpplæringHolder(List.of(vurdertOpplæring)));

        Response response = restTjeneste.hentVurdertNødvendigOpplæring(new BehandlingUuidDto(behandling.getUuid()));
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(200);

        NødvendigOpplæringDto result = (NødvendigOpplæringDto) response.getEntity();
        assertThat(result).isNotNull();
        assertThat(result.getPerioder()).hasSize(2);
        assertThat(result.getVurderinger()).hasSize(2);

        NødvendighetVurderingDto medVurdering = result.getVurderinger().stream().filter(v -> v.getJournalpostId().getJournalpostId().equals(journalpostId1)).findFirst().orElseThrow();
        assertThat(medVurdering.getPerioder()).hasSize(1);
        assertThat(medVurdering.getPerioder().get(0)).isEqualTo(kursperiode1);
        assertThat(medVurdering.getResultat()).isEqualTo(Resultat.IKKE_GODKJENT);

        NødvendighetVurderingDto utenVurdering = result.getVurderinger().stream().filter(v -> v.getJournalpostId().getJournalpostId().equals(journalpostId2)).findFirst().orElseThrow();
        assertThat(utenVurdering.getPerioder()).hasSize(1);
        assertThat(utenVurdering.getPerioder().get(0)).isEqualTo(kursperiode2);
        assertThat(utenVurdering.getResultat()).isEqualTo(Resultat.MÅ_VURDERES);
    }
}
