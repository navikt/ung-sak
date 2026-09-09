package no.nav.ung.sak.behandlingslager.vilkårsavklaring;

import jakarta.persistence.*;
import no.nav.ung.sak.behandlingslager.BaseEntitet;
import org.hibernate.annotations.BatchSize;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Aggregat/holder for foreslåtte vilkårsavklaringer for ett vilkår. Holderen deles mellom grunnlag innenfor samme
 * behandling så lenge innholdet er uendret, men aldri på tvers av behandlinger — foreslåtte avklaringer gjelder kun
 * behandlingen de ble foreslått i, jf. {@link VilkårsavklaringGrunnlag}.
 * Klassen er pakkeprivat med vilje: mutasjon skal kun skje gjennom setterne på {@link VilkårsavklaringGrunnlag},
 * som sørger for at det lages en ny holder-instans ved endring slik at et delt sett aldri muteres.
 */
@Entity(name = "VilkårAvklaringForeslåttHolder")
@Table(name = "VILKAAR_AVKLARING_FORESLAATT_HOLDER")
class VilkårAvklaringForeslåttHolder extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_VILKAAR_AVKLARING_FORES_HOLDER")
    private Long id;

    @BatchSize(size = 20)
    @JoinColumn(name = "vilkaar_avklaring_fores_holder_id", nullable = false)
    @OneToMany(cascade = CascadeType.ALL)
    private Set<VilkårPeriodeAvklaringForeslått> periodeAvklaringerForeslått = new LinkedHashSet<>();

    public VilkårAvklaringForeslåttHolder() {
    }

    private VilkårAvklaringForeslåttHolder(Collection<VilkårPeriodeAvklaringForeslått> avklaringer) {
        this.periodeAvklaringerForeslått = avklaringer.stream()
            .map(VilkårPeriodeAvklaringForeslått::new)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * Lager en ny holder med kopier av avklaringene. Brukes av setteren på {@link VilkårsavklaringGrunnlag} for å
     * unngå å mutere en holder som kan være delt med et tidligere grunnlag.
     */
    static VilkårAvklaringForeslåttHolder lagHolder(Collection<VilkårPeriodeAvklaringForeslått> avklaringer) {
        return new VilkårAvklaringForeslåttHolder(avklaringer);
    }

    Long getId() {
        return id;
    }

    /**
     * Avklaringene som er foreslått og behandlet i behandlingen som eier grunnlaget.
     */
    Set<VilkårPeriodeAvklaringForeslått> hentForeslåtteAvklaringer() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(periodeAvklaringerForeslått));
    }

    /**
     * En holder som ikke er satt er innholdsmessig det samme som en tom holder — brukes for å unngå at det lages
     * en ny (tom) holder når et grunnlag uten avklaringer får satt et tomt sett.
     */
    boolean harSammeInnholdSom(VilkårAvklaringForeslåttHolder other) {
        return other == null ? periodeAvklaringerForeslått.isEmpty() : equals(other);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof VilkårAvklaringForeslåttHolder that)) return false;
        return Objects.equals(periodeAvklaringerForeslått, that.periodeAvklaringerForeslått);
    }

    @Override
    public int hashCode() {
        return Objects.hash(periodeAvklaringerForeslått);
    }
}
