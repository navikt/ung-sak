package no.nav.ung.sak.formidling.vedtak.regler;

import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.sak.formidling.innhold.VedtaksbrevInnholdBygger;

import java.util.Objects;

public record Vedtaksbrev(
        DokumentMalType dokumentMalType,
        VedtaksbrevInnholdBygger vedtaksbrevBygger,
        VedtaksbrevEgenskaper vedtaksbrevEgenskaper,
        String forklaring
) implements VedtaksbrevRegelResultat {
    public Vedtaksbrev {
        Objects.requireNonNull(dokumentMalType, "dokumentMalType cannot be null");
        Objects.requireNonNull(vedtaksbrevBygger, "vedtaksbrevBygger cannot be null");
        Objects.requireNonNull(vedtaksbrevEgenskaper, "vedtaksbrevEgenskaper cannot be null");
    }

    @Override
    public String toString() {
        return "Vedtaksbrev{" +
            "dokumentMalType=" + dokumentMalType +
            ", vedtaksbrevBygger=" + (vedtaksbrevBygger.getClass().getSimpleName()) +
            ", vedtaksbrevEgenskaper=" + vedtaksbrevEgenskaper +
            ", forklaring='" + forklaring + '\'' +
            '}';
    }

}
