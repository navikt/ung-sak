package no.nav.ung.sak.web.app.tjenester.behandling.vedtak;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.ung.kodeverk.behandling.BehandlingStatus;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.SkjermlenkeType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.ung.sak.kontrakt.vedtak.TotrinnskontrollAksjonspunkterDto;
import no.nav.ung.sak.kontrakt.vedtak.TotrinnskontrollSkjermlenkeContextDto;
import no.nav.ung.sak.produksjonsstyring.totrinn.TotrinnTjeneste;
import no.nav.ung.sak.produksjonsstyring.totrinn.Totrinnsvurdering;

@ApplicationScoped
public class TotrinnskontrollAksjonspunkterTjeneste {

    private TotrinnsaksjonspunktDtoTjeneste totrinnsaksjonspunktDtoTjeneste;
    private TotrinnTjeneste totrinnTjeneste;

    protected TotrinnskontrollAksjonspunkterTjeneste() {
        // for CDI-proxy
    }

    @Inject
    public TotrinnskontrollAksjonspunkterTjeneste(TotrinnsaksjonspunktDtoTjeneste totrinnsaksjonspunktDtoTjeneste,
                                                  TotrinnTjeneste totrinnTjeneste) {
        this.totrinnsaksjonspunktDtoTjeneste = totrinnsaksjonspunktDtoTjeneste;
        this.totrinnTjeneste = totrinnTjeneste;
    }

    public List<TotrinnskontrollSkjermlenkeContextDto> hentTotrinnsSkjermlenkeContext(Behandling behandling) {
        List<TotrinnskontrollSkjermlenkeContextDto> skjermlenkeContext = new ArrayList<>();
        List<Aksjonspunkt> aksjonspunkter = behandling.getAksjonspunkterMedTotrinnskontroll();
        Map<SkjermlenkeType, List<TotrinnskontrollAksjonspunkterDto>> skjermlenkeMap = new HashMap<>();
        Collection<Totrinnsvurdering> ttVurderinger = totrinnTjeneste.hentTotrinnaksjonspunktvurderinger(behandling);
        // Behandling er ikkje i fatte vedtak og har ingen totrinnsvurderinger -> returnerer tom liste
        if (!BehandlingStatus.FATTER_VEDTAK.equals(behandling.getStatus()) && ttVurderinger.isEmpty()) {
            return Collections.emptyList();
        }
        for (var ap : aksjonspunkter) {
            var builder = new Totrinnsvurdering.Builder(behandling, ap.getAksjonspunktDefinisjon());
            var vurdering = ttVurderinger.stream().filter(v -> v.getAksjonspunktDefinisjon().equals(ap.getAksjonspunktDefinisjon())) .findFirst();
            vurdering.ifPresent(ttVurdering -> {
                if (ttVurdering.isGodkjent()) {
                    builder.medGodkjent(ttVurdering.isGodkjent());
                }
            });
            lagTotrinnsaksjonspunkt(skjermlenkeMap, builder.build());
        }
        for (var skjermlenke : skjermlenkeMap.entrySet()) {
            var context = new TotrinnskontrollSkjermlenkeContextDto(skjermlenke.getKey(), skjermlenke.getValue());
            skjermlenkeContext.add(context);
        }
        return skjermlenkeContext;
    }

    private void lagTotrinnsaksjonspunkt(Map<SkjermlenkeType, List<TotrinnskontrollAksjonspunkterDto>> skjermlenkeMap,
                                         Totrinnsvurdering vurdering) {
        TotrinnskontrollAksjonspunkterDto totrinnsAksjonspunkt = totrinnsaksjonspunktDtoTjeneste.lagTotrinnskontrollAksjonspunktDto(vurdering);
        SkjermlenkeType skjermlenkeType = SkjermlenkeType.finnSkjermlenkeType(vurdering.getAksjonspunktDefinisjon());
        if (skjermlenkeType != null && !SkjermlenkeType.UDEFINERT.equals(skjermlenkeType)) {
            List<TotrinnskontrollAksjonspunkterDto> aksjonspktContextListe = skjermlenkeMap.computeIfAbsent(skjermlenkeType,
                k -> new ArrayList<>());
            aksjonspktContextListe.add(totrinnsAksjonspunkt);
        }
    }

    public List<TotrinnskontrollSkjermlenkeContextDto> hentTotrinnsvurderingSkjermlenkeContext(Behandling behandling) {
        List<TotrinnskontrollSkjermlenkeContextDto> skjermlenkeContext = new ArrayList<>();
        Collection<Totrinnsvurdering> totrinnaksjonspunktvurderinger = totrinnTjeneste.hentTotrinnaksjonspunktvurderinger(behandling);
        Map<SkjermlenkeType, List<TotrinnskontrollAksjonspunkterDto>> skjermlenkeMap = new HashMap<>();
        for (var vurdering : totrinnaksjonspunktvurderinger) {
            lagTotrinnsaksjonspunkt(skjermlenkeMap, vurdering);
        }
        for (Map.Entry<SkjermlenkeType, List<TotrinnskontrollAksjonspunkterDto>> skjermlenke : skjermlenkeMap.entrySet()) {
            var context = new TotrinnskontrollSkjermlenkeContextDto(skjermlenke.getKey(), skjermlenke.getValue());
            skjermlenkeContext.add(context);
        }
        return skjermlenkeContext;
    }
}
