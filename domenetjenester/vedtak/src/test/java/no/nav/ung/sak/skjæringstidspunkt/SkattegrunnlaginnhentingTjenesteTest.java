package no.nav.ung.sak.skjæringstidspunkt;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class SkattegrunnlaginnhentingTjenesteTest {

    public static final LocalDate SKATTEOPPGJØR_2019 = LocalDate.of(2019, 5, 1);
    public static final LocalDate SKATTEOPPGJØR_2020 = LocalDate.of(2020, 5, 1);

    @Test
    void skal_utlede_3_år_før_stp_dersom_siste_tilgjengelige_skatteoppjør_er_året_før_stp_og_dagens_dato_er_skatteoppgjørsdato() {

        var stp_2019 = LocalDate.of(2019, 1, 1);
        var dagensDato = SKATTEOPPGJØR_2019;
        var periode = SkattegrunnlaginnhentingTjeneste.utledSkattegrunnlagOpplysningsperiode(stp_2019, stp_2019.plusMonths(3), dagensDato);


        assertThat(periode.getFom()).isEqualTo(LocalDate.of(2016, 1, 1));
    }


    @Test
    void skal_utlede_3_år_før_stp_dersom_siste_tilgjengelige_skatteoppjør_er_året_etter_stp() {
        var stp_2019 = LocalDate.of(2019, 1, 1);
        var dagensDato = SKATTEOPPGJØR_2020;
        var periode = SkattegrunnlaginnhentingTjeneste.utledSkattegrunnlagOpplysningsperiode(stp_2019, stp_2019.plusMonths(3), dagensDato);


        assertThat(periode.getFom()).isEqualTo(LocalDate.of(2016, 1, 1));
    }

    @Test
    void skal_utlede_4_år_før_stp_dersom_dagens_dato_er_dagen_før_skatteoppgjørsdato_for_året_før_stp() {
        var stp_2020 = LocalDate.of(2020, 1, 1);
        var dagensDato = SKATTEOPPGJØR_2020.minusDays(1);
        var periode = SkattegrunnlaginnhentingTjeneste.utledSkattegrunnlagOpplysningsperiode(stp_2020, stp_2020.plusMonths(3), dagensDato);


        assertThat(periode.getFom()).isEqualTo(LocalDate.of(2016, 1, 1));
    }

    @Test
    void skal_utlede_3_år_før_stp_dersom_dagens_dato_er_dagen_etter_skatteoppgjørsdato_for_samme_år_som_stp() {
        var stp_2019 = LocalDate.of(2019, 1, 1);
        var dagensDato = SKATTEOPPGJØR_2020.minusDays(1);
        var periode = SkattegrunnlaginnhentingTjeneste.utledSkattegrunnlagOpplysningsperiode(stp_2019, stp_2019.plusMonths(3), dagensDato);


        assertThat(periode.getFom()).isEqualTo(LocalDate.of(2016, 1, 1));
    }


    @Test
    void skal_sette_første_år_til_2016_om_stp_2018() {
        var stp_2018 = LocalDate.of(2018, 1, 1);
        var dagensDato = SKATTEOPPGJØR_2020.minusDays(1);
        var periode = SkattegrunnlaginnhentingTjeneste.utledSkattegrunnlagOpplysningsperiode(stp_2018, stp_2018.plusMonths(3), dagensDato);


        assertThat(periode.getFom()).isEqualTo(LocalDate.of(2016, 1, 1));
    }

    @Test
    void skal_sette_siste_år_til_2016_om_stp_fagsakperiode_til_2015() {
        var stp_2015 = LocalDate.of(2015, 1, 1);
        var dagensDato = SKATTEOPPGJØR_2020.minusDays(1);
        var periode = SkattegrunnlaginnhentingTjeneste.utledSkattegrunnlagOpplysningsperiode(stp_2015, stp_2015.plusMonths(3), dagensDato);


        assertThat(periode.getTom()).isEqualTo(LocalDate.of(2016, 12, 31));
    }

}
