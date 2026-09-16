package no.nav.ung.ytelse.aktivitetspenger.mottak;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.søknad.felles.type.Landkode;
import no.nav.k9.søknad.felles.type.Periode;
import no.nav.k9.søknad.ytelse.aktivitetspenger.v1.medlemskap.Medlemskap;
import no.nav.k9.søknad.ytelse.aktivitetspenger.v1.medlemskap.Utenlandsopphold;
import no.nav.k9.søknad.ytelse.aktivitetspenger.v1.medlemskap.Utenlandsopphold.UtenlandsoppholdPeriodeInfo;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.medlemskap.OppgittForutgåendeMedlemskapRepository;
import no.nav.ung.sak.behandlingslager.behandling.medlemskap.OppgittUtenlandsopphold;
import no.nav.ung.sak.db.util.CdiDbAwareTest;
import no.nav.ung.sak.typer.JournalpostId;
import no.nav.ung.ytelse.aktivitetspenger.testdata.AktivitetspengerTestScenarioBuilder;
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
    void skal_lagre_forutgående_periode_5_år_før_virkningstidspunkt() {
        LocalDate virkningstidspunkt = LocalDate.of(2026, 5, 1);
        String utenlandskNasjonalId = "010185-1234";
        var utenlandsopphold = new Utenlandsopphold(Map.of(
            new Periode(LocalDate.of(2021, 5, 1), LocalDate.of(2024, 4, 30)),
            new UtenlandsoppholdPeriodeInfo(Landkode.of("DEU"), false, null),
            new Periode(LocalDate.of(2024, 5, 1), LocalDate.of(2026, 4, 30)),
            new UtenlandsoppholdPeriodeInfo(Landkode.of("FIN"), true, utenlandskNasjonalId)
        ));

        Medlemskap medlemskap = new Medlemskap(false, false, true, utenlandsopphold);
        persister.lagreMedlemskapGrunnlag(medlemskap, virkningstidspunkt, JP, behandling.getId());


        var grunnlag = forutgåendeMedlemskapRepository.hentGrunnlag(behandling.getId());

        var periode = grunnlag.getOppgittePerioder().iterator().next();
        assertThat(periode.getPeriode().getFomDato()).isEqualTo(LocalDate.of(2021, 5, 1));
        assertThat(periode.getPeriode().getTomDato()).isEqualTo(LocalDate.of(2026, 4, 30));
        assertThat(periode.harBoddINorge()).isEqualTo(false);
        assertThat(periode.harJobbetINorge()).isEqualTo(false);
        assertThat(periode.harJobbetUtenforNorge()).isEqualTo(true);
        assertThat(periode.getUtenlandsopphold()).hasSize(2);
        assertThat(periode.getUtenlandsopphold()).extracting(OppgittUtenlandsopphold::getLandkode)
            .containsExactlyInAnyOrder("DEU", "FIN");
    }

    @Test
    void skal_lagre_tom_utenlandsopphold_når_ingen_utenlandsopphold_oppgitt() {
        LocalDate virkningstidspunkt = LocalDate.of(2026, 1, 1);
        var utenlandsopphold = new Utenlandsopphold(Map.of());
        var medlemskap = new Medlemskap(true, null, false, utenlandsopphold);
        persister.lagreMedlemskapGrunnlag(medlemskap, virkningstidspunkt, JP, behandling.getId());


        var grunnlag = forutgåendeMedlemskapRepository.hentGrunnlag(behandling.getId());

        var periode = grunnlag.getOppgittePerioder().iterator().next();
        assertThat(periode.harBoddINorge()).isEqualTo(true);
        assertThat(periode.harJobbetINorge()).isNull();
        assertThat(periode.harJobbetUtenforNorge()).isEqualTo(false);
        assertThat(periode.getPeriode().getFomDato()).isEqualTo(LocalDate.of(2021, 1, 1));
        assertThat(periode.getPeriode().getTomDato()).isEqualTo(LocalDate.of(2025, 12, 31));
        assertThat(periode.getUtenlandsopphold()).isEmpty();
    }
}
