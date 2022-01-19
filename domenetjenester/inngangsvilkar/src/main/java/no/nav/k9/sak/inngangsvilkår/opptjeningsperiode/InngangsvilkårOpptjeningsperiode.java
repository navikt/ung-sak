package no.nav.k9.sak.inngangsvilkår.opptjeningsperiode;

import java.util.Collection;
import java.util.NavigableMap;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.k9.kodeverk.vilkår.VilkårTypeKoder;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingskontroll.VilkårTypeRef;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.inngangsvilkår.Inngangsvilkår;
import no.nav.k9.sak.inngangsvilkår.VilkårData;

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
    public NavigableMap<DatoIntervallEntitet, VilkårData> vurderVilkår(BehandlingReferanse ref, Collection<DatoIntervallEntitet> periode) {
        return opptjeningsperiodeVilkårTjeneste.vurderOpptjeningsperiodeVilkår(ref, periode);
    }

}
