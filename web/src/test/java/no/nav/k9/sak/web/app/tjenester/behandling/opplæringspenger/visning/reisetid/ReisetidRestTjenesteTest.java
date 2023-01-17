package no.nav.k9.sak.web.app.tjenester.behandling.opplæringspenger.visning.reisetid;

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
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertOpplæringRepository;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertReisetid;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertReisetidHolder;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.KursPeriode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.PerioderFraSøknad;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.UttakPeriode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.UttakPerioderGrunnlagRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.UttakPerioderHolder;

@CdiDbAwareTest
class ReisetidRestTjenesteTest {

    private ReisetidRestTjeneste restTjeneste;
    @Inject
    private BehandlingRepositoryProvider repositoryProvider;
    @Inject
    private BehandlingRepository behandlingRepository;
    @Inject
    private VurdertOpplæringRepository vurdertOpplæringRepository;
    @Inject
    private UttakPerioderGrunnlagRepository uttakPerioderGrunnlagRepository;

    private Behandling behandling;
    private final Periode kursperiode = new Periode(LocalDate.now().minusWeeks(1), LocalDate.now());

    @BeforeEach
    void setup() {
        restTjeneste = new ReisetidRestTjeneste(behandlingRepository, vurdertOpplæringRepository, uttakPerioderGrunnlagRepository);

        TestScenarioBuilder scenario = TestScenarioBuilder.builderMedSøknad(FagsakYtelseType.OPPLÆRINGSPENGER);
        behandling = scenario.lagre(repositoryProvider);
    }

    private void setupPerioderFraSøknad(DatoIntervallEntitet reiseperiodeTil, DatoIntervallEntitet reiseperiodeHjem) {
        PerioderFraSøknad perioderFraSøknad = new PerioderFraSøknad(new JournalpostId("1814"),
            List.of(new UttakPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(kursperiode.getFom(), kursperiode.getTom()), Duration.ofHours(7).plusMinutes(30))),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            List.of(new KursPeriode(kursperiode.getFom(), kursperiode.getTom(), reiseperiodeTil, reiseperiodeHjem, "institusjon", null, "fordi", "forda")));

