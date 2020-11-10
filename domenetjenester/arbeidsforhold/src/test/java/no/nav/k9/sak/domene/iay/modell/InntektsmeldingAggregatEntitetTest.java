package no.nav.k9.sak.domene.iay.modell;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import no.nav.k9.sak.typer.Arbeidsgiver;

public class InntektsmeldingAggregatEntitetTest {

    private static final String ORGNR = "123";

    private final BigDecimal bruttoInntekt = BigDecimal.TEN;

    @Test
    public void skal_lagre_i_riktig_rekkefølge() {
        LocalDateTime nå = LocalDateTime.now();
        Arbeidsgiver arbeidsgiver = Arbeidsgiver.virksomhet(ORGNR);

        InntektsmeldingBuilder førsteInntektsmeldingBuilder = InntektsmeldingBuilder.builder();
        førsteInntektsmeldingBuilder.medKanalreferanse("AR123")
            .medBeløp(bruttoInntekt)
            .medArbeidsgiver(arbeidsgiver)
            .medInnsendingstidspunkt(nå);

        InntektsmeldingBuilder sisteInntektsmedlingBuilder = InntektsmeldingBuilder.builder();
        sisteInntektsmedlingBuilder.medKanalreferanse("AR124")
            .medBeløp(bruttoInntekt)
            .medArbeidsgiver(arbeidsgiver)
            .medInnsendingstidspunkt(nå);

        InntektsmeldingAggregat inntektsmeldingAggregat = new InntektsmeldingAggregat();
        inntektsmeldingAggregat.leggTil(førsteInntektsmeldingBuilder.build());
        inntektsmeldingAggregat.leggTil(sisteInntektsmedlingBuilder.build());

        List<Inntektsmelding> inntektsmeldinger = inntektsmeldingAggregat.getInntektsmeldingerSomSkalBrukes();
        assertThat(inntektsmeldinger).hasSize(1);
        assertThat(inntektsmeldinger.get(0).getKanalreferanse()).isEqualTo("AR124");
    }

    @Test
    public void skal_bruk_ar_hvis_altinn_involvert() {
        LocalDateTime nå = LocalDateTime.now();
        Arbeidsgiver arbeidsgiver = Arbeidsgiver.virksomhet(ORGNR);

        InntektsmeldingBuilder førsteInntektsmeldingBuilder = InntektsmeldingBuilder.builder();
        førsteInntektsmeldingBuilder.medKanalreferanse("AR123")
            .medKildesystem("U4BW")
            .medBeløp(bruttoInntekt)
            .medArbeidsgiver(arbeidsgiver)
            .medInnsendingstidspunkt(nå);

        InntektsmeldingBuilder sisteInntektsmedlingBuilder = InntektsmeldingBuilder.builder();
        sisteInntektsmedlingBuilder.medKanalreferanse("AR124")
            .medKildesystem("AltinnPortal")
            .medArbeidsgiver(arbeidsgiver)
            .medBeløp(bruttoInntekt)
            .medInnsendingstidspunkt(nå.minusMinutes(2));

        InntektsmeldingAggregat inntektsmeldingAggregat = new InntektsmeldingAggregat();
        inntektsmeldingAggregat.leggTil(førsteInntektsmeldingBuilder.build());
        inntektsmeldingAggregat.leggTil(sisteInntektsmedlingBuilder.build());

        List<Inntektsmelding> inntektsmeldinger = inntektsmeldingAggregat.getInntektsmeldingerSomSkalBrukes();
        assertThat(inntektsmeldinger).hasSize(1);
        assertThat(inntektsmeldinger.get(0).getKanalreferanse()).isEqualTo("AR124");
    }

    @Test
    public void skal_ikke_lagre_når_eldre_kanalreferanse_kommer_inn_med_lik_innsendingstidspunkt() {
        LocalDateTime nå = LocalDateTime.now();
        Arbeidsgiver arbeidsgiver = Arbeidsgiver.virksomhet(ORGNR);

        InntektsmeldingBuilder sisteInntektsmeldingBuilder = InntektsmeldingBuilder.builder();
        sisteInntektsmeldingBuilder
            .medKanalreferanse("AR125")
            .medBeløp(bruttoInntekt)
            .medArbeidsgiver(arbeidsgiver)
            .medInnsendingstidspunkt(nå);

        InntektsmeldingBuilder førsteInntektsmeldingBuilder = InntektsmeldingBuilder.builder();
        førsteInntektsmeldingBuilder
            .medKanalreferanse("AR124")
            .medBeløp(bruttoInntekt)
            .medArbeidsgiver(arbeidsgiver)
            .medInnsendingstidspunkt(nå);

        InntektsmeldingAggregat inntektsmeldingAggregat = new InntektsmeldingAggregat();
        inntektsmeldingAggregat.leggTil(sisteInntektsmeldingBuilder.build());
        inntektsmeldingAggregat.leggTil(førsteInntektsmeldingBuilder.build());

        List<Inntektsmelding> inntektsmeldinger = inntektsmeldingAggregat.getInntektsmeldingerSomSkalBrukes();
        assertThat(inntektsmeldinger).hasSize(1);
        assertThat(inntektsmeldinger.get(0).getKanalreferanse()).isEqualTo("AR125");
    }

    @Test
    public void skal_benytte_kanalreferanse_i_sortering() {
        LocalDateTime nå = LocalDateTime.now();
        LocalDateTime omTi = LocalDateTime.now().plusMinutes(10);
        Arbeidsgiver arbeidsgiver = Arbeidsgiver.virksomhet(ORGNR);

        InntektsmeldingBuilder sisteInntektsmeldingBuilder = InntektsmeldingBuilder.builder();
        sisteInntektsmeldingBuilder
            .medKanalreferanse("AR124")
            .medBeløp(bruttoInntekt)
            .medArbeidsgiver(arbeidsgiver)
            .medInnsendingstidspunkt(omTi);

        InntektsmeldingBuilder førsteInntektsmeldingBuilder = InntektsmeldingBuilder.builder();
        førsteInntektsmeldingBuilder
            .medKanalreferanse("AR125")
            .medBeløp(bruttoInntekt)
            .medArbeidsgiver(arbeidsgiver)
            .medInnsendingstidspunkt(nå);

        InntektsmeldingAggregat inntektsmeldingAggregat = new InntektsmeldingAggregat();
        inntektsmeldingAggregat.leggTil(sisteInntektsmeldingBuilder.build());
        inntektsmeldingAggregat.leggTil(førsteInntektsmeldingBuilder.build());

        List<Inntektsmelding> inntektsmeldinger = inntektsmeldingAggregat.getInntektsmeldingerSomSkalBrukes();
        assertThat(inntektsmeldinger).hasSize(1);
        assertThat(inntektsmeldinger.get(0).getKanalreferanse()).isEqualTo("AR125");
    }
}
