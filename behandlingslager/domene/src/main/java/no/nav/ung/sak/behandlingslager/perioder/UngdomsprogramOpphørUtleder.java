package no.nav.ung.sak.behandlingslager.perioder;

import no.nav.k9.felles.konfigurasjon.konfig.Tid;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;

/**
 * Avgjør om et tidligere opphør av ungdomsprogrammet ble vedtatt i en <b>annen, allerede avsluttet</b> behandling,
 * eller om opphøret og opphevelsen havnet på <b>samme, fortsatt åpne</b> behandling (dvs. slått sammen før
 * opphøret rakk å bli vedtatt, jf. OpprettRevurderingEllerOpprettDiffTask). I sistnevnte tilfelle finnes det
 * ikke noe opphørsvedtak å reversere, og det skal ikke sendes brev.
 */
public final class UngdomsprogramOpphørUtleder {

    private UngdomsprogramOpphørUtleder() {
    }

    /**
     * @return {@code true} dersom ungdomsprogrammet hadde en (lukket) sluttdato ved forrige vedtak
     * (reell opphevelse av et iverksatt opphørsvedtak). {@code false} dersom opphør og opphevelse havnet
     * i samme, fortsatt åpne behandling (originalbehandling/grunnlag/perioder mangler, eller sluttdato
     * fortsatt var åpen) — da ble opphøret aldri iverksatt.
     */
    public static boolean opphørAvUngdomsprogrammetVarInkludertIVedtaket(Behandling behandling, UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository) {
        return behandling.getOriginalBehandlingId()
            .flatMap(ungdomsprogramPeriodeRepository::hentGrunnlag)
            .map(grunnlag -> grunnlag.getUngdomsprogramPerioder().getPerioder())
            .filter(perioder -> !perioder.isEmpty())
            .map(perioder -> perioder.stream().noneMatch(it -> Tid.TIDENES_ENDE.equals(it.getPeriode().getTomDato())))
            .orElse(false);
    }

}
