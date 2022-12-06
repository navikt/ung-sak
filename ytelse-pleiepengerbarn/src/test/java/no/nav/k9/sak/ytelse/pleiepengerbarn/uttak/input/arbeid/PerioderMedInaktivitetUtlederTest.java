package no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.input.arbeid;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.kodeverk.arbeidsforhold.ArbeidType;
import no.nav.k9.kodeverk.arbeidsforhold.PermisjonsbeskrivelseType;
import no.nav.k9.kodeverk.uttak.UttakArbeidType;
import no.nav.k9.sak.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.iay.modell.AktivitetsAvtaleBuilder;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.k9.sak.domene.iay.modell.VersjonType;
import no.nav.k9.sak.domene.iay.modell.YrkesaktivitetBuilder;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;

class PerioderMedInaktivitetUtlederTest {

    public static final long DUMMY_BEHANDLING_ID = 1L;
    private PerioderMedInaktivitetUtleder utleder = new PerioderMedInaktivitetUtleder();
    private InntektArbeidYtelseTjeneste iayTjeneste;

    @BeforeEach
    public void setUp() {
        iayTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();
    }

    @Test
    void skal_utlede_tom_tidslinje_hvis_ingen_perioder_er_til_vurdering() {
        var input = new InaktivitetUtlederInput(AktørId.dummy(), LocalDateTimeline.empty(), null, true);
        var utledetTidslinje = utleder.utled(input);

        assertThat(utledetTidslinje).isEmpty();
    }

