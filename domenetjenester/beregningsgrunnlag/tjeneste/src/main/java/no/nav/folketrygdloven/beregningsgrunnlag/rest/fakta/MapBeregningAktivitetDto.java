package no.nav.folketrygdloven.beregningsgrunnlag.rest.fakta;

import java.util.List;
import java.util.Optional;

import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningAktivitetEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.rest.dto.BeregningAktivitetDto;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.arbeidsgiver.ArbeidsgiverOpplysninger;
import no.nav.foreldrepenger.domene.arbeidsgiver.ArbeidsgiverTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdInformasjon;

class MapBeregningAktivitetDto {

    private MapBeregningAktivitetDto() {
        // skjul
    }

    static BeregningAktivitetDto mapBeregningAktivitet(BeregningAktivitetEntitet beregningAktivitet,
                                                       List<BeregningAktivitetEntitet> saksbehandletAktiviteter,
                                                       List<BeregningAktivitetEntitet> forrigeAktiviteter,
                                                       List<BeregningAktivitetEntitet> forrigeSaksbehandletAktiviteter,
                                                       Optional<ArbeidsforholdInformasjon> arbeidsforholdInformasjon,
                                                       ArbeidsgiverTjeneste arbeidsgiverTjeneste) {
        BeregningAktivitetDto dto = new BeregningAktivitetDto();
        mapArbeidsgiver(dto, beregningAktivitet.getArbeidsgiver(), arbeidsgiverTjeneste);
        dto.setArbeidsforholdId(beregningAktivitet.getArbeidsforholdRef().getReferanse());
        arbeidsforholdInformasjon.ifPresent(info -> {
            var eksternArbeidsforholdId = info.finnEkstern(beregningAktivitet.getArbeidsgiver(), beregningAktivitet.getArbeidsforholdRef());
            if (eksternArbeidsforholdId != null) {
                dto.setEksternArbeidsforholdId(eksternArbeidsforholdId.getReferanse());
            }
        });
        dto.setArbeidsforholdType(beregningAktivitet.getOpptjeningAktivitetType());
        dto.setFom(beregningAktivitet.getPeriode().getFomDato());
        dto.setTom(beregningAktivitet.getPeriode().getTomDato());
        if (saksbehandletAktiviteter.isEmpty()) {
            if (forrigeAktiviteter.contains(beregningAktivitet) && !forrigeSaksbehandletAktiviteter.isEmpty()) {
                dto.setSkalBrukes(forrigeSaksbehandletAktiviteter.contains(beregningAktivitet));
            }
        } else {
            dto.setSkalBrukes(saksbehandletAktiviteter.contains(beregningAktivitet));
        }
        return dto;
    }

    private static void mapArbeidsgiver(BeregningAktivitetDto beregningAktivitetDto, Arbeidsgiver arbeidsgiver, ArbeidsgiverTjeneste arbeidsgiverTjeneste) {
        if (arbeidsgiver == null) {
            return;
        }
        ArbeidsgiverOpplysninger opplysninger = arbeidsgiverTjeneste.hent(arbeidsgiver);
        if (opplysninger != null) {
            if (arbeidsgiver.getErVirksomhet()) {
                beregningAktivitetDto.setArbeidsgiverId(opplysninger.getIdentifikator());
            } else if (arbeidsgiver.erAktørId()) {
                beregningAktivitetDto.setArbeidsgiverId(opplysninger.getIdentifikator());
                beregningAktivitetDto.setAktørId(arbeidsgiver.getAktørId());
                beregningAktivitetDto.setAktørIdString(arbeidsgiver.getAktørId().getId());

            }
            beregningAktivitetDto.setArbeidsgiverNavn(opplysninger.getNavn());
        }
    }
}
