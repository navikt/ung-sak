package repo.sykdom;

import no.nav.k9.sak.typer.AktørId;

import javax.persistence.*;

@Entity(name = "SykdomPerson")
@Table(name = "SYKDOM_PERSON")
public class SykdomPerson {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_SYKDOM_PERSON")
    private Long id;

    @Column(name = "AKTOER_ID", nullable = false)
    private AktørId aktoerId;

    @Column(name = "NORSK_IDENTITETSNUMMER", nullable = false)
    private String norskIdentitetsnummer; //Datatype?

    SykdomPerson() {
        // hibernate
    }

    public SykdomPerson(AktørId aktoerId, String norskIdentitetsnummer) {
        this.aktoerId = aktoerId;
        this.norskIdentitetsnummer = norskIdentitetsnummer;
    }

    public Long getId() {
        return id;
    }

    public AktørId getAktoerId() {
        return aktoerId;
    }

    public String getNorskIdentitetsnummer() {
        return norskIdentitetsnummer;
    }
}
