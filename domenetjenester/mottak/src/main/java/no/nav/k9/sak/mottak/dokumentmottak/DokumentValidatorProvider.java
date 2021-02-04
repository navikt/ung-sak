package no.nav.k9.sak.mottak.dokumentmottak;

import java.util.Collection;
import java.util.Set;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import no.nav.k9.sak.mottak.dokumentmottak.søknad.SøknadParser;
import no.nav.k9.sak.mottak.repo.MottattDokument;

@Dependent
public class DokumentValidatorProvider {

    private DokumentmottakImplementasjonsvelger<DokumentValidator> dokumentmottakImplementasjonsvelger;

    @Inject
    public DokumentValidatorProvider(@Any Instance<DokumentValidator> dokumentValidatorerer, SøknadParser søknadParser) {
        this.dokumentmottakImplementasjonsvelger = new DokumentmottakImplementasjonsvelger<>(dokumentValidatorerer, søknadParser);
    }

    public DokumentValidator finnValidator(MottattDokument mottattDokument) {
        return dokumentmottakImplementasjonsvelger.velgImplementasjon(Set.of(mottattDokument));
    }

    public DokumentValidator finnValidator(Collection<MottattDokument> mottattDokument) {
        return dokumentmottakImplementasjonsvelger.velgImplementasjon(mottattDokument);
    }

}
