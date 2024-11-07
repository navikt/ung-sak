package no.nav.k9.sak.behandlingslager.notat;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity(name = "NotatSakTekst")
@Table(name = "notat_sak_tekst")
public class NotatSakTekst extends NotatTekstEntitet {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_notat_sak_tekst")
    private Long id;


    public NotatSakTekst(String tekst, long versjon) {
        super(tekst, versjon);
    }

    public NotatSakTekst() { }

}
