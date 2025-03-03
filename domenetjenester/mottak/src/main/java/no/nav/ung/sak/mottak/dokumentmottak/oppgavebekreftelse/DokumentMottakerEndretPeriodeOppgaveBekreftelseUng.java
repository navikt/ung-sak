package no.nav.ung.sak.mottak.dokumentmottak.oppgavebekreftelse;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.oppgave.bekreftelse.ung.periodeendring.DatoEndring;
import no.nav.k9.oppgave.bekreftelse.ung.periodeendring.EndretFomDatoBekreftelse;
import no.nav.k9.oppgave.bekreftelse.ung.periodeendring.EndretTomDatoBekreftelse;
import no.nav.ung.kodeverk.dokument.Brevkode;
import no.nav.ung.kodeverk.dokument.DokumentStatus;
import no.nav.ung.kodeverk.ungdomsytelse.periodeendring.UngdomsytelsePeriodeEndringType;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.motattdokument.MottattDokument;
import no.nav.ung.sak.behandlingslager.behandling.motattdokument.MottatteDokumentRepository;
import no.nav.ung.sak.behandlingslager.behandling.startdato.UngdomsytelseBekreftetPeriodeEndring;
import no.nav.ung.sak.behandlingslager.behandling.startdato.UngdomsytelseStartdatoRepository;
import no.nav.ung.sak.mottak.dokumentmottak.DokumentGruppeRef;
import no.nav.ung.sak.mottak.dokumentmottak.Dokumentmottaker;
import no.nav.ung.sak.mottak.dokumentmottak.Trigger;

import java.util.Collection;
import java.util.List;

import static no.nav.ung.kodeverk.behandling.FagsakYtelseType.UNGDOMSYTELSE;


@ApplicationScoped
@FagsakYtelseTypeRef(UNGDOMSYTELSE)
@DokumentGruppeRef(Brevkode.UNGDOMSYTELSE_BEKREFTELSE_PERIODEENDRING_KODE)
public class DokumentMottakerEndretPeriodeOppgaveBekreftelseUng implements Dokumentmottaker {

    private OppgaveBekreftelseParser oppgaveBekreftelseParser;
    private MottatteDokumentRepository mottatteDokumentRepository;
    private UngdomsytelseStartdatoRepository ungdomsytelseStartdatoRepository;

    public DokumentMottakerEndretPeriodeOppgaveBekreftelseUng() {
    }

    @Inject
    public DokumentMottakerEndretPeriodeOppgaveBekreftelseUng(OppgaveBekreftelseParser oppgaveBekreftelseParser,
                                                              MottatteDokumentRepository mottatteDokumentRepository,
                                                              UngdomsytelseStartdatoRepository ungdomsytelseStartdatoRepository) {
        this.oppgaveBekreftelseParser = oppgaveBekreftelseParser;
        this.mottatteDokumentRepository = mottatteDokumentRepository;
        this.ungdomsytelseStartdatoRepository = ungdomsytelseStartdatoRepository;
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
            DatoEndring bekreftelse = oppgaveBekreftelse.getBekreftelse();
            final var bekreftetPeriodeEndring = new UngdomsytelseBekreftetPeriodeEndring(
                bekreftelse.getNyDato(),
                dokument.getJournalpostId(),
                finnBekreftetPeriodeEndring(bekreftelse));

            ungdomsytelseStartdatoRepository.lagre(behandlingId, bekreftetPeriodeEndring);

            // TODO: Håndtere utvidelse av fagsakperiode dersom fom-dato kan flyttes lenger bak i tid (utvide periode med ytelse/fagsakperiode)

        }
        mottatteDokumentRepository.oppdaterStatus(mottattDokument.stream().toList(), DokumentStatus.GYLDIG);
    }

    private static UngdomsytelsePeriodeEndringType finnBekreftetPeriodeEndring(DatoEndring bekreftelse) {
        if (bekreftelse instanceof EndretTomDatoBekreftelse) {
            return UngdomsytelsePeriodeEndringType.ENDRET_OPPHØRSDATO;
        } else if (bekreftelse instanceof EndretFomDatoBekreftelse) {
            return UngdomsytelsePeriodeEndringType.ENDRET_STARTDATO;
        }
        throw new IllegalArgumentException("Kunne ikke håndtere bekreftelse av type " + bekreftelse.getType());
    }

    @Override
    public List<Trigger> getTriggere(Collection<MottattDokument> mottattDokument) {
        return List.of(); // Skal ikke generere ekstra trigger
    }

}
