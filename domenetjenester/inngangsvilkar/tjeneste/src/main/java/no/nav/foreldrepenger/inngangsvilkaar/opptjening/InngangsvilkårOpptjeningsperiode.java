package no.nav.foreldrepenger.inngangsvilkaar.opptjening;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.inngangsvilkaar.Inngangsvilkår;
import no.nav.foreldrepenger.inngangsvilkaar.VilkårData;
import no.nav.foreldrepenger.inngangsvilkaar.VilkårTypeRef;
import no.nav.k9.kodeverk.vilkår.VilkårTypeKoder;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

@ApplicationScoped
@VilkårTypeRef(VilkårTypeKoder.FP_VK_21)
@FagsakYtelseTypeRef
public class InngangsvilkårOpptjeningsperiode implements Inngangsvilkår {

    private OpptjeningsperiodeVilkårTjeneste opptjeningsperiodeVilkårTjeneste;

    InngangsvilkårOpptjeningsperiode() {
        // for CDI proxy
    }

    @Inject
    public InngangsvilkårOpptjeningsperiode(@FagsakYtelseTypeRef OpptjeningsperiodeVilkårTjeneste opptjeningsperiodeVilkårTjeneste) {
        this.opptjeningsperiodeVilkårTjeneste = opptjeningsperiodeVilkårTjeneste;
    }

    @Override
    public VilkårData vurderVilkår(BehandlingReferanse ref, DatoIntervallEntitet periode) {
        return opptjeningsperiodeVilkårTjeneste.vurderOpptjeningsperiodeVilkår(ref, ref.getFørsteUttaksdato(), periode);
    }
}
