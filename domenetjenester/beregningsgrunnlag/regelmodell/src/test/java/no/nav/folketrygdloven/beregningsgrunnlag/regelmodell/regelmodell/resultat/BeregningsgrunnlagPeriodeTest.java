package no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.resultat;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;

import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.Grunnbeløp;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.AktivitetStatus;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.AktivitetStatusMedHjemmel;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.Periode;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.grunnlag.inntekt.Inntektsgrunnlag;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.resultat.Beregningsgrunnlag;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.resultat.BeregningsgrunnlagPeriode;

public class BeregningsgrunnlagPeriodeTest {

    /*
    Rekkefølge for beregning av BeregningsgrunnlagPrStatus er viktig pga avhengigheter. Denne testen tester at status SN
     og ATFL_SN returneres sist.
     */

    private static final LocalDate skjæringstidspunkt = LocalDate.of(2018, Month.JANUARY, 15);

    @Test
    public void skal_teste_at_aktivitetstatuser_SN_og_ATFLSN_returneres_sist() {
        //Arrange
        List<AktivitetStatusMedHjemmel> alleStatuser = List.of(AktivitetStatus.values()).stream()
                .map(as -> new AktivitetStatusMedHjemmel(as, null))
                .collect(Collectors.toList());
        BeregningsgrunnlagPeriode bgPeriode = BeregningsgrunnlagPeriode.builder()
            .medPeriode(Periode.of(skjæringstidspunkt, null))
            .build();
        Beregningsgrunnlag.builder()
            .medInntektsgrunnlag(new Inntektsgrunnlag())
            .medSkjæringstidspunkt(skjæringstidspunkt)
            .medAktivitetStatuser(alleStatuser)
            .medBeregningsgrunnlagPeriode(bgPeriode)
            .medGrunnbeløpSatser(List.of(new Grunnbeløp(LocalDate.of(2000, Month.JANUARY, 1), LocalDate.of(2099,  Month.DECEMBER,  31), 90000L, 90000L)))
            .build();
        //Act
        List<AktivitetStatusMedHjemmel> aktivitetStatuser = bgPeriode.getAktivitetStatuser();
        //Assert
        List<AktivitetStatus> toSisteStatuser = aktivitetStatuser.stream()
            .skip(aktivitetStatuser.size()-2)
            .map(as -> as.getAktivitetStatus())
            .collect(Collectors.toList());
        assertThat(toSisteStatuser).containsExactlyInAnyOrder(AktivitetStatus.SN, AktivitetStatus.ATFL_SN);
    }

}
