package no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.input.arbeid;

import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.kodeverk.uttak.UttakArbeidType;
import no.nav.k9.kodeverk.vilkår.VilkårUtfallMerknad;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.domene.iay.modell.Inntektsmelding;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.perioder.KravDokument;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;
import no.nav.k9.sak.ytelse.pleiepengerbarn.kompletthetssjekk.KompletthetForBeregningTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.ArbeidPeriode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.PerioderFraSøknad;
import no.nav.pleiepengerbarn.uttak.kontrakter.Arbeid;
import no.nav.pleiepengerbarn.uttak.kontrakter.Arbeidsforhold;
import no.nav.pleiepengerbarn.uttak.kontrakter.ArbeidsforholdPeriodeInfo;
import no.nav.pleiepengerbarn.uttak.kontrakter.LukketPeriode;

public class MapArbeid {

    private KompletthetForBeregningTjeneste kompletthetForBeregningTjeneste;

    public MapArbeid(KompletthetForBeregningTjeneste kompletthetForBeregningTjeneste) {
        this.kompletthetForBeregningTjeneste = kompletthetForBeregningTjeneste;
    }

    public List<Arbeid> map(Set<KravDokument> kravDokumenter,
                            Set<PerioderFraSøknad> perioderFraSøknader,
                            LocalDateTimeline<Boolean> tidslinjeTilVurdering,
                            Set<Inntektsmelding> sakInntektsmeldinger,
                            Vilkår vilkår) {

        final Map<DatoIntervallEntitet, Map<AktivitetIdentifikator, LocalDateTimeline<WrappedArbeid>>> arbeidsforholdPerPeriode = new HashMap<>();

        var perioderTilVurdering = tidslinjeTilVurdering.toSegments()
            .stream()
            .filter(it -> Objects.nonNull(it.getValue()))
            .map(it -> DatoIntervallEntitet.fra(it.getFom(), it.getTom()))
            .collect(Collectors.toSet());

        for (DatoIntervallEntitet periode : perioderTilVurdering) {
            var identifikatorerFraInntektsmelding = utledRelevanteKeys(periode, sakInntektsmeldinger);
            final Map<AktivitetIdentifikator, LocalDateTimeline<WrappedArbeid>> arbeidsforhold = new HashMap<>();

            var vilkårPeriode = vilkår.finnPeriodeForSkjæringstidspunkt(periode.getFomDato());
            if (erInnaktivVedPerioden(List.of(vilkårPeriode))) {
                arbeidsforhold.put(new AktivitetIdentifikator(UttakArbeidType.INAKTIV, null, null),
                    new LocalDateTimeline<>(List.of(new LocalDateSegment<>(periode.getFomDato(), periode.getTomDato(), new WrappedArbeid(new ArbeidPeriode(periode, UttakArbeidType.INAKTIV, null, null, Duration.ofMinutes((long) (7.5 * 60)), Duration.ZERO))))));
            } else {
                kravDokumenter.stream()
                    .sorted(KravDokument::compareTo)
                    .forEachOrdered(at -> {
                        var dokumenter = perioderFraSøknader.stream()
                            .filter(it -> it.getJournalpostId().equals(at.getJournalpostId()))
                            .collect(Collectors.toSet());
                        if (dokumenter.size() == 1) {
                            dokumenter.stream()
                                .map(PerioderFraSøknad::getArbeidPerioder)
                                .flatMap(Collection::stream)
                                .forEach(p -> {
                                    var keys = utledRelevanteKeys(p, identifikatorerFraInntektsmelding);
                                    for (AktivitetIdentifikator key : keys) {
                                        var perioder = arbeidsforhold.getOrDefault(key, new LocalDateTimeline<>(List.of()));
                                        var timeline = new LocalDateTimeline<>(List.of(new LocalDateSegment<>(p.getPeriode().getFomDato(), p.getPeriode().getTomDato(), new WrappedArbeid(p))));
                                        perioder = perioder.combine(timeline, StandardCombinators::coalesceRightHandSide, LocalDateTimeline.JoinStyle.CROSS_JOIN);
                                        arbeidsforhold.put(key, perioder.intersection(periode.toLocalDateInterval()));
                                    }
                                });
                        } else {
                            throw new IllegalStateException("Fant " + dokumenter.size() + " for dokumentet : " + at);
                        }
                    });
            }
            arbeidsforholdPerPeriode.put(periode, arbeidsforhold);
        }

        var arbeidsforhold = new HashMap<AktivitetIdentifikator, LocalDateTimeline<WrappedArbeid>>();

        for (Map<AktivitetIdentifikator, LocalDateTimeline<WrappedArbeid>> aktiviteterIPeriode : arbeidsforholdPerPeriode.values()) {
            for (AktivitetIdentifikator aktivitetIdentifikator : aktiviteterIPeriode.keySet()) {
                var perioder = arbeidsforhold.getOrDefault(aktivitetIdentifikator, new LocalDateTimeline<>(List.of()));
                var arbeidIPerioden = aktiviteterIPeriode.get(aktivitetIdentifikator);
                perioder = perioder.combine(arbeidIPerioden, StandardCombinators::coalesceRightHandSide, LocalDateTimeline.JoinStyle.CROSS_JOIN);
                arbeidsforhold.put(aktivitetIdentifikator, perioder);
            }
        }

        return arbeidsforhold.entrySet()
            .stream()
            .map(arbeidPeriodes -> {
                var perioder = new HashMap<LukketPeriode, ArbeidsforholdPeriodeInfo>();
                arbeidPeriodes.getValue()
                    .compress()
                    .toSegments()
                    .forEach(p -> {
                        var periode = p.getValue().getPeriode();
                        var antallLinjerPerArbeidsgiver = arbeidsforhold.keySet().stream().filter(it -> Objects.equals(periode.getAktivitetType(), it.getAktivitetType()) && periode.getArbeidsgiver() != null && Objects.equals(it.getArbeidsgiver(), periode.getArbeidsgiver())).count();
                        var jobberNormalt = justerIHenholdTilAntallet(antallLinjerPerArbeidsgiver, Optional.ofNullable(periode.getJobberNormaltTimerPerDag()).orElse(justerIHenholdTilAntallet(antallLinjerPerArbeidsgiver, Duration.ZERO)));
                        var jobberFaktisk = justerIHenholdTilAntallet(antallLinjerPerArbeidsgiver, Optional.ofNullable(periode.getFaktiskArbeidTimerPerDag()).orElse(justerIHenholdTilAntallet(antallLinjerPerArbeidsgiver, Duration.ZERO)));
                        perioder.put(new LukketPeriode(p.getFom(), p.getTom()),
                            new ArbeidsforholdPeriodeInfo(jobberNormalt, jobberFaktisk));
                    });

                return new Arbeid(mapArbeidsforhold(arbeidPeriodes.getKey()), perioder);
            })
            .collect(Collectors.toList());
    }

