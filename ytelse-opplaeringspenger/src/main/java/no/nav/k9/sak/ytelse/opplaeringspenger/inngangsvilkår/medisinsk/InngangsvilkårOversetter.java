package no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.medisinsk;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.kodeverk.sykdom.Resultat;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.kontrakt.sykdom.SykdomVurderingType;
import no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.medisinsk.regelmodell.LangvarigSykdomDokumentasjon;
import no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.medisinsk.regelmodell.MedisinskVilkårGrunnlag;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.medisinsk.MedisinskGrunnlag;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.pleietrengendesykdom.PleietrengendeTidslinjeUtils;

public class InngangsvilkårOversetter {

    public MedisinskVilkårGrunnlag oversettTilRegelModellMedisinsk(DatoIntervallEntitet periode, MedisinskGrunnlag sykdomGrunnlagBehandling) {

        var vilkårTidslinje = new LocalDateTimeline<>(periode.getFomDato(), periode.getTomDato(), LangvarigSykdomDokumentasjon.DOKUMENTERT);

        var tidslinjeLangvarigSykdom = PleietrengendeTidslinjeUtils.tilTidslinjeForType(
            sykdomGrunnlagBehandling.getGrunnlagsdata().getVurderinger(),
            SykdomVurderingType.LANGVARIG_SYKDOM)
            .filterValue(sykdomVurdering -> sykdomVurdering.getResultat() == Resultat.OPPFYLT);

        var relevantTidslinjeLangvarigSykdom = vilkårTidslinje.intersection(tidslinjeLangvarigSykdom);

        return new MedisinskVilkårGrunnlag(periode.getFomDato(), periode.getTomDato(), relevantTidslinjeLangvarigSykdom);
    }

}
