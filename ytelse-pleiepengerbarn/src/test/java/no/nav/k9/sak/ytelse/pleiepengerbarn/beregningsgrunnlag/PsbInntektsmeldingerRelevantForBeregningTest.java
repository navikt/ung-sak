package no.nav.k9.sak.ytelse.pleiepengerbarn.beregningsgrunnlag;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

import org.junit.jupiter.api.Test;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.domene.iay.modell.InntektsmeldingBuilder;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.typer.Arbeidsgiver;

class PsbInntektsmeldingerRelevantForBeregningTest {

    private PsbInntektsmeldingerRelevantForBeregning tjeneste = new PsbInntektsmeldingerRelevantForBeregning();

    @Test
    void skal_velge_rett_inntektsmelding() {
        var startDato = LocalDate.now();
        Arbeidsgiver virksomhet = Arbeidsgiver.virksomhet("000000000");
        var inntektsmelding1 = InntektsmeldingBuilder.builder()
            .medYtelse(FagsakYtelseType.PSB)
            .medArbeidsgiver(virksomhet)
            .medJournalpostId("1")
            .medStartDatoPermisjon(startDato)
            .medBeløp(BigDecimal.TEN)
            .medKanalreferanse("AR123")
            .medRefusjon(BigDecimal.TEN)
            .build();
        var inntektsmelding2 = InntektsmeldingBuilder.builder()
            .medYtelse(FagsakYtelseType.PSB)
            .medArbeidsgiver(virksomhet)
            .medStartDatoPermisjon(startDato)
            .medJournalpostId("2")
            .medBeløp(BigDecimal.ONE)
            .medKanalreferanse("AR124")
            .medRefusjon(BigDecimal.ONE)
            .build();
        var sakInntektsmeldinger = Set.of(inntektsmelding1, inntektsmelding2);
        var vilkårsperiode = DatoIntervallEntitet.fraOgMedTilOgMed(startDato, startDato);
        var relevanteInntektsmeldinger = tjeneste.utledInntektsmeldingerSomGjelderForPeriode(sakInntektsmeldinger, vilkårsperiode);
        assertThat(relevanteInntektsmeldinger).containsOnly(inntektsmelding2);
    }
}
