package no.nav.ung.sak.mottak.dokumentmottak.oppgavebekreftelse;

import static no.nav.ung.kodeverk.behandling.FagsakYtelseType.UNGDOMSYTELSE;

import java.util.Collection;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.dokument.Brevkode;
import no.nav.ung.kodeverk.dokument.DokumentStatus;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.motattdokument.MottattDokument;
import no.nav.ung.sak.behandlingslager.behandling.motattdokument.MottatteDokumentRepository;
import no.nav.ung.sak.mottak.dokumentmottak.DokumentGruppeRef;
import no.nav.ung.sak.mottak.dokumentmottak.Dokumentmottaker;
import no.nav.ung.sak.mottak.dokumentmottak.HistorikkinnslagTjeneste;
import no.nav.ung.sak.mottak.dokumentmottak.Trigger;


@ApplicationScoped
@FagsakYtelseTypeRef(UNGDOMSYTELSE)
@DokumentGruppeRef(Brevkode.UNGDOMSYTELSE_OPPGAVE_BEKREFTELSE_KODE)
public class DokumentMottakerOppgaveBekreftelseUng implements Dokumentmottaker {

    private OppgaveBekreftelseParser oppgaveBekreftelseParser;
    private MottatteDokumentRepository mottatteDokumentRepository;
    private Instance<BekreftelseHåndterer> bekreftelseMottakere;
    private HistorikkinnslagTjeneste historikkinnslagTjeneste;

    public DokumentMottakerOppgaveBekreftelseUng() {
    }

    @Inject
    public DokumentMottakerOppgaveBekreftelseUng(OppgaveBekreftelseParser oppgaveBekreftelseParser,
                                                 MottatteDokumentRepository mottatteDokumentRepository,
                                                 @Any Instance<BekreftelseHåndterer> bekreftelseMottakere,
                                                 HistorikkinnslagTjeneste historikkinnslagTjeneste) {
        this.oppgaveBekreftelseParser = oppgaveBekreftelseParser;
        this.mottatteDokumentRepository = mottatteDokumentRepository;
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
                dokument.getJournalpostId(), behandling, oppgaveBekreftelse, dokument.getInnsendingstidspunkt(), dokument.getType()
            ));
            historikkinnslagTjeneste.opprettHistorikkinnslagForVedlegg(behandling.getFagsakId(), dokument.getJournalpostId());

        }
        mottatteDokumentRepository.oppdaterStatus(mottattDokument.stream().toList(), DokumentStatus.GYLDIG);
    }

    @Override
    public List<Trigger> getTriggere(Collection<MottattDokument> mottattDokument) {
        return List.of(); // Skal ikke generere ekstra trigger
    }

}
