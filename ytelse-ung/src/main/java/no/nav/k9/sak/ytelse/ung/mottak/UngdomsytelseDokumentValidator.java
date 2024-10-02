package no.nav.k9.sak.ytelse.ung.mottak;

import java.util.Collection;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.kodeverk.dokument.Brevkode;
import no.nav.k9.sak.behandlingslager.behandling.motattdokument.MottattDokument;
import no.nav.k9.sak.mottak.dokumentmottak.DokumentGruppeRef;
import no.nav.k9.sak.mottak.dokumentmottak.DokumentValidator;
import no.nav.k9.sak.mottak.dokumentmottak.SøknadParser;
import no.nav.k9.søknad.ytelse.ung.v1.UngdomsytelseSøknadValidator;

@ApplicationScoped
@DokumentGruppeRef(Brevkode.UNGDOMSYTELSE_SOKNAD_KODE)
public class UngdomsytelseDokumentValidator implements DokumentValidator {

    private SøknadParser søknadParser;

    UngdomsytelseDokumentValidator() {
        // CDI
    }

    @Inject
    public UngdomsytelseDokumentValidator(SøknadParser søknadParser) {
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
        if (!Objects.equals(Brevkode.UNGDOMSYTELSE_SOKNAD, mottattDokument.getType())) {
            throw new IllegalArgumentException("Forventet brevkode: " + Brevkode.UNGDOMSYTELSE_SOKNAD + ", fikk: " + mottattDokument.getType());
        }
        var søknad = søknadParser.parseSøknad(mottattDokument);
        new UngdomsytelseSøknadValidator().forsikreValidert(søknad);
    }


}
