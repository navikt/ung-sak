package no.nav.foreldrepenger.behandling.steg.medlemskap.fp;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandling.steg.medlemskap.KontrollerFaktaLøpendeMedlemskapSteg;
import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.foreldrepenger.domene.medlem.UtledVurderingsdatoerForMedlemskapTjeneste;
import no.nav.foreldrepenger.domene.medlem.VurderMedlemskapTjeneste;
import no.nav.foreldrepenger.domene.medlem.impl.MedlemResultat;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;

@BehandlingStegRef(kode = "KOFAK_LOP_MEDL")
@BehandlingTypeRef("BT-004") //Revurdering
@FagsakYtelseTypeRef
@ApplicationScoped
public class KontrollerFaktaLøpendeMedlemskapStegRevurdering implements KontrollerFaktaLøpendeMedlemskapSteg {

    private UtledVurderingsdatoerForMedlemskapTjeneste tjeneste;

    private BehandlingRepository behandlingRepository;
    private VurderMedlemskapTjeneste vurderMedlemskapTjeneste;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private VilkårResultatRepository vilkårResultatRepository;

    KontrollerFaktaLøpendeMedlemskapStegRevurdering() {
        //CDI
    }

    @Inject
    public KontrollerFaktaLøpendeMedlemskapStegRevurdering(UtledVurderingsdatoerForMedlemskapTjeneste vurderingsdatoer,
                                                           BehandlingRepositoryProvider provider,
                                                           VurderMedlemskapTjeneste vurderMedlemskapTjeneste,
                                                           SkjæringstidspunktTjeneste skjæringstidspunktTjeneste) {
        this.tjeneste = vurderingsdatoer;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.behandlingRepository = provider.getBehandlingRepository();
        this.vurderMedlemskapTjeneste = vurderMedlemskapTjeneste;
        this.vilkårResultatRepository = provider.getVilkårResultatRepository();
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        Long behandlingId = kontekst.getBehandlingId();
        Skjæringstidspunkt skjæringstidspunkter = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandlingId);
        if (skalVurdereLøpendeMedlemskap(kontekst.getBehandlingId(), skjæringstidspunkter)) {
            Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
            Set<LocalDate> finnVurderingsdatoer = tjeneste.finnVurderingsdatoer(behandlingId);
            Set<MedlemResultat> resultat = new HashSet<>();
            if (!finnVurderingsdatoer.isEmpty()) {
                BehandlingReferanse ref = BehandlingReferanse.fra(behandling, skjæringstidspunkter);
                finnVurderingsdatoer.forEach(dato -> resultat.addAll(vurderMedlemskapTjeneste.vurderMedlemskap(ref, dato)));
            }
            if (!resultat.isEmpty()) {
                return BehandleStegResultat.utførtMedAksjonspunkter(List.of(AksjonspunktDefinisjon.AVKLAR_FORTSATT_MEDLEMSKAP));
            }
        }
        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

    private boolean skalVurdereLøpendeMedlemskap(Long behandlingId, Skjæringstidspunkt skjæringstidspunkter) {
        final var vilkårene = vilkårResultatRepository.hentHvisEksisterer(behandlingId);
        return vilkårene.map(Vilkårene::getVilkårene)
            .orElse(Collections.emptyList())
            .stream()
            .filter(v -> v.getVilkårType().equals(VilkårType.MEDLEMSKAPSVILKÅRET))
            .map(Vilkår::getPerioder)
            .flatMap(Collection::stream)
            .filter(vp -> vp.getPeriode().inkluderer(skjæringstidspunkter.getUtledetSkjæringstidspunkt()))
            .anyMatch(vp -> vp.getGjeldendeUtfall().equals(Utfall.OPPFYLT));
    }
}
