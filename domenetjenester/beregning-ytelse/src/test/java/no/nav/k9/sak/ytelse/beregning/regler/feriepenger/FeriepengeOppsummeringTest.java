package no.nav.k9.sak.ytelse.beregning.regler.feriepenger;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.time.Year;

import org.junit.jupiter.api.Test;

import no.nav.k9.kodeverk.arbeidsforhold.Inntektskategori;
import no.nav.k9.sak.ytelse.beregning.regelmodell.MottakerType;

class FeriepengeOppsummeringTest {

    @Test
    void skal_ikke_gi_differanse() {
        var builderUtenFerietillegg = new FeriepengeOppsummering.Builder();
        var orgnr = "99999999";
        builderUtenFerietillegg.leggTil(Year.of(2020), MottakerType.ARBEIDSGIVER, Inntektskategori.ARBEIDSTAKER, orgnr, 10L);

        var builderMedFerietillegg = new FeriepengeOppsummering.Builder();
        builderMedFerietillegg.leggTil(Year.of(2020), MottakerType.ARBEIDSGIVER, Inntektskategori.ARBEIDSTAKER, orgnr, 10L);

        var årMedDifferanse = FeriepengeOppsummering.utledOpptjeningsårSomHarDifferanse(builderMedFerietillegg.build(), builderUtenFerietillegg.build());

        assertThat(årMedDifferanse.isEmpty()).isTrue();

    }

    @Test
    void skal_gi_differanse() {
        var builderUtenFerietillegg = new FeriepengeOppsummering.Builder();
        var orgnr = "99999999";
        builderUtenFerietillegg.leggTil(Year.of(2020), MottakerType.ARBEIDSGIVER, Inntektskategori.ARBEIDSTAKER, orgnr, 10L);

        var builderMedFerietillegg = new FeriepengeOppsummering.Builder();
        builderMedFerietillegg.leggTil(Year.of(2020), MottakerType.ARBEIDSGIVER, Inntektskategori.ARBEIDSTAKER, orgnr, 11L);

        var årMedDifferanse = FeriepengeOppsummering.utledOpptjeningsårSomHarDifferanse(builderMedFerietillegg.build(), builderUtenFerietillegg.build());

        assertThat(årMedDifferanse.isEmpty()).isFalse();

    }

    @Test
    void skal_ikke_ta_med_ferietillegg() {
        var builderUtenFerietillegg = new FeriepengeOppsummering.Builder();
        var orgnr = "99999999";
        builderUtenFerietillegg.leggTil(Year.of(2020), MottakerType.ARBEIDSGIVER, Inntektskategori.ARBEIDSTAKER, orgnr, 10L);

        var builderMedFerietillegg = new FeriepengeOppsummering.Builder();
        builderMedFerietillegg.leggTil(Year.of(2020), MottakerType.ARBEIDSGIVER, Inntektskategori.ARBEIDSTAKER, orgnr, 10L);
        builderMedFerietillegg.leggTil(Year.of(2020), MottakerType.BRUKER, Inntektskategori.DAGPENGER, null, 10L);

        var årMedDifferanse = FeriepengeOppsummering.utledOpptjeningsårSomHarDifferanse(builderMedFerietillegg.build(), builderUtenFerietillegg.build());

        assertThat(årMedDifferanse.isEmpty()).isTrue();

    }
}
