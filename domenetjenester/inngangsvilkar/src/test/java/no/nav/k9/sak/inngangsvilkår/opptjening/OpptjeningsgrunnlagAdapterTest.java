package no.nav.k9.sak.inngangsvilkår.opptjening;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;
import no.nav.k9.sak.domene.iay.modell.Opptjeningsnøkkel;
import no.nav.k9.sak.domene.opptjening.OpptjeningAktivitetPeriode;
import no.nav.k9.sak.domene.opptjening.VurderingsStatus;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.inngangsvilkår.opptjening.regelmodell.AktivitetPeriode;

class OpptjeningsgrunnlagAdapterTest {

    @Test
    void skal_mappe_en_arbeidsaktivitet_til_vurdering() {
        var opptjeningsgrunnlagAdapter = new OpptjeningsgrunnlagAdapter(LocalDate.now(), LocalDate.now(), LocalDate.now());

        var fomDato = LocalDate.now().minusDays(10);
        var periode = DatoIntervallEntitet.fraOgMedTilOgMed(fomDato, fomDato.plusDays(10));
        var opptjeningAktiveter = new ArrayList<>(List.of(lagArbeidsaktivitet(VurderingsStatus.TIL_VURDERING, periode)));

        var opptjeningsgrunnlag = opptjeningsgrunnlagAdapter.mapTilGrunnlag(opptjeningAktiveter,
            List.of());

        assertThat(opptjeningsgrunnlag.getAktivitetPerioder().size()).isEqualTo(1);
        var actual = opptjeningsgrunnlag.getAktivitetPerioder().get(0);
        assertThat(actual.getDatoInterval()).isEqualTo(periode.toLocalDateInterval());
        assertThat(actual.getVurderingsStatus()).isEqualTo(AktivitetPeriode.VurderingsStatus.TIL_VURDERING);
    }

    @Test
    void skal_mappe_to_ikke_overlappende_arbeidsaktiviteter_med_samme_identifikator_til_vurdering() {
        var opptjeningsgrunnlagAdapter = new OpptjeningsgrunnlagAdapter(LocalDate.now(), LocalDate.now(), LocalDate.now());

        var fomDato = LocalDate.now().minusDays(10);
        var tomDato = fomDato.plusDays(10);
        var periode = DatoIntervallEntitet.fraOgMedTilOgMed(fomDato, tomDato);
        var periode2 = DatoIntervallEntitet.fraOgMedTilOgMed(tomDato.plusDays(1), tomDato.plusDays(10));

        var opptjeningAktiveter = new ArrayList<>(List.of(lagArbeidsaktivitet(VurderingsStatus.TIL_VURDERING, periode),
            lagArbeidsaktivitet(VurderingsStatus.TIL_VURDERING, periode2)));

        var opptjeningsgrunnlag = opptjeningsgrunnlagAdapter.mapTilGrunnlag(opptjeningAktiveter,
            List.of());

        assertThat(opptjeningsgrunnlag.getAktivitetPerioder().size()).isEqualTo(2);
        var actual = opptjeningsgrunnlag.getAktivitetPerioder().get(0);
        assertThat(actual.getDatoInterval()).isEqualTo(periode.toLocalDateInterval());
        assertThat(actual.getVurderingsStatus()).isEqualTo(AktivitetPeriode.VurderingsStatus.TIL_VURDERING);

        var actual2 = opptjeningsgrunnlag.getAktivitetPerioder().get(1);
        assertThat(actual2.getDatoInterval()).isEqualTo(periode2.toLocalDateInterval());
        assertThat(actual2.getVurderingsStatus()).isEqualTo(AktivitetPeriode.VurderingsStatus.TIL_VURDERING);
    }

