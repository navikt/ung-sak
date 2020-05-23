package no.nav.k9.sak.dokument.bestill;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.List;

import no.nav.k9.kodeverk.behandling.RevurderingVarslingÅrsak;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.Venteårsak;
import no.nav.k9.kodeverk.dokument.DokumentMalType;
import no.nav.k9.kodeverk.historikk.HistorikkAktør;
import no.nav.k9.kodeverk.produksjonsstyring.OppgaveÅrsak;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.vedtak.VedtakVarsel;
import no.nav.k9.sak.behandlingslager.behandling.vedtak.VedtakVarselRepository;
import no.nav.k9.sak.kontrakt.dokument.BestillBrevDto;
import no.nav.k9.sak.produksjonsstyring.oppgavebehandling.OppgaveBehandlingKobling;
import no.nav.k9.sak.produksjonsstyring.oppgavebehandling.OppgaveBehandlingKoblingRepository;
import no.nav.k9.sak.produksjonsstyring.oppgavebehandling.OppgaveTjeneste;

class VarselRevurderingHåndterer {

    private Period defaultVenteFrist;
    private DokumentBestillerApplikasjonTjeneste dokumentBestillerApplikasjonTjeneste;
    private OppgaveBehandlingKoblingRepository oppgaveBehandlingKoblingRepository;
    private OppgaveTjeneste oppgaveTjeneste;
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;
    private VedtakVarselRepository vedtakVarselRepository;

    VarselRevurderingHåndterer(Period defaultVenteFrist,
                               OppgaveBehandlingKoblingRepository oppgaveBehandlingKoblingRepository,
                               VedtakVarselRepository vedtakVarselRepository,
                               OppgaveTjeneste oppgaveTjeneste,
                               BehandlingskontrollTjeneste behandlingskontrollTjeneste,
                               DokumentBestillerApplikasjonTjeneste dokumentBestillerApplikasjonTjeneste) {
        this.defaultVenteFrist = defaultVenteFrist;
        this.oppgaveBehandlingKoblingRepository = oppgaveBehandlingKoblingRepository;
        this.vedtakVarselRepository = vedtakVarselRepository;
        this.oppgaveTjeneste = oppgaveTjeneste;
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
        this.dokumentBestillerApplikasjonTjeneste = dokumentBestillerApplikasjonTjeneste;
    }

    void oppdater(Behandling behandling, VarselRevurderingAksjonspunkt adapter) {
        Long behandlingId = behandling.getId();
        BestillBrevDto bestillBrevDto = new BestillBrevDto(behandlingId, DokumentMalType.REVURDERING_DOK, adapter.getFritekst());
        bestillBrevDto.setÅrsakskode(RevurderingVarslingÅrsak.ANNET.getKode());
        dokumentBestillerApplikasjonTjeneste.bestillDokument(bestillBrevDto, HistorikkAktør.SAKSBEHANDLER);
        
        settBehandlingPåVent(behandling, adapter.getFrist(), fraDto(adapter.getVenteÅrsakKode()), adapter.getBegrunnelse());
        registrerVarselOmRevurdering(behandlingId);
    }

    private void registrerVarselOmRevurdering(Long behandlingId) {
        var varsel = vedtakVarselRepository.hentHvisEksisterer(behandlingId).orElse(new VedtakVarsel());
        varsel.setHarSendtVarselOmRevurdering(true);
        vedtakVarselRepository.lagre(behandlingId, varsel);
    }

    private void settBehandlingPåVent(Behandling behandling, LocalDate frist, Venteårsak venteårsak, String venteårsakVariant) {
        opprettTaskAvsluttOppgave(behandling);
        behandlingskontrollTjeneste.settBehandlingPåVentUtenSteg(behandling, AksjonspunktDefinisjon.AUTO_MANUELT_SATT_PÅ_VENT,
            bestemFristForBehandlingVent(frist), venteårsak, venteårsakVariant);
    }

    private void opprettTaskAvsluttOppgave(Behandling behandling) {
        OppgaveÅrsak oppgaveÅrsak = behandling.erRevurdering() ? OppgaveÅrsak.REVURDER_VL : OppgaveÅrsak.BEHANDLE_SAK_VL;
        List<OppgaveBehandlingKobling> oppgaver = oppgaveBehandlingKoblingRepository.hentOppgaverRelatertTilBehandling(behandling.getId());
        if (OppgaveBehandlingKobling.getAktivOppgaveMedÅrsak(oppgaveÅrsak, oppgaver).isPresent()) {
            oppgaveTjeneste.opprettTaskAvsluttOppgave(behandling, oppgaveÅrsak);
        }
    }

    private LocalDateTime bestemFristForBehandlingVent(LocalDate frist) {
        return frist != null
            ? LocalDateTime.of(frist, LocalDateTime.now().toLocalTime())
            : LocalDateTime.now().plus(defaultVenteFrist);
    }

    private Venteårsak fraDto(String kode) {
        return Venteårsak.fraKode(kode);
    }
}
