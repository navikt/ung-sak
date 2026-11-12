package no.nav.ung.ytelse.ungdomsprogramytelsen.perioder;

import jakarta.enterprise.context.ApplicationScoped;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.kontroll.InntektskontrollFritakUtleder;

import java.time.YearMonth;

@ApplicationScoped
@FagsakYtelseTypeRef(FagsakYtelseType.UNGDOMSYTELSE)
public class UngdomsytelseInntektskontrollFritakUtleder implements InntektskontrollFritakUtleder {

    @Override
    public LocalDateTimeline<Boolean> finnPerioderFritattForKontroll(LocalDateTimeline<YearMonth> ytelsesPerioder) {
        var fritatteSegmenter = ytelsesPerioder.stream()
            .filter(it -> ytelsesPerioder.intersection(
                new LocalDateInterval(it.getFom().minusDays(1), it.getFom().minusDays(1))).isEmpty())
            .map(it -> new LocalDateSegment<>(it.getLocalDateInterval(), true))
            .toList();
        return new LocalDateTimeline<>(fritatteSegmenter);
    }
}
