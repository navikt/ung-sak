package no.nav.ung.sak.domene.behandling.steg.registerinntektkontroll;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.ung.kodeverk.behandling.BehandlingStegType;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandlingskontroll.*;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.domene.typer.tid.JsonObjectMapper;
import no.nav.ung.sak.ytelse.kontroll.KontrollerteInntektperioderTjeneste;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

import static no.nav.ung.kodeverk.behandling.BehandlingStegType.KONTROLLER_REGISTER_INNTEKT;
import static no.nav.ung.kodeverk.behandling.FagsakYtelseType.UNGDOMSYTELSE;

/**
 * Fullfører kontroll av inntekt basert på rapporterte inntekter og svar på etterlysninger. Steget oppretter perioder for kontrollert inntekt dersom det enten ikke er avvik eller bruker har godkjent bruk av registerinntekt.
 * Steget returnerer aksjonspunkt dersom bruker ikke har godkjent registerinntekt eller dersom det er bruker har rapportert inntekt uten at det er inntekt i registeret.
 */
@ApplicationScoped
@BehandlingStegRef(value = KONTROLLER_REGISTER_INNTEKT)
@BehandlingTypeRef
@FagsakYtelseTypeRef(UNGDOMSYTELSE)
public class KontrollerInntektSteg implements BehandlingSteg {

    private static final Logger log = LoggerFactory.getLogger(KontrollerInntektSteg.class);
    private KontrollerteInntektperioderTjeneste kontrollerteInntektperioderTjeneste;
    private BehandlingRepository behandlingRepository;
    private KontrollerInntektInputMapper inputMapper;
    private int akseptertDifferanse;

    @Inject
    public KontrollerInntektSteg(KontrollerteInntektperioderTjeneste kontrollerteInntektperioderTjeneste,
                                 BehandlingRepository behandlingRepository,
                                 KontrollerInntektInputMapper inputMapper,
                                 @KonfigVerdi(value = "AKSEPTERT_DIFFERANSE_KONTROLL", defaultVerdi = "15") int akseptertDifferanse) {
        this.kontrollerteInntektperioderTjeneste = kontrollerteInntektperioderTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.inputMapper = inputMapper;
        this.akseptertDifferanse = akseptertDifferanse;
    }

    public KontrollerInntektSteg() {
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        Long behandlingId = kontekst.getBehandlingId();
        kontrollerteInntektperioderTjeneste.ryddPerioderFritattForKontrollEllerTilVurderingIBehandlingen(behandlingId);


        var behandling = behandlingRepository.hentBehandling(behandlingId);

        if (!erKontrollbehandling(behandling)) {
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        }

        Fagsak fagsak = behandling.getFagsak();
        boolean ikkeDigitalBruker = fagsak.erIkkeDigitalBruker();

        // Ved ikke-digital bruker opprettes aksjonspunkt for manuell kontroll av inntekt alltid.
        if (ikkeDigitalBruker) {
            log.info("Fagsak {} gjelder ikke-digital bruker, oppretter aksjonspunkt for manuell kontroll av inntekt.", fagsak.getSaksnummer());
            return BehandleStegResultat.utførtMedAksjonspunkter(List.of(AksjonspunktDefinisjon.KONTROLLER_INNTEKT));
        }

        var behandlingReferanse = BehandlingReferanse.fra(behandling);
        var input = inputMapper.mapInput(behandlingReferanse);
        var kontrollResultat = new KontrollerInntektTjeneste(BigDecimal.valueOf(akseptertDifferanse)).utførKontroll(input);

        log.info("Kontrollresultat ble {}", kontrollResultat.toSegments());
        // Oppretter kontrollerte perioder
        opprettKontrollerteInntektPerioder(kontekst, kontrollResultat, input);

        // Oppretter aksjonspunkt
        final var skalOppretteAksjonspunkt = !kontrollResultat.filterValue(it -> it.type().equals(KontrollResultatType.OPPRETT_AKSJONSPUNKT)).isEmpty();
        if (skalOppretteAksjonspunkt) {
            return BehandleStegResultat.utførtMedAksjonspunkter(List.of(AksjonspunktDefinisjon.KONTROLLER_INNTEKT));
        }
        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

    @Override
    public void vedHoppOverBakover(BehandlingskontrollKontekst kontekst, BehandlingStegModell modell,
                                   BehandlingStegType tilSteg, BehandlingStegType fraSteg) {
        if (!tilSteg.equals(KONTROLLER_REGISTER_INNTEKT)) {
            final var behandlingId = kontekst.getBehandlingId();
            final var behandling = behandlingRepository.hentBehandling(behandlingId);
            if (behandling.getOriginalBehandlingId().isPresent()) {
                kontrollerteInntektperioderTjeneste.gjenopprettTilOriginal(behandling.getOriginalBehandlingId().get(), behandlingId);
            }
        }
    }

    private void opprettKontrollerteInntektPerioder(BehandlingskontrollKontekst kontekst,
                                                    LocalDateTimeline<Kontrollresultat> kontrollResultat,
                                                    KontrollerInntektInput input) {
        var ferdigKontrollertTidslinje = kontrollResultat.filterValue(it -> it.type() == KontrollResultatType.FERDIG_KONTROLLERT);
        var aksjonspunktTidslinje = kontrollResultat.filterValue(it -> it.type() == KontrollResultatType.OPPRETT_AKSJONSPUNKT);

        log.info("Bruker inntekt fra bruker eller godkjent inntekt fra register for perioder {}", ferdigKontrollertTidslinje);
        if (!aksjonspunktTidslinje.isEmpty()) {
            log.info("Forkaster tidligere vurderte perioder pga aksjonspunkt {}", aksjonspunktTidslinje);
        }
        try {
            kontrollerteInntektperioderTjeneste.opprettKontrollerteInntekterPerioderFraBruker(
                kontekst.getBehandlingId(),
                ferdigKontrollertTidslinje.mapValue(Kontrollresultat::inntektsresultat),
                input.gjeldendeRapporterteInntekter(),
                JsonObjectMapper.getJson(input),
                JsonObjectMapper.getJson(kontrollResultat),
                aksjonspunktTidslinje.mapValue(_ -> true)
            );
        } catch (IOException e) {
            throw new IllegalStateException("Kunn ikke serialisere input eller kontrollresultat til JSON", e);
        }
    }

    private static boolean erKontrollbehandling(Behandling behandling) {
        return behandling.getBehandlingÅrsakerTyper().contains(BehandlingÅrsakType.RE_KONTROLL_REGISTER_INNTEKT);
    }

}
