package no.nav.ung.sak.behandlingskontroll;

import java.util.Objects;

import no.nav.ung.kodeverk.behandling.BehandlingStegStatus;
import no.nav.ung.kodeverk.behandling.BehandlingStegType;
import no.nav.ung.sak.behandlingskontroll.transisjoner.FellesTransisjoner;
import no.nav.ung.sak.behandlingskontroll.transisjoner.TransisjonIdentifikator;

/**
 * Brukes for intern håndtering av flyt på et steg. Inneholder kode for stegets nye status. Hvis status er fremoverføring,
 * er også steget det skal fremoverføres til inkludert.
 */
public class StegProsesseringResultat {
    private final BehandlingStegStatus nyStegStatus;
    private final TransisjonIdentifikator transisjon;

    private StegProsesseringResultat(BehandlingStegStatus nyStegStatus, TransisjonIdentifikator transisjon) {
        this.nyStegStatus = nyStegStatus;
        this.transisjon = transisjon;
    }

    public static StegProsesseringResultat medMuligTransisjon(BehandlingStegStatus nyStegStatus, TransisjonIdentifikator transisjon) {
        return new StegProsesseringResultat(nyStegStatus, transisjon);
    }

    public static StegProsesseringResultat utenOverhopp(BehandlingStegStatus nyStegStatus) {
        return new StegProsesseringResultat(nyStegStatus, FellesTransisjoner.UTFØRT);
    }

    public TransisjonIdentifikator getTransisjon() {
        return transisjon;
    }

    public BehandlingStegStatus getNyStegStatus() {
        return nyStegStatus;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<nyStegStatus=" + nyStegStatus + ", transisjon=" + transisjon + ">"; // NOSONAR //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    }

    static void validerKombinasjon(BehandlingStegStatus nyStegStatus, BehandlingStegType målsteg) {
        Objects.requireNonNull(nyStegStatus, "resultat må være satt"); //$NON-NLS-1$
        if (BehandlingStegStatus.FREMOVERFØRT.equals(nyStegStatus)) {
            Objects.requireNonNull(målsteg, "målsteg må være satt ved fremoverføring"); //$NON-NLS-1$
        } else {
            if (målsteg != null) {
                throw new IllegalArgumentException("målsteg skal ikke være satt ved resultat " + nyStegStatus.getKode());
            }
        }
    }
}
