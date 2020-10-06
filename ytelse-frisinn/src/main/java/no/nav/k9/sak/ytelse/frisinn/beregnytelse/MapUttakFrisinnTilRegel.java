package no.nav.k9.sak.ytelse.frisinn.beregnytelse;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.uttak.UttakArbeidType;
import no.nav.k9.sak.domene.uttak.repo.UttakAktivitetPeriode;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.ytelse.beregning.regelmodell.UttakResultat;
import no.nav.k9.sak.ytelse.beregning.regelmodell.UttakResultatPeriode;
import no.nav.k9.sak.ytelse.beregning.regelmodell.beregningsgrunnlag.Arbeidsforhold;

public final class MapUttakFrisinnTilRegel {

    private MapUttakFrisinnTilRegel() {
        // Skjuler default
    }

    public static UttakResultat map(no.nav.k9.sak.domene.uttak.repo.UttakAktivitet fastsattUttak, FagsakYtelseType fagsakYtelseType) {
        return new UttakResultat(fagsakYtelseType, mapPerioder(fastsattUttak.getPerioder()));
    }

    private static List<UttakResultatPeriode> mapPerioder(Set<UttakAktivitetPeriode> perioder) {
        var segmenter = perioder.stream().map(periode -> new LocalDateSegment<>(periode.getPeriode().getFomDato(), periode.getPeriode().getTomDato(), mapUttakAkvititeter(periode))).collect(Collectors.toList());
        var timeline = LocalDateTimeline.buildGroupOverlappingSegments(segmenter);
        List<UttakResultatPeriode> res = new ArrayList<>();
        timeline.toSegments().forEach(seg -> {
            res.add(new UttakResultatPeriode(seg.getFom(), seg.getTom(), seg.getValue(), false));
        });
        return res;
    }

    private static no.nav.k9.sak.ytelse.beregning.regelmodell.UttakAktivitet mapUttakAkvititeter(UttakAktivitetPeriode periode) {
        return new no.nav.k9.sak.ytelse.beregning.regelmodell.UttakAktivitet(
            BigDecimal.valueOf(100),
            BigDecimal.valueOf(100),
            lagArbeidsforhold(periode),
            periode.getAktivitetType(),
            skalGradere(periode));
    }

    private static Arbeidsforhold lagArbeidsforhold(UttakAktivitetPeriode periode) {
        if (periode.getAktivitetType().equals(UttakArbeidType.FRILANSER)) {
            return Arbeidsforhold.frilansArbeidsforhold();
        }
        Arbeidsgiver arbeidsgiver = periode.getArbeidsgiver();
        if (arbeidsgiver != null) {
            String arbeidsforholdRef = periode.getArbeidsforholdRef() == null ? null : periode.getArbeidsforholdRef().getReferanse();
            if (arbeidsgiver.erAktørId()) {
                return Arbeidsforhold.nyttArbeidsforholdHosPrivatperson(arbeidsgiver.getIdentifikator(), arbeidsforholdRef);
            } else if (arbeidsgiver.getErVirksomhet()) {
                return Arbeidsforhold.nyttArbeidsforholdHosVirksomhet(arbeidsgiver.getIdentifikator(), arbeidsforholdRef);
            } else {
                throw new IllegalStateException("Ukjent arbeidsgivertype for arbeidsgiver " + arbeidsgiver);
            }
        }
        return null;
    }

    private static boolean skalGradere(UttakAktivitetPeriode periode) {
        BigDecimal prosentIJobb = periode.getSkalJobbeProsent();
        return prosentIJobb != null && prosentIJobb.compareTo(BigDecimal.ZERO) > 0;
    }
}
