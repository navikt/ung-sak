package no.nav.k9.sak.kontrakt.dokument;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import no.nav.k9.sak.typer.JournalpostId;

class DokumentDtoTest {
    @Test
    void skal_lage_lenke() {
        var dto = new DokumentDto("k9/api/asdf/asd?saksnummer=ASDF");
        dto.setDokumentId("1");
        dto.setJournalpostId(new JournalpostId("10"));

        Assertions.assertThat(dto.getHref()).isEqualTo("k9/api/asdf/asd?saksnummer=ASDF&journalpostId=10&dokumentId=1");
    }
}
