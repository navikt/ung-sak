package no.nav.k9.sak.ytelse.pleiepengerlivetsslutt.mottak;

import java.util.Collection;
import java.util.Objects;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.kodeverk.dokument.Brevkode;
import no.nav.k9.sak.behandlingslager.behandling.motattdokument.MottattDokument;
import no.nav.k9.sak.mottak.dokumentmottak.DokumentGruppeRef;
import no.nav.k9.sak.mottak.dokumentmottak.DokumentValidator;
import no.nav.k9.sak.mottak.dokumentmottak.SøknadParser;
import no.nav.k9.søknad.ytelse.psb.v1.PleiepengerSyktBarnSøknadValidator;

@ApplicationScoped
@DokumentGruppeRef(Brevkode.SØKNAD_PLEIEPENGER_LIVETS_SLUTTFASE_KODE)
public class PLSSøknadDokumentValidator implements DokumentValidator {

    private static final Logger log = LoggerFactory.getLogger(PLSSøknadDokumentValidator.class);

    private SøknadParser søknadParser;

    PLSSøknadDokumentValidator() {
        // CDI
    }

    @Inject
    public PLSSøknadDokumentValidator(SøknadParser søknadParser) {
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

        // TODO PLS: Erstatt med PLSSøknadValidator
        new PleiepengerSyktBarnSøknadValidator().forsikreValidert(søknad);
    }
}
