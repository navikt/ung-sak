package no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår;

import java.util.ArrayList;
import java.util.Collection;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateSegmentCombinator;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.sak.kontrakt.sykdom.SykdomVurderingType;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomGrunnlag;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomInnleggelser;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomVurdering;

public class SykdomSamletVurdering {
    public SykdomVurdering ktp;
    public SykdomVurdering toOp;
    public SykdomInnleggelser innleggelser;

    public SykdomSamletVurdering kombiner(SykdomSamletVurdering annet) {
        SykdomSamletVurdering ny = new SykdomSamletVurdering();
        if (this.ktp != null && annet.ktp != null && !this.ktp.equals(annet.ktp)) {
            throw new IllegalStateException("Uventet overlapp mellom vurderinger");
        }

        if (this.toOp != null && annet.toOp != null && !this.toOp.equals(annet.toOp)) {
            throw new IllegalStateException("Uventet overlapp mellom vurderinger");
        }

        if (this.innleggelser != null && annet.innleggelser != null && !this.innleggelser.equals(annet.innleggelser)) {
            throw new IllegalStateException("Uventet overlapp mellom innleggelser");
        }
        ny.ktp = this.ktp != null ? this.ktp : annet.ktp;
        ny.toOp = this.toOp != null ? this.toOp : annet.toOp;
        ny.innleggelser = this.innleggelser != null ? this.innleggelser : annet.innleggelser;
        return ny;
    }

    public static LocalDateTimeline<SykdomSamletVurdering> grunnlagTilTidslinje(SykdomGrunnlag grunnlag) {
        final Collection<LocalDateSegment<SykdomSamletVurdering>> segments = new ArrayList<>();

        grunnlag.getVurderinger().forEach(v -> {
            SykdomVurderingType type = v.getSykdomVurdering().getType();
            v.getPerioder().forEach(p -> {
                SykdomSamletVurdering tuppel = new SykdomSamletVurdering();
                if (type == SykdomVurderingType.KONTINUERLIG_TILSYN_OG_PLEIE) {
                    tuppel.ktp = v.getSykdomVurdering();
                } else if (type == SykdomVurderingType.TO_OMSORGSPERSONER) {
                    tuppel.toOp = v.getSykdomVurdering();
                } else {
                    throw new IllegalArgumentException("Ukjent vurderingstype");
                }
                segments.add(new LocalDateSegment<>(p.getFom(), p.getTom(), tuppel));
            });
        });

        if (grunnlag.getInnleggelser() != null) {
            grunnlag.getInnleggelser().getPerioder().forEach(p -> {
                SykdomSamletVurdering tuppel = new SykdomSamletVurdering();
                tuppel.innleggelser = p.getInnleggelser();
                segments.add(new LocalDateSegment<>(p.getFom(), p.getTom(), tuppel));
            });
        }

        final LocalDateTimeline<SykdomSamletVurdering> tidslinje = new LocalDateTimeline<>(segments, new LocalDateSegmentCombinator<SykdomSamletVurdering, SykdomSamletVurdering, SykdomSamletVurdering>() {
            @Override
            public LocalDateSegment<SykdomSamletVurdering> combine(LocalDateInterval localDateInterval, LocalDateSegment<SykdomSamletVurdering> segment1, LocalDateSegment<SykdomSamletVurdering> segment2) {
                SykdomSamletVurdering tuppel = segment1.getValue().kombiner(segment2.getValue());
                return new LocalDateSegment<>(localDateInterval, tuppel);
            }
        });

        tidslinje.compress();
        return tidslinje;
    }

    public static LocalDateTimeline<Boolean> finnGrunnlagsforskjeller(LocalDateTimeline<SykdomSamletVurdering> forrigeTidslinje, LocalDateTimeline<SykdomSamletVurdering> nyTidslinje) {
        return nyTidslinje.combine(forrigeTidslinje, new LocalDateSegmentCombinator<SykdomSamletVurdering, SykdomSamletVurdering, Boolean>() {
            @Override
            public LocalDateSegment<Boolean> combine(LocalDateInterval localDateInterval, LocalDateSegment<SykdomSamletVurdering> left, LocalDateSegment<SykdomSamletVurdering> right) {
                if(right == null) {
                    return new LocalDateSegment<>(localDateInterval, true);
                } else if (left == null) {
                    return new LocalDateSegment<>(localDateInterval, true);
                }
                SykdomInnleggelser innleggelser1 = left.getValue().innleggelser;
                SykdomInnleggelser innleggelser2 = right.getValue().innleggelser;
                if (innleggelser1 == null && innleggelser2 != null || innleggelser1 != null && innleggelser2 == null) {
                    return new LocalDateSegment<>(localDateInterval, true);
                }

                SykdomVurdering toOp1 = left.getValue().toOp;
                SykdomVurdering toOp2 = right.getValue().toOp;
                if (toOp1 == null && toOp2 != null || toOp1 != null && toOp2 == null) {
                    return new LocalDateSegment<>(localDateInterval, true);
                } else if (toOp1 != null && toOp2 != null) {
                    if (toOp1.getSisteVersjon().getResultat() != toOp2.getSisteVersjon().getResultat()) {
                        return new LocalDateSegment<>(localDateInterval, true);
                    }
                }

                SykdomVurdering ktp1 = left.getValue().ktp;
                SykdomVurdering ktp2 = right.getValue().ktp;
                if (ktp1 == null && ktp2 != null || ktp1 != null && ktp2 == null) {
                    return new LocalDateSegment<>(localDateInterval, true);
                } else if (ktp1 != null && ktp2 != null) {
                    if (ktp1.getSisteVersjon().getResultat() != ktp2.getSisteVersjon().getResultat()) {
                        return new LocalDateSegment<>(localDateInterval, true);
                    }
                }
                return null;
            }
        }, LocalDateTimeline.JoinStyle.CROSS_JOIN).compress();
    }
}

