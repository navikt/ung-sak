package no.nav.foreldrepenger.inngangsvilkaar.medisinsk;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.domene.typer.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.inngangsvilkaar.Inngangsvilkår;
import no.nav.foreldrepenger.inngangsvilkaar.VilkårData;
import no.nav.foreldrepenger.inngangsvilkaar.VilkårTypeRef;
import no.nav.foreldrepenger.inngangsvilkaar.impl.InngangsvilkårOversetter;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.medisinsk.Medisinskvilkår;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.medisinsk.MedisinskvilkårGrunnlag;
import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.kodeverk.vilkår.VilkårTypeKoder;

@ApplicationScoped
@VilkårTypeRef(VilkårTypeKoder.PSB_VK_1)
public class Medisinsk implements Inngangsvilkår {

    private InngangsvilkårOversetter inngangsvilkårOversetter;

    Medisinsk() {
        // for CDI proxy
    }

    @Inject
    public Medisinsk(InngangsvilkårOversetter inngangsvilkårOversetter) {
        this.inngangsvilkårOversetter = inngangsvilkårOversetter;
    }

    @Override
    public VilkårData vurderVilkår(BehandlingReferanse ref, DatoIntervallEntitet periode) {
        MedisinskvilkårGrunnlag grunnlag = inngangsvilkårOversetter.oversettTilRegelModellMedisinsk(ref, periode);

        Evaluation evaluation = new Medisinskvilkår().evaluer(grunnlag);

        return inngangsvilkårOversetter.tilVilkårData(VilkårType.MEDISINSKEVILKÅR, evaluation, grunnlag, periode);
    }
}
