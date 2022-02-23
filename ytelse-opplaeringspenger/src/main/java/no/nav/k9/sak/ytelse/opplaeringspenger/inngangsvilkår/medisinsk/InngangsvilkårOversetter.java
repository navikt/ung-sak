package no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.medisinsk;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.kontrakt.sykdom.SykdomVurderingType;
import no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.medisinsk.regelmodell.MedisinskVilkårGrunnlag;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomGrunnlag;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomGrunnlagBehandling;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomUtils;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomVurderingVersjon;

public class InngangsvilkårOversetter {

    public MedisinskVilkårGrunnlag oversettTilRegelModellMedisinsk(VilkårType vilkåret, Long behandlingId, DatoIntervallEntitet periode, SykdomGrunnlagBehandling sykdomGrunnlagBehandling) {

        final var vilkårsGrunnlag = new MedisinskVilkårGrunnlag(periode.getFomDato(), periode.getTomDato());


        return vilkårsGrunnlag;
    }

    private LocalDateTimeline<SykdomVurderingVersjon> toTidslinjeFor(SykdomGrunnlag grunnlag, SykdomVurderingType type) {
        return SykdomUtils.tilTidslinjeForType(grunnlag.getVurderinger(), type);
    }
}
