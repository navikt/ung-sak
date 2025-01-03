package no.nav.ung.sak.formidling.dokarkiv.dto;

import java.util.List;

public class OpprettJournalpostRequestBuilder {
    private String journalpostType;
    private OpprettJournalpostRequest.AvsenderMottaker avsenderMottaker;
    private OpprettJournalpostRequest.Bruker bruker;
    private String tema;
    private String behandlingstema;
    private String tittel;
    private String kanal;
    private String journalfoerendeEnhet;
    private String eksternReferanseId;
    private List<OpprettJournalpostRequest.Tilleggsopplysning> tilleggsopplysninger;
    private OpprettJournalpostRequest.Sak sak;
    private List<OpprettJournalpostRequest.Dokument> dokumenter;

    public OpprettJournalpostRequestBuilder journalpostType(String journalpostType) {
        this.journalpostType = journalpostType;
        return this;
    }

    public OpprettJournalpostRequestBuilder avsenderMottaker(OpprettJournalpostRequest.AvsenderMottaker avsenderMottaker) {
        this.avsenderMottaker = avsenderMottaker;
        return this;
    }

    public OpprettJournalpostRequestBuilder bruker(OpprettJournalpostRequest.Bruker bruker) {
        this.bruker = bruker;
        return this;
    }

    public OpprettJournalpostRequestBuilder tema(String tema) {
        this.tema = tema;
        return this;
    }

    public OpprettJournalpostRequestBuilder behandlingstema(String behandlingstema) {
        this.behandlingstema = behandlingstema;
        return this;
    }

    public OpprettJournalpostRequestBuilder tittel(String tittel) {
        this.tittel = tittel;
        return this;
    }

    public OpprettJournalpostRequestBuilder kanal(String kanal) {
        this.kanal = kanal;
        return this;
    }

    public OpprettJournalpostRequestBuilder journalfoerendeEnhet(String journalfoerendeEnhet) {
        this.journalfoerendeEnhet = journalfoerendeEnhet;
        return this;
    }

    public OpprettJournalpostRequestBuilder eksternReferanseId(String eksternReferanseId) {
        this.eksternReferanseId = eksternReferanseId;
        return this;
    }

    public OpprettJournalpostRequestBuilder tilleggsopplysninger(List<OpprettJournalpostRequest.Tilleggsopplysning> tilleggsopplysninger) {
        this.tilleggsopplysninger = tilleggsopplysninger;
        return this;
    }

    public OpprettJournalpostRequestBuilder sak(OpprettJournalpostRequest.Sak sak) {
        this.sak = sak;
        return this;
    }

    public OpprettJournalpostRequestBuilder dokumenter(List<OpprettJournalpostRequest.Dokument> dokumenter) {
        this.dokumenter = dokumenter;
        return this;
    }

    public OpprettJournalpostRequest build() {
        return new OpprettJournalpostRequest(journalpostType, avsenderMottaker, bruker, tema, behandlingstema, tittel, kanal, journalfoerendeEnhet, eksternReferanseId, tilleggsopplysninger, sak, dokumenter);
    }
}
