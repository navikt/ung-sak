package no.nav.ung.sak.mottak.dokumentmottak;

import jakarta.enterprise.context.Dependent;
import no.nav.k9.søknad.JsonUtils;
import no.nav.k9.søknad.Søknad;
import no.nav.ung.sak.behandlingslager.behandling.motattdokument.MottattDokument;

import java.util.Objects;

@Dependent
public class SøknadParser {

    public Søknad parseSøknad(MottattDokument mottattDokument) {
        var payload = Objects.requireNonNull( mottattDokument.getPayload(), "mangler payload");
        try {
            return JsonUtils.fromString(payload, Søknad.class);
        } catch (Exception e) {
            throw new DokumentValideringException("Parsefeil i søknad", e);
        }
    }
}