    @Test
    void skal_utlede_tidslinje_hvis_hvor_kun_periodene_hvor_det_ikke_finnes_yrkesaktivitet() {
        var fom = LocalDate.now();
        var tom = LocalDate.now().plusDays(14);
        var builder = InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.empty(), VersjonType.REGISTER);
        var brukerAktørId = AktørId.dummy();
        iayTjeneste.lagreIayAggregat(DUMMY_BEHANDLING_ID, builder.leggTilAktørArbeid(builder.getAktørArbeidBuilder(brukerAktørId)
            .leggTilYrkesaktivitet(YrkesaktivitetBuilder.oppdatere(Optional.empty())
                .medArbeidsgiver(Arbeidsgiver.virksomhet("000000000"))
                .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
                .leggTilAktivitetsAvtale(AktivitetsAvtaleBuilder.ny()
                    .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom.minusDays(3), tom.minusDays(3)))))));
        var grunnlag = iayTjeneste.hentGrunnlag(DUMMY_BEHANDLING_ID);

        var periodeTilVurdering = new LocalDateTimeline<>(List.of(new LocalDateSegment<>(fom, tom, true)));
        var input = new InaktivitetUtlederInput(brukerAktørId, periodeTilVurdering, grunnlag, true);
        var utledetTidslinje = utleder.utled(input);

        assertThat(utledetTidslinje).hasSize(1);
        var aktivitetIdentifikator = utledetTidslinje.keySet().iterator().next();
        assertThat(aktivitetIdentifikator.getAktivitetType()).isEqualTo(UttakArbeidType.IKKE_YRKESAKTIV_UTEN_ERSTATNING);
    }

    @Test
    void skal_utlede_tidslinje_der_inaktivt_arbeid_erstattes_av_nytt() {
        var fom = LocalDate.now();
        var tom = LocalDate.now().plusDays(14);
        var builder = InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.empty(), VersjonType.REGISTER);
        var brukerAktørId = AktørId.dummy();
        iayTjeneste.lagreIayAggregat(DUMMY_BEHANDLING_ID, builder.leggTilAktørArbeid(builder.getAktørArbeidBuilder(brukerAktørId)
            .leggTilYrkesaktivitet(YrkesaktivitetBuilder.oppdatere(Optional.empty())
                .medArbeidsgiver(Arbeidsgiver.virksomhet("000000000"))
                .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
                .leggTilAktivitetsAvtale(AktivitetsAvtaleBuilder.ny()
                    .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom.minusDays(3), tom.minusDays(3)))))
            .leggTilYrkesaktivitet(YrkesaktivitetBuilder.oppdatere(Optional.empty())
                .medArbeidsgiver(Arbeidsgiver.virksomhet("111111111"))
                .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
                .leggTilAktivitetsAvtale(AktivitetsAvtaleBuilder.ny()
                    .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(tom.minusDays(1), tom.plusDays(1)))))));
        var grunnlag = iayTjeneste.hentGrunnlag(DUMMY_BEHANDLING_ID);

        var periodeTilVurdering = new LocalDateTimeline<>(List.of(new LocalDateSegment<>(fom, tom, true)));
        var input = new InaktivitetUtlederInput(brukerAktørId, periodeTilVurdering, grunnlag, true);
        var utledetTidslinje = utleder.utled(input);

        assertThat(utledetTidslinje).hasSize(2);

        var ikkeYrkesaktiv = utledetTidslinje.entrySet()
            .stream().filter(e -> e.getKey().getAktivitetType().equals(UttakArbeidType.IKKE_YRKESAKTIV_UTEN_ERSTATNING))
            .findFirst()
            .orElseThrow();
        assertThat(ikkeYrkesaktiv.getKey().getArbeidsgiver().getArbeidsgiverOrgnr()).isEqualTo("000000000");
        var ikkeYrkesaktivSegmenter = ikkeYrkesaktiv.getValue().toSegments();
        assertThat(ikkeYrkesaktivSegmenter.size()).isEqualTo(1);
        var segmentIkkeYrkesaktiv = ikkeYrkesaktivSegmenter.iterator().next();
        assertThat(segmentIkkeYrkesaktiv.getFom()).isEqualTo(tom.minusDays(2));
        assertThat(segmentIkkeYrkesaktiv.getTom()).isEqualTo(tom.minusDays(2));

        var erstattetIkkeYrkesaktiv = utledetTidslinje.entrySet()
            .stream().filter(e -> e.getKey().getAktivitetType().equals(UttakArbeidType.IKKE_YRKESAKTIV))
            .findFirst()
            .orElseThrow();
        assertThat(erstattetIkkeYrkesaktiv.getKey().getArbeidsgiver().getArbeidsgiverOrgnr()).isEqualTo("000000000");
        var erstattetIkkeYrkesaktivSegmenter = erstattetIkkeYrkesaktiv.getValue().toSegments();
        assertThat(erstattetIkkeYrkesaktivSegmenter.size()).isEqualTo(1);
        var segmentErstattetIkkeYrkesaktiv = erstattetIkkeYrkesaktivSegmenter.iterator().next();
        assertThat(segmentErstattetIkkeYrkesaktiv.getFom()).isEqualTo(tom.minusDays(1));
        assertThat(segmentErstattetIkkeYrkesaktiv.getTom()).isEqualTo(tom);
    }


    @Test
    void skal_utlede_tidslinje_der_inaktivt_arbeid_erstattes_av_nytt_samme_dag_som_bortfalt() {
        var fom = LocalDate.now();
        var tom = LocalDate.now().plusDays(14);
        var builder = InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.empty(), VersjonType.REGISTER);
        var brukerAktørId = AktørId.dummy();
        iayTjeneste.lagreIayAggregat(DUMMY_BEHANDLING_ID, builder.leggTilAktørArbeid(builder.getAktørArbeidBuilder(brukerAktørId)
            .leggTilYrkesaktivitet(YrkesaktivitetBuilder.oppdatere(Optional.empty())
                .medArbeidsgiver(Arbeidsgiver.virksomhet("000000000"))
                .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
                .leggTilAktivitetsAvtale(AktivitetsAvtaleBuilder.ny()
                    .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom.minusDays(3), tom.minusDays(3)))))
            .leggTilYrkesaktivitet(YrkesaktivitetBuilder.oppdatere(Optional.empty())
                .medArbeidsgiver(Arbeidsgiver.virksomhet("111111111"))
                .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
                .leggTilAktivitetsAvtale(AktivitetsAvtaleBuilder.ny()
                    .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(tom.minusDays(2), tom.plusDays(1)))))));
        var grunnlag = iayTjeneste.hentGrunnlag(DUMMY_BEHANDLING_ID);

        var periodeTilVurdering = new LocalDateTimeline<>(List.of(new LocalDateSegment<>(fom, tom, true)));
        var input = new InaktivitetUtlederInput(brukerAktørId, periodeTilVurdering, grunnlag, true);
        var utledetTidslinje = utleder.utled(input);

        assertThat(utledetTidslinje).hasSize(1);
        var erstattetIkkeYrkesaktiv = utledetTidslinje.entrySet()
            .stream().filter(e -> e.getKey().getAktivitetType().equals(UttakArbeidType.IKKE_YRKESAKTIV))
            .findFirst()
            .orElseThrow();
        assertThat(erstattetIkkeYrkesaktiv.getKey().getArbeidsgiver().getArbeidsgiverOrgnr()).isEqualTo("000000000");
        var erstattetIkkeYrkesaktivSegmenter = erstattetIkkeYrkesaktiv.getValue().toSegments();
        assertThat(erstattetIkkeYrkesaktivSegmenter.size()).isEqualTo(1);
        var segmentErstattetIkkeYrkesaktiv = erstattetIkkeYrkesaktivSegmenter.iterator().next();
        assertThat(segmentErstattetIkkeYrkesaktiv.getFom()).isEqualTo(tom.minusDays(2));
        assertThat(segmentErstattetIkkeYrkesaktiv.getTom()).isEqualTo(tom);
    }


    @Test
    void skal_utlede_tidslinje_der_inaktivt_arbeid_erstattes_av_nytt_utenfor_periode() {
        var fom = LocalDate.now();
        var tom = LocalDate.now().plusDays(14);
        var builder = InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.empty(), VersjonType.REGISTER);
        var brukerAktørId = AktørId.dummy();
        iayTjeneste.lagreIayAggregat(DUMMY_BEHANDLING_ID, builder.leggTilAktørArbeid(builder.getAktørArbeidBuilder(brukerAktørId)
            .leggTilYrkesaktivitet(YrkesaktivitetBuilder.oppdatere(Optional.empty())
                .medArbeidsgiver(Arbeidsgiver.virksomhet("000000000"))
                .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
                .leggTilAktivitetsAvtale(AktivitetsAvtaleBuilder.ny()
                    .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom.minusDays(3), tom.minusDays(3)))))
            .leggTilYrkesaktivitet(YrkesaktivitetBuilder.oppdatere(Optional.empty())
                .medArbeidsgiver(Arbeidsgiver.virksomhet("111111111"))
                .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
                .leggTilAktivitetsAvtale(AktivitetsAvtaleBuilder.ny()
                    .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(tom.plusDays(1), tom.plusDays(1)))))));
        var grunnlag = iayTjeneste.hentGrunnlag(DUMMY_BEHANDLING_ID);

        var periodeTilVurdering = new LocalDateTimeline<>(List.of(new LocalDateSegment<>(fom, tom, true)));
        var input = new InaktivitetUtlederInput(brukerAktørId, periodeTilVurdering, grunnlag, true);
        var utledetTidslinje = utleder.utled(input);

        assertThat(utledetTidslinje).hasSize(1);
        var ikkeYrkesaktiv = utledetTidslinje.entrySet()
            .stream().filter(e -> e.getKey().getAktivitetType().equals(UttakArbeidType.IKKE_YRKESAKTIV_UTEN_ERSTATNING))
            .findFirst()
            .orElseThrow();
        assertThat(ikkeYrkesaktiv.getKey().getArbeidsgiver().getArbeidsgiverOrgnr()).isEqualTo("000000000");
        var ikkeYrkesaktivSegmenter = ikkeYrkesaktiv.getValue().toSegments();
        assertThat(ikkeYrkesaktivSegmenter.size()).isEqualTo(1);
        var segmentIkkeYrkesaktiv = ikkeYrkesaktivSegmenter.iterator().next();
        assertThat(segmentIkkeYrkesaktiv.getFom()).isEqualTo(tom.minusDays(2));
        assertThat(segmentIkkeYrkesaktiv.getTom()).isEqualTo(tom);
    }

    @Test
    void skal_ikke_utlede_aktivitet_hvis_ikke_aktiv_dagen_før_stp() {
        var fom = LocalDate.now();
        var tom = LocalDate.now().plusDays(14);
        var builder = InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.empty(), VersjonType.REGISTER);
        var brukerAktørId = AktørId.dummy();
        iayTjeneste.lagreIayAggregat(DUMMY_BEHANDLING_ID, builder.leggTilAktørArbeid(builder.getAktørArbeidBuilder(brukerAktørId)
            .leggTilYrkesaktivitet(YrkesaktivitetBuilder.oppdatere(Optional.empty())
                .medArbeidsgiver(Arbeidsgiver.virksomhet("000000000"))
                .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
                .leggTilAktivitetsAvtale(AktivitetsAvtaleBuilder.ny()
                    .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom.plusDays(2), tom.minusDays(3))))
                .leggTilAktivitetsAvtale(AktivitetsAvtaleBuilder.ny()
                    .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom.plusDays(5), tom.minusDays(8)))))));
        var grunnlag = iayTjeneste.hentGrunnlag(DUMMY_BEHANDLING_ID);

        var periodeTilVurdering = new LocalDateTimeline<>(List.of(new LocalDateSegment<>(fom, tom, true)));
        var input = new InaktivitetUtlederInput(brukerAktørId, periodeTilVurdering, grunnlag, true);
        var utledetTidslinje = utleder.utled(input);

        assertThat(utledetTidslinje).hasSize(0);
    }

    @Test
    void skal_utlede_aktivitet_hvis_ikke_aktivitet_slutter_dagen_før_stp() {
        var fom = LocalDate.now();
        var tom = LocalDate.now().plusDays(14);
        var builder = InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.empty(), VersjonType.REGISTER);
        var brukerAktørId = AktørId.dummy();
        iayTjeneste.lagreIayAggregat(DUMMY_BEHANDLING_ID, builder.leggTilAktørArbeid(builder.getAktørArbeidBuilder(brukerAktørId)
            .leggTilYrkesaktivitet(YrkesaktivitetBuilder.oppdatere(Optional.empty())
                .medArbeidsgiver(Arbeidsgiver.virksomhet("000000000"))
                .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
                .leggTilAktivitetsAvtale(AktivitetsAvtaleBuilder.ny()
                    .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom.minusDays(10), fom.minusDays(1)))))));
        var grunnlag = iayTjeneste.hentGrunnlag(DUMMY_BEHANDLING_ID);

        var periodeTilVurdering = new LocalDateTimeline<>(List.of(new LocalDateSegment<>(fom, tom, true)));
        var input = new InaktivitetUtlederInput(brukerAktørId, periodeTilVurdering, grunnlag, true);
        var utledetTidslinje = utleder.utled(input);

        assertThat(utledetTidslinje).hasSize(1);
        var key = utledetTidslinje.keySet().iterator().next();
        var segmenter = utledetTidslinje.get(key).toSegments().stream().map(LocalDateSegment::getLocalDateInterval).toList();
        assertThat(segmenter).contains(new LocalDateInterval(fom, tom));
        assertThat(key.getAktivitetType()).isEqualTo(UttakArbeidType.IKKE_YRKESAKTIV_UTEN_ERSTATNING);
    }

    @Test
    void skal_utlede_aktivitet_hvis_ikke_periode_med_ikke_aktiv_midt_inne_i_ytelse() {
        var fom = LocalDate.now();
        var tom = LocalDate.now().plusDays(14);
        var builder = InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.empty(), VersjonType.REGISTER);
        var brukerAktørId = AktørId.dummy();
        iayTjeneste.lagreIayAggregat(DUMMY_BEHANDLING_ID, builder.leggTilAktørArbeid(builder.getAktørArbeidBuilder(brukerAktørId)
            .leggTilYrkesaktivitet(YrkesaktivitetBuilder.oppdatere(Optional.empty())
                .medArbeidsgiver(Arbeidsgiver.virksomhet("000000000"))
                .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
                .leggTilAktivitetsAvtale(AktivitetsAvtaleBuilder.ny()
                    .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom.minusDays(2), fom.plusDays(3))))
                .leggTilAktivitetsAvtale(AktivitetsAvtaleBuilder.ny()
                    .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom.plusDays(5), tom))))));
        var grunnlag = iayTjeneste.hentGrunnlag(DUMMY_BEHANDLING_ID);

        var periodeTilVurdering = new LocalDateTimeline<>(List.of(new LocalDateSegment<>(fom, tom, true)));
        var input = new InaktivitetUtlederInput(brukerAktørId, periodeTilVurdering, grunnlag, true);
        var utledetTidslinje = utleder.utled(input);

        assertThat(utledetTidslinje).hasSize(1);
        var aktivitetIdentifikator = utledetTidslinje.keySet().iterator().next();
        assertThat(aktivitetIdentifikator.getAktivitetType()).isEqualTo(UttakArbeidType.IKKE_YRKESAKTIV_UTEN_ERSTATNING);
    }

    @Test
    void skal_IKKE_utlede_aktivitet_hvis_starter_på_stp() {
        var fom = LocalDate.now();
        var tom = LocalDate.now().plusDays(14);
        var builder = InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.empty(), VersjonType.REGISTER);
        var brukerAktørId = AktørId.dummy();
        iayTjeneste.lagreIayAggregat(DUMMY_BEHANDLING_ID, builder.leggTilAktørArbeid(builder.getAktørArbeidBuilder(brukerAktørId)
            .leggTilYrkesaktivitet(YrkesaktivitetBuilder.oppdatere(Optional.empty())
                .medArbeidsgiver(Arbeidsgiver.virksomhet("000000000"))
                .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
                .leggTilAktivitetsAvtale(AktivitetsAvtaleBuilder.ny()
                    .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom, fom.plusDays(3)))))));
        var grunnlag = iayTjeneste.hentGrunnlag(DUMMY_BEHANDLING_ID);

        var periodeTilVurdering = new LocalDateTimeline<>(List.of(new LocalDateSegment<>(fom.minusDays(90), tom.minusDays(50), true), new LocalDateSegment<>(fom, tom, true)));
        var input = new InaktivitetUtlederInput(brukerAktørId, periodeTilVurdering, grunnlag, true);
        var utledetTidslinje = utleder.utled(input);

        assertThat(utledetTidslinje).hasSize(0);
    }

    @Test
    void skal_utlede_aktivitet_hvis_permittert_på_stp() {
        var fom = LocalDate.now();
        var tom = LocalDate.now().plusDays(14);
        var builder = InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.empty(), VersjonType.REGISTER);
        var brukerAktørId = AktørId.dummy();
        var yrkesaktivitetBuilder = YrkesaktivitetBuilder.oppdatere(Optional.empty());
        iayTjeneste.lagreIayAggregat(DUMMY_BEHANDLING_ID, builder.leggTilAktørArbeid(builder.getAktørArbeidBuilder(brukerAktørId)
            .leggTilYrkesaktivitet(yrkesaktivitetBuilder
                .medArbeidsgiver(Arbeidsgiver.virksomhet("000000000"))
                .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
                .leggTilPermisjon(yrkesaktivitetBuilder.getPermisjonBuilder()
                    .medPermisjonsbeskrivelseType(PermisjonsbeskrivelseType.PERMITTERING)
                    .medProsentsats(BigDecimal.valueOf(100))
                    .medPeriode(fom, fom)
                    .build())
                .leggTilAktivitetsAvtale(AktivitetsAvtaleBuilder.ny()
                    .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom, fom.plusDays(3)))))));
        var grunnlag = iayTjeneste.hentGrunnlag(DUMMY_BEHANDLING_ID);

        var periodeTilVurdering = new LocalDateTimeline<>(List.of(new LocalDateSegment<>(fom.minusDays(90), tom.minusDays(50), true), new LocalDateSegment<>(fom, tom, true)));
        var input = new InaktivitetUtlederInput(brukerAktørId, periodeTilVurdering, grunnlag, true);
        var utledetTidslinje = utleder.utled(input);

        assertThat(utledetTidslinje).hasSize(0);
    }

    @Test
    void skal_utlede_aktivitet_hvis_permittert_på_stp_2() {
        var fom = LocalDate.now();
        var tom = LocalDate.now().plusDays(14);
        var builder = InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.empty(), VersjonType.REGISTER);
        var brukerAktørId = AktørId.dummy();
        var yrkesaktivitetBuilder1 = YrkesaktivitetBuilder.oppdatere(Optional.empty());
        var yrkesaktivitetBuilder = YrkesaktivitetBuilder.oppdatere(Optional.empty());
        iayTjeneste.lagreIayAggregat(DUMMY_BEHANDLING_ID, builder.leggTilAktørArbeid(builder.getAktørArbeidBuilder(brukerAktørId)
            .leggTilYrkesaktivitet(yrkesaktivitetBuilder1
                .medArbeidsgiver(Arbeidsgiver.virksomhet("000000000"))
                .medArbeidsforholdId(InternArbeidsforholdRef.nyRef())
                .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
                .leggTilPermisjon(yrkesaktivitetBuilder1.getPermisjonBuilder()
                    .medPermisjonsbeskrivelseType(PermisjonsbeskrivelseType.PERMITTERING)
                    .medProsentsats(BigDecimal.valueOf(100))
                    .medPeriode(fom, fom)
                    .build())
                .leggTilAktivitetsAvtale(AktivitetsAvtaleBuilder.ny()
                    .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom, fom.plusDays(3)))))
            .leggTilYrkesaktivitet(yrkesaktivitetBuilder
                .medArbeidsgiver(Arbeidsgiver.virksomhet("000000000"))
                .medArbeidsforholdId(InternArbeidsforholdRef.nyRef())
                .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
                .leggTilAktivitetsAvtale(AktivitetsAvtaleBuilder.ny()
                    .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom, fom.plusDays(3)))))));
        var grunnlag = iayTjeneste.hentGrunnlag(DUMMY_BEHANDLING_ID);

        var periodeTilVurdering = new LocalDateTimeline<>(List.of(new LocalDateSegment<>(fom.minusDays(90), tom.minusDays(50), true), new LocalDateSegment<>(fom, tom, true)));
        var input = new InaktivitetUtlederInput(brukerAktørId, periodeTilVurdering, grunnlag, true);
        var utledetTidslinje = utleder.utled(input);

        assertThat(utledetTidslinje).hasSize(0);
    }

    @Test
    void skal_utlede_aktivitet_hvis_permittert_på_stp_3() {
        var fom = LocalDate.now();
        var tom = LocalDate.now().plusDays(14);
        var builder = InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.empty(), VersjonType.REGISTER);
        var brukerAktørId = AktørId.dummy();
        var yrkesaktivitetBuilder = YrkesaktivitetBuilder.oppdatere(Optional.empty());
        iayTjeneste.lagreIayAggregat(DUMMY_BEHANDLING_ID, builder.leggTilAktørArbeid(builder.getAktørArbeidBuilder(brukerAktørId)
            .leggTilYrkesaktivitet(yrkesaktivitetBuilder
                .medArbeidsgiver(Arbeidsgiver.virksomhet("000000000"))
                .medArbeidsforholdId(InternArbeidsforholdRef.nyRef())
                .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
                .leggTilAktivitetsAvtale(AktivitetsAvtaleBuilder.ny()
                    .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom, fom.plusDays(3)))))
            .leggTilYrkesaktivitet(yrkesaktivitetBuilder
                .medArbeidsgiver(Arbeidsgiver.virksomhet("000000000"))
                .medArbeidsforholdId(InternArbeidsforholdRef.nyRef())
                .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
                .leggTilPermisjon(yrkesaktivitetBuilder.getPermisjonBuilder()
                    .medPermisjonsbeskrivelseType(PermisjonsbeskrivelseType.PERMITTERING)
                    .medProsentsats(BigDecimal.valueOf(100))
                    .medPeriode(fom, fom)
                    .build())
                .leggTilAktivitetsAvtale(AktivitetsAvtaleBuilder.ny()
                    .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom, fom.plusDays(3)))))));
        var grunnlag = iayTjeneste.hentGrunnlag(DUMMY_BEHANDLING_ID);

        var periodeTilVurdering = new LocalDateTimeline<>(List.of(new LocalDateSegment<>(fom.minusDays(90), tom.minusDays(50), true), new LocalDateSegment<>(fom, tom, true)));
        var input = new InaktivitetUtlederInput(brukerAktørId, periodeTilVurdering, grunnlag, true);
        var utledetTidslinje = utleder.utled(input);

        assertThat(utledetTidslinje).hasSize(0);
    }
}
