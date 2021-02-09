package no.nav.k9.sak.ytelse.omsorgspenger.inngangsvilkår.søknadsfrist;

import java.util.Objects;

import javax.enterprise.context.Dependent;

import com.fasterxml.jackson.core.JsonProcessingException;

import no.nav.k9.sak.mottak.dokumentmottak.DokumentValideringException;
import no.nav.k9.sak.mottak.repo.MottattDokument;
import no.nav.k9.søknad.JsonUtils;
import no.nav.k9.søknad.Søknad;

@Dependent
public class SøknadParser {

    public Søknad parseSøknad(MottattDokument mottattDokument) {
        var payload = mottattDokument.getPayload();
        var jsonReader = JsonUtils.getObjectMapper().readerFor(Søknad.class);
        try {
            return jsonReader.readValue(Objects.requireNonNull(payload, "mangler payload"));
        } catch (RuntimeException | JsonProcessingException e) {
            throw new DokumentValideringException("Parsefeil i søknad ", e);
        }
    }
}
