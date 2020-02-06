package no.nav.k9.sak.kontrakt.person;

import no.nav.k9.kodeverk.person.Diskresjonskode;
import no.nav.k9.sak.typer.AktørId;

public abstract class PersonIdentDto {

    private String fnr;
    private Long aktoerId;
    private Diskresjonskode diskresjonskode;

    public Diskresjonskode getDiskresjonskode() {
        return diskresjonskode;
    }

    public AktørId getAktoerId() {
        return aktoerId == null ? null : new AktørId(aktoerId);
    }

    public String getFnr() {
        return fnr;
    }

    public void setFnr(String fnr) {
        this.fnr = fnr;
    }

    public void setDiskresjonskode(Diskresjonskode diskresjonskode) {
        this.diskresjonskode = diskresjonskode;
    }

    public void setAktoerId(AktørId aktoerId) {
        this.aktoerId = Long.parseLong(aktoerId.getId());
    }

}
