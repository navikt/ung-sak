package no.nav.ung.sak.behandlingslager.vilkårsavklaring;

import jakarta.persistence.*;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingslager.BaseEntitet;
import org.hibernate.annotations.BatchSize;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Grunnlag som kobler en behandling og ett vilkår til vilkårsavklarings-aggregatet.
 * Brukes for avklaring og varsling på tvers av alle vilkår
 */
@Entity(name = "VilkårsavklaringGrunnlag")
@Table(name = "GR_VILKAAR_AVKLARING")
public class VilkårsavklaringGrunnlag extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_GR_VILKAAR_AVKLARING")
    private Long id;

    @Column(name = "behandling_id", nullable = false, updatable = false)
    private Long behandlingId;

    @Enumerated(EnumType.STRING)
    @Column(name = "vilkaar_type", nullable = false, updatable = false)
    private VilkårType vilkårType;

    @BatchSize(size = 20)
    @JoinColumn(name = "gr_vilkaar_avklaring_id", nullable = false)
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<VilkårPeriodeAvklaringForeslått> foreslåtteAvklaringer = new LinkedHashSet<>();

    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.REFRESH})
    @JoinColumn(name = "avklaring_holder_id", updatable = false)
    private VilkårAvklaringHolder ferdigstilteAvklaringer;

    @Column(name = "aktiv", nullable = false)
    private boolean aktiv = true;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    public VilkårsavklaringGrunnlag() {
    }

    VilkårsavklaringGrunnlag(Long behandlingId, VilkårType vilkårType) {
        Objects.requireNonNull(behandlingId, "behandlingId");
        Objects.requireNonNull(vilkårType, "vilkårType");
        this.behandlingId = behandlingId;
        this.vilkårType = vilkårType;
    }

    private VilkårsavklaringGrunnlag(Long behandlingId,
                                      VilkårType vilkårType,
                                      Set<VilkårPeriodeAvklaringForeslått> foreslåtteAvklaringer,
                                      VilkårAvklaringHolder ferdigstilteAvklaringer) {
        this(behandlingId, vilkårType);
        this.foreslåtteAvklaringer = foreslåtteAvklaringer.stream()
            .map(VilkårPeriodeAvklaringForeslått::new)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        this.ferdigstilteAvklaringer = ferdigstilteAvklaringer;
    }

    /**
     * Bygger nytt sett med foreslåtte avklaringer — kun hvis innholdet faktisk er endret. Rører ikke holderen,
     * slik at ferdigstilte avklaringer ikke dupliseres ved lagring av forslag.
     */
    void setForeslåtteAvklaringer(Set<VilkårPeriodeAvklaringForeslått> nyeAvklaringer) {
        var kopi = nyeAvklaringer.stream()
            .map(VilkårPeriodeAvklaringForeslått::new)
            .collect(Collectors.toCollection(LinkedHashSet::new));

        if (kopi.equals(this.foreslåtteAvklaringer)) {
            return;
        }
        this.foreslåtteAvklaringer = kopi;
    }

    /**
     * Ferdigstiller de foreslåtte avklaringene. Eneste skriver av holderen — gjør copy-on-write der.
     * De foreslåtte avklaringene beholdes urørt, slik at operasjonen er idempotent og fortsatt forteller hva
     * som ble behandlet i denne behandlingen.
     */
    void ferdigstillForeslåtteAvklaringer() {
        var nyHolder = VilkårAvklaringHolder.lagKopi(this.ferdigstilteAvklaringer);
        nyHolder.ferdigstillAvklaringer(foreslåtteAvklaringer);

        if (nyHolder.equals(this.ferdigstilteAvklaringer)) {
            return;
        }
        this.ferdigstilteAvklaringer = nyHolder;
    }

    public Long getId() {
        return id;
    }

    public Long getBehandlingId() {
        return behandlingId;
    }

    public VilkårType getVilkårType() {
        return vilkårType;
    }

    /**
     * Avklaringene som er foreslått og behandlet i denne behandlingen — uavhengig av om de er ferdigstilt.
     */
    public Set<VilkårPeriodeAvklaring> getForeslåtteAvklaringer() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(foreslåtteAvklaringer));
    }

    /**
     * Alle ferdigstilte avklaringer på saken for dette vilkåret, akkumulert på tvers av behandlinger.
     */
    public Set<VilkårPeriodeAvklaring> getFerdigstilteAvklaringer() {
        return Optional.ofNullable(ferdigstilteAvklaringer)
            .map(VilkårAvklaringHolder::hentFerdigstilteAvklaringer)
            .orElse(Set.of());
    }

    public boolean isAktiv() {
        return aktiv;
    }

    void deaktiver() {
        this.aktiv = false;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof VilkårsavklaringGrunnlag that)) return false;
        return vilkårType == that.vilkårType
            && Objects.equals(foreslåtteAvklaringer, that.foreslåtteAvklaringer)
            && Objects.equals(ferdigstilteAvklaringer, that.ferdigstilteAvklaringer);
    }

    @Override
    public int hashCode() {
        return Objects.hash(vilkårType, foreslåtteAvklaringer, ferdigstilteAvklaringer);
    }

    @Override
    public String toString() {
        return "VilkårsavklaringGrunnlag{behandlingId=" + behandlingId
            + ", vilkårType=" + vilkårType
            + ", aktiv=" + aktiv + '}';
    }

    /**
     * Nytt grunnlag for samme behandling, med kopi av de foreslåtte avklaringene og referanse til samme holder.
     * Brukes ved lagring av nye/endrede avklaringer på en behandling som allerede har et grunnlag.
     */
    public static VilkårsavklaringGrunnlag nyttGrunnlagMedReferanserFra(VilkårsavklaringGrunnlag grunnlag) {
        return new VilkårsavklaringGrunnlag(
            grunnlag.getBehandlingId(),
            grunnlag.getVilkårType(),
            grunnlag.foreslåtteAvklaringer,
            grunnlag.ferdigstilteAvklaringer
        );
    }

    /**
     * Nytt grunnlag for en ny behandling, med referanse til samme holder (ferdigstilte avklaringer) som den
     * forrige behandlingen. Foreslåtte avklaringer utelates strukturelt — de gjelder kun for behandlingen de
     * ble foreslått i.
     */
    public static VilkårsavklaringGrunnlag nyttGrunnlagForBehandlingMedReferanserFra(Long behandlingId, VilkårsavklaringGrunnlag grunnlag) {
        return new VilkårsavklaringGrunnlag(
            behandlingId,
            grunnlag.getVilkårType(),
            Set.of(),
            grunnlag.ferdigstilteAvklaringer
        );
    }
}
