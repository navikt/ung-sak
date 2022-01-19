package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.etablerttilsyn.sak;

import java.time.Duration;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import no.nav.k9.kodeverk.api.IndexKey;
import no.nav.k9.sak.behandlingslager.BaseEntitet;
import no.nav.k9.sak.behandlingslager.diff.ChangeTracked;
import no.nav.k9.sak.behandlingslager.diff.DiffIgnore;
import no.nav.k9.sak.behandlingslager.diff.IndexKeyComposer;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.typer.JournalpostId;

@Entity(name = "EtablertTilsynPeriode")
@Table(name = "ETABLERT_TILSYN_PERIODE")
public class EtablertTilsynPeriode extends BaseEntitet implements IndexKey {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_ETABLERT_TILSYN_PERIODE")
    private Long id;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "fomDato", column = @Column(name = "fom", nullable = false)),
        @AttributeOverride(name = "tomDato", column = @Column(name = "tom", nullable = false))
    })
    private DatoIntervallEntitet periode;

    @ChangeTracked
    @Column(name = "varighet")
    private Duration varighet;
    
    @DiffIgnore
    @Column(name = "journalpost_id")
    private JournalpostId journalpostId;

    @ManyToOne
    @JoinColumn(name = "etablert_tilsyn_id", nullable = false, updatable = false, unique = true)
    private EtablertTilsyn etablertTilsyn;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;


    public EtablertTilsynPeriode() {
    }

    public EtablertTilsynPeriode(DatoIntervallEntitet periode, Duration varighet, JournalpostId journalpostId) {
        this.periode = periode;
        this.varighet = varighet;
        this.journalpostId = journalpostId;
    }

    EtablertTilsynPeriode(EtablertTilsynPeriode etablertTilsynPeriode) {
        this(etablertTilsynPeriode.periode, etablertTilsynPeriode.varighet, etablertTilsynPeriode.journalpostId);
    }

    public DatoIntervallEntitet getPeriode() {
        return periode;
    }

    @Override
    public String getIndexKey() {
        return IndexKeyComposer.createKey(periode);
    }

    public Long getId() {
        return id;
    }

    public EtablertTilsyn getEtablertTilsyn() {
        return etablertTilsyn;
    }
    
    public Duration getVarighet() {
        return varighet;
    }
    
    public JournalpostId getJournalpostId() {
        return journalpostId;
    }
    
    void setEtablertTilsyn(EtablertTilsyn etablertTilsyn) {
        this.etablertTilsyn = etablertTilsyn;
    }

    public long getVersjon() {
        return versjon;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((periode == null) ? 0 : periode.hashCode());
        result = prime * result + ((varighet == null) ? 0 : varighet.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        EtablertTilsynPeriode other = (EtablertTilsynPeriode) obj;
        if (periode == null) {
            if (other.periode != null)
                return false;
        } else if (!periode.equals(other.periode))
            return false;
        if (varighet == null) {
            if (other.varighet != null)
                return false;
        } else if (!varighet.equals(other.varighet))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "EtablertTilsynPeriode [periode=" + periode + ", varighet=" + varighet + ", journalpostId=" + journalpostId + "]";
    }
}
