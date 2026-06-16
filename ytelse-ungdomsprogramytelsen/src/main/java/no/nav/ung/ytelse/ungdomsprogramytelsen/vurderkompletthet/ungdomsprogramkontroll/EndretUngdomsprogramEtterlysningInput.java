package no.nav.ung.ytelse.ungdomsprogramytelsen.vurderkompletthet.ungdomsprogramkontroll;

import no.nav.ung.kodeverk.varsel.EtterlysningType;
import no.nav.ung.sak.behandlingslager.behandling.startdato.StartdatoGrunnlag;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeGrunnlag;

import java.util.Optional;

public record EndretUngdomsprogramEtterlysningInput(
        EtterlysningType etterlysningType,
        Optional<EtterlysningOgGrunnlag> gjeldendeEtterlysningOgGrunnlag,
        UngdomsprogramPeriodeGrunnlag gjeldendePeriodeGrunnlag,
        UngdomsprogramPeriodeGrunnlag initiellPeriodegrunnlag,
        Optional<StartdatoGrunnlag> ungdomsytelseStartdatoGrunnlag) {
}
