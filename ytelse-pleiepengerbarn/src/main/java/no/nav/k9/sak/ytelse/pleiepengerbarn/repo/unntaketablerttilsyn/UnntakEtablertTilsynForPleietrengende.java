package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.unntaketablerttilsyn;

import no.nav.k9.sak.behandlingslager.BaseEntitet;
import no.nav.k9.sak.typer.AktørId;
import org.hibernate.annotations.Immutable;

import javax.persistence.*;
import java.util.Objects;

@Entity(name = "UnntakEtablertTilsynForPleietrengende")
@Table(name = "PSB_UNNTAK_ETABLERT_TILSYN_PLEIETRENGENDE")
public class UnntakEtablertTilsynForPleietrengende extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_PSB_UNNTAK_ETABLERT_TILSYN_PLEIETRENGENDE")
    private Long id;

    @Embedded
    @AttributeOverrides(@AttributeOverride(name = "aktørId", column = @Column(name = "PLEIETRENGENDE_AKTOER_ID", unique = true, nullable = false, updatable = false)))
    private AktørId pleietrengendeAktørId;

    @ManyToOne
    @Immutable
    @JoinColumn(name = "BEREDSKAP_ID", nullable = false, updatable = false, unique = true)
    private UnntakEtablertTilsyn beredskap;

    @ManyToOne
    @Immutable
    @JoinColumn(name = "NATTEVAAK_ID", nullable = false, updatable = false, unique = true)
    private UnntakEtablertTilsyn nattevåk;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    UnntakEtablertTilsynForPleietrengende() {
        // hibernate
    }

    public UnntakEtablertTilsynForPleietrengende(AktørId pleietrengendeAktørId) {
        this.pleietrengendeAktørId = pleietrengendeAktørId;
    }

    UnntakEtablertTilsynForPleietrengende(UnntakEtablertTilsynForPleietrengende unntakEtablertTilsynForPleietrengende) {
        this(unntakEtablertTilsynForPleietrengende.pleietrengendeAktørId);
    }

    public Long getId() {
        return id;
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
