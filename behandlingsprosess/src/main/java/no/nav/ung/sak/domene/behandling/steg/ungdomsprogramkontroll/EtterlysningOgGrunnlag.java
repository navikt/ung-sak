package no.nav.ung.sak.domene.behandling.steg.ungdomsprogramkontroll;

import no.nav.ung.sak.behandlingslager.etterlysning.Etterlysning;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeGrunnlag;

public record EtterlysningOgGrunnlag(
    Etterlysning etterlysning,
    UngdomsprogramPeriodeGrunnlag grunnlag
) {
}
