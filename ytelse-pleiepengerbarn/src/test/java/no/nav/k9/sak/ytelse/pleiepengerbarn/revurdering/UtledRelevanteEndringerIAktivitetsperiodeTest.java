package no.nav.k9.sak.ytelse.pleiepengerbarn.revurdering;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.kodeverk.arbeidsforhold.AktivitetStatus;
import no.nav.k9.kodeverk.arbeidsforhold.ArbeidType;
import no.nav.k9.kodeverk.arbeidsforhold.Inntektskategori;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.registerendringer.Endringstype;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;

class UtledRelevanteEndringerIAktivitetsperiodeTest {

    @Test
    void skal_utvide_med_dagen_før_stp_ved_revurdering() {

        var fomAktivitetsEndring = LocalDate.now();
        var tomAktivitetsEndring = fomAktivitetsEndring.plusDays(10);
        var arbeidsgiver = Arbeidsgiver.virksomhet("123456789");
        var stp = fomAktivitetsEndring.minusDays(5);
        var ref = InternArbeidsforholdRef.nyRef();

        var endringVedStp = new LocalDateSegment<>(stp.minusDays(1), stp, Endringstype.NY_PERIODE);
        var aktivitetsperiodeEndring = new AktivitetsperiodeEndring(new AktivitetsIdentifikator(arbeidsgiver, ref, ArbeidType.ORDINÆRT_ARBEIDSFORHOLD),
            new LocalDateTimeline<>(List.of(
                endringVedStp,
                new LocalDateSegment<>(fomAktivitetsEndring, tomAktivitetsEndring, Endringstype.NY_PERIODE))));

        var utbetalingsendring = new UtbetalingsendringerForMottaker(new MottakerNøkkel(true, arbeidsgiver, InternArbeidsforholdRef.nullRef(), AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER),
            new LocalDateTimeline<>(fomAktivitetsEndring, tomAktivitetsEndring, Boolean.TRUE));

        var relevanteEndringer = UtledRelevanteEndringerIAktivitetsperiode.finnRelevanteEndringerIAktivitetsperiode(
            List.of(aktivitetsperiodeEndring),
            List.of(utbetalingsendring),
            Set.of(DatoIntervallEntitet.fraOgMedTilOgMed(stp, tomAktivitetsEndring))
        );

        assertThat(relevanteEndringer.size()).isEqualTo(1);
        assertThat(relevanteEndringer.get(0).getArbeidsgiver()).isEqualTo(arbeidsgiver);
        assertThat(relevanteEndringer.get(0).getArbeidsforholdRef()).isEqualTo(ref);
        var segmenter = relevanteEndringer.get(0).getEndringerForUtbetaling().toSegments();
        assertThat(segmenter.size()).isEqualTo(1);
        assertThat(segmenter.contains(new LocalDateSegment<>(fomAktivitetsEndring, tomAktivitetsEndring, Endringstype.NY_PERIODE))).isTrue();
    }

    @Test
    void skal_utlede_endringer_ved_kun_endring_før_stp() {

        var fomAktivitetsEndring = LocalDate.now();
        var tomAktivitetsEndring = fomAktivitetsEndring.plusDays(10);
        var arbeidsgiver = Arbeidsgiver.virksomhet("123456789");
        var stp = fomAktivitetsEndring.minusDays(5);
        var ref = InternArbeidsforholdRef.nyRef();

        var endringVedStp = new LocalDateSegment<>(stp.minusDays(1), stp, Endringstype.NY_PERIODE);
        var aktivitetsperiodeEndring = new AktivitetsperiodeEndring(new AktivitetsIdentifikator(arbeidsgiver, ref, ArbeidType.ORDINÆRT_ARBEIDSFORHOLD),
            new LocalDateTimeline<>(List.of(
                endringVedStp)));

        var utbetalingsendring = new UtbetalingsendringerForMottaker(new MottakerNøkkel(true, arbeidsgiver, InternArbeidsforholdRef.nullRef(), AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER),
            new LocalDateTimeline<>(fomAktivitetsEndring, tomAktivitetsEndring, Boolean.TRUE));

        var relevanteEndringer = UtledRelevanteEndringerIAktivitetsperiode.finnRelevanteEndringerIAktivitetsperiode(
            List.of(aktivitetsperiodeEndring),
            List.of(utbetalingsendring),
            Set.of(DatoIntervallEntitet.fraOgMedTilOgMed(stp, tomAktivitetsEndring))
        );

        assertThat(relevanteEndringer.size()).isEqualTo(1);
        assertThat(relevanteEndringer.get(0).getArbeidsgiver()).isEqualTo(arbeidsgiver);
        assertThat(relevanteEndringer.get(0).getArbeidsforholdRef()).isEqualTo(ref);
        var segmenter = relevanteEndringer.get(0).getEndringerForUtbetaling().toSegments();
        assertThat(segmenter.size()).isEqualTo(1);
        assertThat(segmenter.contains(new LocalDateSegment<>(fomAktivitetsEndring, tomAktivitetsEndring, Endringstype.NY_PERIODE))).isTrue();
    }

    @Test
    void skal_velge_endring_før_stp_ved_ulike_endringer() {

        var fomAktivitetsEndring = LocalDate.now();
        var tomAktivitetsEndring = fomAktivitetsEndring.plusDays(10);
        var arbeidsgiver = Arbeidsgiver.virksomhet("123456789");
        var stp = fomAktivitetsEndring.minusDays(5);
        var ref = InternArbeidsforholdRef.nyRef();

        var endringVedStp = new LocalDateSegment<>(stp.minusDays(1), stp, Endringstype.FJERNET_PERIODE);
        var aktivitetsperiodeEndring = new AktivitetsperiodeEndring(new AktivitetsIdentifikator(arbeidsgiver, ref, ArbeidType.ORDINÆRT_ARBEIDSFORHOLD),
            new LocalDateTimeline<>(List.of(
                endringVedStp,
                new LocalDateSegment<>(fomAktivitetsEndring, tomAktivitetsEndring, Endringstype.NY_PERIODE))));

        var utbetalingsendring = new UtbetalingsendringerForMottaker(new MottakerNøkkel(true, arbeidsgiver, InternArbeidsforholdRef.nullRef(), AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER),
            new LocalDateTimeline<>(fomAktivitetsEndring, tomAktivitetsEndring, Boolean.TRUE));

        var relevanteEndringer = UtledRelevanteEndringerIAktivitetsperiode.finnRelevanteEndringerIAktivitetsperiode(
            List.of(aktivitetsperiodeEndring),
            List.of(utbetalingsendring),
            Set.of(DatoIntervallEntitet.fraOgMedTilOgMed(stp, tomAktivitetsEndring))
        );

        assertThat(relevanteEndringer.size()).isEqualTo(1);
        assertThat(relevanteEndringer.get(0).getArbeidsgiver()).isEqualTo(arbeidsgiver);
        assertThat(relevanteEndringer.get(0).getArbeidsforholdRef()).isEqualTo(ref);
        var segmenter = relevanteEndringer.get(0).getEndringerForUtbetaling().toSegments();
        assertThat(segmenter.size()).isEqualTo(1);
        assertThat(segmenter.contains(new LocalDateSegment<>(fomAktivitetsEndring, tomAktivitetsEndring, Endringstype.FJERNET_PERIODE))).isTrue();
    }


}
