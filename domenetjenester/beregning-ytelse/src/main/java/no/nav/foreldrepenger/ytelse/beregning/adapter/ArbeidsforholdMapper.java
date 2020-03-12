package no.nav.foreldrepenger.ytelse.beregning.adapter;

import java.util.Optional;

import no.nav.folketrygdloven.beregningsgrunnlag.modell.BGAndelArbeidsforhold;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.Arbeidsforhold;
import no.nav.k9.kodeverk.arbeidsforhold.AktivitetStatus;
import no.nav.k9.kodeverk.uttak.UttakArbeidType;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;

public final class ArbeidsforholdMapper {

    private ArbeidsforholdMapper() {}

    public static Arbeidsforhold mapArbeidsforholdFraUttakAktivitet(Optional<Arbeidsgiver> arbeidsgiver, Optional<InternArbeidsforholdRef> arbeidsforholdRef, UttakArbeidType uttakArbeidType) {
        if (UttakArbeidType.FRILANSER.equals(uttakArbeidType)) {
            return Arbeidsforhold.frilansArbeidsforhold();
        }
        return lagArbeidsforhold(arbeidsgiver, arbeidsforholdRef);
    }

    static Arbeidsforhold mapArbeidsforholdFraBeregningsgrunnlag(BeregningsgrunnlagPrStatusOgAndel andel) {
        if (AktivitetStatus.FRILANSER.equals(andel.getAktivitetStatus())) {
            return Arbeidsforhold.frilansArbeidsforhold();
        }
        Optional<Arbeidsgiver> arbeidsgiver = andel.getBgAndelArbeidsforhold().map(BGAndelArbeidsforhold::getArbeidsgiver);
        Optional<InternArbeidsforholdRef> arbeidsforholdRef = andel.getBgAndelArbeidsforhold().map(BGAndelArbeidsforhold::getArbeidsforholdRef);
        return lagArbeidsforhold(arbeidsgiver, arbeidsforholdRef);
    }

    private static Arbeidsforhold lagArbeidsforhold(Optional<Arbeidsgiver> arbeidsgiverOpt, Optional<InternArbeidsforholdRef> arbeidsforholdRef) {
        if (arbeidsgiverOpt.isPresent()) {
            Arbeidsgiver arbeidsgiver = arbeidsgiverOpt.get();
            if (arbeidsgiver.getErVirksomhet()) {
                return lagArbeidsforholdHosVirksomhet(arbeidsgiver, arbeidsforholdRef);
            }
            if (arbeidsgiver.erAktørId()) {
                return lagArbeidsforholdHosPrivatperson(arbeidsgiver, arbeidsforholdRef);
            } else {
                throw new IllegalStateException("Utviklerfeil: Arbeidsgiver er verken virksomhet eller aktørId");
            }
        }
        return null;
    }

    private static Arbeidsforhold lagArbeidsforholdHosVirksomhet(Arbeidsgiver arbgiver, Optional<InternArbeidsforholdRef> arbeidsforholdRef) {
        return arbeidsforholdRef.isPresent()
            ? Arbeidsforhold.nyttArbeidsforholdHosVirksomhet(arbgiver.getIdentifikator(), arbeidsforholdRef.get().getReferanse())
            : Arbeidsforhold.nyttArbeidsforholdHosVirksomhet(arbgiver.getIdentifikator());
    }

    private static Arbeidsforhold lagArbeidsforholdHosPrivatperson(Arbeidsgiver arbgiver, Optional<InternArbeidsforholdRef> arbeidsforholdRef) {
        return arbeidsforholdRef.isPresent()
            ? Arbeidsforhold.nyttArbeidsforholdHosPrivatperson(arbgiver.getIdentifikator(), arbeidsforholdRef.get().getReferanse())
            : Arbeidsforhold.nyttArbeidsforholdHosPrivatperson(arbgiver.getIdentifikator());
    }

}
