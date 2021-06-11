package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.unntaketablerttilsyn;

import no.nav.k9.sak.behandlingslager.BaseEntitet;
import no.nav.k9.sak.typer.AktørId;

import javax.persistence.*;
import java.util.Objects;

@Entity(name = "UnntakEtablertTilsynForPleietrengende")
@Table(name = "psb_unntak_etablert_tilsyn_pleietrengende")
public class UnntakEtablertTilsynForPleietrengende extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_PSB_UNNTAK_ETABLERT_TILSYN_PLEIETRENGENDE")
    private Long id;

    @Embedded
    @AttributeOverrides(@AttributeOverride(name = "aktørId", column = @Column(name = "pleietrengende_aktoer_id", unique = true, nullable = false, updatable = false)))
    private AktørId pleietrengendeAktørId;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "beredskap_id", nullable = true, updatable = false, unique = true)
    private UnntakEtablertTilsyn beredskap;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "nattevaak_id", nullable = true, updatable = false, unique = true)
    private UnntakEtablertTilsyn nattevåk;
    
    @Column(name = "aktiv", nullable = false)
    private boolean aktiv = true;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    UnntakEtablertTilsynForPleietrengende() {
        // hibernate
    }

    public UnntakEtablertTilsynForPleietrengende(AktørId pleietrengendeAktørId, UnntakEtablertTilsyn beredskap, UnntakEtablertTilsyn nattevåk) {
        this.pleietrengendeAktørId = pleietrengendeAktørId;
        this.beredskap = beredskap;
        this.nattevåk = nattevåk;
    }

    UnntakEtablertTilsynForPleietrengende(UnntakEtablertTilsynForPleietrengende unntakEtablertTilsynForPleietrengende) {
        this(unntakEtablertTilsynForPleietrengende.pleietrengendeAktørId,
            unntakEtablertTilsynForPleietrengende.getBeredskap(),
            unntakEtablertTilsynForPleietrengende.getNattevåk());
    }

    public Long getId() {
        return id;
    }

    public AktørId getPleietrengendeAktørId() {
        return pleietrengendeAktørId;
    }

    public UnntakEtablertTilsyn getBeredskap() {
        return beredskap;
    }

    public UnntakEtablertTilsyn getNattevåk() {
        return nattevåk;
    }

    public UnntakEtablertTilsynForPleietrengende medBeredskap(UnntakEtablertTilsyn beredskap) {
        this.beredskap = beredskap;
        return this;
    }

    public UnntakEtablertTilsynForPleietrengende medNattevåk(UnntakEtablertTilsyn nattevåk) {
        this.nattevåk = nattevåk;
        return this;
    }
    
    void setAktiv(boolean aktiv) {
        this.aktiv = aktiv;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UnntakEtablertTilsynForPleietrengende that = (UnntakEtablertTilsynForPleietrengende) o;
        return pleietrengendeAktørId.equals(that.pleietrengendeAktørId) && Objects.equals(beredskap, that.beredskap) && Objects.equals(nattevåk, that.nattevåk);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pleietrengendeAktørId, beredskap, nattevåk);
    }

    @Override
    public String toString() {
        return "UnntakEtablertTilsynForPleietrengende{" +
            "id=" + id +
            ", pleietrengendeAktørId=" + pleietrengendeAktørId +
            ", beredskap=" + beredskap +
            ", nattevåk=" + nattevåk +
            '}';
    }
}
