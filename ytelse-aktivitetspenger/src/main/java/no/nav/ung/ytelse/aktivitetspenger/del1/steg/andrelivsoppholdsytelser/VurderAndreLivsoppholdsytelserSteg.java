package no.nav.ung.ytelse.aktivitetspenger.del1.steg.andrelivsoppholdsytelser;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.ung.sak.behandlingskontroll.BehandlingSteg;
import no.nav.ung.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.ung.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.ung.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;

import java.util.List;

import static no.nav.ung.kodeverk.behandling.BehandlingStegType.VURDER_ANDRE_LIVSHOPPHOLDSYTELSER;

@ApplicationScoped
@BehandlingStegRef(value = VURDER_ANDRE_LIVSHOPPHOLDSYTELSER)
@BehandlingTypeRef
@FagsakYtelseTypeRef(FagsakYtelseType.AKTIVITETSPENGER)
public class VurderAndreLivsoppholdsytelserSteg implements BehandlingSteg {
    private BehandlingRepository behandlingRepository;
    private VilkårResultatRepository vilkårResultatRepository;

    VurderAndreLivsoppholdsytelserSteg() {
        // for CDI proxy
    }

    @Inject
    public VurderAndreLivsoppholdsytelserSteg(BehandlingRepository behandlingRepository,
                                              VilkårResultatRepository vilkårResultatRepository) {
        this.behandlingRepository = behandlingRepository;
        this.vilkårResultatRepository = vilkårResultatRepository;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        AksjonspunktDefinisjon aksjonspunktDefinisjon = AksjonspunktDefinisjon.VURDER_ANDRE_LIVSOPPHOLDSYTELSER;
        VilkårType vilkårtype = VilkårType.ANDRE_LIVSOPPHOLDSYTELSER_VILKÅR;
        VilkårType forrigeVilkårtype = VilkårType.BOSTEDSVILKÅR;

        Long behandlingId = kontekst.getBehandlingId();
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        if (vilkårResultatRepository.erNoeInnevilgetFor(behandlingId, forrigeVilkårtype)) {
            return BehandleStegResultat.utførtMedAksjonspunkter(List.of(aksjonspunktDefinisjon));
        } else {
            //kan ikke innvilge når tidligere vilkår ikke er oppfylt
            vilkårResultatRepository.settUtfallForAllePerioder(behandlingId, vilkårtype, Utfall.IKKE_RELEVANT);
            behandling.getAksjonspunktMedDefinisjonOptional(aksjonspunktDefinisjon).ifPresent(Aksjonspunkt::avbryt);
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        }
    }


}