    @Test
    void skal_mappe_to_overlappende_arbeidsaktiviteter_med_samme_identifikator_til_vurdering() {
        var opptjeningsgrunnlagAdapter = new OpptjeningsgrunnlagAdapter(LocalDate.now(), LocalDate.now(), LocalDate.now());

        var fomDato = LocalDate.now().minusDays(10);
        var tomDato = fomDato.plusDays(10);
        var periode = DatoIntervallEntitet.fraOgMedTilOgMed(fomDato, tomDato);

        var opptjeningAktiveter = new ArrayList<>(List.of(lagArbeidsaktivitet(VurderingsStatus.TIL_VURDERING, periode),
            lagArbeidsaktivitet(VurderingsStatus.TIL_VURDERING, periode)));

        var opptjeningsgrunnlag = opptjeningsgrunnlagAdapter.mapTilGrunnlag(opptjeningAktiveter,
            List.of());

        assertThat(opptjeningsgrunnlag.getAktivitetPerioder().size()).isEqualTo(1);
        var actual = opptjeningsgrunnlag.getAktivitetPerioder().get(0);
        assertThat(actual.getDatoInterval()).isEqualTo(periode.toLocalDateInterval());
        assertThat(actual.getVurderingsStatus()).isEqualTo(AktivitetPeriode.VurderingsStatus.TIL_VURDERING);

    }

    @Test
    void skal_mappe_to_overlappende_arbeidsaktiviteter_med_samme_identifikator_en_til_vurdering_en_underkjent() {
        var opptjeningsgrunnlagAdapter = new OpptjeningsgrunnlagAdapter(LocalDate.now(), LocalDate.now(), LocalDate.now());

        var fomDato = LocalDate.now().minusDays(10);
        var tomDato = fomDato.plusDays(10);
        var periode = DatoIntervallEntitet.fraOgMedTilOgMed(fomDato, tomDato);
        var periode2 = DatoIntervallEntitet.fraOgMedTilOgMed(tomDato.minusDays(1), tomDato.plusDays(10));

        var opptjeningAktiveter = new ArrayList<>(List.of(lagArbeidsaktivitet(VurderingsStatus.TIL_VURDERING, periode),
            lagArbeidsaktivitet(VurderingsStatus.UNDERKJENT, periode2)));

        var opptjeningsgrunnlag = opptjeningsgrunnlagAdapter.mapTilGrunnlag(opptjeningAktiveter,
            List.of());

        assertThat(opptjeningsgrunnlag.getAktivitetPerioder().size()).isEqualTo(3);
        var actual = opptjeningsgrunnlag.getAktivitetPerioder().get(0);
        assertThat(actual.getDatoInterval().getFomDato()).isEqualTo(periode.getFomDato());
        assertThat(actual.getDatoInterval().getTomDato()).isEqualTo(periode2.getFomDato().minusDays(1));
        assertThat(actual.getVurderingsStatus()).isEqualTo(AktivitetPeriode.VurderingsStatus.TIL_VURDERING);

        var actual2 = opptjeningsgrunnlag.getAktivitetPerioder().get(1);
        assertThat(actual2.getDatoInterval().getFomDato()).isEqualTo(periode2.getFomDato());
        assertThat(actual2.getDatoInterval().getTomDato()).isEqualTo(periode.getTomDato());
        assertThat(actual2.getVurderingsStatus()).isEqualTo(AktivitetPeriode.VurderingsStatus.TIL_VURDERING);


        var actual3 = opptjeningsgrunnlag.getAktivitetPerioder().get(2);
        assertThat(actual3.getDatoInterval().getFomDato()).isEqualTo(periode.getTomDato().plusDays(1));
        assertThat(actual3.getDatoInterval().getTomDato()).isEqualTo(periode2.getTomDato());
        assertThat(actual3.getVurderingsStatus()).isEqualTo(AktivitetPeriode.VurderingsStatus.VURDERT_UNDERKJENT);
    }


    private static OpptjeningAktivitetPeriode lagArbeidsaktivitet(VurderingsStatus vurderingsStatus, DatoIntervallEntitet periode) {
        return OpptjeningAktivitetPeriode.Builder.ny().medPeriode(periode)
            .medVurderingsStatus(vurderingsStatus)
            .medOpptjeningsnøkkel(Opptjeningsnøkkel.forOrgnummer("123456789"))
            .medOpptjeningAktivitetType(OpptjeningAktivitetType.ARBEID).build();
    }
}
