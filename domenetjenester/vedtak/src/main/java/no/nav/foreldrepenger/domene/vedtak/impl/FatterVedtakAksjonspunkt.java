package no.nav.foreldrepenger.domene.vedtak.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.VurderÅrsak;
import no.nav.foreldrepenger.domene.vedtak.VedtakAksjonspunktData;
import no.nav.foreldrepenger.domene.vedtak.VedtakTjeneste;
import no.nav.foreldrepenger.produksjonsstyring.totrinn.TotrinnTjeneste;
import no.nav.foreldrepenger.produksjonsstyring.totrinn.Totrinnsvurdering;
import no.nav.vedtak.sikkerhet.context.SubjectHandler;

@ApplicationScoped
public class FatterVedtakAksjonspunkt {

    private VedtakTjeneste vedtakTjeneste;
    private TotrinnTjeneste totrinnTjeneste;
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;

    public FatterVedtakAksjonspunkt() {
    }

    @Inject
    public FatterVedtakAksjonspunkt(BehandlingskontrollTjeneste behandlingskontrollTjeneste,
                                    VedtakTjeneste vedtakTjeneste,
                                    TotrinnTjeneste totrinnTjeneste) {
        this.vedtakTjeneste = vedtakTjeneste;
        this.totrinnTjeneste = totrinnTjeneste;
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
    }

    public void oppdater(Behandling behandling, Collection<VedtakAksjonspunktData> aksjonspunkter) {
        BehandlingskontrollKontekst kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(behandling);
        behandling.setAnsvarligBeslutter(SubjectHandler.getSubjectHandler().getUid());

        List<Totrinnsvurdering> totrinnsvurderinger = new ArrayList<>();
        List<Aksjonspunkt> skalReåpnes = new ArrayList<>();

        for (VedtakAksjonspunktData aks : aksjonspunkter) {
            boolean erTotrinnGodkjent = aks.isGodkjent();
            Aksjonspunkt aksjonspunkt = behandling.getAksjonspunktFor(aks.getAksjonspunktDefinisjon());
            if (!aks.isGodkjent()) {
                skalReåpnes.add(aksjonspunkt);
            }

            Set<String> koder = aks.getVurderÅrsakskoder();
            Collection<VurderÅrsak> vurderÅrsaker = koder.stream().map(VurderÅrsak::fraKode).collect(Collectors.toSet());

            Totrinnsvurdering.Builder vurderingBuilder = new Totrinnsvurdering.Builder(behandling, aks.getAksjonspunktDefinisjon());
            vurderingBuilder.medGodkjent(erTotrinnGodkjent);
            vurderÅrsaker.forEach(vurderingBuilder::medVurderÅrsak);
            vurderingBuilder.medBegrunnelse(aks.getBegrunnelse());
            totrinnsvurderinger.add(vurderingBuilder.build());
        }
        totrinnTjeneste.settNyeTotrinnaksjonspunktvurderinger(behandling, totrinnsvurderinger);
        vedtakTjeneste.lagHistorikkinnslagFattVedtak(behandling);
        // Noe spesialhåndtering ifm totrinn og tilbakeføring fra FVED
        behandlingskontrollTjeneste.lagreAksjonspunkterReåpnet(kontekst, skalReåpnes, Optional.of(Boolean.TRUE));
    }
}
