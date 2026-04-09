package no.nav.ung.ytelse.aktivitetspenger.mottak.inntektrapportering;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.søknad.ytelse.aktivitetspenger.v1.AktivitetspengerSøknadValidator;
import no.nav.k9.søknad.ytelse.ung.v1.UngdomsytelseSøknadValidator;
import no.nav.ung.kodeverk.dokument.Brevkode;
import no.nav.ung.sak.behandlingslager.behandling.motattdokument.MottattDokument;
import no.nav.ung.sak.mottak.dokumentmottak.DokumentGruppeRef;
import no.nav.ung.sak.mottak.dokumentmottak.DokumentValidator;
import no.nav.ung.sak.mottak.dokumentmottak.SøknadParser;

import java.util.Collection;
import java.util.Objects;

@ApplicationScoped
@DokumentGruppeRef(Brevkode.AKTIVITETSPENGER_INNTEKTRAPPORTERING_KODE)
public class AktivitetspengerInntektrapporteringValidator implements DokumentValidator {

    private SøknadParser søknadParser;

    AktivitetspengerInntektrapporteringValidator() {
        // CDI
    }

    @Inject
    public AktivitetspengerInntektrapporteringValidator(SøknadParser søknadParser) {
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
        if (!Objects.equals(Brevkode.AKTIVITETSPENGER_INNTEKTRAPPORTERING, mottattDokument.getType())) {
            throw new IllegalArgumentException("Forventet brevkode: " + Brevkode.AKTIVITETSPENGER_INNTEKTRAPPORTERING + ", fikk: " + mottattDokument.getType());
        }
        var søknad = søknadParser.parseSøknad(mottattDokument);
        new AktivitetspengerSøknadValidator().forsikreValidert(søknad);
    }


}
