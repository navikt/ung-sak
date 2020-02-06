package no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt;

import java.util.List;
import java.util.Objects;

import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningAktivitetAggregatEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningAktivitetEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningAktivitetNøkkel;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt.BeregningsaktivitetLagreDto;

class SaksbehandletBeregningsaktivitetTjeneste {
    private SaksbehandletBeregningsaktivitetTjeneste() {
        // skjul public constructor
    }

    static BeregningAktivitetAggregatEntitet lagSaksbehandletVersjon(BeregningAktivitetAggregatEntitet registerAktiviteter,
                                                                     List<BeregningsaktivitetLagreDto> handlingListe) {
        BeregningAktivitetAggregatEntitet.Builder saksbehandletBuilder = BeregningAktivitetAggregatEntitet.builder()
            .medSkjæringstidspunktOpptjening(registerAktiviteter.getSkjæringstidspunktOpptjening());
        registerAktiviteter.getBeregningAktiviteter().stream()
            .filter(ba -> !skalFjernes(handlingListe, ba))
            .forEach(ba -> saksbehandletBuilder.leggTilAktivitet(new BeregningAktivitetEntitet(ba)));
        return saksbehandletBuilder.build();
    }

    private static boolean skalFjernes(List<BeregningsaktivitetLagreDto> handlingListe, BeregningAktivitetEntitet beregningAktivitet) {
        BeregningAktivitetNøkkel nøkkel = beregningAktivitet.getNøkkel();
        return handlingListe.stream()
            .filter(baDto -> Objects.equals(getNøkkel(baDto), nøkkel))
            .anyMatch(baDto -> !baDto.getSkalBrukes());
    }

    private static Object getNøkkel(BeregningsaktivitetLagreDto dto) {
        BeregningAktivitetNøkkel.Builder builder = BeregningAktivitetNøkkel.builder()
            .medOpptjeningAktivitetType(dto.getOpptjeningAktivitetType())
            .medFom(dto.getFom())
            .medTom(dto.getTom())
            .medArbeidsforholdRef(dto.getArbeidsforholdRef());
        if (dto.getOppdragsgiverOrg() != null) {
            builder.medArbeidsgiverIdentifikator(dto.getOppdragsgiverOrg());
        } else {
            builder.medArbeidsgiverIdentifikator(dto.getArbeidsgiverIdentifikator());
        }
        return builder.build();
    }

}
