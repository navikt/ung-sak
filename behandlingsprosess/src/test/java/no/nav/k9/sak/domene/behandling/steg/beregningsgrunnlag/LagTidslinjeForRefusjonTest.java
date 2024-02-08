package no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag;

import static no.nav.k9.kodeverk.uttak.Tid.TIDENES_ENDE;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import no.nav.folketrygdloven.beregningsgrunnlag.inntektsmelding.LagTidslinjeForRefusjon;
import no.nav.k9.sak.domene.iay.modell.InntektsmeldingBuilder;
import no.nav.k9.sak.domene.iay.modell.Refusjon;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.Beløp;

class LagTidslinjeForRefusjonTest {

    public static final LocalDate STP = LocalDate.now();

    @Test
    void skal_returnere_tidslinje_for_hele_perioder_med_nullbeløp_dersom_ingen_refusjon() {

        var im = InntektsmeldingBuilder
            .builder().medArbeidsgiver(Arbeidsgiver.virksomhet("123456789"))
            .medBeløp(BigDecimal.TEN)
            .medKanalreferanse("kanalreferanse")
            .build();


        var tidslinjeRefusjon = LagTidslinjeForRefusjon.lagRefusjontidslinje(im, DatoIntervallEntitet.fraOgMedTilOgMed(STP, STP.plusDays(10)).getFomDato());

        var segmenter = tidslinjeRefusjon.toSegments();
        assertThat(segmenter.size()).isEqualTo(1);
        var enesteSegment = segmenter.iterator().next();
        assertThat(enesteSegment.getValue()).isEqualTo(Beløp.ZERO);
    }


    @Test
    void skal_returnere_tidslinje_for_hele_perioder_med_beløp_dersom_ingen_refusjon_uten_opphør() {

        var im = InntektsmeldingBuilder
            .builder().medArbeidsgiver(Arbeidsgiver.virksomhet("123456789"))
            .medBeløp(BigDecimal.TEN)
            .medRefusjon(BigDecimal.TWO)
            .medKanalreferanse("kanalreferanse")
            .build();


        var tidslinjeRefusjon = LagTidslinjeForRefusjon.lagRefusjontidslinje(im, DatoIntervallEntitet.fraOgMedTilOgMed(STP, STP.plusDays(10)).getFomDato());

        var segmenter = tidslinjeRefusjon.toSegments();
        assertThat(segmenter.size()).isEqualTo(1);
        var enesteSegment = segmenter.iterator().next();
        assertThat(enesteSegment.getValue()).isEqualTo(new Beløp(2));
    }


    @Test
    void skal_returnere_riktig_tidslinje_med_opphør() {
        var tom = STP.plusDays(10);

        var im = InntektsmeldingBuilder
            .builder().medArbeidsgiver(Arbeidsgiver.virksomhet("123456789"))
            .medBeløp(BigDecimal.TEN)
            .medRefusjon(BigDecimal.TWO, tom.minusDays(1))
            .medKanalreferanse("kanalreferanse")
            .build();


        var tidslinjeRefusjon = LagTidslinjeForRefusjon.lagRefusjontidslinje(im, DatoIntervallEntitet.fraOgMedTilOgMed(STP, tom).getFomDato());

        var segmenter = tidslinjeRefusjon.toSegments();
        assertThat(segmenter.size()).isEqualTo(2);
        var iterator = segmenter.iterator();
        var førsteSegment = iterator.next();
        assertThat(førsteSegment.getValue()).isEqualTo(new Beløp(2));
        assertThat(førsteSegment.getFom()).isEqualTo(STP);
        assertThat(førsteSegment.getTom()).isEqualTo(tom.minusDays(1));

        var andreSegment = iterator.next();
        assertThat(andreSegment.getValue()).isEqualTo(new Beløp(0));
        assertThat(andreSegment.getFom()).isEqualTo(tom);
        assertThat(andreSegment.getTom()).isEqualTo(TIDENES_ENDE);
    }

    @Test
    void skal_returnere_riktig_tidslinje_med_opphør_andre_dag() {
        var tom = STP.plusDays(10);

        var im = InntektsmeldingBuilder
            .builder().medArbeidsgiver(Arbeidsgiver.virksomhet("123456789"))
            .medBeløp(BigDecimal.TEN)
            .medRefusjon(BigDecimal.TWO, STP)
            .medKanalreferanse("kanalreferanse")
            .build();


        var tidslinjeRefusjon = LagTidslinjeForRefusjon.lagRefusjontidslinje(im, DatoIntervallEntitet.fraOgMedTilOgMed(STP, tom).getFomDato());

        var segmenter = tidslinjeRefusjon.toSegments();
        assertThat(segmenter.size()).isEqualTo(2);
        var iterator = segmenter.iterator();
        var førsteSegment = iterator.next();
        assertThat(førsteSegment.getValue()).isEqualTo(new Beløp(2));
        assertThat(førsteSegment.getFom()).isEqualTo(STP);
        assertThat(førsteSegment.getTom()).isEqualTo(STP);

        var andreSegment = iterator.next();
        assertThat(andreSegment.getValue()).isEqualTo(new Beløp(0));
        assertThat(andreSegment.getFom()).isEqualTo(STP.plusDays(1));
        assertThat(andreSegment.getTom()).isEqualTo(TIDENES_ENDE);
    }

