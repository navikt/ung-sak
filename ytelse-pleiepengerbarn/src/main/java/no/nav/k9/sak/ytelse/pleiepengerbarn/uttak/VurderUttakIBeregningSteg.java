package no.nav.k9.sak.ytelse.pleiepengerbarn.uttak;

import static no.nav.k9.kodeverk.behandling.BehandlingStegType.VURDER_UTTAK_V2;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_NÆRSTÅENDE;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_SYKT_BARN;

import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
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
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.utsatt.UtsattBehandlingAvPeriodeRepository;
import no.nav.k9.sak.utsatt.UtsattPeriode;
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
    private UtsattBehandlingAvPeriodeRepository utsattBehandlingAvPeriodeRepository;
    private Boolean utsattBehandlingAvPeriode;

    VurderUttakIBeregningSteg() {
        // for proxy
    }

    @Inject
    public VurderUttakIBeregningSteg(BehandlingRepository behandlingRepository,
                                     MapInputTilUttakTjeneste mapInputTilUttakTjeneste,
                                     UttakTjeneste uttakTjeneste,
                                     EtablertTilsynTjeneste etablertTilsynTjeneste,
                                     SamtidigUttakTjeneste samtidigUttakTjeneste,
                                     UtsattBehandlingAvPeriodeRepository utsattBehandlingAvPeriodeRepository,
                                     @KonfigVerdi(value = "utsatt.behandling.av.periode.aktivert", defaultVerdi = "false") Boolean utsattBehandlingAvPeriode) {
        this.behandlingRepository = behandlingRepository;
        this.mapInputTilUttakTjeneste = mapInputTilUttakTjeneste;
        this.uttakTjeneste = uttakTjeneste;
        this.etablertTilsynTjeneste = etablertTilsynTjeneste;
        this.samtidigUttakTjeneste = samtidigUttakTjeneste;
        this.utsattBehandlingAvPeriodeRepository = utsattBehandlingAvPeriodeRepository;
        this.utsattBehandlingAvPeriode = utsattBehandlingAvPeriode;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        var behandlingId = kontekst.getBehandlingId();
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var ref = BehandlingReferanse.fra(behandling);

        etablertTilsynTjeneste.opprettGrunnlagForTilsynstidlinje(ref);

        final Uttaksgrunnlag request = mapInputTilUttakTjeneste.hentUtOgMapRequest(ref);
        uttakTjeneste.opprettUttaksplan(request);

        if (utsattBehandlingAvPeriode) {
            return eksperimentærHåndteringAvSamtidigUttak(behandling, kontekst, ref);
        } else {
            return ordinærHåndteringAvSamtidigUttak(behandling, kontekst, ref);
        }
    }

    private BehandleStegResultat eksperimentærHåndteringAvSamtidigUttak(Behandling behandling, BehandlingskontrollKontekst kontekst, BehandlingReferanse ref) {
        var uttakPrioriteringsrekkefølge = samtidigUttakTjeneste.utledPrioriteringsrekkefølge(ref);
        log.info("[Eksperimentær] annenSakSomMåBehandlesFørst={}, Har perioder uten prio={}", uttakPrioriteringsrekkefølge.isAnnenSakBehandlesFørst(), !uttakPrioriteringsrekkefølge.getTidslinjeUtenPrioritet().isEmpty());

        if (uttakPrioriteringsrekkefølge.harGjensidigAvhengighet()) {
            var utsattePerioder = uttakPrioriteringsrekkefølge.getTidslinjeUtenPrioritet()
                .toSegments()
                .stream()
                .map(it -> DatoIntervallEntitet.fra(it.getLocalDateInterval()))
                .collect(Collectors.toCollection(TreeSet::new));
            log.info("[Eksperimentær] Utsettelse behandling av perioder {}", utsattePerioder);

            utsattBehandlingAvPeriodeRepository.leggTil(ref.getBehandlingId(), utsattePerioder.stream().map(UtsattPeriode::new).collect(Collectors.toSet()));

            final Uttaksgrunnlag oppdatertRequests = mapInputTilUttakTjeneste.hentUtOgMapRequest(ref);
            uttakTjeneste.opprettUttaksplan(oppdatertRequests);

            final boolean annenSakSomFortsattMåBehandlesFørst = samtidigUttakTjeneste.isAnnenSakSomMåBehandlesFørst(ref);
            log.info("[Eksperimentær] Etter utsettelse annenSakSomFortsattMåBehandlesFørst={}", annenSakSomFortsattMåBehandlesFørst);
            if (annenSakSomFortsattMåBehandlesFørst) {
                return BehandleStegResultat.utførtMedAksjonspunkter(List.of(AksjonspunktDefinisjon.VENT_ANNEN_PSB_SAK));
            } else if (behandling.harÅpentAksjonspunktMedType(AksjonspunktDefinisjon.VENT_ANNEN_PSB_SAK)) {
                avbrytAksjonspunkt(behandling, kontekst);
            }
        } else if (uttakPrioriteringsrekkefølge.isAnnenSakBehandlesFørst()) {
            return BehandleStegResultat.utførtMedAksjonspunkter(List.of(AksjonspunktDefinisjon.VENT_ANNEN_PSB_SAK));
        } else if (behandling.harÅpentAksjonspunktMedType(AksjonspunktDefinisjon.VENT_ANNEN_PSB_SAK)) {
            avbrytAksjonspunkt(behandling, kontekst);
        }

        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

    private BehandleStegResultat ordinærHåndteringAvSamtidigUttak(Behandling behandling, BehandlingskontrollKontekst kontekst, BehandlingReferanse ref) {
        final boolean annenSakSomMåBehandlesFørst = samtidigUttakTjeneste.isAnnenSakSomMåBehandlesFørst(ref);
        log.info("[Ordinær] annenSakSomMåBehandlesFørst={}", annenSakSomMåBehandlesFørst);
        if (annenSakSomMåBehandlesFørst) {
            return BehandleStegResultat.utførtMedAksjonspunkter(List.of(AksjonspunktDefinisjon.VENT_ANNEN_PSB_SAK));
        } else if (behandling.harÅpentAksjonspunktMedType(AksjonspunktDefinisjon.VENT_ANNEN_PSB_SAK)) {
            avbrytAksjonspunkt(behandling, kontekst);
        }

        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

    private void avbrytAksjonspunkt(Behandling behandling, BehandlingskontrollKontekst kontekst) {
        behandling.getAksjonspunktFor(AksjonspunktDefinisjon.VENT_ANNEN_PSB_SAK)
            .avbryt();
        behandlingRepository.lagre(behandling, kontekst.getSkriveLås());
    }

    @Override
    public void vedHoppOverBakover(BehandlingskontrollKontekst kontekst, BehandlingStegModell modell, BehandlingStegType tilSteg, BehandlingStegType fraSteg) {
        if (!VURDER_UTTAK_V2.equals(tilSteg)) {
            var behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
            uttakTjeneste.slettUttaksplan(behandling.getUuid());
        }
    }
}
