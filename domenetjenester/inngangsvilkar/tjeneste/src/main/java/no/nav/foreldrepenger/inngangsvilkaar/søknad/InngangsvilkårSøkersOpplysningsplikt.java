package no.nav.foreldrepenger.inngangsvilkaar.søknad;

import static java.util.Collections.singletonList;

import java.util.Collections;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårTypeKoder;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Utfall;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.typer.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.inngangsvilkaar.Inngangsvilkår;
import no.nav.foreldrepenger.inngangsvilkaar.VilkårData;
import no.nav.foreldrepenger.inngangsvilkaar.VilkårTypeRef;
import no.nav.foreldrepenger.kompletthet.KompletthetsjekkerProvider;

@ApplicationScoped
@VilkårTypeRef(VilkårTypeKoder.FP_VK_34)
public class InngangsvilkårSøkersOpplysningsplikt implements Inngangsvilkår {

    private KompletthetsjekkerProvider kompletthetsjekkerProvider;

    public InngangsvilkårSøkersOpplysningsplikt() {
        // for CDI proxy
    }

    @Inject
    public InngangsvilkårSøkersOpplysningsplikt(KompletthetsjekkerProvider kompletthetsjekkerProvider) {
        this.kompletthetsjekkerProvider = kompletthetsjekkerProvider;
    }

    @Override
    public VilkårData vurderVilkår(BehandlingReferanse ref, DatoIntervallEntitet periode) {
        return vurderOpplysningspliktOppfyltAutomatisk(ref, periode);
    }

    private VilkårData vurderOpplysningspliktOppfyltAutomatisk(BehandlingReferanse ref, DatoIntervallEntitet periode) {
        VilkårData oppfylt = new VilkårData(periode, VilkårType.SØKERSOPPLYSNINGSPLIKT, Utfall.OPPFYLT, Collections.emptyList());

        VilkårData manuellVurdering = new VilkårData(periode, VilkårType.SØKERSOPPLYSNINGSPLIKT, Utfall.IKKE_VURDERT,
            singletonList(AksjonspunktDefinisjon.SØKERS_OPPLYSNINGSPLIKT_MANU));

        FagsakYtelseType ytelseType = ref.getFagsakYtelseType();
        BehandlingType behandlingType = ref.getBehandlingType();
        if (BehandlingType.REVURDERING.equals(behandlingType)) {
            // For revurdering skal det ikke utføres vilkårskontroll om opplysningsplikt (NOOP)
            return oppfylt;
        }

        boolean søknadKomplett = this.kompletthetsjekkerProvider.finnKompletthetsjekkerFor(ytelseType, behandlingType).erForsendelsesgrunnlagKomplett(ref);
        if (søknadKomplett) {
            return oppfylt;
        }

        return manuellVurdering;
    }
}

