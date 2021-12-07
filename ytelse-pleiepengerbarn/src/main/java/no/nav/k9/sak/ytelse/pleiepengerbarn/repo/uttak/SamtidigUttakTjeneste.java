package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Optional;
import java.util.TreeSet;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.felles.konfigurasjon.env.Environment;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.BehandlingModell;
import no.nav.k9.sak.behandlingskontroll.impl.BehandlingModellRepository;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.domene.typer.tid.JsonObjectMapper;
import no.nav.k9.sak.ytelse.pleiepengerbarn.inngangsvilkår.søknadsfrist.PleietrengendeKravprioritet;
import no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.BekreftetUttakTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.input.MapInputTilUttakTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.tjeneste.UttakTjeneste;
import no.nav.pleiepengerbarn.uttak.kontrakter.LukketPeriode;
import no.nav.pleiepengerbarn.uttak.kontrakter.Simulering;
import no.nav.pleiepengerbarn.uttak.kontrakter.Simuleringsgrunnlag;
import no.nav.pleiepengerbarn.uttak.kontrakter.Uttaksgrunnlag;
import no.nav.pleiepengerbarn.uttak.kontrakter.Årsak;

@Dependent
public class SamtidigUttakTjeneste {

    private static final Logger log = LoggerFactory.getLogger(SamtidigUttakTjeneste.class);
    private MapInputTilUttakTjeneste mapInputTilUttakTjeneste;
    private UttakTjeneste uttakTjeneste;
    private FagsakRepository fagsakRepository;
    private BehandlingRepository behandlingRepository;
    private BehandlingModellRepository behandlingModellRepository;
    private PleietrengendeKravprioritet pleietrengendeKravprioritet;
    private BekreftetUttakTjeneste bekreftetUttakTjeneste;
    private Boolean enableAvslagBeregning;


    @Inject
    public SamtidigUttakTjeneste(MapInputTilUttakTjeneste mapInputTilUttakTjeneste,
                                 UttakTjeneste uttakTjeneste,
                                 FagsakRepository fagsakRepository,
                                 BehandlingRepository behandlingRepository,
                                 BehandlingModellRepository behandlingModellRepository,
                                 PleietrengendeKravprioritet pleietrengendeKravprioritet,
                                 BekreftetUttakTjeneste bekreftetUttakTjeneste,
                                 @KonfigVerdi(value = "psb.enable.bekreft.uttak", defaultVerdi = "false") Boolean benyttNyFlyt) {
        this.mapInputTilUttakTjeneste = mapInputTilUttakTjeneste;
        this.uttakTjeneste = uttakTjeneste;
        this.fagsakRepository = fagsakRepository;
        this.behandlingRepository = behandlingRepository;
        this.behandlingModellRepository = behandlingModellRepository;
        this.pleietrengendeKravprioritet = pleietrengendeKravprioritet;
        this.bekreftetUttakTjeneste = bekreftetUttakTjeneste;
        this.enableAvslagBeregning = benyttNyFlyt;
    }


    public boolean isAnnenSakSomMåBehandlesFørst(BehandlingReferanse ref) {
        final List<Fagsak> andreFagsaker = hentAndreFagsakerPåPleietrengende(ref);
        final List<Behandling> andreÅpneBehandlinger = åpneBehandlingerFra(andreFagsaker);

        if (andreÅpneBehandlinger.isEmpty()) {
            return false;
        }

        if (anyHarÅpenBehandlingSomIkkeHarKommetTilUttak(andreÅpneBehandlinger)) {
            /*
             * Krever at andre behandlinger kommer til uttak før vi går videre.
             *
             * TODO: Den beste løsningen er å se på kravprioritet om det er nødvendig å vente på
             *       uttak eller ikke. Hvis det må ventes må alle sakene behandles frem til vedtak.
             */
            return true;
        }

        if (!isEndringerMedUbesluttedeData(ref)) {
            return false;
        }

        if (anyHarÅpenBehandlingSomKanBesluttes(andreÅpneBehandlinger)) {
            return true;
        }

        return false;
    }

