package no.nav.foreldrepenger.inngangsvilkaar.medlemskap;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.domene.typer.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.inngangsvilkaar.Inngangsvilkår;
import no.nav.foreldrepenger.inngangsvilkaar.VilkårData;
import no.nav.foreldrepenger.inngangsvilkaar.VilkårTypeRef;
import no.nav.foreldrepenger.inngangsvilkaar.impl.InngangsvilkårOversetter;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.medlemskap.Medlemskapsvilkår;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.medlemskap.MedlemskapsvilkårGrunnlag;
import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.kodeverk.vilkår.VilkårTypeKoder;

@ApplicationScoped
@VilkårTypeRef(VilkårTypeKoder.FP_VK_2)
public class InngangsvilkårMedlemskap implements Inngangsvilkår {

    private InngangsvilkårOversetter inngangsvilkårOversetter;

    InngangsvilkårMedlemskap() {
        // for CDI proxy
    }

    @Inject
    public InngangsvilkårMedlemskap(InngangsvilkårOversetter inngangsvilkårOversetter) {
        this.inngangsvilkårOversetter = inngangsvilkårOversetter;
    }

    @Override
    public VilkårData vurderVilkår(BehandlingReferanse ref, DatoIntervallEntitet periode) {
        MedlemskapsvilkårGrunnlag grunnlag = inngangsvilkårOversetter.oversettTilRegelModellMedlemskap(ref, periode);

        Evaluation evaluation = new Medlemskapsvilkår().evaluer(grunnlag);

        return inngangsvilkårOversetter.tilVilkårData(VilkårType.MEDLEMSKAPSVILKÅRET, evaluation, grunnlag, periode);
    }
}
