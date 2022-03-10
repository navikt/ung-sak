package no.nav.k9.sak.domene.opptjening;


import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import no.nav.k9.kodeverk.arbeidsforhold.ArbeidType;
import no.nav.k9.kodeverk.arbeidsforhold.PermisjonsbeskrivelseType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.iay.modell.AktivitetsAvtale;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.domene.iay.modell.Opptjeningsnøkkel;
import no.nav.k9.sak.domene.iay.modell.VersjonType;
import no.nav.k9.sak.domene.iay.modell.YrkesaktivitetBuilder;
import no.nav.k9.sak.domene.opptjening.aksjonspunkt.VurderStatusInput;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;

public class OpptjeningAktivitetArbeidVurdererTest {

    private final Arbeidsgiver virksomhet = Arbeidsgiver.virksomhet("974760673");
    private final InternArbeidsforholdRef arbeidsforholdId = InternArbeidsforholdRef.nyRef();
    private InntektArbeidYtelseTjeneste iayTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();
    private LocalDate skjæringstidspunkt = LocalDate.now();
    private DatoIntervallEntitet inntektsperiodeEttÅr = DatoIntervallEntitet.fraOgMedTilOgMed(skjæringstidspunkt.minusYears(1), skjæringstidspunkt);
    private DatoIntervallEntitet opptjeningPeriode28Dager = DatoIntervallEntitet.fraOgMedTilOgMed(skjæringstidspunkt.minusDays(28), skjæringstidspunkt);

    private OpptjeningAktivitetArbeidVurderer vurderer = new OpptjeningAktivitetArbeidVurderer();
    private AktørId dummy = AktørId.dummy();
    ;

    @Test
    public void skal_godkjenne_permisjon_inntil_14_dager() {
        var permisjonPeriode14Dager = DatoIntervallEntitet.fraOgMedTilOgMed(skjæringstidspunkt.minusDays(13), skjæringstidspunkt);
        var iayBuilder = opprettIAYMedYrkesaktivitet();
        leggTilPermisjon(iayBuilder, permisjonPeriode14Dager);
        InntektArbeidYtelseGrunnlag iayGrunnlag = lagreIayGrunnlag(iayBuilder);

        VurderStatusInput input = byggInput(iayGrunnlag, opptjeningPeriode28Dager);

        assertThat(vurderer.vurderArbeid(input)).isEqualTo(VurderingsStatus.TIL_VURDERING);
    }

    @Test
    public void skal_underkjenne_permisjon_over_14_dager() {
        var permisjonPeriode15Dager = DatoIntervallEntitet.fraOgMedTilOgMed(skjæringstidspunkt.minusDays(14), skjæringstidspunkt);

        var iayBuilder = opprettIAYMedYrkesaktivitet();
        leggTilPermisjon(iayBuilder, permisjonPeriode15Dager);
        var iayGrunnlag = lagreIayGrunnlag(iayBuilder);

        VurderStatusInput input = byggInput(iayGrunnlag, opptjeningPeriode28Dager);

        assertThat(vurderer.vurderArbeid(input)).isEqualTo(VurderingsStatus.UNDERKJENT);
    }

    @Test
    public void skal_underkjenne_sammenhengende_permisjoner_som_overstiger_14_dager() {
        var permisjonPeriode1 = DatoIntervallEntitet.fraOgMedTilOgMed(skjæringstidspunkt.minusDays(15), skjæringstidspunkt.minusDays(10));
        var permisjonPeriode2 = DatoIntervallEntitet.fraOgMedTilOgMed(skjæringstidspunkt.minusDays(10), skjæringstidspunkt);

        var iayBuilder = opprettIAYMedYrkesaktivitet();
        leggTilPermisjon(iayBuilder, permisjonPeriode1);
        leggTilPermisjon(iayBuilder, permisjonPeriode2);
        var iayGrunnlag = lagreIayGrunnlag(iayBuilder);

        VurderStatusInput input = byggInput(iayGrunnlag, opptjeningPeriode28Dager);

        assertThat(vurderer.vurderArbeid(input)).isEqualTo(VurderingsStatus.UNDERKJENT);
    }


