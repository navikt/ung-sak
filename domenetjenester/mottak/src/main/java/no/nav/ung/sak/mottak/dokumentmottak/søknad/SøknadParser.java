package no.nav.ung.sak.mottak.dokumentmottak.søknad;

import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;

import jakarta.enterprise.context.Dependent;

import no.nav.ung.sak.behandlingslager.behandling.motattdokument.MottattDokument;
import no.nav.ung.sak.mottak.dokumentmottak.DokumentValideringException;
import no.nav.k9.søknad.JsonUtils;
import no.nav.k9.søknad.Søknad;

@Dependent
public class SøknadParser {

    public Søknad parseSøknad(MottattDokument mottattDokument) {
        var payload = mottattDokument.getPayload();
        var jsonReader = JsonUtils.getObjectMapper().readerFor(Søknad.class);
        try {
            return jsonReader.readValue(Objects.requireNonNull(payload, "mangler payload"));
        } catch (Exception e) {
            throw new DokumentValideringException("Parsefeil i søknad om utbetaling av omsorgspenger", e);
        }
    }

    public Collection<Søknad> parseSøknader(Collection<MottattDokument> mottatteDokumenter) {
        return mottatteDokumenter.stream()
            .map(this::parseSøknad)
            .collect(Collectors.toList());
    }
}
