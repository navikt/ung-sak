package no.nav.k9.sak.inngangsvilkaar.opptjening;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.kodeverk.vilkår.VilkårTypeKoder;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.inngangsvilkaar.Inngangsvilkår;
import no.nav.k9.sak.inngangsvilkaar.VilkårData;
import no.nav.k9.sak.inngangsvilkaar.VilkårTypeRef;

@ApplicationScoped
@FagsakYtelseTypeRef
@VilkårTypeRef(VilkårTypeKoder.FP_VK_23)
public class InngangsvilkårOpptjening implements Inngangsvilkår {

    private OpptjeningsVilkårTjeneste opptjeningsVilkårTjeneste;

    InngangsvilkårOpptjening() {
        // for CDI proxy
    }

    @Inject
    public InngangsvilkårOpptjening(@FagsakYtelseTypeRef OpptjeningsVilkårTjeneste opptjeningsVilkårTjeneste) {
        this.opptjeningsVilkårTjeneste = opptjeningsVilkårTjeneste;
    }

    @Override
    public VilkårData vurderVilkår(BehandlingReferanse ref, DatoIntervallEntitet periode) {
        // returner egen output i tillegg for senere lagring
        return opptjeningsVilkårTjeneste.vurderOpptjeningsVilkår(ref, periode);
    }

}
