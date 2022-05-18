package no.nav.k9.sak.kontrakt.beregninginput;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import no.nav.k9.sak.typer.OrgNummer;

class OverstyrBeregningAktivitetTest {

    @Test
    void skal_returnere_false_ved_manglende_orgnr_og_aktørid() {
        var overstyrBeregningAktivitet = new OverstyrBeregningAktivitet(null, null, 1, 1, null, null, true);
        assertThat(overstyrBeregningAktivitet.harOrgnrEllerAktørid()).isFalse();
    }

    @Test
    void skal_returnere_true_ved_orgnr() {
        var overstyrBeregningAktivitet = new OverstyrBeregningAktivitet(new OrgNummer("910909088"), null, 1, 1, null, null, true);
        assertThat(overstyrBeregningAktivitet.harOrgnrEllerAktørid()).isTrue();
    }

    @Test
    void skal_returnere_false_ved_startdato_lik_opphør() {
        var overstyrBeregningAktivitet = new OverstyrBeregningAktivitet(new OrgNummer("910909088"), null, 1, 1, LocalDate.now(), LocalDate.now(), true);
        assertThat(overstyrBeregningAktivitet.erStartdatoRefusjonFørOpphør()).isFalse();
    }

    @Test
    void skal_returnere_false_ved_startdato_etter_opphør() {
        var overstyrBeregningAktivitet = new OverstyrBeregningAktivitet(new OrgNummer("910909088"), null, 1, 1, LocalDate.now().plusDays(1), LocalDate.now(), true);
        assertThat(overstyrBeregningAktivitet.erStartdatoRefusjonFørOpphør()).isFalse();
    }

    @Test
    void skal_returnere_true_ved_startdato_før_opphør() {
        var overstyrBeregningAktivitet = new OverstyrBeregningAktivitet(new OrgNummer("910909088"), null, 1, 1, LocalDate.now().minusDays(1), LocalDate.now(), true);
        assertThat(overstyrBeregningAktivitet.erStartdatoRefusjonFørOpphør()).isTrue();
    }

    @Test
    void skal_returnere_true_når_startdato_er_null() {
        var overstyrBeregningAktivitet = new OverstyrBeregningAktivitet(new OrgNummer("910909088"), null, 1, 1, null, LocalDate.now(), true);
        assertThat(overstyrBeregningAktivitet.erStartdatoRefusjonFørOpphør()).isTrue();
    }

    @Test
    void skal_returnere_true_når_opphør_er_null() {
        var overstyrBeregningAktivitet = new OverstyrBeregningAktivitet(new OrgNummer("910909088"), null, 1, 1, LocalDate.now(), null, true);
        assertThat(overstyrBeregningAktivitet.erStartdatoRefusjonFørOpphør()).isTrue();
    }

}
