package no.nav.ung.sak.formidling.domene;

import no.nav.ung.kodeverk.dokument.DokumentMalType;

public class BrevbestillingEntitetBuilder {
    private String saksnummer;
    private DokumentMalType dokumentMalType;
    private String dokumentdata;
    private BrevMottaker mottaker;

    public BrevbestillingEntitetBuilder saksnummer(String saksnummer) {
        this.saksnummer = saksnummer;
        return this;
    }

    public BrevbestillingEntitetBuilder dokumentMalType(DokumentMalType dokumentMalType) {
        this.dokumentMalType = dokumentMalType;
        return this;
    }


    public BrevbestillingEntitetBuilder dokumentdata(String dokumentdata) {
        this.dokumentdata = dokumentdata;
        return this;
    }

    public BrevbestillingEntitetBuilder mottaker(BrevMottaker mottaker) {
        this.mottaker = mottaker;
        return this;
    }

    public BrevbestillingEntitet createBrevbestillingEntitet() {
        return new BrevbestillingEntitet(saksnummer, dokumentMalType, BrevbestillingStatusType.NY, dokumentdata, mottaker);
    }
}
