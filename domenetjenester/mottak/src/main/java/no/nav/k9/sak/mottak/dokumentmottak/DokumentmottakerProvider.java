package no.nav.k9.sak.mottak.dokumentmottak;

import java.util.Collection;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import no.nav.k9.sak.mottak.dokumentmottak.søknad.SøknadParser;
import no.nav.k9.sak.mottak.repo.MottattDokument;

@Dependent
public class DokumentmottakerProvider {

    private DokumentmottakImplementasjonsvelger<Dokumentmottaker> dokumentmottakImplementasjonsvelger;

    @Inject
    public DokumentmottakerProvider(@Any Instance<Dokumentmottaker> dokumentmottakere, SøknadParser søknadParser) {
        this.dokumentmottakImplementasjonsvelger = new DokumentmottakImplementasjonsvelger<>(dokumentmottakere, søknadParser);
    }

    public Dokumentmottaker getDokumentmottaker(Collection<MottattDokument> mottattDokument) {
        return dokumentmottakImplementasjonsvelger.velgImplementasjon(mottattDokument);
    }
}
