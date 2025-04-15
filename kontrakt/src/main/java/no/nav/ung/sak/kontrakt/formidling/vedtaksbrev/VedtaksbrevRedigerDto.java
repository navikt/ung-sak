package no.nav.ung.sak.kontrakt.formidling.vedtaksbrev;

import java.util.UUID;

public record VedtaksbrevRedigerDto(
    Long behandlingId, UUID kladdId, String html
) {
}