    private List<Fagsak> hentAndreFagsakerPåPleietrengende(BehandlingReferanse ref) {
        final List<Fagsak> fagsaker = fagsakRepository.finnFagsakRelatertTil(FagsakYtelseType.PLEIEPENGER_SYKT_BARN, ref.getPleietrengendeAktørId(), null, null, null);
        if (!fagsaker.stream().anyMatch(f -> f.getSaksnummer().equals(ref.getSaksnummer()))) {
            throw new IllegalStateException("Utviklerfeil: Klarte ikke å finne saken.");
        }
        return fagsaker.stream().filter(f -> !f.getSaksnummer().equals(ref.getSaksnummer())).toList();
    }

    private List<Behandling> åpneBehandlingerFra(final List<Fagsak> fagsaker) {
        return fagsaker.stream()
            .map(f -> behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(f.getId()))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .filter(b -> !b.getStatus().erFerdigbehandletStatus())
            .toList();

    }

    private boolean anyHarÅpenBehandlingSomIkkeHarKommetTilUttak(List<Behandling> åpneBehandlinger) {
        return åpneBehandlinger.stream().anyMatch(behandling -> {
            final BehandlingReferanse ref = BehandlingReferanse.fra(behandling);
            return !harKommetTilUttak(ref);
        });
    }

    private boolean anyHarÅpenBehandlingSomKanBesluttes(List<Behandling> åpneBehandlinger) {
        return åpneBehandlinger.stream().anyMatch(behandling -> {
            final BehandlingReferanse ref = BehandlingReferanse.fra(behandling);
            return !isEndringerMedUbesluttedeData(ref);
        });
    }

    /*
    private boolean harUbehandledePerioderMedLavereKravprioritet(BehandlingReferanse ref) {
        final LocalDateTimeline<List<Kravprioritet>> besluttetKravprioritet = pleietrengendeKravprioritet.vurderKravprioritet(ref.getFagsakId(), ref.getPleietrengendeAktørId(), false);
        final LocalDateTimeline<List<Kravprioritet>> ubesluttetKravprioritet = pleietrengendeKravprioritet.vurderKravprioritet(ref.getFagsakId(), ref.getPleietrengendeAktørId(), true);
        // Her legges det inn en utregning av hvilke nye perioder der man ikke har kravprio (dvs ikke høyeste prioritering) ... og så sjekkes disse mot perioderTilVurdering.
    }
    */

    public boolean isSkalHaTilbakehopp(BehandlingReferanse ref) {
        if (!harKommetTilUttak(ref)) {
            return false;
        }

        final Behandling behandling = behandlingRepository.hentBehandling(ref.getBehandlingId());
        final boolean harÅpentVenteaksjonspunkt = behandling.getÅpentAksjonspunktMedDefinisjonOptional(AksjonspunktDefinisjon.VENT_ANNEN_PSB_SAK).isPresent();

        final boolean annenSakSomMåBehandlesFørst = isAnnenSakSomMåBehandlesFørst(ref);
        if (annenSakSomMåBehandlesFørst && !harÅpentVenteaksjonspunkt) {
            // Send til steg for å sette på vent.
            return true;
        }

        if (!annenSakSomMåBehandlesFørst && harÅpentVenteaksjonspunkt) {
            // Fremtving at steget skal kjøres på nytt for å fjerne venting.
            return true;
        }

        // Det skal kun slippes én behandling gjennom.

        final Simulering simulering = simulerUttakKunBesluttet(ref);

        return simulering.getUttakplanEndret();
    }

    private boolean harKommetTilUttak(BehandlingReferanse ref) {
        final var behandling = behandlingRepository.hentBehandling(ref.getBehandlingId());
        final BehandlingStegType steg = behandling.getAktivtBehandlingSteg();
        final BehandlingModell modell = behandlingModellRepository.getModell(behandling.getType(), behandling.getFagsakYtelseType());
        return !modell.erStegAFørStegB(steg, BehandlingStegType.VURDER_UTTAK);
    }

    private boolean harKommetTilBeregning(BehandlingReferanse ref) {
        final var behandling = behandlingRepository.hentBehandling(ref.getBehandlingId());
        final BehandlingStegType steg = behandling.getAktivtBehandlingSteg();
        final BehandlingModell modell = behandlingModellRepository.getModell(behandling.getType(), behandling.getFagsakYtelseType());
        return !modell.erStegAFørStegB(steg, BehandlingStegType.VURDER_KOMPLETTHET_BEREGNING);
    }

