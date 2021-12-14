package no.nav.k9.sak.ytelse.pleiepengerbarn.uttak;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
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
@BehandlingStegRef(kode = "VURDER_UTTAK_V2")
@BehandlingTypeRef
@FagsakYtelseTypeRef("PSB")
public class VurderUttakIBeregningSteg implements BehandlingSteg {

    private static final Logger log = LoggerFactory.getLogger(VurderUttakIBeregningSteg.class);

    private BehandlingRepository behandlingRepository;
    private MapInputTilUttakTjeneste mapInputTilUttakTjeneste;
    private UttakTjeneste uttakTjeneste;
    private EtablertTilsynTjeneste etablertTilsynTjeneste;
    private SamtidigUttakTjeneste samtidigUttakTjeneste;
    private Boolean enableBekreftUttak;

    VurderUttakIBeregningSteg() {
        // for proxy
    }

    @Inject
    public VurderUttakIBeregningSteg(BehandlingRepository behandlingRepository,
                                     MapInputTilUttakTjeneste mapInputTilUttakTjeneste,
                                     UttakTjeneste uttakTjeneste,
                                     EtablertTilsynTjeneste etablertTilsynTjeneste,
                                     SamtidigUttakTjeneste samtidigUttakTjeneste,
                                     @KonfigVerdi(value = "psb.enable.bekreft.uttak", defaultVerdi = "false") Boolean enableBekreftUttak) {
        this.behandlingRepository = behandlingRepository;
        this.mapInputTilUttakTjeneste = mapInputTilUttakTjeneste;
        this.uttakTjeneste = uttakTjeneste;
        this.etablertTilsynTjeneste = etablertTilsynTjeneste;
        this.samtidigUttakTjeneste = samtidigUttakTjeneste;
        this.enableBekreftUttak = enableBekreftUttak;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        if (enableBekreftUttak) {
            var behandlingId = kontekst.getBehandlingId();
            var behandling = behandlingRepository.hentBehandling(behandlingId);
            var ref = BehandlingReferanse.fra(behandling);

            etablertTilsynTjeneste.opprettGrunnlagForTilsynstidlinje(ref);

            final Uttaksgrunnlag request = mapInputTilUttakTjeneste.hentUtOgMapRequest(ref);
            uttakTjeneste.opprettUttaksplan(request);

            final boolean annenSakSomMåBehandlesFørst = samtidigUttakTjeneste.isAnnenSakSomMåBehandlesFørst(ref);
            log.info("annenSakSomMåBehandlesFørst={}", annenSakSomMåBehandlesFørst);
            if (annenSakSomMåBehandlesFørst) {
                return BehandleStegResultat.tilbakeførtMedAksjonspunkter(List.of(AksjonspunktDefinisjon.VENT_ANNEN_PSB_SAK));
                // TODO: Legge tilbake og flytte aksjonspunktet til dette steget
//                return BehandleStegResultat.utførtMedAksjonspunkter(List.of(AksjonspunktDefinisjon.VENT_ANNEN_PSB_SAK));
            }

        }
        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

    @Override
    public void vedHoppOverBakover(BehandlingskontrollKontekst kontekst, BehandlingStegModell modell, BehandlingStegType tilSteg, BehandlingStegType fraSteg) {
        if (enableBekreftUttak) {
            if (!BehandlingStegType.VURDER_UTTAK_V2.equals(tilSteg)) {
                var behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
                uttakTjeneste.slettUttaksplan(behandling.getUuid());
            }
        }
    }
}
