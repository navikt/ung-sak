package no.nav.k9.sak.ytelse.pleiepengerbarn.uttak;

import static no.nav.k9.kodeverk.behandling.BehandlingStegType.VURDER_UTTAK_V2;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_NÆRSTÅENDE;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_SYKT_BARN;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingSteg;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegModell;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.etablerttilsyn.EtablertTilsynTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.SamtidigUttakTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.input.MapInputTilUttakTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.tjeneste.UttakTjeneste;
import no.nav.pleiepengerbarn.uttak.kontrakter.Uttaksgrunnlag;

@ApplicationScoped
@BehandlingStegRef(value = VURDER_UTTAK_V2)
@BehandlingTypeRef
@FagsakYtelseTypeRef(PLEIEPENGER_SYKT_BARN)
@FagsakYtelseTypeRef(PLEIEPENGER_NÆRSTÅENDE)
public class VurderUttakIBeregningSteg implements BehandlingSteg {

    private static final Logger log = LoggerFactory.getLogger(VurderUttakIBeregningSteg.class);

    private BehandlingRepository behandlingRepository;
    private MapInputTilUttakTjeneste mapInputTilUttakTjeneste;
    private UttakTjeneste uttakTjeneste;
    private EtablertTilsynTjeneste etablertTilsynTjeneste;
    private SamtidigUttakTjeneste samtidigUttakTjeneste;

    VurderUttakIBeregningSteg() {
        // for proxy
    }

    @Inject
    public VurderUttakIBeregningSteg(BehandlingRepository behandlingRepository,
                                     MapInputTilUttakTjeneste mapInputTilUttakTjeneste,
                                     UttakTjeneste uttakTjeneste,
                                     EtablertTilsynTjeneste etablertTilsynTjeneste,
                                     SamtidigUttakTjeneste samtidigUttakTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.mapInputTilUttakTjeneste = mapInputTilUttakTjeneste;
        this.uttakTjeneste = uttakTjeneste;
        this.etablertTilsynTjeneste = etablertTilsynTjeneste;
        this.samtidigUttakTjeneste = samtidigUttakTjeneste;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        var behandlingId = kontekst.getBehandlingId();
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var ref = BehandlingReferanse.fra(behandling);

        etablertTilsynTjeneste.opprettGrunnlagForTilsynstidlinje(ref);

        final Uttaksgrunnlag request = mapInputTilUttakTjeneste.hentUtOgMapRequest(ref);
        uttakTjeneste.opprettUttaksplan(request);

        final boolean annenSakSomMåBehandlesFørst = samtidigUttakTjeneste.isAnnenSakSomMåBehandlesFørst(ref);
        log.info("annenSakSomMåBehandlesFørst={}", annenSakSomMåBehandlesFørst);

        if (annenSakSomMåBehandlesFørst) {
            return BehandleStegResultat.utførtMedAksjonspunkter(List.of(AksjonspunktDefinisjon.VENT_ANNEN_PSB_SAK));
        } else if (behandling.harÅpentAksjonspunktMedType(AksjonspunktDefinisjon.VENT_ANNEN_PSB_SAK)) {
            behandling.getAksjonspunktFor(AksjonspunktDefinisjon.VENT_ANNEN_PSB_SAK)
                .avbryt();
            behandlingRepository.lagre(behandling, kontekst.getSkriveLås());
        }

        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

    @Override
    public void vedHoppOverBakover(BehandlingskontrollKontekst kontekst, BehandlingStegModell modell, BehandlingStegType tilSteg, BehandlingStegType fraSteg) {
        if (!VURDER_UTTAK_V2.equals(tilSteg)) {
            var behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
            uttakTjeneste.slettUttaksplan(behandling.getUuid());
        }
    }
}
