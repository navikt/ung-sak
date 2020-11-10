package no.nav.k9.sak.ytelse.frisinn.vilkår;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlagBuilder;
import no.nav.k9.sak.domene.iay.modell.OppgittOpptjeningBuilder;
import no.nav.k9.sak.domene.iay.modell.OppgittOpptjeningBuilder.EgenNæringBuilder;
import no.nav.k9.sak.domene.iay.modell.OppgittOpptjeningBuilder.OppgittFrilansBuilder;
import no.nav.k9.sak.domene.iay.modell.OppgittOpptjeningBuilder.OppgittFrilansOppdragBuilder;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

public class UtledPerioderMedEndringTjenesteTest {

    private final DatoIntervallEntitet periodeApril = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2020, 4, 1), LocalDate.of(2020, 4, 30));
    private final DatoIntervallEntitet periodeMai = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2020, 5, 1), LocalDate.of(2020, 5, 31));
    private final DatoIntervallEntitet iFjor = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 12, 31));

    @Test
    public void skal_gi_ingen_perioder_når_det_ikke_er_diff() {

        UtledPerioderMedEndringTjeneste tjeneste = new UtledPerioderMedEndringTjeneste();

        InntektArbeidYtelseGrunnlagBuilder iayGrunnlag = lagOppgittOpptjening(BigDecimal.TEN, BigDecimal.TEN, BigDecimal.TEN);
        InntektArbeidYtelseGrunnlagBuilder nytt = lagOppgittOpptjening(BigDecimal.TEN, BigDecimal.TEN, BigDecimal.TEN);


        List<DatoIntervallEntitet> datoIntervallEntitets = tjeneste.utledDiffIperioder(iayGrunnlag.build(), nytt.build());

        assertThat(datoIntervallEntitets).isEmpty();

    }

    @Test
    public void skal_gi_diff_periode_det_er_diff_i() {

        UtledPerioderMedEndringTjeneste tjeneste = new UtledPerioderMedEndringTjeneste();

        InntektArbeidYtelseGrunnlagBuilder iayGrunnlag = lagOppgittOpptjening(BigDecimal.TEN, BigDecimal.TEN, BigDecimal.TEN);
        InntektArbeidYtelseGrunnlagBuilder nytt = lagOppgittOpptjening(BigDecimal.ONE, BigDecimal.TEN, BigDecimal.TEN);


        List<DatoIntervallEntitet> datoIntervallEntitets = tjeneste.utledDiffIperioder(iayGrunnlag.build(), nytt.build());

        assertThat(datoIntervallEntitets).containsExactly(periodeApril);
    }

    @Test
    public void skal_gi_diff_i_alle_perioder_hvis_register_opplysning_har_diff() {

        UtledPerioderMedEndringTjeneste tjeneste = new UtledPerioderMedEndringTjeneste();

        InntektArbeidYtelseGrunnlagBuilder iayGrunnlag = lagOppgittOpptjening(BigDecimal.TEN, BigDecimal.TEN, BigDecimal.TEN);
        InntektArbeidYtelseGrunnlagBuilder nytt = lagOppgittOpptjeningMedIfjor(BigDecimal.ONE, BigDecimal.TEN, BigDecimal.TEN, BigDecimal.TEN);


        List<DatoIntervallEntitet> datoIntervallEntitets = tjeneste.utledDiffIperioder(iayGrunnlag.build(), nytt.build());

        assertThat(datoIntervallEntitets).containsExactly(periodeApril, periodeMai);
    }


    private InntektArbeidYtelseGrunnlagBuilder lagOppgittOpptjening(BigDecimal flInntektApril, BigDecimal flInntektMai, BigDecimal snMai) {
        OppgittOpptjeningBuilder oppgittOpptjening = OppgittOpptjeningBuilder.ny();
        var frilans = OppgittFrilansBuilder.ny();
        var aprilOppdrag = OppgittFrilansOppdragBuilder.ny();
        aprilOppdrag.medInntekt(flInntektApril).medPeriode(periodeApril);

        var maiOppdrag = OppgittFrilansOppdragBuilder.ny();
        maiOppdrag.medInntekt(flInntektMai).medPeriode(periodeMai);

        frilans.medFrilansOppdrag(List.of(aprilOppdrag.build(), maiOppdrag.build()));
        oppgittOpptjening.leggTilFrilansOpplysninger(frilans.build());

        var mai = EgenNæringBuilder.ny();
        mai.medBruttoInntekt(snMai).medPeriode(periodeMai);

        oppgittOpptjening.leggTilFrilansOpplysninger(frilans.build());
        oppgittOpptjening.leggTilEgneNæringer(List.of(mai));

        var iayGrunnlag = InntektArbeidYtelseGrunnlagBuilder.nytt();


        iayGrunnlag.medOppgittOpptjening(oppgittOpptjening);
        return iayGrunnlag;
    }

    private InntektArbeidYtelseGrunnlagBuilder lagOppgittOpptjeningMedIfjor(BigDecimal flInntektApril, BigDecimal flInntektMai, BigDecimal snMai, BigDecimal snIFjor) {
        OppgittOpptjeningBuilder oppgittOpptjening = OppgittOpptjeningBuilder.ny();
        var frilans = OppgittFrilansBuilder.ny();
        var aprilOppdrag = OppgittFrilansOppdragBuilder.ny();
        aprilOppdrag.medInntekt(flInntektApril).medPeriode(periodeApril);

        var maiOppdrag = OppgittFrilansOppdragBuilder.ny();
        maiOppdrag.medInntekt(flInntektMai).medPeriode(periodeMai);

        frilans.medFrilansOppdrag(List.of(aprilOppdrag.build(), maiOppdrag.build()));
        oppgittOpptjening.leggTilFrilansOpplysninger(frilans.build());

        var mai = EgenNæringBuilder.ny();
        mai.medBruttoInntekt(snMai).medPeriode(periodeMai);

        var iFjorBuilder = EgenNæringBuilder.ny();
        iFjorBuilder.medBruttoInntekt(snIFjor).medPeriode(iFjor);

        oppgittOpptjening.leggTilFrilansOpplysninger(frilans.build());
        oppgittOpptjening.leggTilEgneNæringer(List.of(mai, iFjorBuilder));

        var iayGrunnlag = InntektArbeidYtelseGrunnlagBuilder.nytt();


        iayGrunnlag.medOppgittOpptjening(oppgittOpptjening);
        return iayGrunnlag;
    }
}
