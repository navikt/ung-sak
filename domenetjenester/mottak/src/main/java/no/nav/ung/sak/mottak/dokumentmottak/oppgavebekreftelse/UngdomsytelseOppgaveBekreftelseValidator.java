package no.nav.ung.sak.mottak.dokumentmottak.oppgavebekreftelse;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.dokument.Brevkode;
import no.nav.ung.sak.behandlingslager.behandling.motattdokument.MottattDokument;
import no.nav.ung.sak.mottak.dokumentmottak.DokumentGruppeRef;
import no.nav.ung.sak.mottak.dokumentmottak.DokumentValidator;

import java.util.Collection;
import java.util.Objects;

@ApplicationScoped
@DokumentGruppeRef(Brevkode.UNGDOMSYTELSE_VARSEL_UTTALELSE_KODE)
public class UngdomsytelseOppgaveBekreftelseValidator implements DokumentValidator {

    private OppgaveBekreftelseParser oppgaveBekreftelseParser;

    UngdomsytelseOppgaveBekreftelseValidator() {
        // CDI
    }

    @Inject
    public UngdomsytelseOppgaveBekreftelseValidator(OppgaveBekreftelseParser oppgaveBekreftelseParser) {
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
        if (!Objects.equals(Brevkode.UNGDOMSYTELSE_VARSEL_UTTALELSE, mottattDokument.getType())) {
            throw new IllegalArgumentException("Forventet brevkode: " + Brevkode.UNGDOMSYTELSE_VARSEL_UTTALELSE + ", fikk: " + mottattDokument.getType());
        }
        // TODO: Gjer valdering
        final var oppgaveBekreftelse = oppgaveBekreftelseParser.parseOppgaveBekreftelse(mottattDokument);

    }


}
