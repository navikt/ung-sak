package no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.dok;

import java.time.LocalDate;
import java.time.Month;
import java.util.Arrays;
import java.util.UUID;

import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.Grunnbeløp;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.Aktivitet;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.AktivitetStatus;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.AktivitetStatusMedHjemmel;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.BeregningsgrunnlagHjemmel;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.Periode;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.grunnlag.inntekt.Arbeidsforhold;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.grunnlag.inntekt.Inntektsgrunnlag;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.resultat.Beregningsgrunnlag;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.resultat.BeregningsgrunnlagPeriode;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.resultat.BeregningsgrunnlagPrArbeidsforhold;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.resultat.BeregningsgrunnlagPrStatus;
import no.nav.fpsak.nare.DynamicRuleService;
import no.nav.fpsak.nare.ServiceArgument;

public class RegelmodellForDokumentasjon {

    protected static final BeregningsgrunnlagPeriode regelmodellMedEttArbeidsforhold =
            Beregningsgrunnlag.builder()
                .medSkjæringstidspunkt(LocalDate.now())
                .medAktivitetStatuser(Arrays.asList(new AktivitetStatusMedHjemmel(AktivitetStatus.ATFL, BeregningsgrunnlagHjemmel.F_14_7_8_30)))
                .medInntektsgrunnlag(new Inntektsgrunnlag())
                .medGrunnbeløpSatser(Arrays.asList(new Grunnbeløp(LocalDate.of(2000, Month.JANUARY, 1), LocalDate.of(2099,  Month.DECEMBER,  31), 90000L, 90000L)))
                .medBeregningsgrunnlagPeriode(BeregningsgrunnlagPeriode.builder()
                        .medBeregningsgrunnlagPrStatus(BeregningsgrunnlagPrStatus.builder()
                                .medAktivitetStatus(AktivitetStatus.ATFL)
                                .medArbeidsforhold(BeregningsgrunnlagPrArbeidsforhold.builder()
                                        .medArbeidsforhold(opprettArbeidsforhold())
                                        .medAndelNr(1)
                                        .build())
                                .build())
                        .medPeriode(Periode.of(LocalDate.now(), LocalDate.now().plusYears(1)))
                        .build())
                .build()
                .getBeregningsgrunnlagPerioder()
                .get(0);

    private RegelmodellForDokumentasjon() {
        super();
    }

    public static <T> void forArbeidsforhold(DynamicRuleService<T> regeltjeneste) {
        Arbeidsforhold arbeidsforhold = opprettArbeidsforhold();
        regeltjeneste.medServiceArgument(new ServiceArgument("dokumentasjon", BeregningsgrunnlagPrArbeidsforhold.builder().medArbeidsforhold(arbeidsforhold).medAndelNr(1).build()));
    }

    private static Arbeidsforhold opprettArbeidsforhold() {
        return Arbeidsforhold.builder()
                .medAktivitet(Aktivitet.ARBEIDSTAKERINNTEKT)
                .medOrgnr("987654321")
                .medArbeidsforholdId(UUID.randomUUID().toString())
                .build();
    }

}
