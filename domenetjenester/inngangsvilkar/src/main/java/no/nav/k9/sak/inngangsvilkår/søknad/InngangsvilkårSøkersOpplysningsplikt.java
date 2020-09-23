package no.nav.k9.sak.inngangsvilkår.søknad;

import static java.util.Collections.singletonList;

import java.util.Collection;
import java.util.Collections;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.kodeverk.vilkår.VilkårTypeKoder;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.inngangsvilkår.Inngangsvilkår;
import no.nav.k9.sak.inngangsvilkår.VilkårData;
import no.nav.k9.sak.inngangsvilkår.VilkårTypeRef;
import no.nav.k9.sak.kompletthet.KompletthetsjekkerProvider;

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
    public NavigableMap<DatoIntervallEntitet, VilkårData> vurderVilkår(BehandlingReferanse ref, Collection<DatoIntervallEntitet> periode) {
        return vurderOpplysningspliktOppfyltAutomatisk(ref, periode);
    }

    private NavigableMap<DatoIntervallEntitet, VilkårData> vurderOpplysningspliktOppfyltAutomatisk(BehandlingReferanse ref, Collection<DatoIntervallEntitet> perioder) {
        if (perioder.isEmpty()) {
            return Collections.emptyNavigableMap();
        }

        NavigableMap<DatoIntervallEntitet, VilkårData> resultater = new TreeMap<>();

        for (var periode : new TreeSet<>(perioder)) {
            FagsakYtelseType ytelseType = ref.getFagsakYtelseType();
            BehandlingType behandlingType = ref.getBehandlingType();

            VilkårData oppfylt = new VilkårData(periode, VilkårType.SØKERSOPPLYSNINGSPLIKT, Utfall.OPPFYLT, Collections.emptyList());
            if (BehandlingType.REVURDERING.equals(behandlingType)) {
                // For revurdering skal det ikke utføres vilkårskontroll om opplysningsplikt (NOOP)
                resultater.put(periode, oppfylt);
            } else {
                boolean søknadKomplett = this.kompletthetsjekkerProvider.finnKompletthetsjekkerFor(ytelseType, behandlingType).erForsendelsesgrunnlagKomplett(ref);
                if (søknadKomplett) {
                    resultater.put(periode, oppfylt);
                } else {
                    VilkårData manuellVurdering = new VilkårData(periode, VilkårType.SØKERSOPPLYSNINGSPLIKT, Utfall.IKKE_VURDERT, singletonList(AksjonspunktDefinisjon.SØKERS_OPPLYSNINGSPLIKT_MANU));
                    resultater.put(periode, manuellVurdering);
                }
            }
        }

        return Collections.unmodifiableNavigableMap(resultater);
    }
}
