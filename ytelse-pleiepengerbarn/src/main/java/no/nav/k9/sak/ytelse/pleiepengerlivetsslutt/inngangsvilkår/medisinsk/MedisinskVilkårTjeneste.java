package no.nav.k9.sak.ytelse.pleiepengerlivetsslutt.inngangsvilkår.medisinsk;

import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.inngangsvilkår.VilkårData;
import no.nav.k9.sak.inngangsvilkår.VilkårUtfallOversetter;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomGrunnlagBehandling;
import no.nav.k9.sak.ytelse.pleiepengerlivetsslutt.inngangsvilkår.medisinsk.regelmodell.MedisinskVilkår;
import no.nav.k9.sak.ytelse.pleiepengerlivetsslutt.inngangsvilkår.medisinsk.regelmodell.MedisinskVilkårGrunnlag;
import no.nav.k9.sak.ytelse.pleiepengerlivetsslutt.inngangsvilkår.medisinsk.regelmodell.MedisinskVilkårResultat;

public class MedisinskVilkårTjeneste {

    private VilkårUtfallOversetter utfallOversetter = new VilkårUtfallOversetter();
    private InngangsvilkårOversetter inngangsvilkårOversetter = new InngangsvilkårOversetter();

    public VilkårData vurderPerioder(VilkårType vilkåret, DatoIntervallEntitet periodeTilVurdering, SykdomGrunnlagBehandling sykdomGrunnlagBehandling) {
        MedisinskVilkårGrunnlag grunnlag = inngangsvilkårOversetter.oversettTilRegelModellMedisinsk(periodeTilVurdering, sykdomGrunnlagBehandling);
        MedisinskVilkårResultat vilkårResultat = new MedisinskVilkårResultat();

        final var evaluation = new MedisinskVilkår().evaluer(grunnlag, vilkårResultat);

        final var vilkårData = utfallOversetter.oversett(vilkåret, evaluation, grunnlag, periodeTilVurdering);
        vilkårData.setEkstraVilkårresultat(vilkårResultat);
        return vilkårData;
    }
}
