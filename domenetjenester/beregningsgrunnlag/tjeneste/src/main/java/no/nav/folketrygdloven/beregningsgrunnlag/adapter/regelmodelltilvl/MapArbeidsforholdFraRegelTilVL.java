package no.nav.folketrygdloven.beregningsgrunnlag.adapter.regelmodelltilvl;

import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.grunnlag.inntekt.Arbeidsforhold;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.grunnlag.inntekt.ReferanseType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.typer.AktørId;

class MapArbeidsforholdFraRegelTilVL {
    private MapArbeidsforholdFraRegelTilVL() {
        // skjul private constructor
    }

    static Arbeidsgiver map(Arbeidsforhold af) {
        if (ReferanseType.AKTØR_ID.equals(af.getReferanseType())) {
            return Arbeidsgiver.person(new AktørId(af.getAktørId()));
        } else if (ReferanseType.ORG_NR.equals(af.getReferanseType())) {
            return Arbeidsgiver.virksomhet(af.getOrgnr());
        }
        return null;
    }
}
