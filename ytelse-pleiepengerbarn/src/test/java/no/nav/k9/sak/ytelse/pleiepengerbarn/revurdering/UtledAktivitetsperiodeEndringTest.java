package no.nav.k9.sak.ytelse.pleiepengerbarn.revurdering;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.k9.kodeverk.arbeidsforhold.ArbeidType;
import no.nav.k9.sak.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.iay.modell.AktivitetsAvtaleBuilder;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.domene.iay.modell.VersjonType;
import no.nav.k9.sak.domene.iay.modell.YrkesaktivitetBuilder;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.registerendringer.Endringstype;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;

class UtledAktivitetsperiodeEndringTest {

    public static final long BEHANDLING_ID = 1L;
    public static final AktørId AKTØR_ID = AktørId.dummy();
    public static final long ORIGINAL_BEHANDLING_ID = 2L;
    private final UtledAktivitetsperiodeEndring utleder = new UtledAktivitetsperiodeEndring();
    private final InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();

    @Test
    void skal_få_ingen_endringer() {
        var vilkårsperiode = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now(), LocalDate.now().plusDays(10));

        var fom = LocalDate.now();
        var ref = InternArbeidsforholdRef.nyRef();
        var periode = DatoIntervallEntitet.fraOgMedTilOgMed(fom, fom.plusMonths(1));
        var orgnr = "123456789";
        var inntektArbeidYtelseGrunnlag = lagIAY(List.of(orgnr), List.of(ref), List.of(periode), BEHANDLING_ID);
        var originalGrunnlag = lagIAY(List.of(orgnr), List.of(ref), List.of(periode), ORIGINAL_BEHANDLING_ID);

        var endringer = utleder.utledEndring(inntektArbeidYtelseGrunnlag, originalGrunnlag, Set.of(vilkårsperiode), AKTØR_ID);

