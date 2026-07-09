package no.nav.ung.ytelse.ungdomsprogramytelsen.formidling.vedtak.resultat;

import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramMaksPeriode;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;

import java.util.Optional;

/**
 * @param harOpphevelseAvOpphør       {@code true} dersom behandlingen har årsak {@code RE_HENDELSE_OPPHØR_OPPHEVET_UNGDOMSPROGRAM}.
 * @param opphørVarFaktiskIverksatt   Kun relevant når {@code harOpphevelseAvOpphør} er {@code true}: er {@code true} dersom opphøret
 *                                    ble vedtatt i en tidligere, avsluttet behandling. {@code false} dersom opphør og opphevelse
 *                                    havnet på samme, fortsatt åpne behandling (opphøret ble aldri vedtatt).
 */
public record UngDetaljertResultatBehandlingGrunnlag(boolean manuellOpprettetBehandling,
                                                     UngdomsprogramMaksPeriode ungdomsprogramMaksPeriode,
                                                     DatoIntervallEntitet ungdomsprogramPeriode,
                                                     boolean harOpphevelseAvOpphør,
                                                     boolean opphørVarFaktiskIverksatt) {

    public Optional<UngdomsprogramMaksPeriode> ungdomsprogramMaksPeriodeOpt() {
        return Optional.ofNullable(ungdomsprogramMaksPeriode);
    }

}
