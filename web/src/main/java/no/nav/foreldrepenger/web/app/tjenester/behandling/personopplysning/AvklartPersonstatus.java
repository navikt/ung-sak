package no.nav.foreldrepenger.web.app.tjenester.behandling.personopplysning;

import no.nav.k9.kodeverk.person.PersonstatusType;

class AvklartPersonstatus {

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
