package no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.medisinsk;

import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.inngangsvilkår.VilkårData;
import no.nav.k9.sak.inngangsvilkår.VilkårUtfallOversetter;
import no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.medisinsk.regelmodell.MedisinskVilkår;
import no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.medisinsk.regelmodell.MedisinskVilkårGrunnlag;
import no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.medisinsk.regelmodell.MedisinskVilkårResultat;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.medisinsk.MedisinskGrunnlag;

public class MedisinskVilkårTjeneste {

    private final InngangsvilkårOversetter inngangsvilkårOversetter = new InngangsvilkårOversetter();
    private final VilkårUtfallOversetter utfallOversetter = new VilkårUtfallOversetter();

    MedisinskVilkårTjeneste() {

    }

    public VilkårData vurderPerioder(VilkårType vilkåret, BehandlingskontrollKontekst kontekst, DatoIntervallEntitet periodeTilVurdering, MedisinskGrunnlag sykdomGrunnlagBehandling) {
        MedisinskVilkårGrunnlag grunnlag = inngangsvilkårOversetter.oversettTilRegelModellMedisinsk(periodeTilVurdering, sykdomGrunnlagBehandling);
        MedisinskVilkårResultat resultat = new MedisinskVilkårResultat();

        final var evaluation = new MedisinskVilkår().evaluer(grunnlag, resultat);

        final var vilkårData = utfallOversetter.oversett(vilkåret, evaluation, grunnlag, periodeTilVurdering);
        vilkårData.setEkstraVilkårresultat(resultat);
        return vilkårData;
    }
}
