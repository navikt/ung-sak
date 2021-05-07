package no.nav.k9.sak.ytelse.pleiepengerbarn.inngangsvilkår.medisinsk;

import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.inngangsvilkår.VilkårData;
import no.nav.k9.sak.inngangsvilkår.VilkårUtfallOversetter;
import no.nav.k9.sak.ytelse.pleiepengerbarn.inngangsvilkår.medisinsk.regelmodell.MedisinskVilkårResultat;
import no.nav.k9.sak.ytelse.pleiepengerbarn.inngangsvilkår.medisinsk.regelmodell.Medisinskvilkår;
import no.nav.k9.sak.ytelse.pleiepengerbarn.inngangsvilkår.medisinsk.regelmodell.MedisinskvilkårGrunnlag;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomGrunnlagBehandling;

public class MedisinskVilkårTjeneste {

    private InngangsvilkårOversetter inngangsvilkårOversetter = new InngangsvilkårOversetter();
    private VilkårUtfallOversetter utfallOversetter = new VilkårUtfallOversetter();

    MedisinskVilkårTjeneste() {

    }

    public VilkårData vurderPerioder(VilkårType vilkåret, BehandlingskontrollKontekst kontekst, DatoIntervallEntitet periodeTilVurdering, SykdomGrunnlagBehandling sykdomGrunnlagBehandling) {
        MedisinskvilkårGrunnlag grunnlag = inngangsvilkårOversetter.oversettTilRegelModellMedisinsk(vilkåret, kontekst.getBehandlingId(), periodeTilVurdering, sykdomGrunnlagBehandling);
        MedisinskVilkårResultat resultat = new MedisinskVilkårResultat();

        final var evaluation = new Medisinskvilkår().evaluer(grunnlag, resultat);

        final var vilkårData = utfallOversetter.oversett(vilkåret, evaluation, grunnlag, periodeTilVurdering);
        vilkårData.setEkstraVilkårresultat(resultat);
        return vilkårData;
    }
}
