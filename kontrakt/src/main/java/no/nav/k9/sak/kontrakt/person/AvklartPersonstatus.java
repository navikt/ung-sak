package no.nav.k9.sak.kontrakt.person;

import no.nav.k9.kodeverk.person.PersonstatusType;

public class AvklartPersonstatus {

    private PersonstatusType orginalPersonstatus;
    private PersonstatusType overstyrtPersonstatus;

    public AvklartPersonstatus(PersonstatusType orginalPersonstatus, PersonstatusType overstyrtPersonstatus) {
        this.orginalPersonstatus = orginalPersonstatus;
        this.overstyrtPersonstatus = overstyrtPersonstatus;
    }

    public PersonstatusType getOrginalPersonstatus() {
        return orginalPersonstatus;
    }

    public PersonstatusType getOverstyrtPersonstatus() {
        return overstyrtPersonstatus;
    }
}
