package no.nav.ung.sak.mottak.dokumentmottak.oppgavebekreftelse;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.oppgave.OppgaveBekreftelse;
import no.nav.ung.kodeverk.dokument.Brevkode;
import no.nav.ung.sak.behandlingslager.behandling.motattdokument.MottattDokument;
import no.nav.ung.sak.mottak.dokumentmottak.DokumentGruppeRef;
import no.nav.ung.sak.mottak.dokumentmottak.DokumentValidator;

import java.util.Collection;
import java.util.Objects;

@ApplicationScoped
@DokumentGruppeRef(Brevkode.UNGDOMSYTELSE_BEKREFTELSE_PERIODEENDRING_KODE)
public class UngdomsytelseEndretPeriodeOppgaveBekreftelseValidator implements DokumentValidator {

    private OppgaveBekreftelseParser oppgaveBekreftelseParser;

    UngdomsytelseEndretPeriodeOppgaveBekreftelseValidator() {
        // CDI
    }

    @Inject
    public UngdomsytelseEndretPeriodeOppgaveBekreftelseValidator(OppgaveBekreftelseParser oppgaveBekreftelseParser) {
        this.oppgaveBekreftelseParser = oppgaveBekreftelseParser;
    }

    @Override
    public void validerDokumenter(Long behandlingId, Collection<MottattDokument> mottatteDokumenter) {
        for (MottattDokument mottattDokument : mottatteDokumenter) {
            validerDokument(mottattDokument);
        }
    }

    @Override
    public void validerDokument(MottattDokument mottattDokument) {
        Objects.requireNonNull(mottattDokument);
        if (!Objects.equals(Brevkode.UNGDOMSYTELSE_BEKREFTELSE_PERIODEENDRING, mottattDokument.getType())) {
            throw new IllegalArgumentException("Forventet brevkode: " + Brevkode.UNGDOMSYTELSE_BEKREFTELSE_PERIODEENDRING + ", fikk: " + mottattDokument.getType());
        }
        // TODO: Gjer valdering
        final var oppgaveBekreftelse = oppgaveBekreftelseParser.parseOppgaveBekreftelse(mottattDokument);

    }


}
