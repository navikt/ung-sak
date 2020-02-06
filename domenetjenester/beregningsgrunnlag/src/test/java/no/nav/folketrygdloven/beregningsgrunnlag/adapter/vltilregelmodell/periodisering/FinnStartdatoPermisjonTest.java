package no.nav.folketrygdloven.beregningsgrunnlag.adapter.vltilregelmodell.periodisering;

import static no.nav.vedtak.konfig.Tid.TIDENES_ENDE;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.Test;

import no.nav.foreldrepenger.behandlingslager.virksomhet.VirksomhetEntitet;
import no.nav.foreldrepenger.domene.arbeidsgiver.ArbeidsgiverTjenesteImpl;
import no.nav.foreldrepenger.domene.iay.modell.AktivitetsAvtaleBuilder;
import no.nav.foreldrepenger.domene.iay.modell.Inntektsmelding;
import no.nav.foreldrepenger.domene.iay.modell.InntektsmeldingBuilder;
import no.nav.foreldrepenger.domene.iay.modell.Yrkesaktivitet;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetBuilder;
import no.nav.foreldrepenger.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.kodeverk.arbeidsforhold.ArbeidType;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;

public class FinnStartdatoPermisjonTest {

    private final static String ORGNR = "123456780";
    private static final Arbeidsgiver ARBEIDSGIVER = ArbeidsgiverTjenesteImpl.fra(new VirksomhetEntitet.Builder().medOrgnr(ORGNR).build());
    private InternArbeidsforholdRef ref = InternArbeidsforholdRef.nyRef();
    private LocalDate SKJÆRINGSTIDSPUNKT = LocalDate.now().minusMonths(1);

    @Test
    public void finnStartdatoPermisjonNårAktivitetTilkommerEtterStpOgHarInntektsmeldingMedStartdatoEtterAnsettelsesdato() {
        // Arrange
        LocalDate ansettelsesDato = LocalDate.now();
        Yrkesaktivitet ya = lagYrkesaktivitet(ansettelsesDato);
        LocalDate startPermisjon = ansettelsesDato.plusMonths(1);
        Inntektsmelding inntektsmelding = InntektsmeldingBuilder.builder()
            .medArbeidsforholdId(ref)
            .medArbeidsgiver(ARBEIDSGIVER)
            .medStartDatoPermisjon(startPermisjon)
            .build();

        // Act
        LocalDate startDato = FinnStartdatoPermisjon.finnStartdatoPermisjon(ya, SKJÆRINGSTIDSPUNKT, ansettelsesDato, List.of(inntektsmelding));

        // Assert
        assertThat(startDato).isEqualTo(startPermisjon);
    }

    @Test
    public void finnStartdatoPermisjonNårAktivitetTilkommerEtterStpOgHarInntektsmeldingMedStartdatoFørAnsettelsesdato() {
        // Arrange
        LocalDate ansettelsesDato = LocalDate.now();
        Yrkesaktivitet ya = lagYrkesaktivitet(ansettelsesDato);
        LocalDate startPermisjon = ansettelsesDato.minusMonths(1);
        Inntektsmelding inntektsmelding = InntektsmeldingBuilder.builder()
            .medArbeidsforholdId(ref)
            .medArbeidsgiver(ARBEIDSGIVER)
            .medStartDatoPermisjon(startPermisjon)
            .build();

        // Act
        LocalDate startDato = FinnStartdatoPermisjon.finnStartdatoPermisjon(ya, SKJÆRINGSTIDSPUNKT, ansettelsesDato, List.of(inntektsmelding));

        // Assert
        assertThat(startDato).isEqualTo(ansettelsesDato);
    }

    @Test
    public void finnStartdatoPermisjonNårAktivitetTilkommerEtterStpUtenInntektsmelding() {
        // Arrange
        LocalDate ansettelsesDato = LocalDate.now();
        Yrkesaktivitet ya = lagYrkesaktivitet(ansettelsesDato);

        // Act
        LocalDate startDato = FinnStartdatoPermisjon.finnStartdatoPermisjon(ya, SKJÆRINGSTIDSPUNKT, ansettelsesDato, List.of());

        // Assert
        assertThat(startDato).isEqualTo(ansettelsesDato);
    }

    private Yrkesaktivitet lagYrkesaktivitet(LocalDate ansettelsesDato) {
        YrkesaktivitetBuilder yrkesaktivitetBuilder = YrkesaktivitetBuilder.oppdatere(Optional.empty());
        AktivitetsAvtaleBuilder aktivitetsavtaleBuilder = yrkesaktivitetBuilder.getAktivitetsAvtaleBuilder();

        DatoIntervallEntitet periode = DatoIntervallEntitet.fraOgMedTilOgMed(ansettelsesDato, TIDENES_ENDE);
        lagAktivitetsavtale(aktivitetsavtaleBuilder, periode);

        AktivitetsAvtaleBuilder ansettelsesPeriode = yrkesaktivitetBuilder.getAktivitetsAvtaleBuilder(periode, true);

        return yrkesaktivitetBuilder.medArbeidsgiver(ARBEIDSGIVER)
            .medArbeidsforholdId(ref)
            .leggTilAktivitetsAvtale(aktivitetsavtaleBuilder)
            .leggTilAktivitetsAvtale(ansettelsesPeriode)
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .build();
    }

    private void lagAktivitetsavtale(AktivitetsAvtaleBuilder aktivitetsavtaleBuilder, DatoIntervallEntitet periode) {
        aktivitetsavtaleBuilder.medPeriode(periode).medProsentsats(BigDecimal.valueOf(100));
    }
}
