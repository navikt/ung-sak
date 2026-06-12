package no.nav.ung.sak.behandlingslager.inngangsvilkår;

import jakarta.persistence.*;
import no.nav.ung.sak.behandlingslager.BaseEntitet;

import java.util.Objects;
import java.util.Optional;

@Entity(name = "AktivitetspengerInngangsvilkårResultatGrunnlag")
@Table(name = "gr_akt_inngangsvilkaar_res")
public class AktivitetspengerInngangsvilkårResultatGrunnlag extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_GR_AKT_INNGANGSVILKAAR_RES")
    @SequenceGenerator(name = "SEQ_GR_AKT_INNGANGSVILKAAR_RES", sequenceName = "seq_gr_akt_inngangsvilkaar_res", allocationSize = 50)
    private Long id;

    @Column(name = "behandling_id", nullable = false, updatable = false)
    private Long behandlingId;

    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.REFRESH})
    @JoinColumn(name = "bistand_resultat_holder_id", updatable = false)
    private BistandsvilkårResultatHolder bistandsvilkårResultatHolder;

    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.REFRESH})
    @JoinColumn(name = "livsopphold_resultat_holder_id", updatable = false)
    private AndreLivsoppholdsytelserResultatHolder andreLivsoppholdsytelserResultatHolder;

    @Column(name = "aktiv", nullable = false)
    private boolean aktiv = true;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    public AktivitetspengerInngangsvilkårResultatGrunnlag() {
    }

    AktivitetspengerInngangsvilkårResultatGrunnlag(Long behandlingId,
                                                   BistandsvilkårResultatHolder bistandHolder,
                                                   AndreLivsoppholdsytelserResultatHolder livsoppholdHolder) {
        Objects.requireNonNull(behandlingId, "behandlingId");
        this.behandlingId = behandlingId;
        this.bistandsvilkårResultatHolder = bistandHolder;
        this.andreLivsoppholdsytelserResultatHolder = livsoppholdHolder;
    }

    public Long getId() {
        return id;
    }

    public Long getBehandlingId() {
        return behandlingId;
    }

    public Optional<BistandsvilkårResultatHolder> getBistandsvilkårResultatHolder() {
        return Optional.ofNullable(bistandsvilkårResultatHolder);
    }

    public Optional<AndreLivsoppholdsytelserResultatHolder> getAndreLivsoppholdsytelserResultatHolder() {
        return Optional.ofNullable(andreLivsoppholdsytelserResultatHolder);
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
        AktivitetspengerInngangsvilkårResultatGrunnlag that = (AktivitetspengerInngangsvilkårResultatGrunnlag) o;
        return Objects.equals(bistandsvilkårResultatHolder, that.bistandsvilkårResultatHolder)
            && Objects.equals(andreLivsoppholdsytelserResultatHolder, that.andreLivsoppholdsytelserResultatHolder);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bistandsvilkårResultatHolder, andreLivsoppholdsytelserResultatHolder);
    }
}
