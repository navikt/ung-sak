package no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.v1;

import no.nav.folketrygdloven.kalkulus.felles.v1.InternArbeidsforholdRefDto;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;

public class MapFraKalkulusTilK9IAY {
    public static InternArbeidsforholdRef mapArbeidsforholdRed(InternArbeidsforholdRefDto arbeidsforholdRef) {
        return InternArbeidsforholdRef.ref(arbeidsforholdRef.getAbakusReferanse());
    }

    public static Arbeidsgiver mapArbeidsgiver(no.nav.folketrygdloven.kalkulus.response.v1.Arbeidsgiver arbeidsgiver) {
        return arbeidsgiver.getArbeidsgiverOrgnr() != null ? Arbeidsgiver.virksomhet(arbeidsgiver.getArbeidsgiverOrgnr()) :
            Arbeidsgiver.fra(new AktørId(arbeidsgiver.getArbeidsgiverAktørId()));
    }
}
