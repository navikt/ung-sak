package no.nav.k9.sak.mottak.dokumentmottak;

import java.util.Objects;

import no.nav.k9.ettersendelse.Ettersendelse;
import no.nav.k9.sak.behandlingslager.behandling.motattdokument.MottattDokument;
import no.nav.k9.søknad.JsonUtils;

public final class EttersendelseParser {

    private EttersendelseParser() {
        //static
    }

    public static Ettersendelse parseDokument(MottattDokument mottattDokument) {
        var payload = mottattDokument.getPayload();
        var jsonReader = JsonUtils.getObjectMapper().readerFor(Ettersendelse.class);
        try {
            return jsonReader.readValue(Objects.requireNonNull(payload, "mangler payload"));
        } catch (Exception e) {
            throw new DokumentValideringException("Parsefeil i ettersendelse", e);
        }
    }
}
