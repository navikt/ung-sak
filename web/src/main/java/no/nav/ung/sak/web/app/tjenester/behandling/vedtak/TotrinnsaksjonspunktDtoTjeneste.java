package no.nav.ung.sak.web.app.tjenester.behandling.vedtak;

import jakarta.enterprise.context.Dependent;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.VurderÅrsak;
import no.nav.ung.sak.kontrakt.vedtak.TotrinnskontrollAksjonspunkterDto;
import no.nav.ung.sak.produksjonsstyring.totrinn.Totrinnsvurdering;
import no.nav.ung.sak.produksjonsstyring.totrinn.VurderÅrsakTotrinnsvurdering;

import java.util.Set;
import java.util.stream.Collectors;

@Dependent
public class TotrinnsaksjonspunktDtoTjeneste {

    protected TotrinnsaksjonspunktDtoTjeneste() {
        // for CDI proxy
    }

    public TotrinnskontrollAksjonspunkterDto lagTotrinnskontrollAksjonspunktDto(Totrinnsvurdering aksjonspunkt) {


        return new TotrinnskontrollAksjonspunkterDto.Builder()
            .medAksjonspunktKode(aksjonspunkt.getAksjonspunktDefinisjon())
            .medBesluttersBegrunnelse(aksjonspunkt.getBegrunnelse())
            .medTotrinnskontrollGodkjent(aksjonspunkt.isGodkjent())
            .medVurderPaNyttArsaker(hentVurderPåNyttÅrsaker(aksjonspunkt))
            .build();
    }

    private Set<VurderÅrsak> hentVurderPåNyttÅrsaker(Totrinnsvurdering aksjonspunkt) {
        return aksjonspunkt.getVurderPåNyttÅrsaker().stream()
            .map(VurderÅrsakTotrinnsvurdering::getÅrsaksType)
            .collect(Collectors.toSet());
    }
}
