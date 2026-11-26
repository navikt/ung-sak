package no.nav.ung.ytelse.ungdomsprogramytelsen.perioder;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.kontroll.InntektskontrollFritakUtleder;

import java.time.YearMonth;

@ApplicationScoped
@FagsakYtelseTypeRef(FagsakYtelseType.UNGDOMSYTELSE)
public class UngdomsytelseInntektskontrollFritakUtleder implements InntektskontrollFritakUtleder {

    private YearMonth kontrollFørsteMndVirkningsdato;

    public UngdomsytelseInntektskontrollFritakUtleder() {
    }

    @Inject
    public UngdomsytelseInntektskontrollFritakUtleder(@KonfigVerdi(value = "KONTROLL_FORSTE_MND_VIRKNIGSDATO", defaultVerdi = "2027-01") String kontrollFørsteMndVirkningsdato) {
        this.kontrollFørsteMndVirkningsdato = YearMonth.parse(kontrollFørsteMndVirkningsdato);
    }


    @Override
    public LocalDateTimeline<Boolean> finnPerioderFritattForKontroll(LocalDateTimeline<YearMonth> ytelsesPerioder) {
        // "Første måneder" før lovendring er fritatt fra kontroll
        var fritatteSegmenter = ytelsesPerioder.stream()
            .filter(it -> erFørsteMåned(ytelsesPerioder, it))
            .filter(it -> it.getValue().isBefore(kontrollFørsteMndVirkningsdato))
            .map(it -> new LocalDateSegment<>(it.getLocalDateInterval(), true))
            .toList();
        return new LocalDateTimeline<>(fritatteSegmenter);
    }

    private static boolean erFørsteMåned(LocalDateTimeline<YearMonth> ytelsesPerioder, LocalDateSegment<YearMonth> it) {
        return ytelsesPerioder.intersection(
            new LocalDateInterval(it.getFom().minusDays(1), it.getFom().minusDays(1))).isEmpty();
    }
}
