package no.nav.k9.sak.ytelse.omsorgspenger.årskvantum;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.aarskvantum.kontrakter.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.AksjonspunktResultat;
import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingSteg;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegModell;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.domene.typer.tid.JsonObjectMapper;
import no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.tjenester.ÅrskvantumTjeneste;

@ApplicationScoped
@BehandlingStegRef(kode = "VURDER_UTTAK")
@BehandlingTypeRef
@FagsakYtelseTypeRef("OMP")
public class VurderÅrskvantumUttakSteg implements BehandlingSteg {

    private static final Logger log = LoggerFactory.getLogger(VurderÅrskvantumUttakSteg.class);

    private BehandlingRepository behandlingRepository;
    private ÅrskvantumTjeneste årskvantumTjeneste;


    VurderÅrskvantumUttakSteg() {
        // for proxy
    }

    @Inject
    public VurderÅrskvantumUttakSteg(BehandlingRepository behandlingRepository,
                                     ÅrskvantumTjeneste årskvantumTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.årskvantumTjeneste = årskvantumTjeneste;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        var behandlingId = kontekst.getBehandlingId();
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var ref = BehandlingReferanse.fra(behandling);

        var forbrukteDager = årskvantumTjeneste.hentÅrskvantumForBehandling(ref.getBehandlingUuid());

        // Hvis vi manuellt har bekreftet uttaksplan ikke generer ny uttaksplan, men kjør videre uten aksjonspunkt
        if (forbrukteDager.getSisteUttaksplan() != null && Bekreftet.MANUELTBEKREFTET.equals(forbrukteDager.getSisteUttaksplan().getBekreftet())) {
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        }

        var årskvantumResultat = årskvantumTjeneste.beregnÅrskvantumUttak(ref);

        var årskvantumAksjonspunkt = skalDetLagesAksjonspunkt(årskvantumResultat);

        if (!årskvantumAksjonspunkt.isEmpty()) {
            try {
                if (log.isDebugEnabled()) { log.debug("Setter behandling på vent etter følgende respons fra årskvantum" +
                    "\nrespons='{}'", JsonObjectMapper.getJson(årskvantumResultat)); }
            } catch (IOException e) {
                log.info("Feilet i serialisering av årskvantum respons='{}' og exception='{}'", årskvantumResultat, e);
            }
            opprettAksjonspunktForÅrskvantum(årskvantumAksjonspunkt);
            return BehandleStegResultat.utførtMedAksjonspunkter(årskvantumAksjonspunkt);
        } else {
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        }

    }

    @Override
    public void vedHoppOverBakover(BehandlingskontrollKontekst kontekst, BehandlingStegModell modell, BehandlingStegType førsteSteg, BehandlingStegType sisteSteg) {
        if (!førsteSteg.equals(sisteSteg)) {
            årskvantumTjeneste.slettUttaksplan(kontekst.getBehandlingId());
        }
    }

    private List<AksjonspunktResultat> opprettAksjonspunktForÅrskvantum(List<AksjonspunktDefinisjon> apDef) {
        var aksjonspunktResultat = new ArrayList<AksjonspunktResultat>();
        apDef.forEach(aksjonspunktDefinisjon ->
            aksjonspunktResultat.add(AksjonspunktResultat.opprettForAksjonspunkt(aksjonspunktDefinisjon))
        );
        return aksjonspunktResultat;
    }

    public List<AksjonspunktDefinisjon> skalDetLagesAksjonspunkt(ÅrskvantumResultat årskvantumResultat) {
        var aksjonspunkter = årskvantumResultat.getUttaksplan().getAksjonspunkter();
        if (!aksjonspunkter.isEmpty()) {
            var aksjonspunktDefinisjoner = new ArrayList<AksjonspunktDefinisjon>();
            aksjonspunkter.forEach(aksjonspunkt -> {
                if (Aksjonspunkt.VURDER_ÅRSKVANTUM_KVOTE_9003.equals(aksjonspunkt)) {
                    aksjonspunktDefinisjoner.add(AksjonspunktDefinisjon.VURDER_ÅRSKVANTUM_KVOTE);
                }
            });
            return aksjonspunktDefinisjoner;
        }
        return Collections.emptyList();
    }

}