        assertThat(endringer.isEmpty()).isTrue();
    }

    @Test
    void skal_utlede_fjernet_periode_når_deler_av_perioden_fjernes() {
        var vilkårsperiode = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now(), LocalDate.now().plusDays(10));

        var fom = LocalDate.now();
        var ref = InternArbeidsforholdRef.nyRef();
        var periode = DatoIntervallEntitet.fraOgMedTilOgMed(fom, fom.plusDays(20));
        var periode2 = DatoIntervallEntitet.fraOgMedTilOgMed(fom, fom.plusDays(10));

        var orgnr = "123456789";
        var originalGrunnlag = lagIAY(List.of(orgnr), List.of(ref), List.of(periode), ORIGINAL_BEHANDLING_ID);
        var inntektArbeidYtelseGrunnlag = lagIAY(List.of(orgnr), List.of(ref), List.of(periode2), BEHANDLING_ID);

        var endringer = utleder.utledEndring(inntektArbeidYtelseGrunnlag, originalGrunnlag, Set.of(vilkårsperiode), AKTØR_ID);

        assertThat(endringer.size()).isEqualTo(1);
        var forventet = new AktivitetsIdentifikator(Arbeidsgiver.virksomhet(orgnr), ref, ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);
        assertThat(endringer.get(0).identifikator()).isEqualTo(forventet);
        var endredeSegmenter = endringer.get(0).endringstidslinje().toSegments();
        assertThat(endredeSegmenter.size()).isEqualTo(1);
        assertThat(endredeSegmenter.contains(new LocalDateSegment<>(periode2.getTomDato().plusDays(1), periode.getTomDato(), Endringstype.FJERNET_PERIODE))).isTrue();
    }

    @Test
    void skal_utlede_fjernet_periode_når_hele_arbeidsforholdet_forsvinner() {
        var vilkårsperiode = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now(), LocalDate.now().plusDays(10));

        var fom = LocalDate.now();
        var ref = InternArbeidsforholdRef.nyRef();
        var ref2 = InternArbeidsforholdRef.nyRef();
        var periode = DatoIntervallEntitet.fraOgMedTilOgMed(fom, fom.plusDays(20));

        var orgnr = "123456789";
        var originalGrunnlag = lagIAY(List.of(orgnr, orgnr), List.of(ref, ref2), List.of(periode, periode), ORIGINAL_BEHANDLING_ID);
        var inntektArbeidYtelseGrunnlag = lagIAY(List.of(orgnr), List.of(ref), List.of(periode), BEHANDLING_ID);

        var endringer = utleder.utledEndring(inntektArbeidYtelseGrunnlag, originalGrunnlag, Set.of(vilkårsperiode), AKTØR_ID);

        assertThat(endringer.size()).isEqualTo(1);
        var forventet = new AktivitetsIdentifikator(Arbeidsgiver.virksomhet(orgnr), ref2, ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);
        assertThat(endringer.get(0).identifikator()).isEqualTo(forventet);
        var endredeSegmenter = endringer.get(0).endringstidslinje().toSegments();
        assertThat(endredeSegmenter.size()).isEqualTo(1);
        assertThat(endredeSegmenter.contains(new LocalDateSegment<>(periode.toLocalDateInterval(), Endringstype.FJERNET_PERIODE))).isTrue();
    }

    @Test
    void skal_utlede_ny_periode_når_hele_arbeidsforholdet_tilkommer() {
        var vilkårsperiode = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now(), LocalDate.now().plusDays(10));

        var fom = LocalDate.now();
        var ref = InternArbeidsforholdRef.nyRef();
        var ref2 = InternArbeidsforholdRef.nyRef();
        var periode = DatoIntervallEntitet.fraOgMedTilOgMed(fom, fom.plusDays(20));

        var orgnr = "123456789";
        var inntektArbeidYtelseGrunnlag = lagIAY(List.of(orgnr, orgnr), List.of(ref, ref2), List.of(periode, periode), BEHANDLING_ID);
        var originalGrunnlag = lagIAY(List.of(orgnr), List.of(ref), List.of(periode), ORIGINAL_BEHANDLING_ID);

        var endringer = utleder.utledEndring(inntektArbeidYtelseGrunnlag, originalGrunnlag, Set.of(vilkårsperiode), AKTØR_ID);

        assertThat(endringer.size()).isEqualTo(1);
        var forventet = new AktivitetsIdentifikator(Arbeidsgiver.virksomhet(orgnr), ref2, ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);
        assertThat(endringer.get(0).identifikator()).isEqualTo(forventet);
        var endredeSegmenter = endringer.get(0).endringstidslinje().toSegments();
        assertThat(endredeSegmenter.size()).isEqualTo(1);
        assertThat(endredeSegmenter.contains(new LocalDateSegment<>(periode.toLocalDateInterval(), Endringstype.NY_PERIODE))).isTrue();
    }

    @Test
    void skal_utlede_ny_periode_når_arbeidsforholdet_forlenges() {
        var vilkårsperiode = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now(), LocalDate.now().plusDays(10));

        var fom = LocalDate.now();
        var ref = InternArbeidsforholdRef.nyRef();
        var periode = DatoIntervallEntitet.fraOgMedTilOgMed(fom, fom.plusDays(20));
        var periode2 = DatoIntervallEntitet.fraOgMedTilOgMed(fom, fom.plusDays(25));

        var orgnr = "123456789";
        var inntektArbeidYtelseGrunnlag = lagIAY(List.of(orgnr), List.of(ref), List.of(periode2), BEHANDLING_ID);
        var originalGrunnlag = lagIAY(List.of(orgnr), List.of(ref), List.of(periode), ORIGINAL_BEHANDLING_ID);

        var endringer = utleder.utledEndring(inntektArbeidYtelseGrunnlag, originalGrunnlag, Set.of(vilkårsperiode), AKTØR_ID);

        assertThat(endringer.size()).isEqualTo(1);
        var forventet = new AktivitetsIdentifikator(Arbeidsgiver.virksomhet(orgnr), ref, ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);
        assertThat(endringer.get(0).identifikator()).isEqualTo(forventet);
        var endredeSegmenter = endringer.get(0).endringstidslinje().toSegments();
        assertThat(endredeSegmenter.size()).isEqualTo(1);
        assertThat(endredeSegmenter.contains(new LocalDateSegment<>(periode.getTomDato().plusDays(1), periode2.getTomDato(), Endringstype.NY_PERIODE))).isTrue();
    }

    private InntektArbeidYtelseGrunnlag lagIAY(List<String> orgnr, List<InternArbeidsforholdRef> arbeidsforholdrefs, List<DatoIntervallEntitet> periode, long behandlingId) {
        var aktørArbeidBuilder = InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder.oppdatere(Optional.empty());
        aktørArbeidBuilder.medAktørId(AKTØR_ID);
        var index = 0;
        for (String it : orgnr) {
            aktørArbeidBuilder.leggTilYrkesaktivitet(YrkesaktivitetBuilder.oppdatere(Optional.empty())
                .medArbeidsgiver(Arbeidsgiver.virksomhet(it))
                .medArbeidsforholdId(arbeidsforholdrefs.get(index))
                .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
                .leggTilAktivitetsAvtale(AktivitetsAvtaleBuilder.ny()
                    .medPeriode(periode.get(index))));
            index++;
        }

        inntektArbeidYtelseTjeneste.lagreIayAggregat(behandlingId, InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.empty(), VersjonType.REGISTER)
            .leggTilAktørArbeid(aktørArbeidBuilder));

        return inntektArbeidYtelseTjeneste.hentGrunnlag(behandlingId);
    }


}
