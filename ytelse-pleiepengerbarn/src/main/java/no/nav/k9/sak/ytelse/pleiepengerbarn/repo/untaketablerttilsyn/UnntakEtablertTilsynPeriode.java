package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.untaketablerttilsyn;

import no.nav.k9.kodeverk.api.IndexKey;
import no.nav.k9.sak.behandlingslager.BaseEntitet;
import no.nav.k9.sak.behandlingslager.diff.ChangeTracked;
import no.nav.k9.sak.behandlingslager.diff.IndexKeyComposer;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.kontrakt.sykdom.Resultat;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomResultatTypeConverter;

import javax.persistence.*;
import java.util.Objects;

@Entity(name = "UnntakEtablertTilsynPeriode")
@Table(name = "UNNTAK_ETABLERT_TILSYN_PERIODE")
public class UnntakEtablertTilsynPeriode extends BaseEntitet implements IndexKey  {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_UNNTAK_ETABLERT_TILSYN_PERIODE")
    private Long id;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "fomDato", column = @Column(name = "fom", nullable = false)),
        @AttributeOverride(name = "tomDato", column = @Column(name = "tom", nullable = false))
    })
    private DatoIntervallEntitet periode;

    @ManyToOne
    @JoinColumn(name = "unntak_etablert_tilsyn_id", nullable = false, updatable = false, unique = true)
    private UnntakEtablertTilsyn unntakEtablertTilsyn;

    @ChangeTracked
    @Column(name = "begrunnelse")
    private String begrunnelse;

    @Column(name = "resultat", nullable = false)
    @Convert(converter = SykdomResultatTypeConverter.class)
    private Resultat resultat;


    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;


    public UnntakEtablertTilsynPeriode() {
        // hibernate
    }

    public UnntakEtablertTilsynPeriode(UnntakEtablertTilsynPeriode unntakEtablertTilsynPeriode) {
        this.begrunnelse = unntakEtablertTilsynPeriode.begrunnelse;
        this.resultat = unntakEtablertTilsynPeriode.resultat;
    }

    public UnntakEtablertTilsynPeriode(UnntakEtablertTilsynPeriode unntakEtablertTilsynPeriode, DatoIntervallEntitet periode) {
        this.begrunnelse = unntakEtablertTilsynPeriode.begrunnelse;
        this.resultat = unntakEtablertTilsynPeriode.resultat;
        this.unntakEtablertTilsyn = unntakEtablertTilsynPeriode.unntakEtablertTilsyn;
        this.periode = periode;
    }

    public Long getId() {
        return id;
    }

    public UnntakEtablertTilsyn getUnntakEtablertTilsyn() {
        return unntakEtablertTilsyn;
    }

    public void setUnntakEtablertTilsyn(UnntakEtablertTilsyn unntakEtablertTilsyn) {
        this.unntakEtablertTilsyn = unntakEtablertTilsyn;
    }

    @Override
    public String getIndexKey() {
        return IndexKeyComposer.createKey(periode);
    }

    public DatoIntervallEntitet getPeriode() {
        return periode;
    }

    public String getBegrunnelse() {
        return begrunnelse;
    }

    public Resultat getResultat() {
        return resultat;
    }

    public UnntakEtablertTilsynPeriode medPeriode(DatoIntervallEntitet periode) {
        this.periode = periode;
        return this;
    }

    public UnntakEtablertTilsynPeriode medBegrunnelse(String begrunnelse) {
        this.begrunnelse = begrunnelse;
        return this;
    }

    public UnntakEtablertTilsynPeriode medResultat(Resultat resultat) {
        this.resultat = resultat;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UnntakEtablertTilsynPeriode that = (UnntakEtablertTilsynPeriode) o;
        return periode.equals(that.periode) && unntakEtablertTilsyn.equals(that.unntakEtablertTilsyn) && begrunnelse.equals(that.begrunnelse) && resultat == that.resultat;
    }

    @Override
    public int hashCode() {
        return Objects.hash(periode, unntakEtablertTilsyn, begrunnelse, resultat);
    }

    @Override
    public String toString() {
        return "UnntakEtablertTilsynPeriode{" +
            "id=" + id +
            ", periode=" + periode +
            ", resultat=" + resultat +
            ", versjon=" + versjon +
            '}';
    }
}
