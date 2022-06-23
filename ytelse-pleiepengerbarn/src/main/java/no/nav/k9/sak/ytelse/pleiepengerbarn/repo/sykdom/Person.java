package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom;

import no.nav.k9.sak.typer.AktørId;

import jakarta.persistence.*;

//Er dette tenkt utvidet til et slags partsregister i k9-sak (personnøkkel), eller burde tabellen fjernes
// og erstattes med aktørid-/fnr-/personnøkkelreferanse både i sykdom_vurderinger og andre steder?
// Jeg foretrekker nok det første alternativet.. Tabellen kan hete bare "Person"

@Entity(name = "Person")
@Table(name = "PERSON")
public class Person {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_PERSON")
    private Long id;

    //Iflg de andre på kontoret, så er aktørId noe som pdl har gått bort fra, eller i hvertfall ikke gir noen garanti om stabilitet osv
    //P.t. er anbefaling å nøkle personer på fnr? I så fall, sanere AktørId? Kjør debatt
    @Embedded
    @AttributeOverrides(@AttributeOverride(name = "aktørId", column = @Column(name = "AKTOER_ID", unique = true, nullable = false, updatable = false)))
    private AktørId aktørId;

    @Column(name = "NORSK_IDENTITETSNUMMER", nullable = true) // TODO.
    private String norskIdentitetsnummer; //Datatype?

    Person() {
        // hibernate
    }

    public Person(AktørId aktørId, String norskIdentitetsnummer) {
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
