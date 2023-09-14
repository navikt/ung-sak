package no.nav.k9.sak.ytelse.pleiepengerbarn.uttak;

import static no.nav.k9.kodeverk.behandling.BehandlingStegType.VURDER_UTTAK_V2;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OPPLÆRINGSPENGER;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_NÆRSTÅENDE;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_SYKT_BARN;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.folketrygdloven.beregningsgrunnlag.tilkommetAktivitet.TilkommetAktivitetTjeneste;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.uttak.UttakArbeidType;
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
import no.nav.k9.sak.behandlingslager.behandling.uttak.UttakNyeReglerRepository;
import no.nav.k9.sak.utsatt.UtsattBehandlingAvPeriodeRepository;
import no.nav.k9.sak.utsatt.UtsattPeriode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.etablerttilsyn.EtablertTilsynTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.SamtidigUttakTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.input.MapInputTilUttakTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.tjeneste.UttakTjeneste;
import no.nav.pleiepengerbarn.uttak.kontrakter.Utbetalingsgrader;
import no.nav.pleiepengerbarn.uttak.kontrakter.Uttaksgrunnlag;
import no.nav.pleiepengerbarn.uttak.kontrakter.UttaksperiodeInfo;
import no.nav.pleiepengerbarn.uttak.kontrakter.Uttaksplan;

@ApplicationScoped
@BehandlingStegRef(value = VURDER_UTTAK_V2)
@BehandlingTypeRef
@FagsakYtelseTypeRef(PLEIEPENGER_SYKT_BARN)
@FagsakYtelseTypeRef(PLEIEPENGER_NÆRSTÅENDE)
@FagsakYtelseTypeRef(OPPLÆRINGSPENGER)
public class VurderUttakIBeregningSteg implements BehandlingSteg {

    private static final Logger log = LoggerFactory.getLogger(VurderUttakIBeregningSteg.class);

    private BehandlingRepository behandlingRepository;
    private MapInputTilUttakTjeneste mapInputTilUttakTjeneste;
    private UttakTjeneste uttakTjeneste;
    private EtablertTilsynTjeneste etablertTilsynTjeneste;
    private SamtidigUttakTjeneste samtidigUttakTjeneste;
    private UtsattBehandlingAvPeriodeRepository utsattBehandlingAvPeriodeRepository;
    private UttakNyeReglerRepository uttakNyeReglerRepository;
    private TilkommetAktivitetTjeneste tilkommetAktivitetTjeneste;

