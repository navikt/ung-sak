package no.nav.ung.sak.domene.behandling.steg.ungdomsprogramkontroll;

import no.nav.ung.kodeverk.etterlysning.EtterlysningType;
import no.nav.ung.sak.behandlingslager.behandling.startdato.UngdomsytelseStartdatoGrunnlag;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeGrunnlag;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public record EndretUngdomsprogramEtterlysningInput(
        EtterlysningType etterlysningType,
        Optional<EtterlysningOgGrunnlag> gjeldendeEtterlysningOgGrunnlag,
        UngdomsprogramPeriodeGrunnlag gjeldendePeriodeGrunnlag,
        UngdomsprogramPeriodeGrunnlag initiellPeriodegrunnlag,
        Optional<UngdomsytelseStartdatoGrunnlag> ungdomsytelseStartdatoGrunnlag) {
}
