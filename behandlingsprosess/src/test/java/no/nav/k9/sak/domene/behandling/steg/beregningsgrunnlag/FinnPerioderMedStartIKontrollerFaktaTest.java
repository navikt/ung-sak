package no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import no.nav.k9.sak.domene.iay.modell.Inntektsmelding;
import no.nav.k9.sak.domene.iay.modell.InntektsmeldingBuilder;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;

class FinnPerioderMedStartIKontrollerFaktaTest {

    @Test
    void skal_gi_ingen_endring_ved_en_IM_uten_referanse() {

        var gjeldendeIM = InntektsmeldingBuilder.builder()
            .medArbeidsgiver(Arbeidsgiver.virksomhet("12346778"))
            .medArbeidsforholdId(InternArbeidsforholdRef.nullRef())
            .medBeløp(BigDecimal.TEN)
            .medKanalreferanse("KANALREFERANSE")
            .build();

        var erEndret = FinnPerioderMedStartIKontrollerFakta.erEndret(List.of(gjeldendeIM), List.of(gjeldendeIM));

        assertThat(erEndret).isFalse();

    }

    @Test
    void skal_gi_ingen_endring_ved_en_IM_med_referanse() {

        var gjeldendeIM = InntektsmeldingBuilder.builder()
            .medArbeidsgiver(Arbeidsgiver.virksomhet("12346778"))
            .medArbeidsforholdId(InternArbeidsforholdRef.nyRef())
            .medBeløp(BigDecimal.TEN)
            .medKanalreferanse("KANALREFERANSE")
            .build();

        var erEndret = FinnPerioderMedStartIKontrollerFakta.erEndret(List.of(gjeldendeIM), List.of(gjeldendeIM));

        assertThat(erEndret).isFalse();

    }

    @Test
    void skal_gi_endring_når_ulike_arbeidsgivere_likt_antall_IM() {

        var gjeldendeIM = InntektsmeldingBuilder.builder()
            .medArbeidsgiver(Arbeidsgiver.virksomhet("12346778"))
            .medArbeidsforholdId(InternArbeidsforholdRef.nullRef())
            .medBeløp(BigDecimal.TEN)
            .medKanalreferanse("KANALREFERANSE")
            .build();

        var forrigeIM = InntektsmeldingBuilder.builder()
            .medArbeidsgiver(Arbeidsgiver.virksomhet("2442323"))
            .medArbeidsforholdId(InternArbeidsforholdRef.nullRef())
            .medBeløp(BigDecimal.TEN)
            .medKanalreferanse("KANALREFERANSE")
            .build();

        var erEndret = FinnPerioderMedStartIKontrollerFakta.erEndret(List.of(gjeldendeIM), List.of(forrigeIM));

        assertThat(erEndret).isTrue();

    }

    @Test
    void skal_gi_endring_når_tilkommet_inntektsmelding_for_samme_AG() {

        var gjeldendeIM = InntektsmeldingBuilder.builder()
            .medArbeidsgiver(Arbeidsgiver.virksomhet("12346778"))
            .medArbeidsforholdId(InternArbeidsforholdRef.nyRef())
            .medBeløp(BigDecimal.TEN)
            .medKanalreferanse("KANALREFERANSE")
            .build();

        var forrigeIM = InntektsmeldingBuilder.builder()
            .medArbeidsgiver(Arbeidsgiver.virksomhet("12346778"))
            .medArbeidsforholdId(InternArbeidsforholdRef.nyRef())
            .medBeløp(BigDecimal.TEN)
            .medKanalreferanse("KANALREFERANSE")
            .build();

        var erEndret = FinnPerioderMedStartIKontrollerFakta.erEndret(List.of(gjeldendeIM, forrigeIM), List.of(forrigeIM));

        assertThat(erEndret).isTrue();

    }

    @Test
    void skal_gi_endring_når_tilkommet_inntektsmelding_for_ny_AG() {

        var gjeldendeIM = InntektsmeldingBuilder.builder()
            .medArbeidsgiver(Arbeidsgiver.virksomhet("12346778"))
            .medArbeidsforholdId(InternArbeidsforholdRef.nyRef())
            .medBeløp(BigDecimal.TEN)
            .medKanalreferanse("KANALREFERANSE")
            .build();

        var forrigeIM = InntektsmeldingBuilder.builder()
            .medArbeidsgiver(Arbeidsgiver.virksomhet("32423423"))
            .medArbeidsforholdId(InternArbeidsforholdRef.nyRef())
            .medBeløp(BigDecimal.TEN)
            .medKanalreferanse("KANALREFERANSE")
            .build();

        var erEndret = FinnPerioderMedStartIKontrollerFakta.erEndret(List.of(gjeldendeIM, forrigeIM), List.of(forrigeIM));

        assertThat(erEndret).isTrue();
    }

    @Test
    void skal_gi_endring_når_forrige_IM_var_tom() {

        var gjeldendeIM = InntektsmeldingBuilder.builder()
            .medArbeidsgiver(Arbeidsgiver.virksomhet("12346778"))
            .medArbeidsforholdId(InternArbeidsforholdRef.nyRef())
            .medBeløp(BigDecimal.TEN)
            .medKanalreferanse("KANALREFERANSE")
            .build();


        var erEndret = FinnPerioderMedStartIKontrollerFakta.erEndret(List.of(gjeldendeIM), List.of());

        assertThat(erEndret).isTrue();
    }


    @Test
    void skal_gi_endring_når_gjeldende_IM_er_tom() {

        var forrigeIM = InntektsmeldingBuilder.builder()
            .medArbeidsgiver(Arbeidsgiver.virksomhet("32423423"))
            .medArbeidsforholdId(InternArbeidsforholdRef.nyRef())
            .medBeløp(BigDecimal.TEN)
            .medKanalreferanse("KANALREFERANSE")
            .build();


        var erEndret = FinnPerioderMedStartIKontrollerFakta.erEndret(List.of(), List.of(forrigeIM));

        assertThat(erEndret).isTrue();
    }

    @Test
    void skal_gi_ingen_endring_når_begge_listene_er_tomme() {

        var erEndret = FinnPerioderMedStartIKontrollerFakta.erEndret(List.of(), List.of());

        assertThat(erEndret).isFalse();
    }


}
