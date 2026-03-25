package no.nav.ung.ytelse.aktivitetspenger.mottak.oppgavebekreftelse;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.dokument.Brevkode;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.motattdokument.MottattDokument;
import no.nav.ung.sak.mottak.dokumentmottak.DokumentGruppeRef;
import no.nav.ung.sak.mottak.dokumentmottak.Dokumentmottaker;
import no.nav.ung.sak.mottak.dokumentmottak.HistorikkinnslagTjeneste;
import no.nav.ung.sak.mottak.dokumentmottak.Trigger;
import no.nav.ung.sak.mottak.dokumentmottak.oppgavebekreftelse.BekreftelseHåndterer;
import no.nav.ung.sak.mottak.dokumentmottak.oppgavebekreftelse.OppgaveBekreftelseInnhold;
import no.nav.ung.sak.mottak.dokumentmottak.oppgavebekreftelse.OppgaveBekreftelseParser;
import no.nav.ung.sak.mottak.dokumentmottak.oppgavebekreftelse.OppgaveTypeRef;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static no.nav.ung.kodeverk.behandling.FagsakYtelseType.AKTIVITETSPENGER;


@ApplicationScoped
@FagsakYtelseTypeRef(AKTIVITETSPENGER)
@DokumentGruppeRef(Brevkode.AKTIVITETSPENGER_VARSEL_UTTALELSE_KODE)
public class DokumentMottakerOppgaveBekreftelseAkt implements Dokumentmottaker {

    private OppgaveBekreftelseParser oppgaveBekreftelseParser;
    private Instance<BekreftelseHåndterer> bekreftelseMottakere;
    private HistorikkinnslagTjeneste historikkinnslagTjeneste;

    public DokumentMottakerOppgaveBekreftelseAkt() {
    }

    @Inject
    public DokumentMottakerOppgaveBekreftelseAkt(OppgaveBekreftelseParser oppgaveBekreftelseParser,
                                                 @Any Instance<BekreftelseHåndterer> bekreftelseMottakere,
                                                 HistorikkinnslagTjeneste historikkinnslagTjeneste) {
        this.oppgaveBekreftelseParser = oppgaveBekreftelseParser;
        this.bekreftelseMottakere = bekreftelseMottakere;
        this.historikkinnslagTjeneste = historikkinnslagTjeneste;
    }

    @Override
    public void lagreDokumentinnhold(Collection<MottattDokument> mottattDokument, Behandling behandling) {
        var behandlingId = behandling.getId();
        for (MottattDokument dokument : mottattDokument) {
            var oppgaveBekreftelse = oppgaveBekreftelseParser.parseOppgaveBekreftelse(dokument);
            dokument.setBehandlingId(behandlingId);
            dokument.setInnsendingstidspunkt(oppgaveBekreftelse.getMottattDato().toLocalDateTime());
            if (oppgaveBekreftelse.getKildesystem().isPresent()) {
                dokument.setKildesystem(oppgaveBekreftelse.getKildesystem().get().getKode());
            }

            BekreftelseHåndterer bekreftelseHåndterer = bekreftelseMottakere
                .select(new OppgaveTypeRef.OppgaveTypeRefLiteral(oppgaveBekreftelse.getBekreftelse().getType()))
                .get();

            bekreftelseHåndterer.håndter(new OppgaveBekreftelseInnhold(
                dokument, behandling, oppgaveBekreftelse, dokument.getType()
            ));
            historikkinnslagTjeneste.opprettHistorikkinnslagForVedlegg(behandling.getFagsakId(), behandlingId, dokument.getJournalpostId());
        }
    }

    @Override
    public List<Trigger> getTriggere(Collection<MottattDokument> mottattDokument) {
        final var triggers = new ArrayList<Trigger>();
        for (MottattDokument dokument : mottattDokument) {
            var oppgaveBekreftelse = oppgaveBekreftelseParser.parseOppgaveBekreftelse(dokument);
            final var oppgaveId = oppgaveBekreftelse.getBekreftelse().getOppgaveReferanse();
            BekreftelseHåndterer bekreftelseHåndterer = bekreftelseMottakere
                .select(new OppgaveTypeRef.OppgaveTypeRefLiteral(oppgaveBekreftelse.getBekreftelse().getType()))
                .get();
            bekreftelseHåndterer.utledTrigger(oppgaveId).ifPresent(triggers::add);
        }
        return triggers;
    }

}
