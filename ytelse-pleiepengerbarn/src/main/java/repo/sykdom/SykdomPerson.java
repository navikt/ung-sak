package repo.sykdom;

import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.søknad.felles.NorskIdentitetsnummer;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.NorskIdent;

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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public AktørId getAktoerId() {
        return aktoerId;
    }

    public void setAktoerId(AktørId aktoerId) {
        this.aktoerId = aktoerId;
    }

    public String getNorskIdentitetsnummer() {
        return norskIdentitetsnummer;
    }

    public void setNorskIdentitetsnummer(String norskIdentitetsnummer) {
        this.norskIdentitetsnummer = norskIdentitetsnummer;
    }
}
