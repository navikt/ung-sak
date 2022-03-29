package no.nav.k9.sak.domene.behandling.steg.varselrevurdering;

import static no.nav.k9.kodeverk.behandling.BehandlingStegType.VARSEL_REVURDERING;
import static no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon.AUTO_SATT_PÅ_VENT_REVURDERING;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;

@BehandlingStegRef(stegtype = VARSEL_REVURDERING)
@BehandlingTypeRef
@FagsakYtelseTypeRef
@ApplicationScoped
public class VarselRevurderingStegImpl implements VarselRevurderingSteg {

    private BehandlingRepository behandlingRepository;

    @Inject
    public VarselRevurderingStegImpl(BehandlingRepository behandlingRepository) {
        this.behandlingRepository = behandlingRepository;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {

        Behandling behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());

        if (harUtførtVentRevurdering(behandling)) {
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        }

        if (behandling.getBehandlingÅrsaker().isEmpty()) {
            throw VarselRevurderingStegFeil.FACTORY.manglerBehandlingsårsakPåRevurdering().toException();
        }

        return BehandleStegResultat.utførtUtenAksjonspunkter();

    }

    private boolean harUtførtVentRevurdering(Behandling behandling) {
        return behandling.getAksjonspunktMedDefinisjonOptional(AUTO_SATT_PÅ_VENT_REVURDERING).map(Aksjonspunkt::erUtført).orElse(Boolean.FALSE);
    }
}
