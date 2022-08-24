package no.nav.k9.sak.ytelse.pleiepengerlivetsslutt.inngangsvilkår.medisinsk;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.kontrakt.sykdom.Resultat;
import no.nav.k9.sak.kontrakt.sykdom.SykdomVurderingType;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.medisinsk.MedisinskGrunnlagsdata;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.medisinsk.MedisinskGrunnlag;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.pleietrengendesykdom.PleietrengendeTidslinjeUtils;
import no.nav.k9.sak.ytelse.pleiepengerlivetsslutt.inngangsvilkår.medisinsk.regelmodell.MedisinskVilkårGrunnlag;

public class InngangsvilkårOversetter {

    public MedisinskVilkårGrunnlag oversettTilRegelModellMedisinsk(DatoIntervallEntitet periode, MedisinskGrunnlag medisinskGrunnlag) {
        var grunnlag = medisinskGrunnlag.getGrunnlagsdata();
        final var vilkårsGrunnlag = new MedisinskVilkårGrunnlag(periode.getFomDato(), periode.getTomDato());

        var tidslinjeHarDokumentertLivetsSluttfase = PleietrengendeTidslinjeUtils.tilTidslinjeForType(grunnlag.getVurderinger(), SykdomVurderingType.LIVETS_SLUTTFASE).filterValue(v -> v.getResultat() == Resultat.OPPFYLT).mapValue(v -> (Void) null);
        var tidslinjeInnleggelse = mapRelevanteInnleggelsesperioder(grunnlag);

        vilkårsGrunnlag
            .medDokumentertLivetsSluttfasePerioder(tidslinjeHarDokumentertLivetsSluttfase)
            .medInnleggelsesPerioder(tidslinjeInnleggelse);

        return vilkårsGrunnlag;
    }


    private LocalDateTimeline<Void> mapRelevanteInnleggelsesperioder(MedisinskGrunnlagsdata medisinskGrunnlagsdataBehandling) {
        var innleggelser = medisinskGrunnlagsdataBehandling.getInnleggelser();
        if (innleggelser == null) {
            return LocalDateTimeline.empty();
        }
        return new LocalDateTimeline<>(innleggelser.getPerioder().stream()
            .map(p -> new LocalDateSegment<>(p.getFom(), p.getTom(), (Void) null))
            .toList());
    }


}
