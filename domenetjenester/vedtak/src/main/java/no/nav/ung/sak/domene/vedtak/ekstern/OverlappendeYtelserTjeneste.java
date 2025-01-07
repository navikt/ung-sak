package no.nav.ung.sak.domene.vedtak.ekstern;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.ung.sak.domene.iay.modell.Ytelse;
import no.nav.ung.sak.domene.iay.modell.YtelseFilter;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.ung.sak.typer.Saksnummer;

@ApplicationScoped
public class OverlappendeYtelserTjeneste {

    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;
    private Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjenester;

    OverlappendeYtelserTjeneste() {
        // CDI
    }

    @Inject
    public OverlappendeYtelserTjeneste(InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste,
                                       @Any Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjenester) {
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
        this.perioderTilVurderingTjenester = perioderTilVurderingTjenester;
    }

    public Map<Ytelse, LocalDateTimeline<Boolean>> finnOverlappendeYtelser(BehandlingReferanse ref, Set<FagsakYtelseType> ytelseTyperSomSjekkesMot) {
        var perioderTilVurderingTidslinje = hentBeregningsgrunnlagPerioderTilVurderingTidslinje(ref);
        var tilkjentYtelseTidslinje = hentTilkjentYtelseTidslinje(ref);
        tilkjentYtelseTidslinje = tilkjentYtelseTidslinje.intersection(perioderTilVurderingTidslinje);
        if (tilkjentYtelseTidslinje.isEmpty()) {
            return Map.of();
        }

        var aktørYtelse = inntektArbeidYtelseTjeneste.hentGrunnlag(ref.getBehandlingId())
            .getAktørYtelseFraRegister(ref.getAktørId());
        if (aktørYtelse.isEmpty()) {
            return Map.of();
        }

        return doFinnOverlappendeYtelser(ref.getSaksnummer(), tilkjentYtelseTidslinje, new YtelseFilter(aktørYtelse.get()).filter(yt -> ytelseTyperSomSjekkesMot.contains(yt.getYtelseType())));
    }

    private Map<Ytelse, LocalDateTimeline<Boolean>> doFinnOverlappendeYtelser(Saksnummer saksnummer, LocalDateTimeline<Boolean> tilkjentYtelseTimeline, YtelseFilter ytelseFilter) {
        Map<Ytelse, LocalDateTimeline<Boolean>> overlappendeYtelser = new HashMap<>();
        for (var yt : ytelseFilter.getFiltrertYtelser()) {
            if (saksnummer.equals(yt.getSaksnummer())) {
                // Skal ikke sjekke overlappende ytelser i IAY mot egen fagsak
                continue;
            }

            //TODO Det sjekkes uansett mot yt.getAnvistFOM-TOM, virker unødvendig å sjekke mot yt.getPeriode. Fjerne sjekken?
            if (innvilgelseOverlapperMedAnnenYtelse(tilkjentYtelseTimeline, yt.getPeriode())) {
                var anvistSegmenter = yt.getYtelseAnvist().stream()
                    .map(ya -> new LocalDateSegment<>(ya.getAnvistFOM(), ya.getAnvistTOM(), Boolean.TRUE))
                    .toList();
                var anvistTimeline = new LocalDateTimeline<>(anvistSegmenter, StandardCombinators::alwaysTrueForMatch);
                var ovelappendeTidslinje = anvistTimeline.intersection(tilkjentYtelseTimeline);
                if (!ovelappendeTidslinje.isEmpty()) {
                    LocalDateTimeline<Boolean> samletOverlapp = overlappendeYtelser.getOrDefault(yt, LocalDateTimeline.empty());
                    overlappendeYtelser.put(yt, samletOverlapp.crossJoin(ovelappendeTidslinje, StandardCombinators::alwaysTrueForMatch).compress());
                }
            }
        }
        return overlappendeYtelser;
    }

    private static boolean innvilgelseOverlapperMedAnnenYtelse(LocalDateTimeline<Boolean> vilkårPeriode, DatoIntervallEntitet ytp) {
        LocalDateTimeline<?> ytpTidslinje = new LocalDateTimeline<>(ytp.getFomDato(), ytp.getTomDato(), null);
        return vilkårPeriode.intersects(ytpTidslinje);
    }

    private LocalDateTimeline<Boolean> hentBeregningsgrunnlagPerioderTilVurderingTidslinje(BehandlingReferanse ref) {
        var perioderTilVurderingTjeneste = VilkårsPerioderTilVurderingTjeneste.finnTjeneste(perioderTilVurderingTjenester, ref.getFagsakYtelseType(), ref.getBehandlingType());
        var perioderTilVurdering = perioderTilVurderingTjeneste.utled(ref.getBehandlingId(), VilkårType.BEREGNINGSGRUNNLAGVILKÅR);
        var periodeSegmenter = perioderTilVurdering.stream().map(it -> new LocalDateSegment<>(it.getFomDato(), it.getTomDato(), true)).toList();

        return new LocalDateTimeline<>(periodeSegmenter);
    }

    // TODO: Implementer dersom vi skal ta hensyn til overlappende ytelser, ellers kan klassen fjernes
    private LocalDateTimeline<Boolean> hentTilkjentYtelseTidslinje(BehandlingReferanse ref) {
        return LocalDateTimeline.empty();
    }

}
