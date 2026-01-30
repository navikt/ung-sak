package no.nav.ung.sak.etterlysning.programperiode;

import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeGrunnlag;
import no.nav.ung.sak.tid.DatoIntervallEntitet;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SisteEndringsdatoUtlederTest {

    @Test
    void skal_ikke_finne_endring_når_det_ikke_finnes_aktuelle_grunnlag() {
        // Arrange
        LocalDate gjeldendeFom = LocalDate.of(2024, 1, 1);
        DatoIntervallEntitet gjeldendePeriode = DatoIntervallEntitet.fraOgMedTilOgMed(gjeldendeFom, LocalDate.of(2024, 12, 31));

        UngdomsprogramPeriodeGrunnlag gjeldendeGrunnlag = mock(UngdomsprogramPeriodeGrunnlag.class);
        when(gjeldendeGrunnlag.hentForEksaktEnPeriodeDersomFinnes()).thenReturn(Optional.of(gjeldendePeriode));

        SisteEndringsdatoUtleder.AktuellDatoHenter datoHenter = grunnlag -> grunnlag.hentForEksaktEnPeriodeDersomFinnes().map(DatoIntervallEntitet::getFomDato);

        List<UngdomsprogramPeriodeGrunnlag> aktuelleGrunnlagSortert = Collections.emptyList();

        // Act
        Optional<SisteEndringsdatoUtleder.EndretDato> resultat =
            SisteEndringsdatoUtleder.finnSistEndretDato(gjeldendeGrunnlag, aktuelleGrunnlagSortert, datoHenter);

        // Assert
        assertThat(resultat).isEmpty();
    }

    @Test
    void skal_ikke_finne_endring_når_dato_er_lik_i_alle_grunnlag() {
        // Arrange
        LocalDate gjeldendeFom = LocalDate.of(2024, 1, 1);
        DatoIntervallEntitet gjeldendePeriode = DatoIntervallEntitet.fraOgMedTilOgMed(gjeldendeFom, LocalDate.of(2024, 12, 31));

        UngdomsprogramPeriodeGrunnlag gjeldendeGrunnlag = mock(UngdomsprogramPeriodeGrunnlag.class);
        when(gjeldendeGrunnlag.hentForEksaktEnPeriodeDersomFinnes()).thenReturn(Optional.of(gjeldendePeriode));

        UngdomsprogramPeriodeGrunnlag grunnlag1 = mock(UngdomsprogramPeriodeGrunnlag.class);
        when(grunnlag1.hentForEksaktEnPeriodeDersomFinnes()).thenReturn(Optional.of(gjeldendePeriode));

        UngdomsprogramPeriodeGrunnlag grunnlag2 = mock(UngdomsprogramPeriodeGrunnlag.class);
        when(grunnlag2.hentForEksaktEnPeriodeDersomFinnes()).thenReturn(Optional.of(gjeldendePeriode));

        SisteEndringsdatoUtleder.AktuellDatoHenter datoHenter = grunnlag -> grunnlag.hentForEksaktEnPeriodeDersomFinnes().map(DatoIntervallEntitet::getFomDato);

        List<UngdomsprogramPeriodeGrunnlag> aktuelleGrunnlagSortert = List.of(grunnlag1, grunnlag2);

        // Act
        Optional<SisteEndringsdatoUtleder.EndretDato> resultat =
            SisteEndringsdatoUtleder.finnSistEndretDato(gjeldendeGrunnlag, aktuelleGrunnlagSortert, datoHenter);

        // Assert
        assertThat(resultat).isEmpty();
    }

    @Test
    void skal_finne_endring_når_dato_er_ulik_i_første_grunnlag() {
        // Arrange
        LocalDate gjeldendeFom = LocalDate.of(2024, 1, 1);
        LocalDate forrigeFom = LocalDate.of(2024, 2, 1);

        DatoIntervallEntitet gjeldendePeriode = DatoIntervallEntitet.fraOgMedTilOgMed(gjeldendeFom, LocalDate.of(2024, 12, 31));
        DatoIntervallEntitet forrigePeriode = DatoIntervallEntitet.fraOgMedTilOgMed(forrigeFom, LocalDate.of(2024, 12, 31));

        UngdomsprogramPeriodeGrunnlag gjeldendeGrunnlag = mock(UngdomsprogramPeriodeGrunnlag.class);
        when(gjeldendeGrunnlag.hentForEksaktEnPeriodeDersomFinnes()).thenReturn(Optional.of(gjeldendePeriode));

        UngdomsprogramPeriodeGrunnlag grunnlag1 = mock(UngdomsprogramPeriodeGrunnlag.class);
        when(grunnlag1.hentForEksaktEnPeriodeDersomFinnes()).thenReturn(Optional.of(forrigePeriode));

        SisteEndringsdatoUtleder.AktuellDatoHenter datoHenter = grunnlag -> grunnlag.hentForEksaktEnPeriodeDersomFinnes().map(DatoIntervallEntitet::getFomDato);

        List<UngdomsprogramPeriodeGrunnlag> aktuelleGrunnlagSortert = List.of(grunnlag1);

        // Act
        Optional<SisteEndringsdatoUtleder.EndretDato> resultat =
            SisteEndringsdatoUtleder.finnSistEndretDato(gjeldendeGrunnlag, aktuelleGrunnlagSortert, datoHenter);

        // Assert
        assertThat(resultat).isPresent();
        assertThat(resultat.get().nyDatoOgGrunnlag().dato()).isEqualTo(gjeldendeFom);
        assertThat(resultat.get().forrigeDatoOgGrunnlag().dato()).isEqualTo(forrigeFom);
    }

    @Test
    void skal_finne_endring_i_første_grunnlag_som_har_ulik_dato() {
        // Arrange
        LocalDate gjeldendeFom = LocalDate.of(2024, 1, 1);
        LocalDate forrigeFom = LocalDate.of(2024, 2, 1);
        LocalDate eldsteFom = LocalDate.of(2024, 3, 1);

        DatoIntervallEntitet gjeldendePeriode = DatoIntervallEntitet.fraOgMedTilOgMed(gjeldendeFom, LocalDate.of(2024, 12, 31));
        DatoIntervallEntitet sameSomGjeldendePeriode = DatoIntervallEntitet.fraOgMedTilOgMed(gjeldendeFom, LocalDate.of(2024, 11, 30)); // Ulik TOM
        DatoIntervallEntitet forrigePeriode = DatoIntervallEntitet.fraOgMedTilOgMed(forrigeFom, LocalDate.of(2024, 12, 31));
        DatoIntervallEntitet eldstePeriode = DatoIntervallEntitet.fraOgMedTilOgMed(eldsteFom, LocalDate.of(2024, 12, 31));

        UngdomsprogramPeriodeGrunnlag gjeldendeGrunnlag = mock(UngdomsprogramPeriodeGrunnlag.class);
        when(gjeldendeGrunnlag.hentForEksaktEnPeriodeDersomFinnes()).thenReturn(Optional.of(gjeldendePeriode));

        UngdomsprogramPeriodeGrunnlag grunnlag1 = mock(UngdomsprogramPeriodeGrunnlag.class);
        when(grunnlag1.hentForEksaktEnPeriodeDersomFinnes()).thenReturn(Optional.of(sameSomGjeldendePeriode)); // Samme FOM, ulik TOM

        UngdomsprogramPeriodeGrunnlag grunnlag2 = mock(UngdomsprogramPeriodeGrunnlag.class);
        when(grunnlag2.hentForEksaktEnPeriodeDersomFinnes()).thenReturn(Optional.of(forrigePeriode)); // Første som er ulik FOM

        UngdomsprogramPeriodeGrunnlag grunnlag3 = mock(UngdomsprogramPeriodeGrunnlag.class);
        when(grunnlag3.hentForEksaktEnPeriodeDersomFinnes()).thenReturn(Optional.of(eldstePeriode)); // Skal ikke nås

        SisteEndringsdatoUtleder.AktuellDatoHenter datoHenter = grunnlag -> grunnlag.hentForEksaktEnPeriodeDersomFinnes().map(DatoIntervallEntitet::getFomDato);

        List<UngdomsprogramPeriodeGrunnlag> aktuelleGrunnlagSortert = List.of(grunnlag1, grunnlag2, grunnlag3);

        // Act
        Optional<SisteEndringsdatoUtleder.EndretDato> resultat =
            SisteEndringsdatoUtleder.finnSistEndretDato(gjeldendeGrunnlag, aktuelleGrunnlagSortert, datoHenter);

        // Assert
        assertThat(resultat).isPresent();
        assertThat(resultat.get().nyDatoOgGrunnlag().dato()).isEqualTo(gjeldendeFom);
        assertThat(resultat.get().forrigeDatoOgGrunnlag().dato()).isEqualTo(forrigeFom);
    }

    @Test
    void skal_finne_endring_i_tredje_grunnlag() {
        // Arrange
        LocalDate gjeldendeFom = LocalDate.of(2024, 1, 1);
        LocalDate forrigeFom = LocalDate.of(2024, 2, 1);

        DatoIntervallEntitet gjeldendePeriode = DatoIntervallEntitet.fraOgMedTilOgMed(gjeldendeFom, LocalDate.of(2024, 12, 31));
        DatoIntervallEntitet sameSomGjeldendePeriode1 = DatoIntervallEntitet.fraOgMedTilOgMed(gjeldendeFom, LocalDate.of(2024, 11, 30)); // Ulik TOM
        DatoIntervallEntitet sameSomGjeldendePeriode2 = DatoIntervallEntitet.fraOgMedTilOgMed(gjeldendeFom, LocalDate.of(2024, 10, 31)); // Ulik TOM
        DatoIntervallEntitet forrigePeriode = DatoIntervallEntitet.fraOgMedTilOgMed(forrigeFom, LocalDate.of(2024, 12, 31));

        UngdomsprogramPeriodeGrunnlag gjeldendeGrunnlag = mock(UngdomsprogramPeriodeGrunnlag.class);
        when(gjeldendeGrunnlag.hentForEksaktEnPeriodeDersomFinnes()).thenReturn(Optional.of(gjeldendePeriode));

        UngdomsprogramPeriodeGrunnlag grunnlag1 = mock(UngdomsprogramPeriodeGrunnlag.class);
        when(grunnlag1.hentForEksaktEnPeriodeDersomFinnes()).thenReturn(Optional.of(sameSomGjeldendePeriode1)); // Samme FOM, ulik TOM

        UngdomsprogramPeriodeGrunnlag grunnlag2 = mock(UngdomsprogramPeriodeGrunnlag.class);
        when(grunnlag2.hentForEksaktEnPeriodeDersomFinnes()).thenReturn(Optional.of(sameSomGjeldendePeriode2)); // Samme FOM, ulik TOM

        UngdomsprogramPeriodeGrunnlag grunnlag3 = mock(UngdomsprogramPeriodeGrunnlag.class);
        when(grunnlag3.hentForEksaktEnPeriodeDersomFinnes()).thenReturn(Optional.of(forrigePeriode)); // Tredje som er ulik FOM

        SisteEndringsdatoUtleder.AktuellDatoHenter datoHenter = grunnlag -> grunnlag.hentForEksaktEnPeriodeDersomFinnes().map(DatoIntervallEntitet::getFomDato);

        List<UngdomsprogramPeriodeGrunnlag> aktuelleGrunnlagSortert = List.of(grunnlag1, grunnlag2, grunnlag3);

        // Act
        Optional<SisteEndringsdatoUtleder.EndretDato> resultat =
            SisteEndringsdatoUtleder.finnSistEndretDato(gjeldendeGrunnlag, aktuelleGrunnlagSortert, datoHenter);

        // Assert
        assertThat(resultat).isPresent();
        assertThat(resultat.get().nyDatoOgGrunnlag().dato()).isEqualTo(gjeldendeFom);
        assertThat(resultat.get().forrigeDatoOgGrunnlag().dato()).isEqualTo(forrigeFom);
    }

    @Test
    void skal_ikke_finne_endring_når_det_ikke_finnes_periode_i_gjeldende_grunnlag() {
        // Arrange
        UngdomsprogramPeriodeGrunnlag gjeldendeGrunnlag = mock(UngdomsprogramPeriodeGrunnlag.class);
        when(gjeldendeGrunnlag.hentForEksaktEnPeriodeDersomFinnes()).thenReturn(Optional.empty());

        SisteEndringsdatoUtleder.AktuellDatoHenter datoHenter = grunnlag ->
            grunnlag.hentForEksaktEnPeriodeDersomFinnes().map(DatoIntervallEntitet::getFomDato);

        List<UngdomsprogramPeriodeGrunnlag> aktuelleGrunnlagSortert = Collections.emptyList();

        // Act
        Optional<SisteEndringsdatoUtleder.EndretDato> resultat =
            SisteEndringsdatoUtleder.finnSistEndretDato(gjeldendeGrunnlag, aktuelleGrunnlagSortert, datoHenter);

        // Assert
        assertThat(resultat).isEmpty();
    }

    @Test
    void skal_finne_endring_nar_andre_grunnlag_ikke_har_periode() {
        // Arrange
        LocalDate gjeldendeFom = LocalDate.of(2024, 1, 1);
        LocalDate forrigeFom = LocalDate.of(2024, 2, 1);

        DatoIntervallEntitet gjeldendePeriode = DatoIntervallEntitet.fraOgMedTilOgMed(gjeldendeFom, LocalDate.of(2024, 12, 31));
        DatoIntervallEntitet forrigePeriode = DatoIntervallEntitet.fraOgMedTilOgMed(forrigeFom, LocalDate.of(2024, 12, 31));

        UngdomsprogramPeriodeGrunnlag gjeldendeGrunnlag = mock(UngdomsprogramPeriodeGrunnlag.class);
        when(gjeldendeGrunnlag.hentForEksaktEnPeriodeDersomFinnes()).thenReturn(Optional.of(gjeldendePeriode));

        UngdomsprogramPeriodeGrunnlag grunnlag1 = mock(UngdomsprogramPeriodeGrunnlag.class);
        when(grunnlag1.hentForEksaktEnPeriodeDersomFinnes()).thenReturn(Optional.of(gjeldendePeriode)); // Samme FOM

        UngdomsprogramPeriodeGrunnlag grunnlag2 = mock(UngdomsprogramPeriodeGrunnlag.class);
        when(grunnlag2.hentForEksaktEnPeriodeDersomFinnes()).thenReturn(Optional.empty()); // Ingen periode

        UngdomsprogramPeriodeGrunnlag grunnlag3 = mock(UngdomsprogramPeriodeGrunnlag.class);
        when(grunnlag3.hentForEksaktEnPeriodeDersomFinnes()).thenReturn(Optional.of(forrigePeriode)); // Første som er ulik FOM

        SisteEndringsdatoUtleder.AktuellDatoHenter datoHenter = grunnlag ->
            grunnlag.hentForEksaktEnPeriodeDersomFinnes().map(DatoIntervallEntitet::getFomDato);

        List<UngdomsprogramPeriodeGrunnlag> aktuelleGrunnlagSortert = List.of(grunnlag1, grunnlag2, grunnlag3);

        // Act
        Optional<SisteEndringsdatoUtleder.EndretDato> resultat =
            SisteEndringsdatoUtleder.finnSistEndretDato(gjeldendeGrunnlag, aktuelleGrunnlagSortert, datoHenter);

        // Assert
        assertThat(resultat).isPresent();
        assertThat(resultat.get().nyDatoOgGrunnlag().dato()).isEqualTo(gjeldendeFom);
        assertThat(resultat.get().forrigeDatoOgGrunnlag().dato()).isNull();
    }
}
