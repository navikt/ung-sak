package no.nav.ung.sak.behandlingslager.perioder;

import no.nav.k9.felles.konfigurasjon.konfig.Tid;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;

/**
 * Avgjør om et tidligere opphør av ungdomsprogrammet ble vedtatt i en <b>annen, allerede avsluttet</b> behandling,
 * eller om opphøret og opphevelsen havnet på <b>samme, fortsatt åpne</b> behandling (dvs. slått sammen før
 * opphøret rakk å bli vedtatt, jf. OpprettRevurderingEllerOpprettDiffTask). I sistnevnte tilfelle finnes det
 * ikke noe opphørsvedtak å reversere, og det skal ikke sendes brev.
 */
public final class OpphørOpphevetUtleder {

    private OpphørOpphevetUtleder() {
    }

    /**
     * @return {@code true} dersom opphøret ble vedtatt i en tidligere, avsluttet behandling (reell
     * opphevelse av et iverksatt vedtak). {@code false} dersom opphør og opphevelse havnet i samme,
     * fortsatt åpne behandling (originalbehandling mangler, eller hadde fortsatt åpen sluttdato) —
     * da ble opphøret aldri iverksatt.
     */
    public static boolean opphørVarFaktiskIverksatt(Behandling behandling, UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository) {
        return behandling.getOriginalBehandlingId()
            .flatMap(ungdomsprogramPeriodeRepository::hentGrunnlag)
            .map(grunnlag -> grunnlag.getUngdomsprogramPerioder().getPerioder().stream()
                .noneMatch(it -> Tid.TIDENES_ENDE.equals(it.getPeriode().getTomDato())))
            .orElse(false);
    }

}
