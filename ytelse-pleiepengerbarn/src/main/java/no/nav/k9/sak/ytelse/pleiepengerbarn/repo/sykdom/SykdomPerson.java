package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom;

import no.nav.k9.sak.typer.AktørId;

import javax.persistence.*;

@Entity(name = "SykdomPerson")
@Table(name = "SYKDOM_PERSON")
public class SykdomPerson {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_SYKDOM_PERSON")
    private Long id;

    @Embedded
    @AttributeOverrides(@AttributeOverride(name = "aktørId", column = @Column(name = "AKTOER_ID", unique = true, nullable = false, updatable = false)))
    private AktørId aktørId;

    @Column(name = "NORSK_IDENTITETSNUMMER", nullable = true) // TODO.
    private String norskIdentitetsnummer; //Datatype?

    SykdomPerson() {
        // hibernate
    }

    public SykdomPerson(AktørId aktørId, String norskIdentitetsnummer) {
        this.aktørId = aktørId;
        this.norskIdentitetsnummer = norskIdentitetsnummer;
    }

    public Long getId() {
        return id;
    }

    public AktørId getAktørId() {
        return aktørId;
    }

    public String getNorskIdentitetsnummer() {
        return norskIdentitetsnummer;
    }
}
