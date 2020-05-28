package no.nav.k9.sak.ytelse.omsorgspenger.inntektsmelding;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.kodeverk.uttak.UttakArbeidType;
import no.nav.k9.sak.domene.iay.modell.Inntektsmelding;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OppgittFraværPeriode;

public class InntektsmeldingFravær {

    public List<OppgittFraværPeriode> trekkUtAlleFraværOgValiderOverlapp(Set<Inntektsmelding> inntektsmeldinger) {
        var aktivitetType = UttakArbeidType.ARBEIDSTAKER;
        List<OppgittFraværPeriode> alle = new ArrayList<>();
        Map<Object, List<OppgittFraværPeriode>> mapByAktivitet = new LinkedHashMap<>();
        for (var im : inntektsmeldinger) {
            var arbeidsgiver = im.getArbeidsgiver();
            var arbeidsforholdRef = im.getArbeidsforholdRef();
            var dummyGruppe = Arrays.asList(aktivitetType, arbeidsgiver, arbeidsforholdRef);

            var liste = im.getOppgittFravær().stream()
                .map(pa -> new OppgittFraværPeriode(pa.getFom(), pa.getTom(), aktivitetType, arbeidsgiver, arbeidsforholdRef, pa.getVarighetPerDag()))
                .collect(Collectors.toList());
            mapByAktivitet.computeIfAbsent(dummyGruppe, k -> new ArrayList<>()).addAll(liste);
            alle.addAll(liste);
        }

        // sjekker mot overlappende data - foreløpig krasj and burn hvis overlappende segmenter
        for (var entry : mapByAktivitet.entrySet()) {
            var segments = entry.getValue().stream().map(ofp -> new LocalDateSegment<>(ofp.getFom(), ofp.getTom(), ofp)).collect(Collectors.toList());
            new LocalDateTimeline<>(segments);
        }
        return alle;
    }

}