    @Test
    void skal_returnere_riktig_tidslinje_med_opphør_første_dag() {
        var tom = STP.plusDays(10);

        var im = InntektsmeldingBuilder
            .builder().medArbeidsgiver(Arbeidsgiver.virksomhet("123456789"))
            .medBeløp(BigDecimal.TEN)
            .medStartDatoPermisjon(STP)
            .medRefusjon(BigDecimal.TWO, STP.minusDays(1))
            .medKanalreferanse("kanalreferanse")
            .build();


        var tidslinjeRefusjon = LagTidslinjeForRefusjon.lagRefusjontidslinje(im, DatoIntervallEntitet.fraOgMedTilOgMed(STP, tom).getFomDato());

        var segmenter = tidslinjeRefusjon.toSegments();
        assertThat(segmenter.size()).isEqualTo(1);
        var iterator = segmenter.iterator();
        var førsteSegment = iterator.next();
        assertThat(førsteSegment.getValue()).isEqualTo(new Beløp(0));
        assertThat(førsteSegment.getFom()).isEqualTo(STP);
        assertThat(førsteSegment.getTom()).isEqualTo(TIDENES_ENDE);
    }

    @Test
    void skal_returnere_riktig_tidslinje_med_endringer_fra_andre_dag() {
        var tom = STP.plusDays(10);

        var im = InntektsmeldingBuilder
            .builder().medArbeidsgiver(Arbeidsgiver.virksomhet("123456789"))
            .medBeløp(BigDecimal.TEN)
            .medStartDatoPermisjon(STP)
            .medRefusjon(BigDecimal.TWO)
            .leggTil(new Refusjon(BigDecimal.valueOf(100), STP.plusDays(1)))
            .medKanalreferanse("kanalreferanse")
            .build();


        var tidslinjeRefusjon = LagTidslinjeForRefusjon.lagRefusjontidslinje(im, DatoIntervallEntitet.fraOgMedTilOgMed(STP, tom).getFomDato());

        var segmenter = tidslinjeRefusjon.toSegments();
        assertThat(segmenter.size()).isEqualTo(2);
        var iterator = segmenter.iterator();
        var førsteSegment = iterator.next();
        assertThat(førsteSegment.getValue()).isEqualTo(new Beløp(2));
        assertThat(førsteSegment.getFom()).isEqualTo(STP);
        assertThat(førsteSegment.getTom()).isEqualTo(STP);

        var andreSegment = iterator.next();
        assertThat(andreSegment.getValue()).isEqualTo(new Beløp(100));
        assertThat(andreSegment.getFom()).isEqualTo(STP.plusDays(1));
        assertThat(andreSegment.getTom()).isEqualTo(TIDENES_ENDE);
    }

    @Test
    void skal_returnere_riktig_tidslinje_med_flere_endringer_og_opphør() {
        var tom = STP.plusDays(10);

        var im = InntektsmeldingBuilder
            .builder().medArbeidsgiver(Arbeidsgiver.virksomhet("123456789"))
            .medBeløp(BigDecimal.TEN)
            .medStartDatoPermisjon(STP)
            .medRefusjon(BigDecimal.TWO, STP.plusDays(2))
            .leggTil(new Refusjon(BigDecimal.valueOf(100), STP.plusDays(1)))
            .leggTil(new Refusjon(BigDecimal.valueOf(101), STP.plusDays(2)))
            .medKanalreferanse("kanalreferanse")
            .build();


        var tidslinjeRefusjon = LagTidslinjeForRefusjon.lagRefusjontidslinje(im, DatoIntervallEntitet.fraOgMedTilOgMed(STP, tom).getFomDato());

        var segmenter = tidslinjeRefusjon.toSegments();
        assertThat(segmenter.size()).isEqualTo(4);
        var iterator = segmenter.iterator();
        var førsteSegment = iterator.next();
        assertThat(førsteSegment.getValue()).isEqualTo(new Beløp(2));
        assertThat(førsteSegment.getFom()).isEqualTo(STP);
        assertThat(førsteSegment.getTom()).isEqualTo(STP);

        var andreSegment = iterator.next();
        assertThat(andreSegment.getValue()).isEqualTo(new Beløp(100));
        assertThat(andreSegment.getFom()).isEqualTo(STP.plusDays(1));
        assertThat(andreSegment.getTom()).isEqualTo(STP.plusDays(1));

        var tredjeSegment = iterator.next();
        assertThat(tredjeSegment.getValue()).isEqualTo(new Beløp(101));
        assertThat(tredjeSegment.getFom()).isEqualTo(STP.plusDays(2));
        assertThat(tredjeSegment.getTom()).isEqualTo(STP.plusDays(2));

        var fjerdeSegment = iterator.next();
        assertThat(fjerdeSegment.getValue()).isEqualTo(new Beløp(0));
        assertThat(fjerdeSegment.getFom()).isEqualTo(STP.plusDays(3));
        assertThat(fjerdeSegment.getTom()).isEqualTo(TIDENES_ENDE);
    }


}
