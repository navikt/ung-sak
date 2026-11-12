package no.nav.ung.ytelse.aktivitetspenger.perioder;

import jakarta.enterprise.context.ApplicationScoped;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.kontroll.InntektskontrollFritakUtleder;

import java.time.YearMonth;

@ApplicationScoped
@FagsakYtelseTypeRef(FagsakYtelseType.AKTIVITETSPENGER)
public class AktivitetspengerInntektskontrollFritakUtleder implements InntektskontrollFritakUtleder {

    @Override
    public LocalDateTimeline<Boolean> finnPerioderFritattForKontroll(LocalDateTimeline<YearMonth> ytelsesPerioder) {
        return LocalDateTimeline.empty();
    }
}
