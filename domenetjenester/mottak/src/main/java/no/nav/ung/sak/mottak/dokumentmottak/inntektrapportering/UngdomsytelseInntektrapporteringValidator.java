package no.nav.ung.sak.mottak.dokumentmottak.inntektrapportering;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.søknad.ytelse.ung.v1.UngdomsytelseSøknadValidator;
import no.nav.ung.kodeverk.dokument.Brevkode;
import no.nav.ung.sak.behandlingslager.behandling.motattdokument.MottattDokument;
import no.nav.ung.sak.mottak.dokumentmottak.DokumentGruppeRef;
import no.nav.ung.sak.mottak.dokumentmottak.DokumentValidator;
import no.nav.ung.sak.mottak.dokumentmottak.SøknadParser;

import java.util.Collection;
import java.util.Objects;

@ApplicationScoped
@DokumentGruppeRef(Brevkode.UNGDOMSYTELSE_INNTEKTRAPPORTERING_KODE)
public class UngdomsytelseInntektrapporteringValidator implements DokumentValidator {

    private SøknadParser søknadParser;

    UngdomsytelseInntektrapporteringValidator() {
        // CDI
    }

    @Inject
    public UngdomsytelseInntektrapporteringValidator(SøknadParser søknadParser) {
        this.søknadParser = søknadParser;
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
        if (!Objects.equals(Brevkode.UNGDOMSYTELSE_INNTEKTRAPPORTERING, mottattDokument.getType())) {
            throw new IllegalArgumentException("Forventet brevkode: " + Brevkode.UNGDOMSYTELSE_INNTEKTRAPPORTERING + ", fikk: " + mottattDokument.getType());
        }
        var søknad = søknadParser.parseSøknad(mottattDokument);
        new UngdomsytelseSøknadValidator().forsikreValidert(søknad);
    }


}
