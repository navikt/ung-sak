package no.nav.k9.sak.ytelse.pleiepengerbarn.revurdering;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.kodeverk.arbeidsforhold.ArbeidType;
import no.nav.k9.sak.domene.arbeidsforhold.AktivPeriodeForArbeidUtleder;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.domene.iay.modell.YtelseFilter;
import no.nav.k9.sak.domene.opptjening.aksjonspunkt.MapYtelsesstidslinjerForPermisjonvalidering;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.registerendringer.Endringstype;
import no.nav.k9.sak.typer.AktørId;

public class UtledAktivitetsperiodeEndring {

    private final MapYtelsesstidslinjerForPermisjonvalidering mapYtelsesstidslinjerForPermisjonvalidering = new MapYtelsesstidslinjerForPermisjonvalidering();

    public List<AktivitetsperiodeEndring> utledEndring(InntektArbeidYtelseGrunnlag iayGrunnlag,
                                                       InntektArbeidYtelseGrunnlag originaltIayGrunnlag,
                                                       Collection<DatoIntervallEntitet> vilkårsperioder,
                                                       AktørId søkerAktørId) {
        var aktivitetsperiodePrIdentifikator = finnAktivitetsperioder(iayGrunnlag, vilkårsperioder, søkerAktørId);
        var originalAktivitetsperiodePrIdentifikator = finnAktivitetsperioder(originaltIayGrunnlag, vilkårsperioder, søkerAktørId);
        var endringer = finnEndretArbeidsforhold(aktivitetsperiodePrIdentifikator, originalAktivitetsperiodePrIdentifikator);
        endringer.addAll(finnFjernetArbeidsforhold(originalAktivitetsperiodePrIdentifikator, aktivitetsperiodePrIdentifikator));
        return endringer.stream().filter(a -> !a.endringstidslinje().isEmpty()).toList();
    }

    private static List<AktivitetsperiodeEndring> finnEndretArbeidsforhold(Set<Aktivitetsperiode> aktivitetsperiodePrIdentifikator, Set<Aktivitetsperiode> originalAktivitetsperiodePrIdentifikator) {
        return aktivitetsperiodePrIdentifikator.stream().map(aktivitetsperiode -> {
            var originalAktivitetsperiode = originalAktivitetsperiodePrIdentifikator.stream().filter(it -> it.identifikator().equals(aktivitetsperiode.identifikator())).findFirst();
            var endringstidslinje = finnEndringstidslinje(aktivitetsperiode, originalAktivitetsperiode);
            return new AktivitetsperiodeEndring(aktivitetsperiode.identifikator(), endringstidslinje);
        }).collect(Collectors.toCollection(ArrayList::new));
    }

    private static List<AktivitetsperiodeEndring> finnFjernetArbeidsforhold(Set<Aktivitetsperiode> originalAktivitetsperiodePrIdentifikator, Set<Aktivitetsperiode> aktivitetsperiodePrIdentifikator) {
        return originalAktivitetsperiodePrIdentifikator.stream()
            .filter(aktivitetsperiode -> aktivitetsperiodePrIdentifikator.stream().noneMatch(it -> it.identifikator().equals(aktivitetsperiode.identifikator())))
            .map(aktivitetsperiode -> new AktivitetsperiodeEndring(aktivitetsperiode.identifikator(), aktivitetsperiode.aktivitettidslinje().mapValue(it -> Endringstype.FJERNET_PERIODE)))
            .collect(Collectors.toCollection(ArrayList::new));
    }

    private static LocalDateTimeline<Endringstype> finnEndringstidslinje(Aktivitetsperiode aktivitetsperiode, Optional<Aktivitetsperiode> originalAktivitetsperiode) {
        return aktivitetsperiode.aktivitettidslinje().combine(originalAktivitetsperiode.map(Aktivitetsperiode::aktivitettidslinje).orElse(LocalDateTimeline.empty()),
            UtledAktivitetsperiodeEndring::finnEndringer,
            LocalDateTimeline.JoinStyle.CROSS_JOIN).filterValue(Objects::nonNull);
    }


    private static LocalDateSegment<Endringstype> finnEndringer(LocalDateInterval di, LocalDateSegment<Boolean> gjeldende, LocalDateSegment<Boolean> original) {
        if (gjeldende != null && original != null) {
            return null; // Ingen endring, filtreres bort utenfor
        }
        if (gjeldende != null) {
            return new LocalDateSegment<>(di, Endringstype.NY_PERIODE);
        }
        return new LocalDateSegment<>(di, Endringstype.FJERNET_PERIODE);
    }

    private Set<Aktivitetsperiode> finnAktivitetsperioder(InntektArbeidYtelseGrunnlag inntektArbeidYtelseGrunnlag, Collection<DatoIntervallEntitet> vilkårsperioder, AktørId aktørId) {
        var aktørArbeidFraRegister = inntektArbeidYtelseGrunnlag.getAktørArbeidFraRegister(aktørId);
        var tidslinjePrYtelse = mapYtelsesstidslinjerForPermisjonvalidering.utledYtelsesTidslinjerForValideringAvPermisjoner(new YtelseFilter(inntektArbeidYtelseGrunnlag.getAktørYtelseFraRegister(aktørId)));
        return aktørArbeidFraRegister.stream().flatMap(a -> a.hentAlleYrkesaktiviteter().stream())
            .filter(a -> a.getArbeidType().equals(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD) || a.getArbeidType().equals(ArbeidType.FRILANSER_OPPDRAGSTAKER_MED_MER))
            .map(ya ->
                new Aktivitetsperiode(new AktivitetsIdentifikator(ya.getArbeidsgiver(), ya.getArbeidsforholdRef(), ya.getArbeidType()),
                    AktivPeriodeForArbeidUtleder.utledAktivTidslinje(ya, inntektArbeidYtelseGrunnlag, vilkårsperioder, tidslinjePrYtelse).mapValue(it -> true))
            ).collect(Collectors.toSet());
    }


    private record Aktivitetsperiode(AktivitetsIdentifikator identifikator,
                                     LocalDateTimeline<Boolean> aktivitettidslinje) {
    }


}
