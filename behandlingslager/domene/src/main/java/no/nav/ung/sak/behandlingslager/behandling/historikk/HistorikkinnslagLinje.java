package no.nav.ung.sak.behandlingslager.behandling.historikk;

import jakarta.persistence.*;
import no.nav.ung.kodeverk.api.IndexKey;
import no.nav.ung.sak.behandlingslager.BaseEntitet;
import no.nav.ung.sak.behandlingslager.diff.IndexKeyComposer;

import java.util.Objects;


@Entity(name = "HistorikkinnslagLinje")
@Table(name = "HISTORIKKINNSLAG_LINJE")
public class HistorikkinnslagLinje extends BaseEntitet implements IndexKey {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_HISTORIKKINNSLAG_LINJE")
    private Long id;

    @Column(name = "tekst")
    private String tekst;

    @Column(name = "SEKVENS_NR", nullable = false)
    private int sekvensNr;

    @Column(name = "type", nullable = false)
    @Enumerated(EnumType.STRING)
    private HistorikkinnslagLinjeType type;

    @ManyToOne(optional = false)
    @JoinColumn(name = "historikkinnslag_id", nullable = false)
    private Historikkinnslag historikkinnslag;


    @Override
    public String getIndexKey() {
        return IndexKeyComposer.createKey(sekvensNr);
    }

    private void validerBoldMarkering(String tekst) {
        var antallBoldMarkører = tekst.split(Historikkinnslag.BOLD_MARKØR, -1).length -1;
        if (antallBoldMarkører % 2 == 1) {
            throw new IllegalArgumentException("Ugyldig bold markering av tekst for tekstlinje");
        }
    }

    private HistorikkinnslagLinje(String tekst, int sekvensNr, HistorikkinnslagLinjeType type) {
        Objects.requireNonNull(type);

        if (HistorikkinnslagLinjeType.TEKST.equals(type)) {
            if (tekst == null || tekst.isEmpty()) {
                throw new IllegalArgumentException("Teksttype må ha tekst");
            }
            validerBoldMarkering(tekst);
        }

        if (HistorikkinnslagLinjeType.LINJESKIFT.equals(type) && tekst != null) {
            throw new IllegalArgumentException("Linjeskift kan ikke ha tekst");
        }

        this.tekst = tekst;
        this.sekvensNr = sekvensNr;
        this.type = type;
    }

    public static HistorikkinnslagLinje tekst(String tekst, int sekvensNr) {
        return new HistorikkinnslagLinje(tekst, sekvensNr, HistorikkinnslagLinjeType.TEKST);
    }

    public static HistorikkinnslagLinje linjeskift(int sekvensNr) {
        return new HistorikkinnslagLinje(null, sekvensNr, HistorikkinnslagLinjeType.LINJESKIFT);
    }

    protected HistorikkinnslagLinje() {
    }

    public String getTekst() {
        return tekst;
    }

    public HistorikkinnslagLinjeType getType() {
        return type;
    }

    public int getSekvensNr() {
        return sekvensNr;
    }

    void setHistorikkinnslag(Historikkinnslag historikkinnslag) {
        this.historikkinnslag = historikkinnslag;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof HistorikkinnslagLinje that)) {
            return false;
        }
        return sekvensNr == that.sekvensNr && Objects.equals(tekst, that.tekst) && type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(tekst, sekvensNr, type);
    }

    @Override
    public String toString() {
        //tekst kan være fritekst fra saksbehandler
        return "HistorikkinnslagLinje{" + "tekst='***" + '\'' + ", sekvensNr=" + sekvensNr + ", type=" + type + '}';
    }
}
