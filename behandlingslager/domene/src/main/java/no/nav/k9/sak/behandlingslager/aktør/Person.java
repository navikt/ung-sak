package no.nav.k9.sak.behandlingslager.aktør;

import javax.persistence.MappedSuperclass;

import no.nav.k9.sak.typer.AktørId;

@MappedSuperclass
public abstract class Person extends Aktør {

    public Person(AktørId aktørId) {
        super(aktørId);
    }

}
