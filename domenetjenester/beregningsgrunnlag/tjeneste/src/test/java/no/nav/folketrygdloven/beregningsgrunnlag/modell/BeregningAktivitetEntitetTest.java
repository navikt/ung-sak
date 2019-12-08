package no.nav.folketrygdloven.beregningsgrunnlag.modell;

import static no.nav.vedtak.konfig.Tid.TIDENES_ENDE;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.time.LocalDate;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.vedtak.felles.jpa.tid.ÅpenDatoIntervallEntitet;

public class BeregningAktivitetEntitetTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private static final InternArbeidsforholdRef ARBEIDSFORHOLD_REF = InternArbeidsforholdRef.namedRef("TEST-REF");
    private static final Arbeidsgiver ARBEIDSGIVER = Arbeidsgiver.fra(AktørId.dummy());
    private static final ÅpenDatoIntervallEntitet PERIODE = ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2019, 1, 1), TIDENES_ENDE);

    @Test
    public void skal_bruke_aktivitet_om_ingen_overstyringer_for_aktivitet() {
        // Arrange
        BeregningAktivitetOverstyringerEntitet overstyringer = lagOverstyring(1);
        BeregningAktivitetEntitet aktivitet = BeregningAktivitetEntitet.builder()
            .medPeriode(PERIODE)
            .medArbeidsforholdRef(InternArbeidsforholdRef.nyRef())
            .medArbeidsgiver(ARBEIDSGIVER)
            .medOpptjeningAktivitetType(OpptjeningAktivitetType.ARBEID)
            .build();

        // Act
        boolean skalBrukes = aktivitet.skalBrukes(overstyringer);

        // Assert
        assertThat(skalBrukes).isTrue();
    }

    @Test
    public void skal_ikkje_bruke_aktivitet_om__det_finnes_overstyringer_for_aktivitet() {
        // Arrange
        BeregningAktivitetOverstyringerEntitet overstyringer = lagOverstyring(1);
        BeregningAktivitetEntitet aktivitet = BeregningAktivitetEntitet.builder()
            .medPeriode(PERIODE)
            .medArbeidsforholdRef(ARBEIDSFORHOLD_REF)
            .medArbeidsgiver(ARBEIDSGIVER)
            .medOpptjeningAktivitetType(OpptjeningAktivitetType.ARBEID)
            .build();

        // Act
        boolean skalBrukes = aktivitet.skalBrukes(overstyringer);

        // Assert
        assertThat(skalBrukes).isFalse();
    }

    @Test
    public void skal_kaste_exception_om_det_finnes_flere_overstyringer_for_aktivitet() {
        // Arrange
        BeregningAktivitetOverstyringerEntitet overstyringer = lagOverstyring(2);
        BeregningAktivitetEntitet aktivitet = BeregningAktivitetEntitet.builder()
            .medPeriode(PERIODE)
            .medArbeidsforholdRef(ARBEIDSFORHOLD_REF)
            .medArbeidsgiver(ARBEIDSGIVER)
            .medOpptjeningAktivitetType(OpptjeningAktivitetType.ARBEID)
            .build();

        // Assert
        expectedException.expect(IllegalStateException.class);

        // Act
        aktivitet.skalBrukes(overstyringer);
    }

    private BeregningAktivitetOverstyringerEntitet lagOverstyring(int antallOverstyringer) {
        BeregningAktivitetOverstyringerEntitet.Builder builder = BeregningAktivitetOverstyringerEntitet.builder();
        for (int i = 0; i < antallOverstyringer; i++) {
            BeregningAktivitetOverstyringEntitet overstyring = BeregningAktivitetOverstyringEntitet.builder()
                .medHandling(BeregningAktivitetHandlingType.IKKE_BENYTT)
                .medArbeidsforholdRef(ARBEIDSFORHOLD_REF)
                .medArbeidsgiver(ARBEIDSGIVER)
                .medPeriode(PERIODE)
                .medOpptjeningAktivitetType(OpptjeningAktivitetType.ARBEID)
                .build();
            builder.leggTilOverstyring(overstyring);
        }
        return builder.build();
    }
}
