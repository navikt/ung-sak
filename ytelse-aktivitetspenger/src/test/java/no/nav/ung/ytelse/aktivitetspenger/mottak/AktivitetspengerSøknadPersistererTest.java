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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@CdiDbAwareTest
class AktivitetspengerSøknadPersistererTest {

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

        persister.lagreForutgåendeMedlemskapGrunnlag(bosteder, søknadsperiode, behandling.getId());
        entityManager.clear();

        var grunnlag = forutgåendeMedlemskapRepository.hentGrunnlag(behandling.getId());
        assertThat(grunnlag.getPeriode().getFomDato()).isEqualTo(LocalDate.of(2021, 5, 1));
        assertThat(grunnlag.getPeriode().getTomDato()).isEqualTo(LocalDate.of(2026, 4, 30));
        assertThat(grunnlag.getBosteder()).hasSize(2);
        assertThat(grunnlag.getBosteder()).extracting(OppgittBosted::getLandkode)
            .containsExactlyInAnyOrder("DEU", "FIN");
    }

    @Test
    void skal_lagre_tom_bostedliste_når_ingen_bosteder_oppgitt() {
        var søknadsperiode = new Periode(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));
        var bosteder = new Bosteder();

        persister.lagreForutgåendeMedlemskapGrunnlag(bosteder, søknadsperiode, behandling.getId());
        entityManager.clear();

        var grunnlag = forutgåendeMedlemskapRepository.hentGrunnlag(behandling.getId());
        assertThat(grunnlag.getPeriode().getFomDato()).isEqualTo(LocalDate.of(2021, 1, 1));
        assertThat(grunnlag.getPeriode().getTomDato()).isEqualTo(LocalDate.of(2025, 12, 31));
        assertThat(grunnlag.getBosteder()).isEmpty();
    }

    @Test
    void skal_deaktivere_eldre_grunnlag_ved_ny_søknad() {
        var søknadsperiode = new Periode(LocalDate.of(2026, 7, 1), LocalDate.of(2027, 6, 30));
        var førsteBosteder = new Bosteder().medPerioder(Map.of(
            new Periode(LocalDate.of(2021, 7, 1), LocalDate.of(2026, 6, 30)),
            new BostedPeriodeInfo().medLand(Landkode.SVERIGE)
        ));

        persister.lagreForutgåendeMedlemskapGrunnlag(førsteBosteder, søknadsperiode, behandling.getId());
        entityManager.clear();
        var førsteGrunnlag = forutgåendeMedlemskapRepository.hentGrunnlag(behandling.getId());

        var andreBosteder = new Bosteder().medPerioder(Map.of(
            new Periode(LocalDate.of(2021, 7, 1), LocalDate.of(2026, 6, 30)),
            new BostedPeriodeInfo().medLand(Landkode.of("DEU"))
        ));

        persister.lagreForutgåendeMedlemskapGrunnlag(andreBosteder, søknadsperiode, behandling.getId());
        entityManager.clear();
        var andreGrunnlag = forutgåendeMedlemskapRepository.hentGrunnlag(behandling.getId());

        assertThat(andreGrunnlag.getId()).isNotEqualTo(førsteGrunnlag.getId());
        assertThat(andreGrunnlag.getBosteder()).hasSize(1);
        assertThat(andreGrunnlag.getBosteder().iterator().next().getLandkode()).isEqualTo("DEU");
    }
}
