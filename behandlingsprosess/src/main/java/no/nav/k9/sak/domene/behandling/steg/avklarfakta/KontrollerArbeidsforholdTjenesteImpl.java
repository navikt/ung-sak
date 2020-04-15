package no.nav.k9.sak.domene.behandling.steg.avklarfakta;

import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.sak.behandling.BehandlingReferanse;
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
@StartpunktRef("KONTROLLER_ARBEIDSFORHOLD")
@ApplicationScoped
public class KontrollerArbeidsforholdTjenesteImpl implements KontrollerFaktaAksjonspunktUtleder {

    private static final Logger logger = LoggerFactory.getLogger(KontrollerArbeidsforholdTjenesteImpl.class);
    private AksjonspunktUtlederForVurderArbeidsforhold aksjonspunktUtlederVurderArbeidsforhold;
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;

    protected KontrollerArbeidsforholdTjenesteImpl() {
        // for CDI proxy
    }

    @Inject
    public KontrollerArbeidsforholdTjenesteImpl(AksjonspunktUtlederForVurderArbeidsforhold utlederTjeneste,
                                                BehandlingskontrollTjeneste behandlingskontrollTjeneste) {
        this.aksjonspunktUtlederVurderArbeidsforhold = utlederTjeneste;
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

    private List<AksjonspunktResultat> filtrerAksjonspunkterTilVenstreForStartpunkt(BehandlingReferanse referanse, List<AksjonspunktResultat> aksjonspunktResultat, StartpunktType startpunkt) {
        // Fjerner aksjonspunkter som ikke skal løses i eller etter steget som følger av startpunktet:
        return aksjonspunktResultat.stream()
            .filter(ap -> skalBeholdeAksjonspunkt(referanse, startpunkt, ap.getAksjonspunktDefinisjon()))
            .collect(Collectors.toList());
    }

    private boolean skalBeholdeAksjonspunkt(BehandlingReferanse referanse, StartpunktType startpunkt, AksjonspunktDefinisjon apDef) {
        boolean skalBeholde = behandlingskontrollTjeneste.skalAksjonspunktLøsesIEllerEtterSteg(
            referanse.getFagsakYtelseType(), referanse.getBehandlingType(), startpunkt.getBehandlingSteg(), apDef);
        if (!skalBeholde) {
            logger.debug("Fjerner aksjonspunkt {} da det skal løses før startsteg {}.",
                apDef.getKode(), startpunkt.getBehandlingSteg().getKode()); // NOSONAR
        }
        return skalBeholde;
    }

    private List<AksjonspunktResultat> utled(BehandlingReferanse ref) {
        var input = new AksjonspunktUtlederInput(ref);
        List<AksjonspunktResultat> aksjonspunktResultater = aksjonspunktUtlederVurderArbeidsforhold.utledAksjonspunkterFor(input);
        return aksjonspunktResultater.stream()
            .distinct() // Unngå samme aksjonspunkt flere multipliser
            .collect(toList());
    }
}
