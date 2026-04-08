package no.nav.ung.sak.ytelseperioder;

import jakarta.enterprise.inject.Instance;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;

/**
 * Tjeneste for å definere hvilke perioder som kvalifiserer for ytelse. Dette vil typisk vere perioder der bruker er under aktiv oppfølging.
 * Periodene brukes til å utlede hvilke perioder som vi kjører inntektskontroll for og hvilke perioder som kan kvalifisere for ytelse gitt at andre vilkår er oppfylt.
 * Implementasjoner må annoteres med @FagsakYtelseTypeRef
 */
public interface KvalifiserteYtelsesperioderTjeneste {

    static KvalifiserteYtelsesperioderTjeneste finnTjeneste(
        FagsakYtelseType ytelseType,
        Instance<KvalifiserteYtelsesperioderTjeneste> periodeTjenester) {
        return FagsakYtelseTypeRef.Lookup.find(periodeTjenester, ytelseType)
            .orElseThrow(() -> new UnsupportedOperationException("Har ikke " + KvalifiserteYtelsesperioderTjeneste.class.getSimpleName() + " for ytelseType=" + ytelseType));
    }

    LocalDateTimeline<Boolean> finnPeriodeTidslinje(Long behandlingId);

    LocalDateTimeline<Boolean> finnInitiellPeriodeTidslinje(Long behandlingId);

}
