package no.nav.k9.sak.domene.iay.modell;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import no.nav.k9.kodeverk.arbeidsforhold.ArbeidType;
import no.nav.k9.kodeverk.arbeidsforhold.ArbeidsforholdHandlingType;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.vedtak.konfig.Tid;

public class YrkesaktivitetEntitetTest {

    ArbeidsforholdInformasjonBuilder builder = ArbeidsforholdInformasjonBuilder.builder(Optional.empty());

    @Test
    public void skal_ikke_legge_overstyrt_periode_når_overstyrt_handling_ikke_er_BRUK_MED_OVERSTYRT_PERIODE() {

        // Arrange
        LocalDate fom = LocalDate.of(2015, 8, 1);
        LocalDate tom = Tid.TIDENES_ENDE;
        LocalDate overstyrtTom = LocalDate.of(2019, 8, 1);

        ArbeidsforholdOverstyringBuilder entitet = ArbeidsforholdOverstyringBuilder.ny()
            .medHandling(ArbeidsforholdHandlingType.BRUK_UTEN_INNTEKTSMELDING)
            .leggTilOverstyrtPeriode(fom, overstyrtTom);
        AktivitetsAvtaleBuilder aktivitetsAvtale = AktivitetsAvtaleBuilder.ny()
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom));
        Yrkesaktivitet ya = YrkesaktivitetBuilder.oppdatere(Optional.empty())
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .leggTilAktivitetsAvtale(aktivitetsAvtale)
            .build();

        Yrkesaktivitet yrkesaktivitet = new Yrkesaktivitet(ya);

        // Act
        overstyrYrkesaktivitet(entitet);

        // Arrange
        List<AktivitetsAvtale> ansettelsesPerioder = getAnsettelsesPerioder(yrkesaktivitet);
        assertThat(ansettelsesPerioder).hasSize(1);
        assertThat(ansettelsesPerioder.get(0).getPeriode().getFomDato()).isEqualTo(fom);
        assertThat(ansettelsesPerioder.get(0).getPeriode().getTomDato()).isEqualTo(tom);

    }

    private void overstyrYrkesaktivitet(ArbeidsforholdOverstyringBuilder overstyring) {
        builder.leggTil(overstyring);
    }

    private List<AktivitetsAvtale> getAnsettelsesPerioder(Yrkesaktivitet ya) {
        return new YrkesaktivitetFilter(builder.build(), ya).getAnsettelsesPerioder(ya);
    }

}
