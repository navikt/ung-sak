package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.untaketablerttilsyn;

import no.nav.k9.sak.behandlingslager.BaseEntitet;
import org.hibernate.annotations.Immutable;

import javax.persistence.*;
import java.util.Objects;

@Entity(name = "UnntakEtablertTilsynGrunnlag")
@Table(name = "GR_UNNTAK_ETABLERT_TILSYN")
public class UnntakEtablertTilsynGrunnlag extends BaseEntitet {


    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_GR_UNNTAK_ETABLERT_TILSYN")
    private Long id;

    @Column(name = "behandling_id", nullable = false, updatable = false, unique = true)
    private Long behandlingId;


    @ManyToOne
    @Immutable
    @JoinColumn(name = "UNNTAK_ETABLERT_TILSYN_PLEIETRENGENDE_ID", nullable = false, updatable = false, unique = true)
    private UnntakEtablertTilsynForPleietrengende unntakEtablertTilsynForPleietrengende;


    @Column(name = "aktiv", nullable = false)
    private boolean aktiv = true;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    UnntakEtablertTilsynGrunnlag() {
    }

    public UnntakEtablertTilsynGrunnlag(Long behandlingId, UnntakEtablertTilsynForPleietrengende unntakEtablertTilsynForPleietrengende) {
        this.behandlingId = behandlingId;
        this.unntakEtablertTilsynForPleietrengende = unntakEtablertTilsynForPleietrengende;
    }

    void setAktiv(boolean aktiv) {
        this.aktiv = aktiv;
    }


    public UnntakEtablertTilsynForPleietrengende getUnntakEtablertTilsynForPleietrengende() {
        return unntakEtablertTilsynForPleietrengende;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UnntakEtablertTilsynGrunnlag that = (UnntakEtablertTilsynGrunnlag) o;
        return unntakEtablertTilsynForPleietrengende.equals(that.unntakEtablertTilsynForPleietrengende);
    }

    @Override
    public int hashCode() {
        return Objects.hash(unntakEtablertTilsynForPleietrengende);
    }

    @Override
    public String toString() {
        return "UnntakEtablertTilsynGrunnlag{" +
            "unntakEtablertTilsynForPleietrengende=" + unntakEtablertTilsynForPleietrengende +
            '}';
    }
}
