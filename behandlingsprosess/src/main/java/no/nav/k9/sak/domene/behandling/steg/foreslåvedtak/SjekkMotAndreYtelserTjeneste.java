package no.nav.k9.sak.domene.behandling.steg.foreslåvedtak;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.historikk.HistorikkAktør;
import no.nav.k9.kodeverk.historikk.HistorikkinnslagType;
import no.nav.k9.kodeverk.produksjonsstyring.OppgaveÅrsak;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.k9.sak.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.k9.sak.behandlingslager.behandling.historikk.HistorikkinnslagDel;
import no.nav.k9.sak.domene.vedtak.ekstern.OverlappendeYtelserTjeneste;
import no.nav.k9.sak.historikk.HistorikkInnslagTekstBuilder;
import no.nav.k9.sak.produksjonsstyring.oppgavebehandling.OppgaveTjeneste;
import no.nav.k9.sak.typer.AktørId;

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
        return !behandling.getFagsakYtelseType().hentYtelserForOverlappSjekk().isEmpty();
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
        var overlappendeYtelser = overlappendeYtelserTjeneste.finnOverlappendeYtelser(BehandlingReferanse.fra(behandling));
        if (!overlappendeYtelser.isEmpty()) {
            String formattert = overlappendeYtelser.keySet().stream()
                .map(key -> key + "=" + overlappendeYtelser.get(key))
                .collect(Collectors.joining(", ", "{", "}"));
            logger.info("Behandling '{}' har overlappende ytelser '{}'", formattert);
        }
        return !overlappendeYtelser.isEmpty();
    }
}
