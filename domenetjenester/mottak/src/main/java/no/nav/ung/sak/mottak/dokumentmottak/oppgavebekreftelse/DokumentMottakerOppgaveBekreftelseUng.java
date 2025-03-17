package no.nav.ung.sak.mottak.dokumentmottak.oppgavebekreftelse;

import static no.nav.ung.kodeverk.behandling.FagsakYtelseType.UNGDOMSYTELSE;

import java.util.Collection;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.k9.oppgave.bekreftelse.ung.periodeendring.DatoEndring;
import no.nav.k9.oppgave.bekreftelse.ung.periodeendring.EndretFomDatoBekreftelse;
import no.nav.k9.oppgave.bekreftelse.ung.periodeendring.EndretTomDatoBekreftelse;
import no.nav.ung.kodeverk.dokument.Brevkode;
import no.nav.ung.kodeverk.dokument.DokumentStatus;
import no.nav.ung.kodeverk.ungdomsytelse.periodeendring.UngdomsprogramPeriodeEndringType;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.motattdokument.MottattDokument;
import no.nav.ung.sak.behandlingslager.behandling.motattdokument.MottatteDokumentRepository;
import no.nav.ung.sak.behandlingslager.behandling.startdato.UngdomsytelseStartdatoRepository;
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
    private UngdomsytelseStartdatoRepository ungdomsytelseStartdatoRepository;
    private HistorikkinnslagTjeneste historikkinnslagTjeneste;
    private Instance<BekreftelseHåndterer> bekreftelseMottakere;

    public DokumentMottakerOppgaveBekreftelseUng() {
    }

    @Inject
    public DokumentMottakerOppgaveBekreftelseUng(OppgaveBekreftelseParser oppgaveBekreftelseParser,
                                                 MottatteDokumentRepository mottatteDokumentRepository,
                                                 UngdomsytelseStartdatoRepository ungdomsytelseStartdatoRepository,
                                                 HistorikkinnslagTjeneste historikkinnslagTjeneste,
                                                 Instance<BekreftelseHåndterer> bekreftelseMottakere
    ) {
        this.oppgaveBekreftelseParser = oppgaveBekreftelseParser;
        this.mottatteDokumentRepository = mottatteDokumentRepository;
        this.ungdomsytelseStartdatoRepository = ungdomsytelseStartdatoRepository;
        this.historikkinnslagTjeneste = historikkinnslagTjeneste;
        this.bekreftelseMottakere = bekreftelseMottakere;
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
                dokument.getJournalpostId(), behandling, oppgaveBekreftelse
            ));
        }
        mottatteDokumentRepository.oppdaterStatus(mottattDokument.stream().toList(), DokumentStatus.GYLDIG);
    }

    private static UngdomsprogramPeriodeEndringType finnBekreftetPeriodeEndring(DatoEndring bekreftelse) {
        if (bekreftelse instanceof EndretTomDatoBekreftelse) {
            return UngdomsprogramPeriodeEndringType.ENDRET_OPPHØRSDATO;
        } else if (bekreftelse instanceof EndretFomDatoBekreftelse) {
            return UngdomsprogramPeriodeEndringType.ENDRET_STARTDATO;
        }
        throw new IllegalArgumentException("Kunne ikke håndtere bekreftelse av type " + bekreftelse.getType());
    }

    @Override
    public List<Trigger> getTriggere(Collection<MottattDokument> mottattDokument) {
        return List.of(); // Skal ikke generere ekstra trigger
    }

}
