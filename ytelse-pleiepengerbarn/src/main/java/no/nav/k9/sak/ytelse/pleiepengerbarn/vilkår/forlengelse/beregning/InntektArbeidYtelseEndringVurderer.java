package no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår.forlengelse.beregning;

import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.typer.AktørId;

class InntektArbeidYtelseEndringVurderer {

    static boolean harEndringRelevantForPerioden(AktørId aktørId,
                                                 InntektArbeidYtelseGrunnlag iayGrunnlag,
                                                 InntektArbeidYtelseGrunnlag originalIayGrunnlag, DatoIntervallEntitet periode) {

        var aktørArbeid = iayGrunnlag.getAktørArbeidFraRegister(aktørId);
        var originalAktørArbeid = originalIayGrunnlag.getAktørArbeidFraRegister(aktørId);
        if (AktørArbeidEndringVurderer.harEndringIArbeid(aktørArbeid, originalAktørArbeid, periode)) {
            return true;
        };

        var aktørYtelse = iayGrunnlag.getAktørYtelseFraRegister(aktørId);
        var originalAktørYtelse = originalIayGrunnlag.getAktørYtelseFraRegister(aktørId);
        if (AktørYtelseEndringVurderer.harEndringIYtelse(aktørYtelse, originalAktørYtelse, periode)) {
            return true;
        }

        var aktørInntekt = iayGrunnlag.getAktørInntektFraRegister(aktørId);
        var originalAktørInntekt = originalIayGrunnlag.getAktørInntektFraRegister(aktørId);
        if (AktørInntektEndringVurderer.harEndringIInntekt(aktørInntekt, originalAktørInntekt, periode)) {
            return true;
        }

        return false;

    }


}
