package no.nav.k9.sak.ytelse.omsorgspenger.mottak;

import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.enterprise.context.Dependent;

import no.nav.k9.sak.mottak.repo.MottattDokument;
import no.nav.k9.søknad.Søknad;

@Dependent
public class SøknadParser {

    public Søknad parseSøknad(MottattDokument mottattDokument) {
        var payload = mottattDokument.getPayload();
        var jsonReader = SøknadOmsorgspengerUtbetalingJsonMapper.getMapper().readerFor(Søknad.class);
        try {
            return jsonReader.readValue(Objects.requireNonNull(payload, "mangler payload"));
        } catch (Exception e) {
            throw SøknadUtbetalingOmsorgspengerFeil.FACTORY.parsefeil(e).toException();
        }
    }

    public Collection<Søknad> parseSøknader(Collection<MottattDokument> mottatteDokumenter) {
        return mottatteDokumenter.stream()
            .map(this::parseSøknad)
            .collect(Collectors.toList());
    }
}
