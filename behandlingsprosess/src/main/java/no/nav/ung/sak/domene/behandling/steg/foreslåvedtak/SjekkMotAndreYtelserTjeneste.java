package no.nav.ung.sak.domene.behandling.steg.foreslåvedtak;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.kodeverk.historikk.HistorikkAktør;
import no.nav.ung.kodeverk.historikk.HistorikkinnslagType;
import no.nav.ung.kodeverk.produksjonsstyring.OppgaveÅrsak;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.ung.sak.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.ung.sak.behandlingslager.behandling.historikk.HistorikkinnslagDel;
import no.nav.ung.sak.domene.vedtak.ekstern.OverlappendeYtelserTjeneste;
import no.nav.ung.sak.historikk.HistorikkInnslagTekstBuilder;
import no.nav.ung.sak.produksjonsstyring.oppgavebehandling.OppgaveTjeneste;
import no.nav.ung.sak.typer.AktørId;

@ApplicationScoped
public class SjekkMotAndreYtelserTjeneste {

    private static final Logger logger = LoggerFactory.getLogger(SjekkMotAndreYtelserTjeneste.class);

    private HistorikkRepository historikkRepository;
    private OppgaveTjeneste oppgaveTjeneste;
    private OverlappendeYtelserTjeneste overlappendeYtelserTjeneste;

    SjekkMotAndreYtelserTjeneste() {
        //CDI proxy
    }

    @Inject
    public SjekkMotAndreYtelserTjeneste(HistorikkRepository historikkRepository, OppgaveTjeneste oppgaveTjeneste, OverlappendeYtelserTjeneste overlappendeYtelserTjeneste) {
        this.historikkRepository = historikkRepository;
        this.oppgaveTjeneste = oppgaveTjeneste;
        this.overlappendeYtelserTjeneste = overlappendeYtelserTjeneste;
    }

    public List<AksjonspunktDefinisjon> sjekkMotGsakOppgaverOgOverlappendeYtelser(AktørId aktørid, Behandling behandling) {
        List<Historikkinnslag> historikkInnslagFraRepo = historikkRepository.hentHistorikk(behandling.getId());
        List<AksjonspunktDefinisjon> aksjonspunktliste = new ArrayList<>();

        if (skalSjekkeGsakOppgaver(behandling)) {
            if (oppgaveTjeneste.harÅpneOppgaverAvType(aktørid, OppgaveÅrsak.VURDER_KONSEKVENS_YTELSE, behandling.getFagsakYtelseType())) {
                aksjonspunktliste.add(AksjonspunktDefinisjon.VURDERE_ANNEN_YTELSE_FØR_VEDTAK);
                opprettHistorikkinnslagOmVurderingFørVedtak(behandling, HistorikkinnslagType.BEH_AVBRUTT_VUR, OppgaveÅrsak.VURDER_KONSEKVENS_YTELSE, historikkInnslagFraRepo);
            }
            if (oppgaveTjeneste.harÅpneOppgaverAvType(aktørid, OppgaveÅrsak.VURDER_DOKUMENT, behandling.getFagsakYtelseType())) {
                aksjonspunktliste.add(AksjonspunktDefinisjon.VURDERE_DOKUMENT_FØR_VEDTAK);
                opprettHistorikkinnslagOmVurderingFørVedtak(behandling, HistorikkinnslagType.BEH_AVBRUTT_VUR, OppgaveÅrsak.VURDER_DOKUMENT, historikkInnslagFraRepo);
            }
        }

        if (skalSjekkeOverlappendeYtelser(behandling)) {
            if (harOverlappendeYtelser(behandling)) {
                aksjonspunktliste.add(AksjonspunktDefinisjon.VURDERE_OVERLAPPENDE_YTELSER_FØR_VEDTAK);
                opprettHistorikkinnslagOmVurderingFørVedtak(behandling, HistorikkinnslagType.BEH_AVBRUTT_OVERLAPP, OppgaveÅrsak.VURDER_KONSEKVENS_YTELSE, historikkInnslagFraRepo);
            }
        }
        return aksjonspunktliste;
    }

    private boolean skalSjekkeGsakOppgaver(Behandling behandling) {
        if (!behandling.getFagsakYtelseType().vurderÅpneOppgaverFørVedtak()) {
            return false;
        }
        boolean sjekkMotÅpneGsakOppgaverUtført = behandling.getAksjonspunkter().stream()
            .anyMatch(ap ->
                (ap.getAksjonspunktDefinisjon().equals(AksjonspunktDefinisjon.VURDERE_ANNEN_YTELSE_FØR_VEDTAK) && ap.erUtført())
                    || (ap.getAksjonspunktDefinisjon().equals(AksjonspunktDefinisjon.VURDERE_DOKUMENT_FØR_VEDTAK) && ap.erUtført()));
        return !sjekkMotÅpneGsakOppgaverUtført;
    }

    private boolean skalSjekkeOverlappendeYtelser(Behandling behandling) {
        return !behandling.getFagsakYtelseType().hentK9YtelserForOverlappSjekk().isEmpty();
    }

    private void opprettHistorikkinnslagOmVurderingFørVedtak(Behandling behandling, HistorikkinnslagType historikkInnslagsType, OppgaveÅrsak begrunnelse, List<Historikkinnslag> historikkInnslagFraRepo) {
        // finne historikkinnslag hvor vi har en begrunnelse?
        List<Historikkinnslag> eksisterendeVurderHistInnslag = historikkInnslagFraRepo.stream()
            .filter(historikkinnslag -> {
                List<HistorikkinnslagDel> historikkinnslagDeler = historikkinnslag.getHistorikkinnslagDeler();
                return historikkinnslagDeler.stream().anyMatch(del -> del.getBegrunnelse().isPresent());
            })
            .collect(Collectors.toList());

        if (eksisterendeVurderHistInnslag.isEmpty()) {
            Historikkinnslag vurderFørVedtakInnslag = new Historikkinnslag();
            vurderFørVedtakInnslag.setType(historikkInnslagsType);
            vurderFørVedtakInnslag.setAktør(HistorikkAktør.VEDTAKSLØSNINGEN);
            HistorikkInnslagTekstBuilder historikkInnslagTekstBuilder = new HistorikkInnslagTekstBuilder()
                .medHendelse(historikkInnslagsType)
                .medBegrunnelse(begrunnelse);
            historikkInnslagTekstBuilder.build(vurderFørVedtakInnslag);
            vurderFørVedtakInnslag.setBehandling(behandling);
            historikkRepository.lagre(vurderFørVedtakInnslag);
        }
    }


    private boolean harOverlappendeYtelser(Behandling behandling) {
        var ytelseTyperSomSjekkesMot = behandling.getFagsakYtelseType().hentK9YtelserForOverlappSjekk();
        var overlappendeYtelser = overlappendeYtelserTjeneste.finnOverlappendeYtelser(BehandlingReferanse.fra(behandling), ytelseTyperSomSjekkesMot);
        if (!overlappendeYtelser.isEmpty()) {
            String formattert = overlappendeYtelser.keySet().stream()
                .map(key -> "Ytelse=" + key.getYtelseType() + ", kilde=" + key.getKilde() + ", saksnummer=" + key.getSaksnummer() + ", antall ytelser anvist=" + key.getYtelseAnvist().size() + ", perioder=" + overlappendeYtelser.get(key).getLocalDateIntervals())
                .collect(Collectors.joining(", ", "{", "}"));
            logger.info("Behandlingen har overlappende ytelser '{}'", formattert);
        }
        return !overlappendeYtelser.isEmpty();
    }
}
