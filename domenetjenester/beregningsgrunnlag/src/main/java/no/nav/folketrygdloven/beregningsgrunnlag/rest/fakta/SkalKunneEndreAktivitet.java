package no.nav.folketrygdloven.beregningsgrunnlag.rest.fakta;

import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.behandlingslager.behandling.aktivitet.AktivitetStatus;

class SkalKunneEndreAktivitet {

    private SkalKunneEndreAktivitet() {
        // Hide constructor
    }

    /**
     * Vurderer om ein gitt andel skal kunne endres i gui.
     *
     * Endring vil seie å kunne slette eller endre arbeidsforhold i nedtrekksmeny for andelen.
     *
     * @param andel Ein gitt beregningsgrunnlagsandel
     * @return boolean som seier om andel/aktivitet skal kunne endres i gui
     */
    static Boolean skalKunneEndreAktivitet(BeregningsgrunnlagPrStatusOgAndel andel) {
        return andel.getLagtTilAvSaksbehandler() && !andel.getAktivitetStatus().equals(AktivitetStatus.DAGPENGER);
    }

}
