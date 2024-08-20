package no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår;

import java.util.Optional;
import java.util.TreeSet;
import java.util.stream.Collectors;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateSegmentCombinator;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.domene.typer.tid.TidslinjeUtil;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.medisinsk.MedisinskGrunnlagsdata;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.pleietrengendesykdom.PleietrengendeSykdomInnleggelser;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.pleietrengendesykdom.PleietrengendeSykdomVurderingVersjon;

public class SykdomSamletVurderingSammenligner {
    private boolean skalSammenligneInnleggelse;

    public SykdomSamletVurderingSammenligner(boolean skalSammenligneInnleggelse) {
        this.skalSammenligneInnleggelse = skalSammenligneInnleggelse;
    }

    public LocalDateTimeline<Boolean> finnEndringerISøktePerioder(Optional<MedisinskGrunnlagsdata> grunnlagBehandling, MedisinskGrunnlagsdata utledetGrunnlag) {
        LocalDateTimeline<SykdomSamletVurdering> grunnlagBehandlingTidslinje;

        if (grunnlagBehandling.isPresent()) {
            final MedisinskGrunnlagsdata forrigeGrunnlag = grunnlagBehandling.get();
            grunnlagBehandlingTidslinje = SykdomSamletVurdering.grunnlagTilTidslinje(forrigeGrunnlag);
        } else {
            grunnlagBehandlingTidslinje = LocalDateTimeline.empty();
        }
        final LocalDateTimeline<SykdomSamletVurdering> forrigeGrunnlagTidslinje = grunnlagBehandlingTidslinje;

        final LocalDateTimeline<SykdomSamletVurdering> nyBehandlingTidslinje = SykdomSamletVurdering.grunnlagTilTidslinje(utledetGrunnlag);
        final LocalDateTimeline<Boolean> endringerSidenForrigeBehandling = finnGrunnlagsforskjeller(forrigeGrunnlagTidslinje, nyBehandlingTidslinje);

        final LocalDateTimeline<Boolean> søktePerioderTimeline = TidslinjeUtil.tilTidslinjeKomprimert(utledetGrunnlag.getSøktePerioder().stream().map(p -> DatoIntervallEntitet.fraOgMedTilOgMed(p.getFom(), p.getTom())).collect(Collectors.toCollection(TreeSet::new)));
        return endringerSidenForrigeBehandling.intersection(søktePerioderTimeline);
    }

    protected LocalDateTimeline<Boolean> finnGrunnlagsforskjeller(LocalDateTimeline<SykdomSamletVurdering> forrigeTidslinje, LocalDateTimeline<SykdomSamletVurdering> nyTidslinje) {
        return nyTidslinje.combine(forrigeTidslinje, new LocalDateSegmentCombinator<SykdomSamletVurdering, SykdomSamletVurdering, Boolean>() {
            @Override
            public LocalDateSegment<Boolean> combine(LocalDateInterval localDateInterval, LocalDateSegment<SykdomSamletVurdering> left, LocalDateSegment<SykdomSamletVurdering> right) {
                if (left == null || right == null) {
                    if (!skalSammenligneInnleggelse && (harKunEndretInnleggelse(left) || harKunEndretInnleggelse(right))) {
                        return null;
                    }
                    return new LocalDateSegment<Boolean>(localDateInterval, true);
                }

                if (skalSammenligneInnleggelse) {
                    PleietrengendeSykdomInnleggelser innleggelser1 = left.getValue().getInnleggelser();
                    PleietrengendeSykdomInnleggelser innleggelser2 = right.getValue().getInnleggelser();
                    if (innleggelser1 == null && innleggelser2 != null || innleggelser1 != null && innleggelser2 == null) {
                        return new LocalDateSegment<Boolean>(localDateInterval, true);
                    }
                }

                PleietrengendeSykdomVurderingVersjon toOp1 = left.getValue().getToOp();
                PleietrengendeSykdomVurderingVersjon toOp2 = right.getValue().getToOp();
                if (toOp1 == null && toOp2 != null || toOp1 != null && toOp2 == null) {
                    return new LocalDateSegment<Boolean>(localDateInterval, true);
                } else if (toOp1 != null && toOp2 != null) {
                    if (toOp1.getResultat() != toOp2.getResultat()) {
                        return new LocalDateSegment<Boolean>(localDateInterval, true);
                    }
                }

                PleietrengendeSykdomVurderingVersjon ktp1 = left.getValue().getKtp();
                PleietrengendeSykdomVurderingVersjon ktp2 = right.getValue().getKtp();
                if (ktp1 == null && ktp2 != null || ktp1 != null && ktp2 == null) {
                    return new LocalDateSegment<Boolean>(localDateInterval, true);
                } else if (ktp1 != null && ktp2 != null) {
                    if (ktp1.getResultat() != ktp2.getResultat()) {
                        return new LocalDateSegment<Boolean>(localDateInterval, true);
                    }
                }

                PleietrengendeSykdomVurderingVersjon slu1 = left.getValue().getSlu();
                PleietrengendeSykdomVurderingVersjon slu2 = right.getValue().getSlu();
                if (slu1 == null && slu2 != null || slu1 != null && slu2 == null) {
                    return new LocalDateSegment<Boolean>(localDateInterval, true);
                } else if (slu1 != null && slu2 != null) {
                    if (slu1.getResultat() != slu2.getResultat()) {
                        return new LocalDateSegment<Boolean>(localDateInterval, true);
                    }
                }
                return null;
            }
        }, LocalDateTimeline.JoinStyle.CROSS_JOIN).compress();
    }

    private static boolean harKunEndretInnleggelse(LocalDateSegment<SykdomSamletVurdering> sykdomVurdering) {
        if (sykdomVurdering != null) {
            PleietrengendeSykdomInnleggelser innleggelser2 = sykdomVurdering.getValue().getInnleggelser();
            PleietrengendeSykdomVurderingVersjon toOp2 = sykdomVurdering.getValue().getToOp();
            PleietrengendeSykdomVurderingVersjon ktp2 = sykdomVurdering.getValue().getKtp();
            PleietrengendeSykdomVurderingVersjon slu2 = sykdomVurdering.getValue().getSlu();

            return innleggelser2 != null && toOp2 == null && ktp2 == null && slu2 == null;
        }
        return false;
    }
}