    private boolean erPåUttakssteget(BehandlingReferanse ref) {
        final var behandling = behandlingRepository.hentBehandling(ref.getBehandlingId());
        final BehandlingStegType steg = behandling.getAktivtBehandlingSteg();
        return steg == BehandlingStegType.VURDER_UTTAK;
    }

    private Simulering simulerUttak(BehandlingReferanse ref) {
        final Uttaksgrunnlag uttaksgrunnlag = mapInputTilUttakTjeneste.hentUtUbesluttededataOgMapRequest(ref);

        if (enableAvslagBeregning) {
            Simuleringsgrunnlag simuleringsgrunnlag = byggRequest(ref, uttaksgrunnlag);
            var simulering = uttakTjeneste.simulerUttaksplanV2(simuleringsgrunnlag);
            if (enableAvslagBeregning && (Environment.current().isDev() || Environment.current().isLocal())) {
                try {
                    log.info("Simulering harForrigeUttaksplan={} endret={} \n respons: {}", (simulering.getForrigeUttaksplan() != null), simulering.getUttakplanEndret(), JsonObjectMapper.getJson(simulering));
                } catch (IOException e) {
                    log.info("Feil ved deserialisering. {}", e.getMessage());
                }
            }
            return simulering;
        } else {
            return uttakTjeneste.simulerUttaksplan(uttaksgrunnlag);
        }
    }

    private Simulering simulerUttakKunBesluttet(BehandlingReferanse ref) {
        final Uttaksgrunnlag uttaksGrunnlag = mapInputTilUttakTjeneste.hentUtOgMapRequest(ref);

        if (enableAvslagBeregning) {
            Simuleringsgrunnlag simuleringsgrunnlag = byggRequest(ref, uttaksGrunnlag);
            var simulering = uttakTjeneste.simulerUttaksplanV2(simuleringsgrunnlag);
            if (enableAvslagBeregning && (Environment.current().isDev() || Environment.current().isLocal())) {
                try {
                    log.info("Simulering harForrigeUttaksplan={} endret={} \n respons: {}", (simulering.getForrigeUttaksplan() != null), simulering.getUttakplanEndret(), JsonObjectMapper.getJson(simulering));
                } catch (IOException e) {
                    log.info("Feil ved deserialisering. {}", e.getMessage());
                }
            }
            return simulering;
        } else {
            return uttakTjeneste.simulerUttaksplan(uttaksGrunnlag);
        }
    }

    private Simuleringsgrunnlag byggRequest(BehandlingReferanse ref, Uttaksgrunnlag uttaksGrunnlag) {
        NavigableSet<DatoIntervallEntitet> avslåttePerioderIBeregning = new TreeSet<>();
        if (enableAvslagBeregning && harKommetTilBeregning(ref)) {
            avslåttePerioderIBeregning = bekreftetUttakTjeneste.utledPerioderTilVurderingSomBlittAvslåttIBeregning(ref.getBehandlingId());
            log.info("Simulering avslåtteperioder={}", avslåttePerioderIBeregning);
        }

        return new Simuleringsgrunnlag(uttaksGrunnlag, opprettMap(avslåttePerioderIBeregning));
    }

    private Map<LukketPeriode, Årsak> opprettMap(NavigableSet<DatoIntervallEntitet> perioderSomHarBlittAvslått) {
        var map = new HashMap<LukketPeriode, Årsak>();
        for (DatoIntervallEntitet periode : perioderSomHarBlittAvslått) {
            map.put(new LukketPeriode(periode.getFomDato(), periode.getTomDato()), Årsak.FOR_LAV_INNTEKT);
        }
        return map;
    }

    public boolean isEndringerMedUbesluttedeData(BehandlingReferanse ref) {
        final Simulering simulering = simulerUttak(ref);
        // Hvis en sak ikke har kommet til uttak betyr det at true returneres her.
        if (enableAvslagBeregning && (Environment.current().isDev() || Environment.current().isLocal())) {
            log.info("Simulering harForrigeUttaksplan={} endret={}", (simulering.getForrigeUttaksplan() != null), simulering.getUttakplanEndret());
        }
        return simulering.getUttakplanEndret();
    }
}
