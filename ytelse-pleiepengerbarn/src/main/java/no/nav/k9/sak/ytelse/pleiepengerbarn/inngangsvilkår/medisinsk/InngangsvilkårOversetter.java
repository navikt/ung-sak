package no.nav.k9.sak.ytelse.pleiepengerbarn.inngangsvilkår.medisinsk;

import java.util.List;
import java.util.stream.Collectors;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.kontrakt.sykdom.Resultat;
import no.nav.k9.sak.kontrakt.sykdom.SykdomVurderingType;
import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.inngangsvilkår.medisinsk.regelmodell.DiagnoseKilde;
import no.nav.k9.sak.ytelse.pleiepengerbarn.inngangsvilkår.medisinsk.regelmodell.InnleggelsesPeriode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.inngangsvilkår.medisinsk.regelmodell.MedisinskvilkårGrunnlag;
import no.nav.k9.sak.ytelse.pleiepengerbarn.inngangsvilkår.medisinsk.regelmodell.PeriodeMedKontinuerligTilsyn;
import no.nav.k9.sak.ytelse.pleiepengerbarn.inngangsvilkår.medisinsk.regelmodell.PeriodeMedUtvidetBehov;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomDiagnosekode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomGrunnlag;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomGrunnlagBehandling;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomUtils;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomVurderingVersjon;

public class InngangsvilkårOversetter {

    public MedisinskvilkårGrunnlag oversettTilRegelModellMedisinsk(VilkårType vilkåret, Long behandlingId, DatoIntervallEntitet periode, SykdomGrunnlagBehandling sykdomGrunnlagBehandling) {
        final Periode vilkårsperiode = new Periode(periode.getFomDato(), periode.getTomDato());


        var grunnlag = sykdomGrunnlagBehandling.getGrunnlag();
        final var vilkårsGrunnlag = new MedisinskvilkårGrunnlag(periode.getFomDato(), periode.getTomDato());

        String diagnosekode = null;
        if (grunnlag.getDiagnosekoder() != null) {
            diagnosekode = grunnlag.getDiagnosekoder()
                .getDiagnosekoder()
                .stream()
                .findAny()
                .map(SykdomDiagnosekode::getDiagnosekode)
                .orElse(null);
        }

        List<InnleggelsesPeriode> relevanteInnleggelsesperioder = List.of();
        if (grunnlag.getInnleggelser() != null && vilkåret != VilkårType.MEDISINSKEVILKÅR_18_ÅR) {
            relevanteInnleggelsesperioder = grunnlag.getInnleggelser()
                .getPerioder()
                .stream()
                .map(sip -> new Periode(sip.getFom(), sip.getTom()))
                .filter(p -> p.overlaps(vilkårsperiode))
                .map(p -> new InnleggelsesPeriode(p.getFom(), p.getTom()))
                .collect(Collectors.toList());
        }

        final var tidslinjeKTP = toTidslinjeFor(grunnlag, SykdomVurderingType.KONTINUERLIG_TILSYN_OG_PLEIE)
            .filterValue(v -> v.getResultat() == Resultat.OPPFYLT);

        final var relevantKontinuerligTilsyn = tidslinjeKTP
            .stream()
            .map(v -> new PeriodeMedKontinuerligTilsyn(v.getFom(), v.getTom()))
            .filter(it -> new Periode(it.getFraOgMed(), it.getTilOgMed()).overlaps(vilkårsperiode))
            .collect(Collectors.toList());

        final var relevantUtvidetBehov = toTidslinjeFor(grunnlag, SykdomVurderingType.TO_OMSORGSPERSONER)
            .intersection(tidslinjeKTP)
            .stream()
            .filter(v -> v.getValue().getResultat() == Resultat.OPPFYLT)
            .map(v -> new PeriodeMedUtvidetBehov(v.getFom(), v.getTom()))
            .filter(it -> new Periode(it.getFraOgMed(), it.getTilOgMed()).overlaps(vilkårsperiode))
            .collect(Collectors.toList());

        final DiagnoseKilde diagnoseKilde = grunnlag.getGodkjenteLegeerklæringer().isEmpty() ? DiagnoseKilde.ANNET : DiagnoseKilde.SYKHUSLEGE;
        
        vilkårsGrunnlag.medDiagnoseKilde(diagnoseKilde)
            .medDiagnoseKode(diagnosekode)
            .medInnleggelsesPerioder(relevanteInnleggelsesperioder)
            .medKontinuerligTilsyn(relevantKontinuerligTilsyn)
            .medUtvidetBehov(relevantUtvidetBehov);

        return vilkårsGrunnlag;
    }

    private LocalDateTimeline<SykdomVurderingVersjon> toTidslinjeFor(SykdomGrunnlag grunnlag, SykdomVurderingType type) {
        return SykdomUtils.tilTidslinjeForType(grunnlag.getVurderinger(), type);
    }
}
