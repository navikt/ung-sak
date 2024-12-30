package no.nav.ung.sak.formidling.domene;

import com.fasterxml.jackson.databind.JsonNode;

import no.nav.ung.kodeverk.dokument.DokumentMalType;

public class BrevbestillingEntitetBuilder {
    private String saksnummer;
    private DokumentMalType dokumentMalType;
    private JsonNode dokumentdata;
    private BrevMottaker mottaker;

    public BrevbestillingEntitetBuilder saksnummer(String saksnummer) {
        this.saksnummer = saksnummer;
        return this;
    }

    public BrevbestillingEntitetBuilder dokumentMalType(DokumentMalType dokumentMalType) {
        this.dokumentMalType = dokumentMalType;
        return this;
    }

    public BrevbestillingEntitetBuilder dokumentdata(JsonNode dokumentdata) {
        this.dokumentdata = dokumentdata;
        return this;
    }

    public BrevbestillingEntitetBuilder mottaker(BrevMottaker mottaker) {
        this.mottaker = mottaker;
        return this;
    }

    public BrevbestillingEntitet build() {
        return new BrevbestillingEntitet(saksnummer, dokumentMalType, BrevbestillingStatusType.NY, dokumentdata, mottaker);
    }
}
