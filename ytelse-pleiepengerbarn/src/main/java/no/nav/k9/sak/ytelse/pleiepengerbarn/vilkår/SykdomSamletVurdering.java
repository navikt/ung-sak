package no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår;

import java.util.ArrayList;
import java.util.Collection;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateSegmentCombinator;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.sak.kontrakt.sykdom.SykdomVurderingType;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.medisinsk.MedisinskGrunnlagsdata;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.pleietrengendesykdom.PleietrengendeSykdomInnleggelser;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.pleietrengendesykdom.PleietrengendeTidslinjeUtils;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.pleietrengendesykdom.PleietrengendeSykdomVurderingVersjon;

public class SykdomSamletVurdering {
    private PleietrengendeSykdomVurderingVersjon ktp;
    private PleietrengendeSykdomVurderingVersjon toOp;
    private PleietrengendeSykdomVurderingVersjon slu;
    private PleietrengendeSykdomInnleggelser innleggelser;

    public SykdomSamletVurdering kombinerForSammeTidslinje(SykdomSamletVurdering annet) {
        SykdomSamletVurdering ny = new SykdomSamletVurdering();
        if (this.getKtp() != null && annet.getKtp() != null && !this.getKtp().equals(annet.getKtp())) {
            throw new IllegalStateException("Uventet overlapp mellom vurderinger");
        }

        if (this.getToOp() != null && annet.getToOp() != null && !this.getToOp().equals(annet.getToOp())) {
            throw new IllegalStateException("Uventet overlapp mellom vurderinger");
        }

        if (this.getSlu() != null && annet.getSlu() != null && !this.getSlu().equals(annet.getSlu())) {
            throw new IllegalStateException("Uventet overlapp mellom vurderinger");
        }

        if (this.getInnleggelser() != null && annet.getInnleggelser() != null && !this.getInnleggelser().equals(annet.getInnleggelser())) {
            throw new IllegalStateException("Uventet overlapp mellom innleggelser");
        }
        ny.setKtp(this.getKtp() != null ? this.getKtp() : annet.getKtp());
        ny.setToOp(this.getToOp() != null ? this.getToOp() : annet.getToOp());
        ny.setSlu(this.getSlu() != null ? this.getSlu() : annet.getSlu());
        ny.setInnleggelser(this.getInnleggelser() != null ? this.getInnleggelser() : annet.getInnleggelser());
        return ny;
    }

    public static LocalDateTimeline<SykdomSamletVurdering> grunnlagTilTidslinje(MedisinskGrunnlagsdata grunnlag) {
        final Collection<LocalDateSegment<SykdomSamletVurdering>> segments = new ArrayList<>();

        PleietrengendeTidslinjeUtils.tilTidslinjeForType(grunnlag.getVurderinger(), SykdomVurderingType.KONTINUERLIG_TILSYN_OG_PLEIE).forEach(s -> {
            SykdomSamletVurdering samletVurdering = new SykdomSamletVurdering();
            samletVurdering.setKtp(s.getValue());
            segments.add(new LocalDateSegment<>(s.getFom(), s.getTom(), samletVurdering));
        });

        PleietrengendeTidslinjeUtils.tilTidslinjeForType(grunnlag.getVurderinger(), SykdomVurderingType.TO_OMSORGSPERSONER).forEach(s -> {
            SykdomSamletVurdering samletVurdering = new SykdomSamletVurdering();
            samletVurdering.setToOp(s.getValue());
            segments.add(new LocalDateSegment<>(s.getFom(), s.getTom(), samletVurdering));
        });

        PleietrengendeTidslinjeUtils.tilTidslinjeForType(grunnlag.getVurderinger(), SykdomVurderingType.LIVETS_SLUTTFASE).forEach(s -> {
            SykdomSamletVurdering samletVurdering = new SykdomSamletVurdering();
            samletVurdering.setSlu(s.getValue());
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
                PleietrengendeSykdomInnleggelser innleggelser1 = left.getValue().getInnleggelser();
                PleietrengendeSykdomInnleggelser innleggelser2 = right.getValue().getInnleggelser();
                if (innleggelser1 == null && innleggelser2 != null || innleggelser1 != null && innleggelser2 == null) {
                    return new LocalDateSegment<>(localDateInterval, true);
                }

                PleietrengendeSykdomVurderingVersjon toOp1 = left.getValue().getToOp();
                PleietrengendeSykdomVurderingVersjon toOp2 = right.getValue().getToOp();
                if (toOp1 == null && toOp2 != null || toOp1 != null && toOp2 == null) {
                    return new LocalDateSegment<>(localDateInterval, true);
                } else if (toOp1 != null && toOp2 != null) {
                    if (toOp1.getResultat() != toOp2.getResultat()) {
                        return new LocalDateSegment<>(localDateInterval, true);
                    }
                }

                PleietrengendeSykdomVurderingVersjon ktp1 = left.getValue().getKtp();
                PleietrengendeSykdomVurderingVersjon ktp2 = right.getValue().getKtp();
                if (ktp1 == null && ktp2 != null || ktp1 != null && ktp2 == null) {
                    return new LocalDateSegment<>(localDateInterval, true);
                } else if (ktp1 != null && ktp2 != null) {
                    if (ktp1.getResultat() != ktp2.getResultat()) {
                        return new LocalDateSegment<>(localDateInterval, true);
                    }
                }

                PleietrengendeSykdomVurderingVersjon slu1 = left.getValue().getSlu();
                PleietrengendeSykdomVurderingVersjon slu2 = right.getValue().getSlu();
                if (slu1 == null && slu2 != null || slu1 != null && slu2 == null) {
                    return new LocalDateSegment<>(localDateInterval, true);
                } else if (slu1 != null && slu2 != null) {
                    if (slu1.getResultat() != slu2.getResultat()) {
                        return new LocalDateSegment<>(localDateInterval, true);
                    }
                }
                return null;
            }
        }, LocalDateTimeline.JoinStyle.CROSS_JOIN).compress();
    }

    public PleietrengendeSykdomVurderingVersjon getKtp() {
        return ktp;
    }

    public void setKtp(PleietrengendeSykdomVurderingVersjon ktp) {
        this.ktp = ktp;
    }

    public PleietrengendeSykdomVurderingVersjon getToOp() {
        return toOp;
    }

    public void setToOp(PleietrengendeSykdomVurderingVersjon toOp) {
        this.toOp = toOp;
    }

    public PleietrengendeSykdomVurderingVersjon getSlu() {
        return slu;
    }

    public void setSlu(PleietrengendeSykdomVurderingVersjon slu) {
        this.slu = slu;
    }

    public PleietrengendeSykdomInnleggelser getInnleggelser() {
        return innleggelser;
    }

    public void setInnleggelser(PleietrengendeSykdomInnleggelser innleggelser) {
        this.innleggelser = innleggelser;
    }
}

