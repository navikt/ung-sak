package no.nav.ung.ytelse.ungdomsprogramytelsen.formidling.vedtak.resultat;

import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramMaksPeriode;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;

import java.util.Optional;

public record UngDetaljertResultatBehandlingGrunnlag(boolean manuellOpprettetBehandling,
                                                     UngdomsprogramMaksPeriode ungdomsprogramMaksPeriode,
                                                     DatoIntervallEntitet ungdomsprogramPeriode) {

    public Optional<UngdomsprogramMaksPeriode> ungdomsprogramMaksPeriodeOpt() {
        return Optional.ofNullable(ungdomsprogramMaksPeriode);
    }

}
