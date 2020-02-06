package no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.Test;

import no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.SaksbehandletBeregningsaktivitetTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningAktivitetAggregatEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningAktivitetEntitet;
import no.nav.foreldrepenger.domene.typer.tid.ÅpenDatoIntervallEntitet;
import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt.BeregningsaktivitetLagreDto;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;

public class SaksbehandletBeregningsaktivitetTjenesteTest {

    private static final LocalDate SKJÆRINGSTIDSPUNKT = LocalDate.now();

    @Test
    public void lagSaksbehandletVersjon_fjerner_ingen_aktiviteter() {
        // Arrange
        ÅpenDatoIntervallEntitet periode = ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT.minusYears(2), SKJÆRINGSTIDSPUNKT);
        BeregningAktivitetAggregatEntitet.Builder builder = BeregningAktivitetAggregatEntitet.builder()
            .medSkjæringstidspunktOpptjening(SKJÆRINGSTIDSPUNKT);
        BeregningAktivitetEntitet arbeid = BeregningAktivitetEntitet.builder()
            .medPeriode(periode)
            .medOpptjeningAktivitetType(OpptjeningAktivitetType.ARBEID)
            .medArbeidsgiver(Arbeidsgiver.person(AktørId.dummy()))
            .medArbeidsforholdRef(InternArbeidsforholdRef.nyRef())
            .build();
        builder.leggTilAktivitet(arbeid);

        BeregningAktivitetEntitet næring = BeregningAktivitetEntitet.builder()
            .medPeriode(periode)
            .medOpptjeningAktivitetType(OpptjeningAktivitetType.NÆRING)
            .build();
        builder.leggTilAktivitet(næring);
        BeregningAktivitetAggregatEntitet register = builder.build();
        BeregningsaktivitetLagreDto næringDto = mapTilDto(næring, true);

        // Act
        BeregningAktivitetAggregatEntitet saksbehandlet = SaksbehandletBeregningsaktivitetTjeneste.lagSaksbehandletVersjon(register, List.of(næringDto));

        // Assert
        assertThat(saksbehandlet.getBeregningAktiviteter()).hasSize(2);
    }

    @Test
    public void lagSaksbehandletVersjon_fjerner_en_aktivitet() {
        // Arrange
        ÅpenDatoIntervallEntitet periode = ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT.minusYears(2), SKJÆRINGSTIDSPUNKT);
        BeregningAktivitetAggregatEntitet.Builder builder = BeregningAktivitetAggregatEntitet.builder()
            .medSkjæringstidspunktOpptjening(SKJÆRINGSTIDSPUNKT);
        BeregningAktivitetEntitet arbeid = BeregningAktivitetEntitet.builder()
            .medPeriode(periode)
            .medOpptjeningAktivitetType(OpptjeningAktivitetType.ARBEID)
            .medArbeidsgiver(Arbeidsgiver.person(AktørId.dummy()))
            .medArbeidsforholdRef(InternArbeidsforholdRef.nyRef())
            .build();
        builder.leggTilAktivitet(arbeid);

        BeregningAktivitetEntitet næring = BeregningAktivitetEntitet.builder()
            .medPeriode(periode)
            .medOpptjeningAktivitetType(OpptjeningAktivitetType.NÆRING)
            .build();
        builder.leggTilAktivitet(næring);
        BeregningAktivitetAggregatEntitet register = builder.build();
        BeregningsaktivitetLagreDto næringDto = mapTilDto(næring, false);

        // Act
        BeregningAktivitetAggregatEntitet saksbehandlet = SaksbehandletBeregningsaktivitetTjeneste.lagSaksbehandletVersjon(register, List.of(næringDto));

        // Assert
        assertThat(saksbehandlet.getBeregningAktiviteter()).hasSize(1);
        assertThat(saksbehandlet.getBeregningAktiviteter()).anySatisfy(ba ->
            assertThat(ba).isEqualTo(arbeid));
    }

    private BeregningsaktivitetLagreDto mapTilDto(BeregningAktivitetEntitet beregningAktivitet, boolean skalBrukes) {
        BeregningsaktivitetLagreDto.Builder builder = BeregningsaktivitetLagreDto.builder()
            .medOpptjeningAktivitetType(beregningAktivitet.getOpptjeningAktivitetType())
            .medFom(beregningAktivitet.getPeriode().getFomDato())
            .medTom(beregningAktivitet.getPeriode().getTomDato())
            .medArbeidsforholdRef(beregningAktivitet.getArbeidsforholdRef().getReferanse())
            .medSkalBrukes(skalBrukes);
        Arbeidsgiver arbeidsgiver = beregningAktivitet.getArbeidsgiver();
        if (arbeidsgiver != null) {
            if (arbeidsgiver.getErVirksomhet()) {
                builder.medOppdragsgiverOrg(arbeidsgiver.getOrgnr());
            } else {
                builder.medArbeidsgiverIdentifikator(arbeidsgiver.getAktørId().getId());
            }
        }
        return builder.build();
    }
}
