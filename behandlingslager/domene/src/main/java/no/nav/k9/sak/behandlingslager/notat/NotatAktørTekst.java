package no.nav.k9.sak.behandlingslager.notat;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity(name = "NotatAktørTekst")
@Table(name = "notat_aktoer_tekst")
public class NotatAktørTekst extends NotatTekstEntitet {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_notat_aktoer_tekst")
    private Long id;


    public NotatAktørTekst(String tekst, long versjon) {
        super(tekst, versjon);
    }

    public NotatAktørTekst() { }

}
