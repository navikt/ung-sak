package no.nav.ung.sak.behandlingslager.aktør;

import jakarta.persistence.MappedSuperclass;

import no.nav.ung.sak.typer.AktørId;

@MappedSuperclass
public abstract class Person extends Aktør {

    public Person(AktørId aktørId) {
        super(aktørId);
    }

}
