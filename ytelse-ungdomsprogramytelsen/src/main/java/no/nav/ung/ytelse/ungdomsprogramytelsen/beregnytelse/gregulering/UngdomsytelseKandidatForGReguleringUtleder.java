package no.nav.ung.ytelse.ungdomsprogramytelsen.beregnytelse.gregulering;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.ytelse.UngdomsytelseGrunnlag;
import no.nav.ung.sak.behandlingslager.ytelse.UngdomsytelseGrunnlagRepository;
import no.nav.ung.sak.behandlingslager.ytelse.sats.UngdomsytelseSatser;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.domene.typer.tid.TidslinjeUtil;
import no.nav.ung.sak.grunnbeløp.Grunnbeløp;
import no.nav.ung.sak.grunnbeløp.GrunnbeløpTidslinje;
import no.nav.ung.sak.ytelse.regulering.PerioderForGReguleringUtleder;

import java.util.NavigableSet;
import java.util.Optional;
import java.util.TreeSet;

@ApplicationScoped
@FagsakYtelseTypeRef(FagsakYtelseType.UNGDOMSYTELSE)
public class UngdomsytelseKandidatForGReguleringUtleder implements PerioderForGReguleringUtleder {

    private UngdomsytelseGrunnlagRepository grunnlagRepository;

    @Inject
    public UngdomsytelseKandidatForGReguleringUtleder(UngdomsytelseGrunnlagRepository grunnlagRepository) {
        this.grunnlagRepository = grunnlagRepository;
    }

    public UngdomsytelseKandidatForGReguleringUtleder() {
    }

    @Override
    public NavigableSet<DatoIntervallEntitet> utledPerioderForGRegulering(Behandling behandling, DatoIntervallEntitet periode) {
        Optional<UngdomsytelseGrunnlag> ungdomsytelseGrunnlag = grunnlagRepository.hentGrunnlag(behandling.getId());

        if (ungdomsytelseGrunnlag.isEmpty()) {
            return new TreeSet<>();
        }

        LocalDateTimeline<Grunnbeløp> overlappendeGrunnbeløpTidslinje = GrunnbeløpTidslinje.hentTidslinje().intersection(periode.toLocalDateInterval());
        LocalDateTimeline<UngdomsytelseSatser> overlappendeSatsTidslinje = ungdomsytelseGrunnlag.get().getSatsTidslinje().intersection(periode.toLocalDateInterval());

        LocalDateTimeline<Boolean> tidslinjeMedDiff = overlappendeSatsTidslinje.combine(overlappendeGrunnbeløpTidslinje,
                (di, lhs, rhs) -> {
                    if (rhs == null) {
                        throw new IllegalStateException("Forventer at grunnbeløptidslinjen eksisterer for hele den overlappende satstidslinjen");
                    }
                    return new LocalDateSegment<>(di, lhs.getValue().grunnbeløp().compareTo(rhs.getValue().verdi()) != 0);
                }, LocalDateTimeline.JoinStyle.LEFT_JOIN)
            .filterValue(it -> it);

        return TidslinjeUtil.tilDatoIntervallEntiteter(tidslinjeMedDiff);
    }
}
