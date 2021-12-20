package no.nav.k9.sak.inngangsvilkår.opptjening;

import java.util.Collection;
import java.util.NavigableMap;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.kodeverk.vilkår.VilkårTypeKoder;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingskontroll.VilkårTypeRef;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.inngangsvilkår.Inngangsvilkår;
import no.nav.k9.sak.inngangsvilkår.VilkårData;

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
    public NavigableMap<DatoIntervallEntitet, VilkårData> vurderVilkår(BehandlingReferanse ref, Collection<DatoIntervallEntitet> periode) {
        // returner egen output i tillegg for senere lagring
        return opptjeningsVilkårTjeneste.vurderOpptjeningsVilkår(ref, periode);
    }

}
