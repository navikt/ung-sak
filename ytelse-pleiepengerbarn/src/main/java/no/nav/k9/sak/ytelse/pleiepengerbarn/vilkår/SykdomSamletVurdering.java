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
    private SykdomVurdering ktp;
    private SykdomVurdering toOp;
    private SykdomInnleggelser innleggelser;

    public SykdomSamletVurdering() {
    }

    public SykdomSamletVurdering(SykdomVurdering ktp, SykdomVurdering toOp, SykdomInnleggelser innleggelser) {
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

        grunnlag.getVurderinger().forEach(v -> {
            SykdomVurderingType type = v.getSykdomVurdering().getType();
            v.getPerioder().forEach(p -> {
                SykdomSamletVurdering samletVurdering = new SykdomSamletVurdering();
                if (type == SykdomVurderingType.KONTINUERLIG_TILSYN_OG_PLEIE) {
                    samletVurdering.setKtp(v.getSykdomVurdering());
                } else if (type == SykdomVurderingType.TO_OMSORGSPERSONER) {
                    samletVurdering.setToOp(v.getSykdomVurdering());
                } else {
                    throw new IllegalArgumentException("Ukjent vurderingstype");
                }
                segments.add(new LocalDateSegment<>(p.getFom(), p.getTom(), samletVurdering));
            });
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

                SykdomVurdering toOp1 = left.getValue().getToOp();
                SykdomVurdering toOp2 = right.getValue().getToOp();
                if (toOp1 == null && toOp2 != null || toOp1 != null && toOp2 == null) {
                    return new LocalDateSegment<>(localDateInterval, true);
                } else if (toOp1 != null && toOp2 != null) {
                    if (toOp1.getSisteVersjon().getResultat() != toOp2.getSisteVersjon().getResultat()) {
                        return new LocalDateSegment<>(localDateInterval, true);
                    }
                }

                SykdomVurdering ktp1 = left.getValue().getKtp();
                SykdomVurdering ktp2 = right.getValue().getKtp();
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

    public SykdomVurdering getKtp() {
        return ktp;
    }

    public void setKtp(SykdomVurdering ktp) {
        this.ktp = ktp;
    }

    public SykdomVurdering getToOp() {
        return toOp;
    }

    public void setToOp(SykdomVurdering toOp) {
        this.toOp = toOp;
    }

    public SykdomInnleggelser getInnleggelser() {
        return innleggelser;
    }

    public void setInnleggelser(SykdomInnleggelser innleggelser) {
        this.innleggelser = innleggelser;
    }
}

