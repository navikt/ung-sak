package no.nav.ung.ytelse.aktivitetspenger.mottak;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.søknad.felles.type.Landkode;
import no.nav.k9.søknad.felles.type.Periode;
import no.nav.k9.søknad.ytelse.aktivitetspenger.v1.Bosteder;
import no.nav.k9.søknad.ytelse.aktivitetspenger.v1.Bosteder.BostedPeriodeInfo;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.medlemskap.OppgittBosted;
import no.nav.ung.sak.behandlingslager.behandling.medlemskap.OppgittForutgåendeMedlemskapRepository;
import no.nav.ung.sak.db.util.CdiDbAwareTest;
import no.nav.ung.sak.test.util.behandling.aktivitetspenger.AktivitetspengerTestScenarioBuilder;
import no.nav.ung.sak.typer.JournalpostId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@CdiDbAwareTest
class AktivitetspengerSøknadPersistererTest {

    private static final JournalpostId JP = new JournalpostId("JP1");

    @Inject
    private EntityManager entityManager;

    @Inject
    private OppgittForutgåendeMedlemskapRepository forutgåendeMedlemskapRepository;

    @Inject
    private AktivitetspengerSøknadPersisterer persister;

    private Behandling behandling;

    @BeforeEach
    void setUp() {
        behandling = AktivitetspengerTestScenarioBuilder.builderMedSøknad().lagre(entityManager);
    }

    @Test
    void skal_lagre_forutgående_periode_5_år_før_søknadsperiode_fom() {
        var søknadsperiode = new Periode(LocalDate.of(2026, 5, 1), LocalDate.of(2027, 4, 30));
        var bosteder = new Bosteder().medPerioder(Map.of(
            new Periode(LocalDate.of(2021, 5, 1), LocalDate.of(2024, 4, 30)),
            new BostedPeriodeInfo().medLand(Landkode.of("DEU")),
            new Periode(LocalDate.of(2024, 5, 1), LocalDate.of(2026, 4, 30)),
            new BostedPeriodeInfo().medLand(Landkode.of("FIN"))
        ));

        persister.lagreForutgåendeMedlemskapGrunnlag(bosteder, søknadsperiode, JP, behandling.getId());


        var grunnlag = forutgåendeMedlemskapRepository.hentGrunnlag(behandling.getId());
        assertThat(grunnlag.getOppgittePerioder()).hasSize(1);
        var periode = grunnlag.getOppgittePerioder().iterator().next();
        assertThat(periode.getPeriode().getFomDato()).isEqualTo(LocalDate.of(2021, 5, 1));
        assertThat(periode.getPeriode().getTomDato()).isEqualTo(LocalDate.of(2026, 4, 30));
        assertThat(periode.getBostederUtland()).hasSize(2);
        assertThat(periode.getBostederUtland()).extracting(OppgittBosted::getLandkode)
            .containsExactlyInAnyOrder("DEU", "FIN");
    }

    @Test
    void skal_lagre_tom_bostedliste_når_ingen_bosteder_oppgitt() {
        var søknadsperiode = new Periode(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));
        var bosteder = new Bosteder();

        persister.lagreForutgåendeMedlemskapGrunnlag(bosteder, søknadsperiode, JP, behandling.getId());


        var grunnlag = forutgåendeMedlemskapRepository.hentGrunnlag(behandling.getId());
        var periode = grunnlag.getOppgittePerioder().iterator().next();
        assertThat(periode.getPeriode().getFomDato()).isEqualTo(LocalDate.of(2021, 1, 1));
        assertThat(periode.getPeriode().getTomDato()).isEqualTo(LocalDate.of(2025, 12, 31));
        assertThat(periode.getBostederUtland()).isEmpty();
    }

    @Test
    void skal_legge_til_perioder_ved_ny_søknad_på_samme_behandling() {
        var søknadsperiode = new Periode(LocalDate.of(2026, 7, 1), LocalDate.of(2027, 6, 30));
        var jp1 = new JournalpostId("JP-FIRST");
        var jp2 = new JournalpostId("JP-SECOND");

        var førsteBosteder = new Bosteder().medPerioder(Map.of(
            new Periode(LocalDate.of(2021, 7, 1), LocalDate.of(2026, 6, 30)),
            new BostedPeriodeInfo().medLand(Landkode.SVERIGE)
        ));

        persister.lagreForutgåendeMedlemskapGrunnlag(førsteBosteder, søknadsperiode, jp1, behandling.getId());


        var andreBosteder = new Bosteder().medPerioder(Map.of(
            new Periode(LocalDate.of(2021, 7, 1), LocalDate.of(2026, 6, 30)),
            new BostedPeriodeInfo().medLand(Landkode.of("DEU"))
        ));

        persister.lagreForutgåendeMedlemskapGrunnlag(andreBosteder, søknadsperiode, jp2, behandling.getId());


        var grunnlag = forutgåendeMedlemskapRepository.hentGrunnlag(behandling.getId());
        assertThat(grunnlag.getOppgittePerioder()).hasSize(2);
    }
}
