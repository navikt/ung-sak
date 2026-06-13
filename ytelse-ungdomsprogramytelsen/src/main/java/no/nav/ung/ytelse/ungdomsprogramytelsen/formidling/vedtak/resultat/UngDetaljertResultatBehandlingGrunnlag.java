package no.nav.ung.ytelse.ungdomsprogramytelsen.formidling.vedtak.resultat;

import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramMaksPeriode;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;

import java.util.Optional;

/**
 * Periodeuavhengig grunnlag for detaljert resultat. Bærer fakta som gjelder hele behandlingen og som er
 * like for alle perioder i tidslinjen ({@link UngdomsprogramMaksPeriode}, ungdomsprogramperioden og om
 * behandlingen er manuelt opprettet).
 */
public record UngDetaljertResultatBehandlingGrunnlag(boolean manuellOpprettetBehandling,
                                                     UngdomsprogramMaksPeriode ungdomsprogramMaksPeriode,
                                                     DatoIntervallEntitet ungdomsprogramPeriode) {

    public Optional<UngdomsprogramMaksPeriode> ungdomsprogramMaksPeriodeOpt() {
        return Optional.ofNullable(ungdomsprogramMaksPeriode);
    }

}
