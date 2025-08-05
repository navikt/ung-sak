package no.nav.ung.sak.formidling.vedtak.regler;

import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.sak.formidling.innhold.VedtaksbrevInnholdBygger;

import java.util.Objects;

/**
 * Vedtaksbrev resultat for ett enkelt brev.
 */
public sealed interface VedtaksbrevRegelResultat permits VedtaksbrevRegelResultat.IngenBrev, VedtaksbrevRegelResultat.Vedtaksbrev {
    String forklaring();
    String safePrint();

    static Vedtaksbrev automatiskBrev(DokumentMalType dokumentMalType, VedtaksbrevInnholdBygger bygger, String forklaring, boolean kanRedigere) {
        return new Vedtaksbrev(
                dokumentMalType,
                bygger,
                new VedtaksbrevEgenskaper(
                        kanRedigere,
                        kanRedigere,
                        kanRedigere,
                        kanRedigere
                ),
            forklaring);
    }

    static Vedtaksbrev tomRedigerbarBrev(VedtaksbrevInnholdBygger bygger, String forklaring) {
        return new Vedtaksbrev(
            DokumentMalType.MANUELT_VEDTAK_DOK,
            bygger,
            new VedtaksbrevEgenskaper(
                true,
                true,
                true,
                false),
            forklaring);
    }

    static IngenBrev ingenBrev(IngenBrevÅrsakType ingenBrevÅrsakType, String forklaring) {
        return new IngenBrev(ingenBrevÅrsakType, forklaring);
    }

    record Vedtaksbrev(
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
        public String safePrint() {
            return "AutomatiskBrev{" +
                "vedtaksbrevEgenskaper=" + vedtaksbrevEgenskaper +
                ", dokumentMalType=" + dokumentMalType +
                ", bygger=" + (vedtaksbrevBygger.getClass().getSimpleName()) +
                ", forklaring='" + forklaring + '\'' +
                '}';
        }
    }

    record IngenBrev(
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
}