    private Set<AktivitetIdentifikator> utledRelevanteKeys(ArbeidPeriode p, Set<AktivitetIdentifikator> identifikatorerFraInntektsmelding) {
        var relevanteKeys = identifikatorerFraInntektsmelding.stream()
            .filter(it -> Objects.equals(p.getArbeidsgiver(), it.getArbeidsgiver()))
            .map(AktivitetIdentifikator::getArbeidsforhold)
            .collect(Collectors.toSet());

        if (relevanteKeys.isEmpty()) {
            var key = new AktivitetIdentifikator(p.getAktivitetType(), p.getArbeidsgiver(), p.getArbeidsforholdRef());
            return Set.of(key);
        } else {
            return relevanteKeys.stream()
                .map(it -> new AktivitetIdentifikator(p.getAktivitetType(), p.getArbeidsgiver(), it))
                .collect(Collectors.toSet());
        }
    }

    private Set<AktivitetIdentifikator> utledRelevanteKeys(DatoIntervallEntitet periode, Set<Inntektsmelding> sakInntektsmeldinger) {
        return kompletthetForBeregningTjeneste.utledRelevanteInntektsmeldingerForPeriode(sakInntektsmeldinger, periode)
            .stream()
            .map(it -> new AktivitetIdentifikator(UttakArbeidType.ARBEIDSTAKER, it.getArbeidsgiver(), it.getArbeidsforholdRef()))
            .collect(Collectors.toSet());
    }

    private boolean erInnaktivVedPerioden(List<VilkårPeriode> perioderTilVurdering) {
        return perioderTilVurdering.stream().anyMatch(it -> Objects.equals(VilkårUtfallMerknad.VM_7847_A, it.getMerknad()));
    }

    private Duration justerIHenholdTilAntallet(long antallLinjerPerArbeidsgiver, Duration duration) {
        if (Duration.ZERO.equals(duration) || antallLinjerPerArbeidsgiver == 0 || antallLinjerPerArbeidsgiver == 1) {
            return duration;
        }
        return duration.dividedBy(antallLinjerPerArbeidsgiver);
    }

    private Arbeidsforhold mapArbeidsforhold(AktivitetIdentifikator identifikator) {
        return new Arbeidsforhold(identifikator.getAktivitetType().getKode(),
            Optional.ofNullable(identifikator.getArbeidsgiver()).map(Arbeidsgiver::getArbeidsgiverOrgnr).orElse(null),
            Optional.ofNullable(identifikator.getArbeidsgiver()).map(Arbeidsgiver::getArbeidsgiverAktørId).map(AktørId::getId).orElse(null),
            Optional.ofNullable(identifikator.getArbeidsforhold()).map(InternArbeidsforholdRef::getReferanse).orElse(null)
        );
    }
}
