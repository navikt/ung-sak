package no.nav.k9.sak.ytelse.pleiepengerlivetsslutt.inngangsvilkår.medisinsk;

import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.inngangsvilkår.VilkårData;
import no.nav.k9.sak.inngangsvilkår.VilkårUtfallOversetter;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomGrunnlagBehandling;
import no.nav.k9.sak.ytelse.pleiepengerlivetsslutt.inngangsvilkår.medisinsk.regelmodell.Medisinskvilkår;
import no.nav.k9.sak.ytelse.pleiepengerlivetsslutt.inngangsvilkår.medisinsk.regelmodell.MedisinskvilkårGrunnlag;

public class MedisinskVilkårTjeneste {

    private InngangsvilkårOversetter inngangsvilkårOversetter = new InngangsvilkårOversetter();
    private VilkårUtfallOversetter utfallOversetter = new VilkårUtfallOversetter();

    MedisinskVilkårTjeneste() {

    }

    public VilkårData vurderPerioder(VilkårType vilkåret, DatoIntervallEntitet periodeTilVurdering, SykdomGrunnlagBehandling sykdomGrunnlagBehandling) {
        MedisinskvilkårGrunnlag grunnlag = inngangsvilkårOversetter.oversettTilRegelModellMedisinsk(periodeTilVurdering, sykdomGrunnlagBehandling);

        final var evaluation = new Medisinskvilkår().evaluer(grunnlag, null);

        final var vilkårData = utfallOversetter.oversett(vilkåret, evaluation, grunnlag, periodeTilVurdering);
        return vilkårData;
    }
}
