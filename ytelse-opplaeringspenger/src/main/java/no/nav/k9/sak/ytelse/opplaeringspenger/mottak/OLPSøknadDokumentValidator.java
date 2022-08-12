package no.nav.k9.sak.ytelse.opplaeringspenger.mottak;

import java.util.Collection;
import java.util.Objects;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.kodeverk.dokument.Brevkode;
import no.nav.k9.sak.behandlingslager.behandling.motattdokument.MottattDokument;
import no.nav.k9.sak.mottak.dokumentmottak.DokumentGruppeRef;
import no.nav.k9.sak.mottak.dokumentmottak.DokumentValidator;
import no.nav.k9.sak.mottak.dokumentmottak.SøknadParser;
import no.nav.k9.søknad.ytelse.pls.v1.PleiepengerLivetsSluttfaseSøknadValidator;

@ApplicationScoped
@DokumentGruppeRef(Brevkode.OPPLÆRINGSPENGER_SOKNAD_KODE)
public class OLPSøknadDokumentValidator implements DokumentValidator {

    private SøknadParser søknadParser;

    OLPSøknadDokumentValidator() {
        // CDI
    }

    @Inject
    public OLPSøknadDokumentValidator(SøknadParser søknadParser) {
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
        if (!Objects.equals(Brevkode.OPPLÆRINGSPENGER_SOKNAD, mottattDokument.getType())) {
            throw new IllegalArgumentException("Forventet brevkode: " + Brevkode.OPPLÆRINGSPENGER_SOKNAD + ", fikk: " + mottattDokument.getType());
        }
        var søknad = søknadParser.parseSøknad(mottattDokument);
        new PleiepengerLivetsSluttfaseSøknadValidator().forsikreValidert(søknad);
    }
}
