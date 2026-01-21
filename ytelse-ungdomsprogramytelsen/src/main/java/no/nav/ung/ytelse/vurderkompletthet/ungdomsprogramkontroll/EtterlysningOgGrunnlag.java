package no.nav.ung.ytelse.vurderkompletthet.ungdomsprogramkontroll;

import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeGrunnlag;

public record EtterlysningOgGrunnlag(
    EtterlysningStatusOgType etterlysningData,
    UngdomsprogramPeriodeGrunnlag grunnlag
) {
}
