package no.nav.ung.sak.domene.behandling.steg.ungdomsprogramkontroll;

import no.nav.ung.sak.behandlingslager.behandling.startdato.UngdomsytelseStartdatoGrunnlag;
import no.nav.ung.sak.behandlingslager.etterlysning.Etterlysning;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeGrunnlag;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public record EndretUngdomsprogramEtterlysningInput(
        List<EtterlysningOgGrunnlag> gjeldendeStartdatoEtterlysning,
        List<EtterlysningOgGrunnlag> gjeldendeSluttdatoEtterlysning,
        UngdomsprogramPeriodeGrunnlag gjeldendePeriodeGrunnlag,
        Optional<UngdomsprogramPeriodeGrunnlag> initiellPeriodegrunnlag,
        Map<UUID, UngdomsprogramPeriodeGrunnlag> grunnlagPrReferanse,
        Optional<UngdomsytelseStartdatoGrunnlag> ungdomsytelseStartdatoGrunnlag) {

    public UngdomsprogramPeriodeGrunnlag finnGrunnlag(UUID grunnlagsreferanse) {
        return grunnlagPrReferanse.get(grunnlagsreferanse);
    }
}
