package no.nav.k9.sak.ytelse.pleiepengerlivetsslutt.inngangsvilkår.medisinsk;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.kontrakt.sykdom.Resultat;
import no.nav.k9.sak.kontrakt.sykdom.SykdomVurderingType;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomGrunnlag;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomGrunnlagBehandling;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomUtils;
import no.nav.k9.sak.ytelse.pleiepengerlivetsslutt.inngangsvilkår.medisinsk.regelmodell.MedisinskVilkårGrunnlag;

public class InngangsvilkårOversetter {

    public MedisinskVilkårGrunnlag oversettTilRegelModellMedisinsk(DatoIntervallEntitet periode, SykdomGrunnlagBehandling sykdomGrunnlagBehandling) {
        var grunnlag = sykdomGrunnlagBehandling.getGrunnlag();
        final var vilkårsGrunnlag = new MedisinskVilkårGrunnlag(periode.getFomDato(), periode.getTomDato());

        var tidslinjeHarDokumentertLivetsSluttfase = SykdomUtils.tilTidslinjeForType(grunnlag.getVurderinger(), SykdomVurderingType.LIVETS_SLUTTFASE).filterValue(v -> v.getResultat() == Resultat.OPPFYLT).mapValue(v -> (Void) null);
        var tidslinjeInnleggelse = mapRelevanteInnleggelsesperioder(grunnlag);

        vilkårsGrunnlag
            .medDokumentertLivetsSluttfasePerioder(tidslinjeHarDokumentertLivetsSluttfase)
            .medInnleggelsesPerioder(tidslinjeInnleggelse);

        return vilkårsGrunnlag;
    }


    private LocalDateTimeline<Void> mapRelevanteInnleggelsesperioder(SykdomGrunnlag sykdomGrunnlagBehandling) {
        var innleggelser = sykdomGrunnlagBehandling.getInnleggelser();
        if (innleggelser == null) {
            return LocalDateTimeline.empty();
        }
        return new LocalDateTimeline<>(innleggelser.getPerioder().stream()
            .map(p -> new LocalDateSegment<>(p.getFom(), p.getTom(), (Void) null))
            .toList());
    }


}