    private boolean brukDatoNyRegelUttak;

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
                                     UttakNyeReglerRepository uttakNyeReglerRepository,
                                     TilkommetAktivitetTjeneste tilkommetAktivitetTjeneste,
                                     @KonfigVerdi(value = "ENABLE_DATO_NY_REGEL_UTTAK", defaultVerdi = "false") boolean brukDatoNyRegelUttak) {
        this.behandlingRepository = behandlingRepository;
        this.mapInputTilUttakTjeneste = mapInputTilUttakTjeneste;
        this.uttakTjeneste = uttakTjeneste;
        this.etablertTilsynTjeneste = etablertTilsynTjeneste;
        this.samtidigUttakTjeneste = samtidigUttakTjeneste;
        this.utsattBehandlingAvPeriodeRepository = utsattBehandlingAvPeriodeRepository;
        this.uttakNyeReglerRepository = uttakNyeReglerRepository;
        this.tilkommetAktivitetTjeneste = tilkommetAktivitetTjeneste;
        this.brukDatoNyRegelUttak = brukDatoNyRegelUttak;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        var behandlingId = kontekst.getBehandlingId();
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var ref = BehandlingReferanse.fra(behandling);

        etablertTilsynTjeneste.opprettGrunnlagForTilsynstidlinje(ref);

        Optional<AksjonspunktDefinisjon> autopunktVentAnnenSak = håndteringAvSamtidigUttak(behandling, kontekst, ref);
        if (autopunktVentAnnenSak.isPresent()) {
            return BehandleStegResultat.utførtMedAksjonspunkter(List.of(autopunktVentAnnenSak.get()));
        }
        Optional<AksjonspunktDefinisjon> aksjonspunktSetteDatoNyeRegler = utledAksjonspunktDatoForNyeRegler(behandling);
        if (aksjonspunktSetteDatoNyeRegler.isPresent()) {
            return BehandleStegResultat.utførtMedAksjonspunkter(List.of(aksjonspunktSetteDatoNyeRegler.get()));
        }

        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

    private Optional<AksjonspunktDefinisjon> håndteringAvSamtidigUttak(Behandling behandling, BehandlingskontrollKontekst kontekst, BehandlingReferanse ref) {
        var kjøreplan = samtidigUttakTjeneste.utledPrioriteringsrekkefølge(ref);
        log.info("[Kjøreplan] annenSakSomMåBehandlesFørst={}, Har perioder uten prio={}, Perioder med prio={}", !kjøreplan.kanAktuellFagsakFortsette(),
            kjøreplan.perioderSomIkkeKanBehandlesForAktuellFagsak(),
            kjøreplan.perioderSomKanBehandlesForAktuellFagsak());

        if (kjøreplan.kanAktuellFagsakFortsette()) {
            var utsattePerioder = kjøreplan.perioderSomSkalUtsettesForAktuellFagsak();
            if (!utsattePerioder.isEmpty()) {
                log.info("[Kjøreplan] Utsettelse behandling av perioder {}", utsattePerioder);
            }

            utsattBehandlingAvPeriodeRepository.lagre(ref.getBehandlingId(), utsattePerioder.stream().map(UtsattPeriode::new).collect(Collectors.toSet()));

            final Uttaksgrunnlag oppdatertRequests = mapInputTilUttakTjeneste.hentUtOgMapRequest(ref);
            uttakTjeneste.opprettUttaksplan(oppdatertRequests);

            if (behandling.harÅpentAksjonspunktMedType(AksjonspunktDefinisjon.VENT_ANNEN_PSB_SAK)) {
                avbrytAksjonspunkt(behandling, kontekst);
            }
            return Optional.empty();
        } else {
            log.info("[Kjøreplan] Venter på behandling av andre fagsaker");
            return Optional.of(AksjonspunktDefinisjon.VENT_ANNEN_PSB_SAK);
        }
    }

    private Optional<AksjonspunktDefinisjon> utledAksjonspunktDatoForNyeRegler(Behandling behandling) {
        if (!brukDatoNyRegelUttak) {
            return Optional.empty();
        }
        final boolean førsteGangManuellRevurdering = behandling.erManueltOpprettet()
                && !behandling.harAksjonspunktMedType(AksjonspunktDefinisjon.VURDER_DATO_NY_REGEL_UTTAK);
        
        final boolean datoHarBlittSatt = uttakNyeReglerRepository.finnDatoForNyeRegler(behandling.getId()).isPresent();        
        if (datoHarBlittSatt && !førsteGangManuellRevurdering) {
            return Optional.empty();
        }
        if (datoHarBlittSatt && førsteGangManuellRevurdering) {
            // Reutled aksjonspunkt ved manuell revurdering.
            return Optional.of(AksjonspunktDefinisjon.VURDER_DATO_NY_REGEL_UTTAK);
        }
        
        /*
         * OBS: Vi må tillate at aksjonspunkt kan bli utledet ved manuell revurdering der
         * det ikke er noe aksjonspunkt der fra før.
         */
        return harAktivitetIkkeYrkesaktivEllerKunYtelse(behandling) || harTilkommmetAktivitet(behandling)
            ? Optional.of(AksjonspunktDefinisjon.VURDER_DATO_NY_REGEL_UTTAK)
            : Optional.empty();
    }

    private boolean harTilkommmetAktivitet(Behandling behandling) {
        //har ikke satt dato for nye regler i uttak (utleder AP for det her), så kan ikke begrense perioden (derav LocalDate.MIN)
        boolean harTilkommetAktivitet = !tilkommetAktivitetTjeneste.finnTilkommedeAktiviteter(behandling.getFagsakId(), LocalDate.MIN).isEmpty();
        log.info("Har {} tilkommet aktivitet", (harTilkommetAktivitet ? "" : "ikke"));
        return harTilkommetAktivitet;
    }

    private boolean harAktivitetIkkeYrkesaktivEllerKunYtelse(Behandling behandling) {
        Uttaksplan uttaksplan = uttakTjeneste.hentUttaksplan(behandling.getUuid(), false);
        return harEnAv(uttaksplan, Set.of(UttakArbeidType.KUN_YTELSE, UttakArbeidType.IKKE_YRKESAKTIV));
    }

    boolean harEnAv(Uttaksplan uttaksplan, Collection<UttakArbeidType> aktivitettyper) {
        for (UttaksperiodeInfo uttaksperiodeInfo : uttaksplan.getPerioder().values()) {
            for (Utbetalingsgrader utbetalingsgrader : uttaksperiodeInfo.getUtbetalingsgrader()) {
                for (UttakArbeidType aktivitettype : aktivitettyper) {
                    if (utbetalingsgrader.getArbeidsforhold().getType().equals(aktivitettype.getKode())) {
                        log.info("Har aktivitet IY/KY");
                        return true;
                    }
                }
            }
        }
        log.info("Har ikke aktivitet IY/KY");
        return false;
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
