package no.nav.ung.sak.behandlingslager.inngangsvilkår;

import jakarta.persistence.*;
import no.nav.ung.sak.behandlingslager.BaseEntitet;

import java.util.Objects;
import java.util.Optional;

@Entity(name = "InngangsvilkårVurderingGrunnlag")
@Table(name = "gr_inngangsvilkaar_vurdering")
public class InngangsvilkårVurderingGrunnlag extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_GR_INNGANGSVILKAAR_VURDERING")
    @SequenceGenerator(name = "SEQ_GR_INNGANGSVILKAAR_VURDERING", sequenceName = "seq_gr_inngangsvilkaar_vurdering", allocationSize = 50)
    private Long id;

    @Column(name = "behandling_id", nullable = false, updatable = false)
    private Long behandlingId;

    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.REFRESH})
    @JoinColumn(name = "bistand_vurd_holder_id", updatable = false)
    private BistandsvilkårVurderingHolder bistandsvilkårVurderingHolder;

    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.REFRESH})
    @JoinColumn(name = "livsopphold_vurd_holder_id", updatable = false)
    private AndreLivsoppholdsytelserVurderingHolder andreLivsoppholdsytelserVurderingHolder;

    @Column(name = "aktiv", nullable = false)
    private boolean aktiv = true;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    public InngangsvilkårVurderingGrunnlag() {
    }

    InngangsvilkårVurderingGrunnlag(Long behandlingId,
                                    BistandsvilkårVurderingHolder bistandHolder,
                                    AndreLivsoppholdsytelserVurderingHolder livsoppholdHolder) {
        Objects.requireNonNull(behandlingId, "behandlingId");
        this.behandlingId = behandlingId;
        this.bistandsvilkårVurderingHolder = bistandHolder;
        this.andreLivsoppholdsytelserVurderingHolder = livsoppholdHolder;
    }

    public Long getId() {
        return id;
    }

    public Long getBehandlingId() {
        return behandlingId;
    }

    public Optional<BistandsvilkårVurderingHolder> getBistandsvilkårVurderingHolder() {
        return Optional.ofNullable(bistandsvilkårVurderingHolder);
    }

    public Optional<AndreLivsoppholdsytelserVurderingHolder> getAndreLivsoppholdsytelserVurderingHolder() {
        return Optional.ofNullable(andreLivsoppholdsytelserVurderingHolder);
    }

    public boolean isAktiv() {
        return aktiv;
    }

    void deaktiver() {
        this.aktiv = false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InngangsvilkårVurderingGrunnlag that = (InngangsvilkårVurderingGrunnlag) o;
        return Objects.equals(bistandsvilkårVurderingHolder, that.bistandsvilkårVurderingHolder)
            && Objects.equals(andreLivsoppholdsytelserVurderingHolder, that.andreLivsoppholdsytelserVurderingHolder);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bistandsvilkårVurderingHolder, andreLivsoppholdsytelserVurderingHolder);
    }
}
