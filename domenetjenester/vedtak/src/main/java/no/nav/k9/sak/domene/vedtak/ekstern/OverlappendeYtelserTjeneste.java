package no.nav.k9.sak.domene.vedtak.ekstern;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Optional;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.iay.modell.AktørYtelse;
import no.nav.k9.sak.domene.iay.modell.YtelseFilter;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

@ApplicationScoped
public class OverlappendeYtelserTjeneste {

    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;
    private Instance<UttakTilOverlappSjekkTjeneste> uttakTilOverlappSjekkTjenester;

    OverlappendeYtelserTjeneste() {
        // CDI
    }

    @Inject
    public OverlappendeYtelserTjeneste(InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste,
                                       @Any Instance<UttakTilOverlappSjekkTjeneste> uttakTilOverlappSjekkTjenester) {
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
        this.uttakTilOverlappSjekkTjenester = uttakTilOverlappSjekkTjenester;
    }

    public Map<FagsakYtelseType, NavigableSet<LocalDateInterval>> finnOverlappendeYtelser(BehandlingReferanse ref) {
        var uttakTilOverlappSjekkTjeneste = getUttakTilOverlappSjekkTjeneste(ref, ref.getFagsakYtelseType());
        if (uttakTilOverlappSjekkTjeneste == null) {
            // Ikke implementert for ytelsetype
            return Map.of();
        }

        var uttakPerioder = uttakTilOverlappSjekkTjeneste.hentInnvilgetUttaksplan(ref);
        var ytelseTyperSomSjekkesMot = uttakTilOverlappSjekkTjeneste.getYtelseTyperSomSjekkesMot();
        var aktørYtelse = inntektArbeidYtelseTjeneste.hentGrunnlag(ref.getBehandlingId())
            .getAktørYtelseFraRegister(ref.getAktørId());

        if (aktørYtelse.isPresent() && !uttakPerioder.isEmpty()) {
            var innvilgetTimeline = new LocalDateTimeline<Boolean>(List.of());
            for (LocalDateSegment<Boolean> periode : uttakPerioder) {
                innvilgetTimeline = innvilgetTimeline.combine(new LocalDateTimeline<>(List.of(periode)), StandardCombinators::coalesceRightHandSide, LocalDateTimeline.JoinStyle.CROSS_JOIN);
            }
            innvilgetTimeline = innvilgetTimeline.compress();

            return doFinnOverlappendeYtelser(innvilgetTimeline, aktørYtelse.get(), ytelseTyperSomSjekkesMot);
        }
        return Map.of();
    }

    Map<FagsakYtelseType, NavigableSet<LocalDateInterval>> doFinnOverlappendeYtelser(LocalDateTimeline<Boolean> vilkårTimeline, AktørYtelse aktørYtelse, List<FagsakYtelseType> ytelseTyperSomSjekkesMot) {
        Map<FagsakYtelseType, NavigableSet<LocalDateInterval>> overlapp = new TreeMap<>();
        if (!vilkårTimeline.isEmpty()) {

            var ytelseFilter = new YtelseFilter(aktørYtelse).filter(yt -> ytelseTyperSomSjekkesMot.contains(yt.getYtelseType()));
            for (var yt : ytelseFilter.getFiltrertYtelser()) {
                var ytp = yt.getPeriode();
                var overlappPeriode = innvilgelseOverlapperMedAnnenYtelse(vilkårTimeline, ytp);
                if (!overlappPeriode.isEmpty()) {
                    if (yt.getYtelseAnvist().isEmpty()) {
                        // er under behandling. flagger hele perioden med overlapp
                        overlapp.put(yt.getYtelseType(), overlappPeriode);
                    } else {
                        var anvistSegmenter = yt.getYtelseAnvist().stream()
                            .map(ya -> new LocalDateSegment<>(ya.getAnvistFOM(), ya.getAnvistTOM(), Boolean.TRUE))
                            .sorted()
                            .collect(Collectors.toCollection(LinkedHashSet::new));

                        var anvistTimeline = new LocalDateTimeline<>(anvistSegmenter, StandardCombinators::alwaysTrueForMatch);
                        var intersection = anvistTimeline.intersection(vilkårTimeline);
                        if (!intersection.isEmpty()) {
                            overlapp.put(yt.getYtelseType(), intersection.getDatoIntervaller());
                        }
                    }
                }
            }
        }
        return overlapp;
    }

    private NavigableSet<LocalDateInterval> innvilgelseOverlapperMedAnnenYtelse(LocalDateTimeline<Boolean> vilkårPeriode, DatoIntervallEntitet ytp) {
        return vilkårPeriode.getDatoIntervaller()
            .stream()
            .map(it -> it.overlap(new LocalDateInterval(ytp.getFomDato(), ytp.getTomDato())))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toCollection(TreeSet::new));
    }

    private UttakTilOverlappSjekkTjeneste getUttakTilOverlappSjekkTjeneste(BehandlingReferanse ref, FagsakYtelseType fagsakYtelseType) {
        return FagsakYtelseTypeRef.Lookup.find(uttakTilOverlappSjekkTjenester, fagsakYtelseType).orElse(null);
    }
}
