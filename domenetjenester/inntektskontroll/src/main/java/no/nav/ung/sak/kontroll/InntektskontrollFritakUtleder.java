package no.nav.ung.sak.kontroll;

import jakarta.enterprise.inject.Instance;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;

import java.time.YearMonth;

public interface InntektskontrollFritakUtleder {

    static InntektskontrollFritakUtleder finnTjeneste(FagsakYtelseType ytelseType,
                                                     Instance<InntektskontrollFritakUtleder> utledere) {
        return FagsakYtelseTypeRef.Lookup.find(utledere, ytelseType)
            .orElseThrow(() -> new UnsupportedOperationException(
                "Har ikke " + InntektskontrollFritakUtleder.class.getSimpleName() + " for ytelseType=" + ytelseType));
    }

    LocalDateTimeline<Boolean> finnPerioderFritattForKontroll(LocalDateTimeline<YearMonth> ytelsesPerioder);
}
