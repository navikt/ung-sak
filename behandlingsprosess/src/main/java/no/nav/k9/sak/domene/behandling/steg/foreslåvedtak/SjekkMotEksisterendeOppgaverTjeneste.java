package no.nav.k9.sak.domene.behandling.steg.foreslåvedtak;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.historikk.HistorikkAktør;
import no.nav.k9.kodeverk.historikk.HistorikkinnslagType;
import no.nav.k9.kodeverk.produksjonsstyring.OppgaveÅrsak;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.k9.sak.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.k9.sak.behandlingslager.behandling.historikk.HistorikkinnslagDel;
import no.nav.k9.sak.historikk.HistorikkInnslagTekstBuilder;
import no.nav.k9.sak.produksjonsstyring.oppgavebehandling.OppgaveTjeneste;
import no.nav.k9.sak.produksjonsstyring.oppgavebehandling.Oppgaveinfo;
import no.nav.k9.sak.typer.AktørId;

@ApplicationScoped
public class SjekkMotEksisterendeOppgaverTjeneste {

    private HistorikkRepository historikkRepository;
    private OppgaveTjeneste oppgaveTjeneste;

    SjekkMotEksisterendeOppgaverTjeneste() {
        //CDI proxy
    }

    @Inject
    public SjekkMotEksisterendeOppgaverTjeneste(HistorikkRepository historikkRepository, OppgaveTjeneste oppgaveTjeneste) {
        this.historikkRepository = historikkRepository;
        this.oppgaveTjeneste = oppgaveTjeneste;
    }

    public List<AksjonspunktDefinisjon> sjekkMotEksisterendeGsakOppgaver(AktørId aktørid, Behandling behandling) {

        if (sjekkMotEksisterendeGsakOppgaverUtført(behandling)) {
            return new ArrayList<>();
        }

        List<Historikkinnslag> historikkInnslagFraRepo = historikkRepository.hentHistorikk(behandling.getId());
        List<AksjonspunktDefinisjon> aksjonspunktliste = new ArrayList<>();
        List<String> oppgaveÅrsakerVurder = Arrays.asList(OppgaveÅrsak.VURDER_DOKUMENT.getKode(),
            Oppgaveinfo.VURDER_KONST_YTELSE_FORELDREPENGER.getOppgaveType()); // FIXME : Tilpass for K9

        List<Oppgaveinfo> oppgaveinfos = oppgaveTjeneste.hentOppgaveListe(aktørid, oppgaveÅrsakerVurder);
        if (oppgaveinfos != null && !oppgaveinfos.isEmpty()) {
            if (oppgaveinfos.contains(Oppgaveinfo.VURDER_KONST_YTELSE_FORELDREPENGER)) {
                aksjonspunktliste.add(AksjonspunktDefinisjon.VURDERE_ANNEN_YTELSE_FØR_VEDTAK);
                opprettHistorikkinnslagOmVurderingFørVedtak(behandling, OppgaveÅrsak.VURDER_KONS_FOR_YTELSE, historikkInnslagFraRepo);
            }
            if (oppgaveinfos.contains(Oppgaveinfo.VURDER_DOKUMENT)) {
                aksjonspunktliste.add(AksjonspunktDefinisjon.VURDERE_DOKUMENT_FØR_VEDTAK);
                opprettHistorikkinnslagOmVurderingFørVedtak(behandling, OppgaveÅrsak.VURDER_DOKUMENT, historikkInnslagFraRepo);
            }
        }
        return aksjonspunktliste;
    }

    private boolean sjekkMotEksisterendeGsakOppgaverUtført(Behandling behandling) {
        return behandling.getAksjonspunkter().stream()
            .anyMatch(ap ->
                (ap.getAksjonspunktDefinisjon().equals(AksjonspunktDefinisjon.VURDERE_ANNEN_YTELSE_FØR_VEDTAK) && ap.erUtført())
                    || (ap.getAksjonspunktDefinisjon().equals(AksjonspunktDefinisjon.VURDERE_DOKUMENT_FØR_VEDTAK) && ap.erUtført()));
    }

    private void opprettHistorikkinnslagOmVurderingFørVedtak(Behandling behandling, OppgaveÅrsak begrunnelse, List<Historikkinnslag> historikkInnslagFraRepo) {
        // finne historikkinnslag hvor vi har en begrunnelse?
        List<Historikkinnslag> eksisterendeVurderHistInnslag = historikkInnslagFraRepo.stream()
            .filter(historikkinnslag -> {
                List<HistorikkinnslagDel> historikkinnslagDeler = historikkinnslag.getHistorikkinnslagDeler();
                return historikkinnslagDeler.stream().anyMatch(del -> del.getBegrunnelse().isPresent());
            })
            .collect(Collectors.toList());

        if (eksisterendeVurderHistInnslag.isEmpty()) {
            Historikkinnslag vurderFørVedtakInnslag = new Historikkinnslag();
            vurderFørVedtakInnslag.setType(HistorikkinnslagType.BEH_AVBRUTT_VUR);
            vurderFørVedtakInnslag.setAktør(HistorikkAktør.VEDTAKSLØSNINGEN);
            HistorikkInnslagTekstBuilder historikkInnslagTekstBuilder = new HistorikkInnslagTekstBuilder()
                .medHendelse(HistorikkinnslagType.BEH_AVBRUTT_VUR)
                .medBegrunnelse(begrunnelse);
            historikkInnslagTekstBuilder.build(vurderFørVedtakInnslag);
            vurderFørVedtakInnslag.setBehandling(behandling);
            historikkRepository.lagre(vurderFørVedtakInnslag);
        }
    }
}
