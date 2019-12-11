package no.nav.folketrygdloven.beregningsgrunnlag.adapter.vltilregelmodell.periodisering;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.Periode;
import no.nav.foreldrepenger.domene.iay.modell.AktivitetsAvtale;

public final class FinnAnsettelsesPeriode {

    private FinnAnsettelsesPeriode() {
        // skjul public constructor
    }

    public static Optional<Periode> finnMinMaksPeriode(Collection<AktivitetsAvtale> ansettelsesPerioder, LocalDate skjæringstidspunkt) {
        List<AktivitetsAvtale> perioderSomSlutterEtterStp = ansettelsesPerioder
            .stream()
            .filter(ap -> !ap.getPeriode().getTomDato().isBefore(skjæringstidspunkt.minusDays(1)))
            .collect(Collectors.toList());
        if (perioderSomSlutterEtterStp.isEmpty()) {
            return Optional.empty();
        }
        LocalDate arbeidsperiodeFom = perioderSomSlutterEtterStp
            .stream()
            .map(a -> a.getPeriode().getFomDato())
            .min(Comparator.naturalOrder()).orElseThrow();

        LocalDate arbeidsperiodeTom = perioderSomSlutterEtterStp
            .stream()
            .map(a -> a.getPeriode().getTomDato())
            .max(Comparator.naturalOrder()).orElseThrow();
        return Optional.of(Periode.of(arbeidsperiodeFom, arbeidsperiodeTom));
    }

}
