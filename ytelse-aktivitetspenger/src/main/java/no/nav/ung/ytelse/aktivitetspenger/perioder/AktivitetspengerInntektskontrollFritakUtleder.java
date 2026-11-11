package no.nav.ung.ytelse.aktivitetspenger.perioder;

import jakarta.enterprise.context.ApplicationScoped;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.kontroll.InntektskontrollFritakUtleder;

import java.time.YearMonth;

@ApplicationScoped
@FagsakYtelseTypeRef(FagsakYtelseType.AKTIVITETSPENGER)
public class AktivitetspengerInntektskontrollFritakUtleder implements InntektskontrollFritakUtleder {

    public static final YearMonth AVP_VIRKNINGSDATO_LOVENDRING_KONTROLL_FØRSTE_MÅNED = YearMonth.of(2027, 1);

    @Override
    public LocalDateTimeline<Boolean> finnPerioderFritattForKontroll(LocalDateTimeline<YearMonth> ytelsesPerioder) {
        // "Første måneder" før lovendring er fritatt fra kontroll
        var fritatteSegmenter = ytelsesPerioder.stream()
            .filter(it -> erFørsteMåned(ytelsesPerioder, it))
            .filter(it -> it.getValue().isBefore(AVP_VIRKNINGSDATO_LOVENDRING_KONTROLL_FØRSTE_MÅNED))
            .map(it -> new LocalDateSegment<>(it.getLocalDateInterval(), true))
            .toList();
        return new LocalDateTimeline<>(fritatteSegmenter);
    }

    private static boolean erFørsteMåned(LocalDateTimeline<YearMonth> ytelsesPerioder, LocalDateSegment<YearMonth> it) {
        return ytelsesPerioder.intersection(
            new LocalDateInterval(it.getFom().minusDays(1), it.getFom().minusDays(1))).isEmpty();
    }
}