        uttakPerioderGrunnlagRepository.lagre(behandling.getId(), perioderFraSøknad);
        uttakPerioderGrunnlagRepository.lagreRelevantePerioder(behandling.getId(), new UttakPerioderHolder(Set.of(perioderFraSøknad)));
    }

    @Test
    void ingenVurdering() {
        DatoIntervallEntitet reiseperiodeTil = DatoIntervallEntitet.fraOgMedTilOgMed(kursperiode.getFom().minusDays(2), kursperiode.getFom().minusDays(1));
        DatoIntervallEntitet reiseperiodeHjem = DatoIntervallEntitet.fraOgMedTilOgMed(kursperiode.getTom().plusDays(1), kursperiode.getTom().plusDays(2));
        setupPerioderFraSøknad(reiseperiodeTil, reiseperiodeHjem);

        Response response = restTjeneste.hentVurdertReisetid(new BehandlingUuidDto(behandling.getUuid()));
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(200);

        ReisetidDto result = (ReisetidDto) response.getEntity();
        assertThat(result).isNotNull();

        assertThat(result.getPerioder()).hasSize(1);
        assertThat(result.getPerioder().get(0).getOpplæringPeriode()).isEqualTo(kursperiode);
        assertThat(result.getPerioder().get(0).getReisetidTil()).isEqualTo(reiseperiodeTil.tilPeriode());
        assertThat(result.getPerioder().get(0).getReisetidHjem()).isEqualTo(reiseperiodeHjem.tilPeriode());
        assertThat(result.getPerioder().get(0).getBeskrivelseFraSoekerTil()).isEqualTo("fordi");
        assertThat(result.getPerioder().get(0).getBeskrivelseFraSoekerHjem()).isEqualTo("forda");

        assertThat(result.getVurderinger()).hasSize(1);

        assertThat(result.getVurderinger().get(0).getOpplæringPeriode()).isEqualTo(kursperiode);
        assertThat(result.getVurderinger().get(0).getReisetidTil()).hasSize(1);
        assertThat(result.getVurderinger().get(0).getReisetidTil().get(0).getPeriode()).isEqualTo(reiseperiodeTil.tilPeriode());
        assertThat(result.getVurderinger().get(0).getReisetidTil().get(0).getResultat()).isEqualTo(Resultat.MÅ_VURDERES);
        assertThat(result.getVurderinger().get(0).getReisetidTil().get(0).getBegrunnelse()).isNull();
        assertThat(result.getVurderinger().get(0).getReisetidTil().get(0).getEndretAv()).isNull();
        assertThat(result.getVurderinger().get(0).getReisetidTil().get(0).getEndretTidspunkt()).isNull();

        assertThat(result.getVurderinger().get(0).getReisetidHjem()).hasSize(1);
        assertThat(result.getVurderinger().get(0).getReisetidHjem().get(0).getPeriode()).isEqualTo(reiseperiodeHjem.tilPeriode());
        assertThat(result.getVurderinger().get(0).getReisetidHjem().get(0).getResultat()).isEqualTo(Resultat.MÅ_VURDERES);
        assertThat(result.getVurderinger().get(0).getReisetidHjem().get(0).getBegrunnelse()).isNull();
        assertThat(result.getVurderinger().get(0).getReisetidHjem().get(0).getEndretAv()).isNull();
        assertThat(result.getVurderinger().get(0).getReisetidHjem().get(0).getEndretTidspunkt()).isNull();
    }

    @Test
    void komplettVurdering() {
        DatoIntervallEntitet reiseperiodeTil = DatoIntervallEntitet.fraOgMedTilOgMed(kursperiode.getFom().minusDays(2), kursperiode.getFom().minusDays(1));
        DatoIntervallEntitet reiseperiodeHjem = DatoIntervallEntitet.fraOgMedTilOgMed(kursperiode.getTom().plusDays(1), kursperiode.getTom().plusDays(2));
        setupPerioderFraSøknad(reiseperiodeTil, reiseperiodeHjem);

        var vurdertReisetidTil = new VurdertReisetid(reiseperiodeTil, true, "fordi");
        var vurdertReisetidHjem = new VurdertReisetid(reiseperiodeHjem, false, "nope");
        vurdertOpplæringRepository.lagre(behandling.getId(), new VurdertReisetidHolder(List.of(vurdertReisetidTil, vurdertReisetidHjem)));

        Response response = restTjeneste.hentVurdertReisetid(new BehandlingUuidDto(behandling.getUuid()));
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(200);

        ReisetidDto result = (ReisetidDto) response.getEntity();
        assertThat(result).isNotNull();

        assertThat(result.getVurderinger()).hasSize(1);

        assertThat(result.getVurderinger().get(0).getOpplæringPeriode()).isEqualTo(kursperiode);
        assertThat(result.getVurderinger().get(0).getReisetidTil()).hasSize(1);
        assertThat(result.getVurderinger().get(0).getReisetidTil().get(0).getPeriode()).isEqualTo(reiseperiodeTil.tilPeriode());
        assertThat(result.getVurderinger().get(0).getReisetidTil().get(0).getResultat()).isEqualTo(Resultat.GODKJENT);
        assertThat(result.getVurderinger().get(0).getReisetidTil().get(0).getBegrunnelse()).isEqualTo(vurdertReisetidTil.getBegrunnelse());
        assertThat(result.getVurderinger().get(0).getReisetidTil().get(0).getEndretAv()).isEqualTo("VL");
        assertThat(result.getVurderinger().get(0).getReisetidTil().get(0).getEndretTidspunkt()).isNotNull();

        assertThat(result.getVurderinger().get(0).getReisetidHjem()).hasSize(1);
        assertThat(result.getVurderinger().get(0).getReisetidHjem().get(0).getPeriode()).isEqualTo(reiseperiodeHjem.tilPeriode());
        assertThat(result.getVurderinger().get(0).getReisetidHjem().get(0).getResultat()).isEqualTo(Resultat.IKKE_GODKJENT);
        assertThat(result.getVurderinger().get(0).getReisetidHjem().get(0).getBegrunnelse()).isEqualTo(vurdertReisetidHjem.getBegrunnelse());
        assertThat(result.getVurderinger().get(0).getReisetidHjem().get(0).getEndretAv()).isEqualTo("VL");
        assertThat(result.getVurderinger().get(0).getReisetidHjem().get(0).getEndretTidspunkt()).isNotNull();
    }

    @Test
    void delvisVurdering() {
        DatoIntervallEntitet reiseperiodeTil = DatoIntervallEntitet.fraOgMedTilOgMed(kursperiode.getFom().minusDays(2), kursperiode.getFom().minusDays(1));
        DatoIntervallEntitet reiseperiodeHjem = DatoIntervallEntitet.fraOgMedTilOgMed(kursperiode.getTom().plusDays(1), kursperiode.getTom().plusDays(2));
        setupPerioderFraSøknad(reiseperiodeTil, reiseperiodeHjem);

        var vurdertReisetid = new VurdertReisetid(DatoIntervallEntitet.fraOgMedTilOgMed(reiseperiodeTil.getFomDato(), reiseperiodeTil.getFomDato()), true, "fordi");
        vurdertOpplæringRepository.lagre(behandling.getId(), new VurdertReisetidHolder(List.of(vurdertReisetid)));

        Response response = restTjeneste.hentVurdertReisetid(new BehandlingUuidDto(behandling.getUuid()));
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(200);

        ReisetidDto result = (ReisetidDto) response.getEntity();
        assertThat(result).isNotNull();

        assertThat(result.getVurderinger()).hasSize(1);

        assertThat(result.getVurderinger().get(0).getOpplæringPeriode()).isEqualTo(kursperiode);
        assertThat(result.getVurderinger().get(0).getReisetidTil()).hasSize(2);

        assertThat(result.getVurderinger().get(0).getReisetidTil().get(0).getPeriode()).isEqualTo(new Periode(reiseperiodeTil.getFomDato(), reiseperiodeTil.getFomDato()));
        assertThat(result.getVurderinger().get(0).getReisetidTil().get(0).getResultat()).isEqualTo(Resultat.GODKJENT);
        assertThat(result.getVurderinger().get(0).getReisetidTil().get(0).getBegrunnelse()).isEqualTo(vurdertReisetid.getBegrunnelse());

        assertThat(result.getVurderinger().get(0).getReisetidTil().get(1).getPeriode()).isEqualTo(new Periode(reiseperiodeTil.getTomDato(), reiseperiodeTil.getTomDato()));
        assertThat(result.getVurderinger().get(0).getReisetidTil().get(1).getResultat()).isEqualTo(Resultat.MÅ_VURDERES);
        assertThat(result.getVurderinger().get(0).getReisetidTil().get(1).getBegrunnelse()).isNull();
    }

    @Test
    void automatiskVurdering() {
        DatoIntervallEntitet reiseperiodeTil = DatoIntervallEntitet.fraOgMedTilOgMed(kursperiode.getFom().minusDays(1), kursperiode.getFom().minusDays(1));
        DatoIntervallEntitet reiseperiodeHjem = DatoIntervallEntitet.fraOgMedTilOgMed(kursperiode.getTom().plusDays(1), kursperiode.getTom().plusDays(1));
        setupPerioderFraSøknad(reiseperiodeTil, reiseperiodeHjem);

        Response response = restTjeneste.hentVurdertReisetid(new BehandlingUuidDto(behandling.getUuid()));
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(200);

        ReisetidDto result = (ReisetidDto) response.getEntity();
        assertThat(result).isNotNull();

        assertThat(result.getVurderinger()).hasSize(1);

        assertThat(result.getVurderinger().get(0).getOpplæringPeriode()).isEqualTo(kursperiode);
        assertThat(result.getVurderinger().get(0).getReisetidTil()).hasSize(1);
        assertThat(result.getVurderinger().get(0).getReisetidTil().get(0).getPeriode()).isEqualTo(reiseperiodeTil.tilPeriode());
        assertThat(result.getVurderinger().get(0).getReisetidTil().get(0).getResultat()).isEqualTo(Resultat.GODKJENT_AUTOMATISK);
        assertThat(result.getVurderinger().get(0).getReisetidTil().get(0).getBegrunnelse()).isNull();
        assertThat(result.getVurderinger().get(0).getReisetidTil().get(0).getEndretAv()).isNull();
        assertThat(result.getVurderinger().get(0).getReisetidTil().get(0).getEndretTidspunkt()).isNull();

        assertThat(result.getVurderinger().get(0).getReisetidHjem()).hasSize(1);
        assertThat(result.getVurderinger().get(0).getReisetidHjem().get(0).getPeriode()).isEqualTo(reiseperiodeHjem.tilPeriode());
        assertThat(result.getVurderinger().get(0).getReisetidHjem().get(0).getResultat()).isEqualTo(Resultat.GODKJENT_AUTOMATISK);
        assertThat(result.getVurderinger().get(0).getReisetidHjem().get(0).getBegrunnelse()).isNull();
        assertThat(result.getVurderinger().get(0).getReisetidHjem().get(0).getEndretAv()).isNull();
        assertThat(result.getVurderinger().get(0).getReisetidHjem().get(0).getEndretTidspunkt()).isNull();
    }

    @Test
    void flerePerioder() {
        DatoIntervallEntitet reiseperiodeTil1 = DatoIntervallEntitet.fraOgMedTilOgMed(kursperiode.getFom().minusDays(2), kursperiode.getFom().minusDays(1));
        DatoIntervallEntitet reiseperiodeHjem1 = DatoIntervallEntitet.fraOgMedTilOgMed(kursperiode.getTom().plusDays(1), kursperiode.getTom().plusDays(2));

        Periode kursperiode2 = new Periode(LocalDate.now().plusDays(1), LocalDate.now().plusWeeks(1));
        DatoIntervallEntitet reiseperiodeTil2 = DatoIntervallEntitet.fraOgMedTilOgMed(kursperiode2.getFom().minusDays(1), kursperiode2.getFom().minusDays(1));
        DatoIntervallEntitet reiseperiodeHjem2 = DatoIntervallEntitet.fraOgMedTilOgMed(kursperiode2.getTom().plusDays(1), kursperiode2.getTom().plusDays(1));

        PerioderFraSøknad perioderFraSøknad = new PerioderFraSøknad(new JournalpostId("1814"),
            List.of(new UttakPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(kursperiode.getFom(), kursperiode.getTom()), Duration.ofHours(7).plusMinutes(30))),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            List.of(
                new KursPeriode(kursperiode.getFom(), kursperiode.getTom(), reiseperiodeTil1, reiseperiodeHjem1, "institusjon", null, null, null),
                new KursPeriode(kursperiode2.getFom(), kursperiode2.getTom(), reiseperiodeTil2, reiseperiodeHjem2, "institusjon", null, null, null)));

        uttakPerioderGrunnlagRepository.lagre(behandling.getId(), perioderFraSøknad);
        uttakPerioderGrunnlagRepository.lagreRelevantePerioder(behandling.getId(), new UttakPerioderHolder(Set.of(perioderFraSøknad)));

        Response response = restTjeneste.hentVurdertReisetid(new BehandlingUuidDto(behandling.getUuid()));
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(200);

        ReisetidDto result = (ReisetidDto) response.getEntity();
        assertThat(result).isNotNull();
        assertThat(result.getPerioder()).hasSize(2);
        assertThat(result.getVurderinger()).hasSize(2);
    }

    @Test
    void ingenReisetid() {
        setupPerioderFraSøknad(null, null);

        Response response = restTjeneste.hentVurdertReisetid(new BehandlingUuidDto(behandling.getUuid()));
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(200);

        ReisetidDto result = (ReisetidDto) response.getEntity();
        assertThat(result).isNotNull();

        assertThat(result.getPerioder()).hasSize(1);
        assertThat(result.getPerioder().get(0).getReisetidTil()).isNull();
        assertThat(result.getPerioder().get(0).getReisetidHjem()).isNull();

        assertThat(result.getVurderinger()).isEmpty();
    }
}
