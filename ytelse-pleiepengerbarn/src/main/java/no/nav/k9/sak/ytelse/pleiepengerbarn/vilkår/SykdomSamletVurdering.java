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
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomUtils;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomVurderingVersjon;

public class SykdomSamletVurdering {
    private SykdomVurderingVersjon ktp;
    private SykdomVurderingVersjon toOp;
    private SykdomInnleggelser innleggelser;

    public SykdomSamletVurdering() {
    }

    public SykdomSamletVurdering(SykdomVurderingVersjon ktp, SykdomVurderingVersjon toOp, SykdomInnleggelser innleggelser) {
        this.ktp = ktp;
        this.toOp = toOp;
        this.innleggelser = innleggelser;
    }

    public SykdomSamletVurdering kombinerForSammeTidslinje(SykdomSamletVurdering annet) {
        SykdomSamletVurdering ny = new SykdomSamletVurdering();
        if (this.getKtp() != null && annet.getKtp() != null && !this.getKtp().equals(annet.getKtp())) {
            throw new IllegalStateException("Uventet overlapp mellom vurderinger");
        }

        if (this.getToOp() != null && annet.getToOp() != null && !this.getToOp().equals(annet.getToOp())) {
            throw new IllegalStateException("Uventet overlapp mellom vurderinger");
        }

        if (this.getInnleggelser() != null && annet.getInnleggelser() != null && !this.getInnleggelser().equals(annet.getInnleggelser())) {
            throw new IllegalStateException("Uventet overlapp mellom innleggelser");
        }
        ny.setKtp(this.getKtp() != null ? this.getKtp() : annet.getKtp());
        ny.setToOp(this.getToOp() != null ? this.getToOp() : annet.getToOp());
        ny.setInnleggelser(this.getInnleggelser() != null ? this.getInnleggelser() : annet.getInnleggelser());
        return ny;
    }

    public static LocalDateTimeline<SykdomSamletVurdering> grunnlagTilTidslinje(SykdomGrunnlag grunnlag) {
        final Collection<LocalDateSegment<SykdomSamletVurdering>> segments = new ArrayList<>();

        SykdomUtils.tilTidslinjeForType(grunnlag.getVurderinger(), SykdomVurderingType.KONTINUERLIG_TILSYN_OG_PLEIE).forEach(s -> {
            SykdomSamletVurdering samletVurdering = new SykdomSamletVurdering();
            samletVurdering.setKtp(s.getValue());
            segments.add(new LocalDateSegment<>(s.getFom(), s.getTom(), samletVurdering));
        });

        SykdomUtils.tilTidslinjeForType(grunnlag.getVurderinger(), SykdomVurderingType.TO_OMSORGSPERSONER).forEach(s -> {
            SykdomSamletVurdering samletVurdering = new SykdomSamletVurdering();
            samletVurdering.setToOp(s.getValue());
            segments.add(new LocalDateSegment<>(s.getFom(), s.getTom(), samletVurdering));
        });

        if (grunnlag.getInnleggelser() != null) {
            grunnlag.getInnleggelser().getPerioder().forEach(p -> {
                SykdomSamletVurdering samletVurdering = new SykdomSamletVurdering();
                samletVurdering.setInnleggelser(p.getInnleggelser());
                segments.add(new LocalDateSegment<>(p.getFom(), p.getTom(), samletVurdering));
            });
        }

        final LocalDateTimeline<SykdomSamletVurdering> tidslinje = new LocalDateTimeline<>(segments, new LocalDateSegmentCombinator<SykdomSamletVurdering, SykdomSamletVurdering, SykdomSamletVurdering>() {
            @Override
            public LocalDateSegment<SykdomSamletVurdering> combine(LocalDateInterval localDateInterval, LocalDateSegment<SykdomSamletVurdering> segment1, LocalDateSegment<SykdomSamletVurdering> segment2) {
                SykdomSamletVurdering tuppel = segment1.getValue().kombinerForSammeTidslinje(segment2.getValue());
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
                if (right == null || left == null) {
                    return new LocalDateSegment<>(localDateInterval, true);
                }
                SykdomInnleggelser innleggelser1 = left.getValue().getInnleggelser();
                SykdomInnleggelser innleggelser2 = right.getValue().getInnleggelser();
                if (innleggelser1 == null && innleggelser2 != null || innleggelser1 != null && innleggelser2 == null) {
                    return new LocalDateSegment<>(localDateInterval, true);
                }

                SykdomVurderingVersjon toOp1 = left.getValue().getToOp();
                SykdomVurderingVersjon toOp2 = right.getValue().getToOp();
                if (toOp1 == null && toOp2 != null || toOp1 != null && toOp2 == null) {
                    return new LocalDateSegment<>(localDateInterval, true);
                } else if (toOp1 != null && toOp2 != null) {
                    if (toOp1.getResultat() != toOp2.getResultat()) {
                        return new LocalDateSegment<>(localDateInterval, true);
                    }
                }

                SykdomVurderingVersjon ktp1 = left.getValue().getKtp();
                SykdomVurderingVersjon ktp2 = right.getValue().getKtp();
                if (ktp1 == null && ktp2 != null || ktp1 != null && ktp2 == null) {
                    return new LocalDateSegment<>(localDateInterval, true);
                } else if (ktp1 != null && ktp2 != null) {
                    if (ktp1.getResultat() != ktp2.getResultat()) {
                        return new LocalDateSegment<>(localDateInterval, true);
                    }
                }
                return null;
            }
        }, LocalDateTimeline.JoinStyle.CROSS_JOIN).compress();
    }

    public SykdomVurderingVersjon getKtp() {
        return ktp;
    }

    public void setKtp(SykdomVurderingVersjon ktp) {
        this.ktp = ktp;
    }

    public SykdomVurderingVersjon getToOp() {
        return toOp;
    }

    public void setToOp(SykdomVurderingVersjon toOp) {
        this.toOp = toOp;
    }

    public SykdomInnleggelser getInnleggelser() {
        return innleggelser;
    }

    public void setInnleggelser(SykdomInnleggelser innleggelser) {
        this.innleggelser = innleggelser;
    }
}

