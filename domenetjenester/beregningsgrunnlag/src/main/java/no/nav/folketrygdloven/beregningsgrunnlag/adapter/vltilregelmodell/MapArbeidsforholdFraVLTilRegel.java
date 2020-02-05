package no.nav.folketrygdloven.beregningsgrunnlag.adapter.vltilregelmodell;

import java.util.Optional;

import no.nav.folketrygdloven.beregningsgrunnlag.adapter.vltilregelmodell.kodeverk.MapOpptjeningAktivitetTypeFraVLTilRegel;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BGAndelArbeidsforhold;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.grunnlag.inntekt.Arbeidsforhold;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.iay.modell.Inntektsmelding;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;

public class MapArbeidsforholdFraVLTilRegel {
    private MapArbeidsforholdFraVLTilRegel() {
        // skjul public constructor
    }

    static Arbeidsforhold arbeidsforholdFor(BeregningsgrunnlagPrStatusOgAndel vlBGPStatus) {
        if (erFrilanser(vlBGPStatus.getAktivitetStatus())) {
            return Arbeidsforhold.frilansArbeidsforhold();
        }
        Optional<Arbeidsgiver> arbeidsgiver = vlBGPStatus.getBgAndelArbeidsforhold().map(BGAndelArbeidsforhold::getArbeidsgiver);
        if (arbeidsgiver.isPresent()) {
            return lagArbeidsforholdHosArbeidsgiver(arbeidsgiver.get(), vlBGPStatus);
        } else {
            return Arbeidsforhold.anonymtArbeidsforhold(MapOpptjeningAktivitetTypeFraVLTilRegel.map(vlBGPStatus.getArbeidsforholdType()));
        }
    }

    private static boolean erFrilanser(no.nav.k9.kodeverk.arbeidsforhold.AktivitetStatus aktivitetStatus) {
        return no.nav.k9.kodeverk.arbeidsforhold.AktivitetStatus.FRILANSER.equals(aktivitetStatus);
    }

    private static Arbeidsforhold lagArbeidsforholdHosArbeidsgiver(Arbeidsgiver arbeidsgiver, BeregningsgrunnlagPrStatusOgAndel vlBGPStatus) {
        String arbRef = arbeidsforholdRefFor(vlBGPStatus);
        if (arbeidsgiver.getErVirksomhet()) {
            return Arbeidsforhold.nyttArbeidsforholdHosVirksomhet(arbeidsgiver.getOrgnr(), arbRef);
        }
        if (arbeidsgiver.erAktørId()) {
            return Arbeidsforhold.nyttArbeidsforholdHosPrivatperson(arbeidsgiver.getAktørId().getId(), arbRef);
        }
        throw new IllegalStateException("Arbeidsgiver må være enten aktør eller virksomhet, men var: " + arbeidsgiver);
    }

    private static String arbeidsforholdRefFor(BeregningsgrunnlagPrStatusOgAndel vlBGPStatus) {
        return vlBGPStatus.getBgAndelArbeidsforhold().map(BGAndelArbeidsforhold::getArbeidsforholdRef).map(InternArbeidsforholdRef::getReferanse).orElse(null);
    }

    public static Arbeidsforhold mapArbeidsforhold(Arbeidsgiver arbeidsgiver, InternArbeidsforholdRef arbeidsforholdRef) {
        if (arbeidsgiver.getErVirksomhet()) {
            return Arbeidsforhold.nyttArbeidsforholdHosVirksomhet(arbeidsgiver.getOrgnr(), arbeidsforholdRef.getReferanse());
        }
        if (arbeidsgiver.erAktørId()) {
            return Arbeidsforhold.nyttArbeidsforholdHosPrivatperson(arbeidsgiver.getAktørId().getId(), arbeidsforholdRef.getReferanse());
        }
        throw new IllegalStateException("Arbeidsgiver må være enten aktør eller virksomhet, men var: " + arbeidsgiver);
    }

    static Arbeidsforhold mapForInntektsmelding(Inntektsmelding im) {
        return mapArbeidsforhold(im.getArbeidsgiver(), im.getArbeidsforholdRef());
    }
}
