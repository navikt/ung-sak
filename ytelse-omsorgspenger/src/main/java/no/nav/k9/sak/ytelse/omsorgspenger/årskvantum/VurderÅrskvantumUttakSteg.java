package no.nav.k9.sak.ytelse.omsorgspenger.årskvantum;

import static no.nav.k9.kodeverk.behandling.BehandlingStegType.VURDER_UTTAK;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OMSORGSPENGER;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.aarskvantum.kontrakter.Aksjonspunkt;
import no.nav.k9.aarskvantum.kontrakter.Bekreftet;
import no.nav.k9.aarskvantum.kontrakter.ÅrskvantumResultat;
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
import no.nav.k9.sak.ytelse.omsorgspenger.repo.FosterbarnRepository;
import no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.tjenester.ÅrskvantumTjeneste;

@ApplicationScoped
@BehandlingStegRef(value = VURDER_UTTAK)
@BehandlingTypeRef
@FagsakYtelseTypeRef(OMSORGSPENGER)
public class VurderÅrskvantumUttakSteg implements BehandlingSteg {

    private BehandlingRepository behandlingRepository;
    private ÅrskvantumTjeneste årskvantumTjeneste;
    private FosterbarnRepository fosterbarnRepository;

    VurderÅrskvantumUttakSteg() {
        // for proxy
    }

    @Inject
    public VurderÅrskvantumUttakSteg(BehandlingRepository behandlingRepository,
                                     ÅrskvantumTjeneste årskvantumTjeneste,
                                     FosterbarnRepository fosterbarnRepository) {
        this.behandlingRepository = behandlingRepository;
        this.årskvantumTjeneste = årskvantumTjeneste;
        this.fosterbarnRepository = fosterbarnRepository;
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
        boolean harBehandletFosterbarnISakenAllerede = fosterbarnRepository.hentHvisEksisterer(behandlingId).isPresent();
        var årskvantumAksjonspunkter = oversettTilAksjonspunkter(årskvantumResultat, harBehandletFosterbarnISakenAllerede);

        if (!årskvantumAksjonspunkter.isEmpty()) {
            opprettAksjonspunktForÅrskvantum(årskvantumAksjonspunkter);
            return BehandleStegResultat.utførtMedAksjonspunkter(årskvantumAksjonspunkter);
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

    private List<AksjonspunktResultat> opprettAksjonspunktForÅrskvantum(Collection<AksjonspunktDefinisjon> apDef) {
        var aksjonspunktResultat = new ArrayList<AksjonspunktResultat>();
        apDef.forEach(aksjonspunktDefinisjon ->
            aksjonspunktResultat.add(AksjonspunktResultat.opprettForAksjonspunkt(aksjonspunktDefinisjon))
        );
        return aksjonspunktResultat;
    }

    public List<AksjonspunktDefinisjon> oversettTilAksjonspunkter(ÅrskvantumResultat årskvantumResultat, boolean harBehandletFosterbarnISakenAllerede) {
        var aksjonspunkter = årskvantumResultat.getUttaksplan().getAksjonspunkter();
        var aksjonspunktDefinisjoner = new ArrayList<AksjonspunktDefinisjon>();
        aksjonspunkter.forEach(aksjonspunkt -> {
            if (Aksjonspunkt.VURDER_ÅRSKVANTUM_KVOTE_9003.equals(aksjonspunkt)) {
                aksjonspunktDefinisjoner.add(AksjonspunktDefinisjon.VURDER_ÅRSKVANTUM_KVOTE);
            } else if (Aksjonspunkt.VURDER_ÅRSKVANTUM_DOK_9004.equals(aksjonspunkt)) {
                aksjonspunktDefinisjoner.add(AksjonspunktDefinisjon.VURDER_ÅRSKVANTUM_DOK);
            } else if (Aksjonspunkt.ÅRSKVANTUM_FOSTERBARN_9014.equals(aksjonspunkt)) {
                aksjonspunktDefinisjoner.add(AksjonspunktDefinisjon.ÅRSKVANTUM_FOSTERBARN);
            } else {
                throw new IllegalStateException("Ukjent aksjonspunkt fra årskvantum. [Kode=" + aksjonspunkt + "]");
            }
        });
        if (harBehandletFosterbarnISakenAllerede) {
            aksjonspunktDefinisjoner.remove(AksjonspunktDefinisjon.ÅRSKVANTUM_FOSTERBARN);
        }
        return aksjonspunktDefinisjoner;
    }

}
