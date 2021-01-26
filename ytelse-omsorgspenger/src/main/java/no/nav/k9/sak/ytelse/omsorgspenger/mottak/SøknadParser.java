package no.nav.k9.sak.ytelse.omsorgspenger.mottak;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import javax.enterprise.context.Dependent;

import no.nav.k9.sak.mottak.repo.MottattDokument;
import no.nav.k9.søknad.Søknad;

@Dependent
public class SøknadParser {


    public SøknadParser() {
    }

    public List<Søknad> parseSøknader(Collection<MottattDokument> mottatteDokumenter) {
        List<Søknad> søknader = new ArrayList<>();
        for (MottattDokument mottattDokument : mottatteDokumenter) {
            var payload = mottattDokument.getPayload();
            var jsonReader = SøknadOmsorgspengerUtbetalingJsonMapper.getMapper().readerFor(Søknad.class);
            Søknad søknad;
            try {
                søknad = jsonReader.readValue(Objects.requireNonNull(payload, "mangler payload"));
            } catch (Exception e) {
                // TODO: Feilhåndtering
                throw new IllegalArgumentException("Kunne ikke parse søknad");
            }
            søknader.add(søknad);
        }
        return søknader;
    }
}
