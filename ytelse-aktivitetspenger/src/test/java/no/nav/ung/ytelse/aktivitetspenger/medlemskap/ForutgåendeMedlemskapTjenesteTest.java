package no.nav.ung.ytelse.aktivitetspenger.medlemskap;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.ung.kodeverk.geografisk.Landkoder;
import no.nav.ung.sak.behandlingslager.behandling.medlemskap.OppgittForutgåendeMedlemskapPeriode;
import no.nav.ung.sak.behandlingslager.behandling.medlemskap.OppgittForutgåendeMedlemskapRepository;
import no.nav.ung.sak.behandlingslager.behandling.medlemskap.OppgittUtenlandsopphold;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.typer.JournalpostId;
import no.nav.ung.ytelse.aktivitetspenger.testdata.AktivitetspengerTestScenarioBuilder;
import no.nav.ung.ytelse.aktivitetspenger.testdata.AktivitetspengerTestScenarioBuilder.MottattDokumentTestGrunnlag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(JpaExtension.class)
@ExtendWith(CdiAwareExtension.class)
class ForutgåendeMedlemskapTjenesteTest {

    private static final JournalpostId JP = new JournalpostId("JP1");

    @Inject
    private EntityManager entityManager;

    @Test
    void skal_mappe_oppgitt_periode_til_medlemskap_dto() {
        var forutgåendeMedlemskapRepository = new OppgittForutgåendeMedlemskapRepository(entityManager);
        var tjeneste = new ForutgåendeMedlemskapTjeneste(forutgåendeMedlemskapRepository);

        var behandling = AktivitetspengerTestScenarioBuilder.builderMedSøknad()
            .medMottattDokument(new MottattDokumentTestGrunnlag(null, null, LocalDateTime.now(), JP))
            .lagre(entityManager);

        forutgåendeMedlemskapRepository.leggTilOppgittPeriode(behandling.getId(),
            OppgittForutgåendeMedlemskapPeriode.builder()
                .medJournalpostId(JP)
                .medFom(LocalDate.of(2019, 7, 1))
                .medTom(LocalDate.of(2024, 6, 30))
                .medHarBoddINorge(false)
                .medHarJobbetINorge(false)
                .medHarJobbetUtenforNorge(true)
                .medUtenlandsopphold(Set.of(
                    new OppgittUtenlandsopphold(LocalDate.of(2019, 7, 1), LocalDate.of(2022, 12, 31), Landkoder.SWE, true, "010185-1234"),
                    new OppgittUtenlandsopphold(LocalDate.of(2023, 1, 1), LocalDate.of(2024, 6, 30), Landkoder.fraKode("DEU"), false, null)
                ))
                .build());

        var medlemskapDto = tjeneste.hentMedlemskapForBehandlingSomDto(behandling.getId()).stream().findFirst().orElseThrow();

        assertThat(medlemskapDto.journalpostId()).isEqualTo(JP.getVerdi());
        assertThat(medlemskapDto.harBoddINorge()).isFalse();
        assertThat(medlemskapDto.harJobbetINorge()).isFalse();
        assertThat(medlemskapDto.harJobbetUtenforNorge()).isTrue();
        assertThat(medlemskapDto.utenlandsopphold()).hasSize(2);

        var sverige = medlemskapDto.utenlandsopphold().stream()
            .filter(u -> u.landkode().equals("SWE"))
            .findFirst().orElseThrow();
        assertThat(sverige.land()).isEqualTo("Sverige");
        assertThat(sverige.harTrygdeavtale()).isTrue();
        assertThat(sverige.periode().getFom()).isEqualTo(LocalDate.of(2019, 7, 1));
        assertThat(sverige.periode().getTom()).isEqualTo(LocalDate.of(2022, 12, 31));
        assertThat(sverige.harJobbetIPerioden()).isTrue();
        assertThat(sverige.utenlandskNasjonalId()).isEqualTo("010185-1234");

        var tyskland = medlemskapDto.utenlandsopphold().stream()
            .filter(u -> u.landkode().equals("DEU"))
            .findFirst().orElseThrow();
        assertThat(tyskland.land()).isEqualTo("Tyskland");
        assertThat(tyskland.harJobbetIPerioden()).isFalse();
        assertThat(tyskland.utenlandskNasjonalId()).isNull();
    }
}
