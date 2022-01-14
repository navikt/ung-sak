package no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår.forlengelse.beregning;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import no.nav.k9.sak.domene.iay.modell.AktørArbeid;
import no.nav.k9.sak.domene.iay.modell.Yrkesaktivitet;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

class AktørArbeidEndringVurderer {

    static boolean harEndringIArbeid(Optional<AktørArbeid> aktørArbeid, Optional<AktørArbeid> originalAktørArbeid, DatoIntervallEntitet periode) {
        if (aktørArbeid.isEmpty() || originalAktørArbeid.isEmpty()) {
            return aktørArbeid.isEmpty() == originalAktørArbeid.isEmpty();
        }
        var arbeid = aktørArbeid.get();
        var originalArbeid = originalAktørArbeid.get();

        var skjæringstidspunkt = periode.getFomDato();

        var relevanteAktiviteter = finnRelevanteAktiviteter(arbeid, skjæringstidspunkt);
        var originalRelevanteAktiviteter = finnRelevanteAktiviteter(originalArbeid, skjæringstidspunkt);

        if (relevanteAktiviteter.size() != originalRelevanteAktiviteter.size()) {
            return true;
        }

        for (Yrkesaktivitet aktivitet : relevanteAktiviteter) {
            var harIngenMatchIOriginal = originalRelevanteAktiviteter.stream().noneMatch(a -> a.gjelderFor(aktivitet.getArbeidsgiver(), aktivitet.getArbeidsforholdRef()));
            if (harIngenMatchIOriginal) {
                return true;
            }
        }
        return false;
    }


    private static List<Yrkesaktivitet> finnRelevanteAktiviteter(AktørArbeid arbeid, LocalDate skjæringstidspunkt) {
        return arbeid.hentAlleYrkesaktiviteter().stream()
            .filter(ya -> ya.getAnsettelsesPeriode().stream().anyMatch(ap -> ap.erAnsettelsesPeriode() && ap.getPeriode().inkluderer(skjæringstidspunkt.minusDays(1))))
            .toList();
    }


}
