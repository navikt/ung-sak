package no.nav.ung.sak.domene.behandling.steg.registerinntektkontroll;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.ung.kodeverk.behandling.BehandlingStegType;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandlingskontroll.*;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.ytelse.kontroll.KontrollerteInntektperioderTjeneste;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.List;

import static no.nav.ung.kodeverk.behandling.BehandlingStegType.KONTROLLER_REGISTER_INNTEKT;
import static no.nav.ung.kodeverk.behandling.FagsakYtelseType.UNGDOMSYTELSE;

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
                                 @KonfigVerdi(value = "AKSEPTERT_DIFFERANSE_KONTROLL", defaultVerdi = "100") int akseptertDifferanse) {
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
        kontrollerteInntektperioderTjeneste.ryddPerioderFritattForKontroll(behandlingId);
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var behandlingReferanse = BehandlingReferanse.fra(behandling);
        var input = inputMapper.mapInput(behandlingReferanse);
        var kontrollResultat = new KontrollerInntektTjeneste(BigDecimal.valueOf(akseptertDifferanse)).utførKontroll(input);

        log.info("Kontrollresultat ble {}", kontrollResultat.toSegments());
        // Oppretter kontrollerte perioder
        opprettKontrollerteInntektPerioder(kontekst, kontrollResultat);

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
                                                    LocalDateTimeline<Kontrollresultat> kontrollResultat) {
        kontrollResultat.filterValue(it -> it.type() == KontrollResultatType.BRUK_GODKJENT_ELLER_RAPPORTERT_INNTEKT_FRA_BRUKER)
            .toSegments()
            .forEach(segment -> {
                log.info("Bruker inntekt fra bruker eller godkjent inntekt fra register for periode {}", segment.getLocalDateInterval());
                kontrollerteInntektperioderTjeneste.opprettKontrollerteInntekterPerioderFraBruker(
                    kontekst.getBehandlingId(),
                    segment.getLocalDateInterval(),
                    segment.getValue().inntektsresultat()
                );
            });
    }

}
