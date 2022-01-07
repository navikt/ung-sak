package no.nav.k9.sak.ytelse.pleiepengerlivetsslutt.mottak;

import java.util.Collection;
import java.util.Objects;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.kodeverk.dokument.Brevkode;
import no.nav.k9.sak.behandlingslager.behandling.motattdokument.MottattDokument;
import no.nav.k9.sak.mottak.dokumentmottak.DokumentGruppeRef;
import no.nav.k9.sak.mottak.dokumentmottak.DokumentValidator;
import no.nav.k9.sak.mottak.dokumentmottak.SøknadParser;
import no.nav.k9.søknad.ytelse.pls.v1.PleiepengerLivetsSluttfaseSøknadValidator;

@ApplicationScoped
@DokumentGruppeRef(Brevkode.SØKNAD_PLEIEPENGER_LIVETS_SLUTTFASE_KODE)
public class PPNSøknadDokumentValidator implements DokumentValidator {

    private SøknadParser søknadParser;

    PPNSøknadDokumentValidator() {
        // CDI
    }

    @Inject
    public PPNSøknadDokumentValidator(SøknadParser søknadParser) {
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
        if (!Objects.equals(Brevkode.SØKNAD_PLEIEPENGER_LIVETS_SLUTTFASE, mottattDokument.getType())) {
            throw new IllegalArgumentException("Forventet brevkode: " + Brevkode.SØKNAD_PLEIEPENGER_LIVETS_SLUTTFASE + ", fikk: " + mottattDokument.getType());
        }
        var søknad = søknadParser.parseSøknad(mottattDokument);
        new PleiepengerLivetsSluttfaseSøknadValidator().forsikreValidert(søknad);
    }
}
