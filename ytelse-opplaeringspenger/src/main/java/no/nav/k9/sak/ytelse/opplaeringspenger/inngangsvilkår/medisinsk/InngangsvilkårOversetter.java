package no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.medisinsk;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.medisinsk.regelmodell.LangvarigSykdomDokumentasjon;
import no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.medisinsk.regelmodell.MedisinskVilkårGrunnlag;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.medisinsk.MedisinskGrunnlag;

public class InngangsvilkårOversetter {

    public MedisinskVilkårGrunnlag oversettTilRegelModellMedisinsk(VilkårType vilkåret, Long behandlingId, DatoIntervallEntitet periode, MedisinskGrunnlag sykdomGrunnlagBehandling) {

        final var vilkårsGrunnlag = new MedisinskVilkårGrunnlag(periode.getFomDato(), periode.getTomDato());

        vilkårsGrunnlag.medDokumentertLangvarigSykdomPerioder(new LocalDateTimeline<>(periode.getFomDato(), periode.getTomDato(), LangvarigSykdomDokumentasjon.DOKUMENTERT));


        return vilkårsGrunnlag;
    }

}