    @Test
    public void skal_underkjenne_permisjoner_som_skjøtes_sammen_over_helg_til_14_dager() {
        var førsteLørdagIPeriode = hentFørsteLørdagIPeriode(opptjeningPeriode28Dager.getFomDato(), opptjeningPeriode28Dager.getTomDato());
        var permisjonPeriode1 = DatoIntervallEntitet.fraOgMedTilOgMed(opptjeningPeriode28Dager.getFomDato(), førsteLørdagIPeriode); // 1-7 dager
        var permisjonPeriode2 = DatoIntervallEntitet.fraOgMedTilOgMed(førsteLørdagIPeriode.plusDays(2), førsteLørdagIPeriode.plusDays(15)); // 14 dager

        var iayBuilder = opprettIAYMedYrkesaktivitet();
        leggTilPermisjon(iayBuilder, permisjonPeriode1);
        leggTilPermisjon(iayBuilder, permisjonPeriode2);
        var iayGrunnlag = lagreIayGrunnlag(iayBuilder);

        VurderStatusInput input = byggInput(iayGrunnlag, opptjeningPeriode28Dager);

        assertThat(vurderer.vurderArbeid(input)).isEqualTo(VurderingsStatus.UNDERKJENT);
    }

    private LocalDate hentFørsteLørdagIPeriode(LocalDate min, LocalDate max) {
        var neste = min.plusDays(1); // Unngå å starte på samme dag
        while (neste.isBefore(max)) {
            if (DayOfWeek.SATURDAY == neste.getDayOfWeek()) {
                break;
            }
            neste = neste.plusDays(1);
        }
        return neste;
    }

    private void leggTilPermisjon(InntektArbeidYtelseAggregatBuilder iayBuilder, DatoIntervallEntitet permisjonPeriode14Dager) {
        var aktørArbeidBuilder = iayBuilder.getAktørArbeidBuilder(dummy);
        YrkesaktivitetBuilder yrkesaktivitetBuilder = aktørArbeidBuilder.getYrkesaktivitetBuilderForNøkkelAvType(
            new Opptjeningsnøkkel(arbeidsforholdId, virksomhet.getIdentifikator(), null), ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);
        yrkesaktivitetBuilder
            .leggTilPermisjon(YrkesaktivitetBuilder.nyPermisjonBuilder()
                .medPeriode(permisjonPeriode14Dager.getFomDato(), permisjonPeriode14Dager.getTomDato())
                .medProsentsats(BigDecimal.valueOf(100))
                .medPermisjonsbeskrivelseType(PermisjonsbeskrivelseType.PERMITTERING)
                .build());
    }

    private InntektArbeidYtelseAggregatBuilder opprettIAYMedYrkesaktivitet() {
        InntektArbeidYtelseAggregatBuilder builder = InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.empty(), VersjonType.REGISTER);
        var aktørArbeidBuilder = builder.getAktørArbeidBuilder(dummy);
        YrkesaktivitetBuilder yrkesaktivitetBuilder = aktørArbeidBuilder.getYrkesaktivitetBuilderForNøkkelAvType(
            new Opptjeningsnøkkel(arbeidsforholdId, virksomhet.getIdentifikator(), null), ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);

        var aktivitetsAvtaleBuilder = yrkesaktivitetBuilder.getAktivitetsAvtaleBuilder()
            .medPeriode(inntektsperiodeEttÅr);

        yrkesaktivitetBuilder
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .medArbeidsgiver(virksomhet)
            .medArbeidsforholdId(arbeidsforholdId)
            .leggTilAktivitetsAvtale(aktivitetsAvtaleBuilder);

        builder.leggTilAktørArbeid(
            aktørArbeidBuilder.leggTilYrkesaktivitet(yrkesaktivitetBuilder));
        return builder;
    }

    private InntektArbeidYtelseGrunnlag lagreIayGrunnlag(InntektArbeidYtelseAggregatBuilder iayBuilder) {
        iayTjeneste.lagreIayAggregat(1L, iayBuilder);
        return iayTjeneste.finnGrunnlag(1L).orElseThrow();
    }

    private VurderStatusInput byggInput(InntektArbeidYtelseGrunnlag iayGrunnlag, DatoIntervallEntitet opptjeningsperiode) {
        var input = new VurderStatusInput(OpptjeningAktivitetType.ARBEID, opprettDummyReferanse());
        input.setOpptjeningsperiode(opptjeningsperiode);
        var yrkesaktiviteter = iayGrunnlag.getAktørArbeidFraRegister(dummy).get().hentAlleYrkesaktiviteter();
        assertThat(yrkesaktiviteter).as("Forventer testcase med bare én yrkesaktivitet").hasSize(1);

        var yrkesaktivitet = yrkesaktiviteter.iterator().next();
        input.setRegisterAktivitet(yrkesaktivitet);
        input.setAktivitetPeriode(yrkesaktivitet.getAnsettelsesPeriode().stream().findFirst().map(AktivitetsAvtale::getPeriode).orElseThrow());
        return input;
    }

    private BehandlingReferanse opprettDummyReferanse() {
        return BehandlingReferanse.fra(FagsakYtelseType.OMP, null, null, dummy, null, null, null, null, null, null, java.util.Optional.empty(), null, null, null);
    }
}
