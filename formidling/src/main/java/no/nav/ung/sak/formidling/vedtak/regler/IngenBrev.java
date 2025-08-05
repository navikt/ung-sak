package no.nav.ung.sak.formidling.vedtak.regler;

import java.util.Objects;

public record IngenBrev(
    IngenBrevÅrsakType ingenBrevÅrsakType,
    String forklaring
) implements VedtaksbrevRegelResultat {

    public IngenBrev {
        Objects.requireNonNull(ingenBrevÅrsakType);
        Objects.requireNonNull(forklaring);
    }

    @Override
    public String safePrint() {
        return "IngenBrev{" +
            "ingenBrevÅrsakType='" + ingenBrevÅrsakType + '\'' +
            ", forklaring='" + forklaring + '\'' +
            '}';
    }
}
