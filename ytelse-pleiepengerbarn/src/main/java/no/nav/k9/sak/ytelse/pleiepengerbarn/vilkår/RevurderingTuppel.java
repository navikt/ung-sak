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

public class RevurderingTuppel {
    public SykdomVurdering ktp;
    public SykdomVurdering toOp;
    public SykdomInnleggelser innleggelser;

    public RevurderingTuppel kombiner(RevurderingTuppel annet) {
        RevurderingTuppel ny = new RevurderingTuppel();
        if (this.ktp != null && annet.ktp != null && !this.ktp.equals(annet.ktp)) {
            throw new IllegalStateException("Uventet overlapp mellom vurderinger");
        }

        if (this.toOp != null && annet.toOp != null && !this.toOp.equals(annet.toOp)) {
            throw new IllegalStateException("Uventet overlapp mellom vurderinger");
        }

        if(this.innleggelser != null && annet.innleggelser != null && !this.innleggelser.equals(annet.innleggelser)) {
            throw new IllegalStateException("Uventet overlapp mellom innleggelser");
        }
        ny.ktp = this.ktp != null ? this.ktp : annet.ktp;
        ny.toOp = this.toOp != null ? this.toOp : annet.toOp;
        ny.innleggelser = this.innleggelser != null ? this.innleggelser : annet.innleggelser;
        return ny;
    }

    public static LocalDateTimeline<RevurderingTuppel> grunnlagTilTidslinje(SykdomGrunnlag forrigeGrunnlag) {
        final Collection<LocalDateSegment<RevurderingTuppel>> segments = new ArrayList<>();

        forrigeGrunnlag.getVurderinger().get(0).getSykdomVurdering().getType();

        forrigeGrunnlag.getVurderinger().forEach(v -> {
            SykdomVurderingType type = v.getSykdomVurdering().getType();
            v.getPerioder().forEach(p -> {
                RevurderingTuppel tuppel = new RevurderingTuppel();
                if (type == SykdomVurderingType.KONTINUERLIG_TILSYN_OG_PLEIE) {
                    tuppel.ktp = v.getSykdomVurdering();
                } else {
                    tuppel.toOp = v.getSykdomVurdering();
                }
                segments.add(new LocalDateSegment<>(p.getFom(), p.getTom(), tuppel));
            });
        });

        forrigeGrunnlag.getInnleggelser().getPerioder().forEach(p -> {
            RevurderingTuppel tuppel = new RevurderingTuppel();
            tuppel.innleggelser = p.getInnleggelser();
            segments.add(new LocalDateSegment<>(p.getFom(), p.getTom(), tuppel));
        });

        final LocalDateTimeline<RevurderingTuppel> tidslinje = new LocalDateTimeline<>(segments, new LocalDateSegmentCombinator<RevurderingTuppel, RevurderingTuppel, RevurderingTuppel>() {
            @Override
            public LocalDateSegment<RevurderingTuppel> combine(LocalDateInterval localDateInterval, LocalDateSegment<RevurderingTuppel> segment1, LocalDateSegment<RevurderingTuppel> segment2) {
                RevurderingTuppel tuppel = segment1.getValue().kombiner(segment2.getValue());
                return new LocalDateSegment<>(localDateInterval, tuppel);
            }
        });

        tidslinje.compress();
        return tidslinje;
    }

    public static LocalDateTimeline<Object> getKombinertTidslinje(LocalDateTimeline<RevurderingTuppel> forrigeTidslinje, LocalDateTimeline<RevurderingTuppel> nyTidslinje) {
        return nyTidslinje.combine(forrigeTidslinje, new LocalDateSegmentCombinator<RevurderingTuppel, RevurderingTuppel, Object>() {
            @Override
            public LocalDateSegment<Object> combine(LocalDateInterval localDateInterval, LocalDateSegment<RevurderingTuppel> left, LocalDateSegment<RevurderingTuppel> right) {
                SykdomInnleggelser innleggelser1 = left.getValue().innleggelser;
                SykdomInnleggelser innleggelser2 = right.getValue().innleggelser;
                if (innleggelser1 == null && innleggelser2 != null || innleggelser1 != null && innleggelser2 == null) {
                    return new LocalDateSegment<>(localDateInterval, left.getValue().kombiner(right.getValue()));
                }

                SykdomVurdering toOp1 = left.getValue().toOp;
                SykdomVurdering toOp2 = right.getValue().toOp;
                if (toOp1 == null && toOp2 != null || toOp1 != null && toOp2 == null) {
                    return new LocalDateSegment<>(localDateInterval, left.getValue().kombiner(right.getValue()));
                } else if (toOp1 != null && toOp2 != null) {
                    if (toOp1.getSisteVersjon().getResultat() != toOp2.getSisteVersjon().getResultat()) {
                        return new LocalDateSegment<>(localDateInterval, left.getValue().kombiner(right.getValue()));
                    }
                }

                SykdomVurdering ktp1 = left.getValue().ktp;
                SykdomVurdering ktp2 = right.getValue().ktp;
                if (ktp1 == null && ktp2 != null || ktp1 != null && ktp2 == null) {
                    return new LocalDateSegment<>(localDateInterval, left.getValue().kombiner(right.getValue()));
                } else if (ktp1 != null && ktp2 != null) {
                    if (ktp1.getSisteVersjon().getResultat() != ktp2.getSisteVersjon().getResultat()) {
                        return new LocalDateSegment<>(localDateInterval, left.getValue().kombiner(right.getValue()));
                    }
                }
                return null;
            }
        }, LocalDateTimeline.JoinStyle.CROSS_JOIN).compress();
    }
}

