package no.nav.ung.sak.behandlingslager.perioder;

import no.nav.k9.felles.konfigurasjon.konfig.Tid;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;

/**
 * Avgjør om et tidligere opphør av ungdomsprogrammet faktisk ble vedtatt/iverksatt, til bruk når en
 * opphevelseshendelse (RE_HENDELSE_OPPHØR_OPPHEVET_UNGDOMSPROGRAM) skal håndteres.
 * <p>
 * <b>Bakgrunn/nyanse mellom «annullert» og «opphevet»:</b> Opphør og opphevelse kan bli slått sammen på samme,
 * fortsatt åpne behandling (jf. OpprettRevurderingEllerOpprettDiffTask) dersom veileder fjerner sluttdatoen
 * igjen før behandlingen med opphørsårsak er ferdig vedtatt (f.eks. mens den venter på uttalelse fra deltaker
 * om sluttdatoen). I dette tilfellet ble opphøret aldri reelt iverksatt — ingen opphørsbrev ble noen gang sendt
 * til bruker — og hendelsen bør da fremstå som at opphøret er <b>annullert</b> (avbrutt før det fikk virkning),
 * ikke <b>opphevet</b> (reversering av et opphør som faktisk fikk virkning og ble vedtatt/kommunisert til bruker
 * med eget brev). Denne klassen skiller mellom disse to tilfellene ved å sjekke om originalbehandlingen (siste
 * vedtatte behandling) faktisk hadde en lukket sluttdato eller fortsatt hadde åpen sluttdato ({@link Tid#TIDENES_ENDE}).
 */
public final class OpphørOpphevetUtleder {

    private OpphørOpphevetUtleder() {
    }

    /**
     * @return {@code true} dersom opphøret faktisk ble vedtatt/iverksatt i en tidligere behandling (dvs.
     * originalbehandlingen hadde en lukket sluttdato) — da er dette en reell <b>opphevelse</b> av et vedtatt
     * opphør. {@code false} dersom opphøret aldri ble iverksatt (originalbehandlingen manglet, eller hadde
     * fortsatt åpen sluttdato) — da er dette en <b>annullering</b> av et opphør som aldri fikk virkning.
     */
    public static boolean opphørVarFaktiskIverksatt(Behandling behandling, UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository) {
        return behandling.getOriginalBehandlingId()
            .flatMap(ungdomsprogramPeriodeRepository::hentGrunnlag)
            .map(grunnlag -> grunnlag.getUngdomsprogramPerioder().getPerioder().stream()
                .noneMatch(it -> Tid.TIDENES_ENDE.equals(it.getPeriode().getTomDato())))
            .orElse(false);
    }

}
