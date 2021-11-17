package no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.input.arbeid;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.kodeverk.arbeidsforhold.ArbeidType;
import no.nav.k9.kodeverk.arbeidsforhold.PermisjonsbeskrivelseType;
import no.nav.k9.sak.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.iay.modell.AktivitetsAvtaleBuilder;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.k9.sak.domene.iay.modell.VersjonType;
import no.nav.k9.sak.domene.iay.modell.YrkesaktivitetBuilder;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Arbeidsgiver;

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
        var input = new InaktivitetUtlederInput(AktørId.dummy(), LocalDateTimeline.EMPTY_TIMELINE, null);
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
        var input = new InaktivitetUtlederInput(brukerAktørId, periodeTilVurdering, grunnlag);
        var utledetTidslinje = utleder.utled(input);

        assertThat(utledetTidslinje).hasSize(1);
    }

    @Test
    void skal_ikke_utlede_aktivitet_hvis_ikke_aktiv_på_stp() {
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
        var input = new InaktivitetUtlederInput(brukerAktørId, periodeTilVurdering, grunnlag);
        var utledetTidslinje = utleder.utled(input);

        assertThat(utledetTidslinje).hasSize(0);
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
        var input = new InaktivitetUtlederInput(brukerAktørId, periodeTilVurdering, grunnlag);
        var utledetTidslinje = utleder.utled(input);

        assertThat(utledetTidslinje).hasSize(1);
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
        var input = new InaktivitetUtlederInput(brukerAktørId, periodeTilVurdering, grunnlag);
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
        var input = new InaktivitetUtlederInput(brukerAktørId, periodeTilVurdering, grunnlag);
        var utledetTidslinje = utleder.utled(input);

        assertThat(utledetTidslinje).hasSize(0);
    }
}
