package no.nav.foreldrepenger.behandlingskontroll.impl.observer;

import java.util.Objects;
import java.util.function.Consumer;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingModell;
import no.nav.foreldrepenger.behandlingskontroll.spi.BehandlingskontrollServiceProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;

final class HåndterRyddingAvAksjonspunktVedTilbakeføring implements Consumer<Aksjonspunkt> {
    private final BehandlingStegType førsteSteg;
    private final BehandlingModell modell;
    private BehandlingskontrollServiceProvider serviceProvider;

    HåndterRyddingAvAksjonspunktVedTilbakeføring(BehandlingskontrollServiceProvider serviceProvider, BehandlingStegType førsteSteg, BehandlingModell modell) {
        this.serviceProvider = serviceProvider;
        this.førsteSteg = førsteSteg;
        this.modell = modell;
    }

    @Override
    public void accept(Aksjonspunkt a) {
        if (skalAvbryte(a)) {
            serviceProvider.getAksjonspunktRepository().setTilAvbrutt(a);
        } else if (skalReåpne(a)) {
            serviceProvider.getAksjonspunktRepository().setReåpnet(a);
        }
    }

    /**
     * Ved tilbakeføring skal følgende reåpnes:
     * - Påfølgende aksjonspunkt som er OVERSTYRING
     */
    protected boolean skalReåpne(Aksjonspunkt a) {
        BehandlingStegType måTidligstLøsesISteg = modell.finnTidligsteStegFor(a.getAksjonspunktDefinisjon())
            .getBehandlingStegType();
        boolean måLøsesIEllerEtterFørsteSteg = !modell.erStegAFørStegB(måTidligstLøsesISteg, førsteSteg);
        return  a.erManueltOpprettet() && måLøsesIEllerEtterFørsteSteg;
    }

    /**
     * Ved tilbakeføring skal alle påfølgende åpne aksjonspunkt (som IKKE ER OVERSTYRING) som identifiseres i eller
     * senere steg Avbrytes. De som er UTFØRT bilr stående og må evt reutledes - obs en del avklarte AP reutledes ikke.
     */
    protected boolean skalAvbryte(Aksjonspunkt a) {
        boolean erFunnetIFørsteStegEllerSenere = !modell.erStegAFørStegB(a.getBehandlingStegFunnet(), førsteSteg);
        boolean erManueltOpprettet = a.erManueltOpprettet();
        boolean erOpprettetIFørsteSteg = erOpprettetIFørsteSteg(a);
        boolean avbryt = !erManueltOpprettet && erFunnetIFørsteStegEllerSenere && !erOpprettetIFørsteSteg;
        return avbryt;
    }

    private boolean erOpprettetIFørsteSteg(Aksjonspunkt ap) {
        return Objects.equals(førsteSteg, ap.getBehandlingStegFunnet());
    }
}
