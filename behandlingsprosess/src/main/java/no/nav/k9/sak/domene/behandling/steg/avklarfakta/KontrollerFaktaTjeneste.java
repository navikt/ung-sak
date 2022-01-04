package no.nav.k9.sak.domene.behandling.steg.avklarfakta;

import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktUtleder;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktUtlederInput;
import no.nav.k9.sak.behandlingskontroll.AksjonspunktResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingskontroll.StartpunktRef;
import no.nav.k9.sak.behandlingslager.hendelser.StartpunktType;
import no.nav.k9.sak.domene.registerinnhenting.KontrollerFaktaAksjonspunktUtleder;

@FagsakYtelseTypeRef
@BehandlingTypeRef
@StartpunktRef
@ApplicationScoped
public class KontrollerFaktaTjeneste implements KontrollerFaktaAksjonspunktUtleder {

    private static final Logger logger = LoggerFactory.getLogger(KontrollerFaktaTjeneste.class);

    private Instance<KontrollerFaktaUtledere> utlederTjenester;
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;

    protected KontrollerFaktaTjeneste() {
        // for CDI proxy
    }

    @Inject
    public KontrollerFaktaTjeneste(@Any Instance<KontrollerFaktaUtledere> utlederTjenester,
                                   BehandlingskontrollTjeneste behandlingskontrollTjeneste) {
        this.utlederTjenester = utlederTjenester;
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
    }

    @Override
    public List<AksjonspunktResultat> utledAksjonspunkter(BehandlingReferanse ref) {
        return utled(ref);
    }

    @Override
    public List<AksjonspunktResultat> utledAksjonspunkterTilHøyreForStartpunkt(BehandlingReferanse ref, StartpunktType startpunktType) {
        List<AksjonspunktResultat> aksjonspunktResultat = utledAksjonspunkter(ref);
        return filtrerAksjonspunkterTilVenstreForStartpunkt(ref, aksjonspunktResultat, startpunktType);
    }

    private List<AksjonspunktResultat> filtrerAksjonspunkterTilVenstreForStartpunkt(BehandlingReferanse referanse, List<AksjonspunktResultat> aksjonspunktResultat,
                                                                                    StartpunktType startpunkt) {
        // Fjerner aksjonspunkter som ikke skal løses i eller etter steget som følger av startpunktet:
        return aksjonspunktResultat.stream()
            .filter(ap -> skalBeholdeAksjonspunkt(referanse, startpunkt, ap.getAksjonspunktDefinisjon()))
            .collect(Collectors.toList());
    }

    private boolean skalBeholdeAksjonspunkt(BehandlingReferanse ref, StartpunktType startpunkt, AksjonspunktDefinisjon apDef) {
        boolean skalBeholde = behandlingskontrollTjeneste.skalAksjonspunktLøsesIEllerEtterSteg(
            ref.getFagsakYtelseType(), ref.getBehandlingType(), startpunkt, apDef);
        if (!skalBeholde) {
            logger.debug("Fjerner aksjonspunkt {} da det skal løses før startpunkt {}.",
                apDef.getKode(), startpunkt.getKode()); // NOSONAR
        }
        return skalBeholde;
    }

    private List<AksjonspunktResultat> utled(BehandlingReferanse ref) {
        final List<AksjonspunktUtleder> aksjonspunktUtleders = findUtlederTjeneste(ref).utledUtledereFor(ref);
        List<AksjonspunktResultat> aksjonspunktResultater = new ArrayList<>();
        for (AksjonspunktUtleder aksjonspunktUtleder : aksjonspunktUtleders) {
            aksjonspunktResultater.addAll(aksjonspunktUtleder.utledAksjonspunkterFor(new AksjonspunktUtlederInput(ref)));
        }
        return aksjonspunktResultater.stream()
            .distinct() // Unngå samme aksjonspunkt flere multipliser
            .collect(toList());
    }

    private KontrollerFaktaUtledere findUtlederTjeneste(BehandlingReferanse ref) {
        return BehandlingTypeRef.Lookup.find(KontrollerFaktaUtledere.class, utlederTjenester, ref.getFagsakYtelseType(), ref.getBehandlingType())
            .orElseThrow(() -> new UnsupportedOperationException("KontrollerFaktaUtledere ikke implementert for ytelse [" + ref.getFagsakYtelseType() + "], behandlingtype [" + ref.getBehandlingType() + "]"));
    }
}
