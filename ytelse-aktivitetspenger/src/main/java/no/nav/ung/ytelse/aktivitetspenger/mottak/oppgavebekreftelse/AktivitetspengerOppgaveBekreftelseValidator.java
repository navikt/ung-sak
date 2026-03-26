package no.nav.ung.ytelse.aktivitetspenger.mottak.oppgavebekreftelse;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.dokument.Brevkode;
import no.nav.ung.sak.behandlingslager.behandling.motattdokument.MottattDokument;
import no.nav.ung.sak.mottak.dokumentmottak.DokumentGruppeRef;
import no.nav.ung.sak.mottak.dokumentmottak.DokumentValidator;
import no.nav.ung.sak.mottak.dokumentmottak.oppgavebekreftelse.OppgaveBekreftelseParser;

import java.util.Collection;
import java.util.Objects;

@ApplicationScoped
@DokumentGruppeRef(Brevkode.AKTIVITETSPENGER_VARSEL_UTTALELSE_KODE)
public class AktivitetspengerOppgaveBekreftelseValidator implements DokumentValidator {

    private OppgaveBekreftelseParser oppgaveBekreftelseParser;

    AktivitetspengerOppgaveBekreftelseValidator() {
        // CDI
    }

    @Inject
    public AktivitetspengerOppgaveBekreftelseValidator(OppgaveBekreftelseParser oppgaveBekreftelseParser) {
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
        if (!Objects.equals(Brevkode.AKTIVITETSPENGER_VARSEL_UTTALELSE, mottattDokument.getType())) {
            throw new IllegalArgumentException("Forventet brevkode: " + Brevkode.AKTIVITETSPENGER_VARSEL_UTTALELSE + ", fikk: " + mottattDokument.getType());
        }
        final var oppgaveBekreftelse = oppgaveBekreftelseParser.parseOppgaveBekreftelse(mottattDokument);
    }

}
