package no.nav.ung.sak.formidling.dokarkiv.dto;

import java.util.List;
import java.util.Objects;

import no.nav.ung.kodeverk.Fagsystem;

public record OpprettJournalpostRequest(
        String journalpostType,
        AvsenderMottaker avsenderMottaker,
        Bruker bruker,
        String tema,
        String behandlingstema,
        String tittel,
        String kanal,
        String journalfoerendeEnhet,
        String eksternReferanseId,
        List<Tilleggsopplysning> tilleggsopplysninger,
        Sak sak,
        List<Dokument> dokumenter
) {
    public static final String OMSORG_PLEIE_OPPLAERINGSPENGER_BEHANDLINGSTEMA = "ab0271";
    public static final String OMSORG_PLEIE_OPPLAERINGSPENGER_TEMA = "OMS";
    public static final String AUTOMATISK_JOURNALFORENDE_ENHET = "9999";
    public static final String TILLEGGSOPPLYSNING_EKSTERNREF_NOKKEL = "ung.formidling.eRef";
    private static final int MAKS_LENGDE_NAVN = 200;

    public OpprettJournalpostRequest {
        Objects.requireNonNull(avsenderMottaker, "avsenderMottaker cannot be null");
        Objects.requireNonNull(sak, "sak cannot be null");
        Objects.requireNonNull(dokumenter, "dokumenter cannot be null");
    }

    @Override
    public String toString() {
        return "OpprettJournalpostRequest{" +
                "journalpostType=" + journalpostType +
                ", sak=" + sak +
                ", kanal=" + kanal +
                ", tema=" + tema +
                ", behandlingstema=" + behandlingstema +
                ", journalfoerendeEnhet=" + journalfoerendeEnhet +
                ", eksternReferanseId=" + eksternReferanseId +
                ", tilleggsopplysninger=" + tilleggsopplysninger +
                '}';
    }

    public record AvsenderMottaker(
            String id,
            String navn,
            String land,
            IdType idType
    ) {
        public AvsenderMottaker {
            if (navn != null && navn.length() > MAKS_LENGDE_NAVN) {
                navn = navn.substring(0, MAKS_LENGDE_NAVN);
            }
        }

        public enum IdType {
            FNR, ORGNR, HPRNR, UTL_ORG
        }
    }

    public record Bruker(
            String id,
            BrukerIdType idType
    ) {
        public enum BrukerIdType {
            FNR, ORGNR
        }
    }

    public record Dokument(
            String tittel,
            String brevkode,
            String dokumentKategori,
            List<DokumentVariantArkivertPDFA> dokumentvarianter
    ) {
        public static Dokument lagDokumentMedPdf(String tittel, byte[] pdfData, String brevkode) {
            return new Dokument(
                    tittel,
                    brevkode,
                    null,
                    List.of(new DokumentVariantArkivertPDFA(pdfData))
            );
        }
    }

    public record DokumentVariantArkivertPDFA(
            String filtype,
            String variantformat,
            byte[] fysiskDokument
    ) {
        public DokumentVariantArkivertPDFA(byte[] fysiskDokument) {
            this("PDFA", "ARKIV", fysiskDokument);
        }

    }

    public record Sak(
            String sakstype,
            String fagsakId,
            String fagsaksystem
    ) {

        public static final Sak GENERELL_FAGSAK = new Sak(Sakstyper.GENERELL_SAK.name(), null, null);

        public static Sak forSaksnummer(String saksnummer) {
            return new Sak(Sakstyper.FAGSAK.name(), saksnummer,
                    Fagsystem.K9SAK.getOffisiellKode()); //TODO endre til ung
        }

        private enum Sakstyper {
            FAGSAK, GENERELL_SAK
        }
    }

    public record Tilleggsopplysning(
            String nokkel,
            String verdi
    ) {}


}
