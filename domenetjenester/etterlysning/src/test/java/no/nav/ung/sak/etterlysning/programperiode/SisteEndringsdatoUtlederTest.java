package no.nav.ung.sak.etterlysning.programperiode;

import no.nav.ung.sak.behandlingslager.behandling.startdato.UngdomsytelseSøktStartdato;
import no.nav.ung.sak.typer.JournalpostId;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SisteEndringsdatoUtlederTest {

    private static PeriodeSnapshot snapshot(LocalDate fom, LocalDate tom) {
        return new PeriodeSnapshot(Optional.ofNullable(fom), Optional.ofNullable(tom), "en beskrivelse");
    }

    @Test
    void skal_ikke_finne_endring_når_det_ikke_finnes_aktuelle_grunnlag() {
        // Arrange
        LocalDate gjeldendeFom = LocalDate.of(2024, 1, 1);
        PeriodeSnapshot gjeldendeSnapshot = snapshot(gjeldendeFom, LocalDate.of(2024, 12, 31));

        // Act
        Optional<SisteEndringsdatoUtleder.EndretDato> resultat =
            SisteEndringsdatoUtleder.finnSistEndretDato(gjeldendeSnapshot, Collections.emptyList(), getFomDato());

        // Assert
        assertThat(resultat).isEmpty();
    }


    @Test
    void skal_ikke_finne_endring_når_dato_er_lik_i_alle_grunnlag() {
        // Arrange
        LocalDate gjeldendeFom = LocalDate.of(2024, 1, 1);
        PeriodeSnapshot gjeldendeSnapshot = snapshot(gjeldendeFom, LocalDate.of(2024, 12, 31));
        PeriodeSnapshot snapshot1 = snapshot(gjeldendeFom, LocalDate.of(2024, 12, 31));
        PeriodeSnapshot snapshot2 = snapshot(gjeldendeFom, LocalDate.of(2024, 12, 31));

        // Act
        Optional<SisteEndringsdatoUtleder.EndretDato> resultat =
            SisteEndringsdatoUtleder.finnSistEndretDato(gjeldendeSnapshot, List.of(snapshot1, snapshot2), getFomDato());

        // Assert
        assertThat(resultat).isEmpty();
    }


    @Test
    void skal_finne_endring_når_dato_er_ulik_i_første_grunnlag() {
        // Arrange
        LocalDate gjeldendeFom = LocalDate.of(2024, 1, 1);
        LocalDate forrigeFom = LocalDate.of(2024, 2, 1);
        PeriodeSnapshot gjeldendeSnapshot = snapshot(gjeldendeFom, LocalDate.of(2024, 12, 31));
        PeriodeSnapshot snapshot1 = snapshot(forrigeFom, LocalDate.of(2024, 12, 31));

        // Act
        Optional<SisteEndringsdatoUtleder.EndretDato> resultat =
            SisteEndringsdatoUtleder.finnSistEndretDato(gjeldendeSnapshot, List.of(snapshot1), getFomDato());

        // Assert
        assertThat(resultat).isPresent();
        assertThat(resultat.get().nyDatoOgBeskrivelse().dato()).isEqualTo(gjeldendeFom);
        assertThat(resultat.get().forrigeDatoOgBeskrivelse().dato()).isEqualTo(forrigeFom);
    }

    @Test
    void skal_finne_endring_i_første_grunnlag_som_har_ulik_dato() {
        // Arrange
        LocalDate gjeldendeFom = LocalDate.of(2024, 1, 1);
        LocalDate forrigeFom = LocalDate.of(2024, 2, 1);
        LocalDate eldsteFom = LocalDate.of(2024, 3, 1);

        PeriodeSnapshot gjeldendeSnapshot = snapshot(gjeldendeFom, LocalDate.of(2024, 12, 31));
        PeriodeSnapshot sameSomGjeldende = snapshot(gjeldendeFom, LocalDate.of(2024, 11, 30)); // Ulik TOM
        PeriodeSnapshot forrige = snapshot(forrigeFom, LocalDate.of(2024, 12, 31));            // Første med ulik FOM
        PeriodeSnapshot eldste = snapshot(eldsteFom, LocalDate.of(2024, 12, 31));              // Skal ikke nås

        // Act
        Optional<SisteEndringsdatoUtleder.EndretDato> resultat =
            SisteEndringsdatoUtleder.finnSistEndretDato(gjeldendeSnapshot, List.of(sameSomGjeldende, forrige, eldste), getFomDato());

        // Assert
        assertThat(resultat).isPresent();
        assertThat(resultat.get().nyDatoOgBeskrivelse().dato()).isEqualTo(gjeldendeFom);
        assertThat(resultat.get().forrigeDatoOgBeskrivelse().dato()).isEqualTo(forrigeFom);
    }

    @Test
    void skal_finne_endring_i_tredje_grunnlag() {
        // Arrange
        LocalDate gjeldendeFom = LocalDate.of(2024, 1, 1);
        LocalDate forrigeFom = LocalDate.of(2024, 2, 1);

        PeriodeSnapshot gjeldendeSnapshot = snapshot(gjeldendeFom, LocalDate.of(2024, 12, 31));
        PeriodeSnapshot sameSomGjeldende1 = snapshot(gjeldendeFom, LocalDate.of(2024, 11, 30)); // Ulik TOM
        PeriodeSnapshot sameSomGjeldende2 = snapshot(gjeldendeFom, LocalDate.of(2024, 10, 31)); // Ulik TOM
        PeriodeSnapshot forrige = snapshot(forrigeFom, LocalDate.of(2024, 12, 31));             // Tredje med ulik FOM

        // Act
        Optional<SisteEndringsdatoUtleder.EndretDato> resultat =
            SisteEndringsdatoUtleder.finnSistEndretDato(gjeldendeSnapshot, List.of(sameSomGjeldende1, sameSomGjeldende2, forrige), getFomDato());

        // Assert
        assertThat(resultat).isPresent();
        assertThat(resultat.get().nyDatoOgBeskrivelse().dato()).isEqualTo(gjeldendeFom);
        assertThat(resultat.get().forrigeDatoOgBeskrivelse().dato()).isEqualTo(forrigeFom);
    }

    @Test
    void skal_finne_endring_nar_andre_grunnlag_ikke_har_periode() {
        // Arrange
        LocalDate gjeldendeFom = LocalDate.of(2024, 1, 1);
        LocalDate forrigeFom = LocalDate.of(2024, 2, 1);

        PeriodeSnapshot gjeldendeSnapshot = snapshot(gjeldendeFom, LocalDate.of(2024, 12, 31));
        PeriodeSnapshot sameSomGjeldende = snapshot(gjeldendeFom, LocalDate.of(2024, 12, 31));
        PeriodeSnapshot ingenPeriode = new PeriodeSnapshot(Optional.empty(), Optional.empty(), "en beskrivelse");
        PeriodeSnapshot forrige = snapshot(forrigeFom, LocalDate.of(2024, 12, 31));

        // Act
        Optional<SisteEndringsdatoUtleder.EndretDato> resultat =
            SisteEndringsdatoUtleder.finnSistEndretDato(gjeldendeSnapshot, List.of(sameSomGjeldende, ingenPeriode, forrige), getFomDato());

        // Assert
        assertThat(resultat).isPresent();
        assertThat(resultat.get().nyDatoOgBeskrivelse().dato()).isEqualTo(gjeldendeFom);
        assertThat(resultat.get().forrigeDatoOgBeskrivelse().dato()).isNull();
    }

    @Test
    void skal_finne_endring_fra_oppgitt_startdato_når_kun_ett_grunnlag_finnes() {
        // Arrange - Perioden endres mellom søknad og innhenting: gjeldende og initiell er identiske, men oppgitt startdato er ulik
        LocalDate gjeldendeFom = LocalDate.of(2024, 2, 1); // Endret av register
        LocalDate oppgittStartdato = LocalDate.of(2024, 1, 1); // Hva bruker søkte på

        PeriodeSnapshot gjeldendeSnapshot = snapshot(gjeldendeFom, LocalDate.of(2024, 12, 31));
        PeriodeSnapshot initiellSnapshot = snapshot(gjeldendeFom, LocalDate.of(2024, 12, 31)); // Identisk med gjeldende
        PeriodeSnapshot oppgittSnapshot = getOppgittSnapshot(oppgittStartdato);

        List<PeriodeSnapshot> sammenligningsliste = List.of(initiellSnapshot, oppgittSnapshot);

        // Act
        Optional<SisteEndringsdatoUtleder.EndretDato> resultat =
            SisteEndringsdatoUtleder.finnSistEndretDato(gjeldendeSnapshot, sammenligningsliste, getFomDato());

        // Assert
        assertThat(resultat).isPresent();
        assertThat(resultat.get().nyDatoOgBeskrivelse().dato()).isEqualTo(gjeldendeFom);
        assertThat(resultat.get().forrigeDatoOgBeskrivelse().dato()).isEqualTo(oppgittStartdato);
    }

    @Test
    void skal_ikke_finne_endring_fra_oppgitt_startdato_når_den_er_lik_gjeldende() {
        // Arrange - Oppgitt startdato er lik gjeldende (ingen endring)
        LocalDate fom = LocalDate.of(2024, 1, 1);

        PeriodeSnapshot gjeldendeSnapshot = snapshot(fom, LocalDate.of(2024, 12, 31));
        PeriodeSnapshot initiellSnapshot = snapshot(fom, LocalDate.of(2024, 12, 31));
        PeriodeSnapshot oppgittSnapshot = getOppgittSnapshot(fom);

        List<PeriodeSnapshot> sammenligningsliste = List.of(initiellSnapshot, oppgittSnapshot);

        // Act
        Optional<SisteEndringsdatoUtleder.EndretDato> resultat =
            SisteEndringsdatoUtleder.finnSistEndretDato(gjeldendeSnapshot, sammenligningsliste, getFomDato());

        // Assert
        assertThat(resultat).isEmpty();
    }

    private static PeriodeSnapshot getOppgittSnapshot(LocalDate fom) {
        return PeriodeSnapshot.fraOppgittStartdato(new UngdomsytelseSøktStartdato(fom, new JournalpostId("123")));
    }


    private static SisteEndringsdatoUtleder.AktuellDatoHenter getFomDato() {
        return PeriodeSnapshot::fomDato;
    }

}
