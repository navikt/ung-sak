package no.nav.ung.sak.behandlingslager.behandling.medlemskap;

import jakarta.persistence.*;
import no.nav.ung.sak.behandlingslager.BaseEntitet;
import org.hibernate.annotations.BatchSize;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Entity(name = "OppgittForutgåendeMedlemskapHolder")
@Table(name = "OPPGITT_FMEDLEMSKAP_HOLDER")
public class OppgittForutgåendeMedlemskapHolder extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_OPPGITT_FMEDLEMSKAP_HOLDER")
    private Long id;

    @BatchSize(size = 20)
    @JoinColumn(name = "oppgitt_fmedlemskap_holder_id", nullable = false)
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<OppgittForutgåendeMedlemskapPeriode> perioder = new LinkedHashSet<>();

    public OppgittForutgåendeMedlemskapHolder() {
    }

    OppgittForutgåendeMedlemskapHolder(OppgittForutgåendeMedlemskapHolder other) {
        this.perioder = other.perioder.stream()
            .map(OppgittForutgåendeMedlemskapPeriode::new)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public Long getId() {
        return id;
    }

    public Set<OppgittForutgåendeMedlemskapPeriode> getPerioder() {
        return Collections.unmodifiableSet(perioder);
    }

    void leggTilPeriode(OppgittForutgåendeMedlemskapPeriode periode) {
        this.perioder.add(periode);
    }
}
