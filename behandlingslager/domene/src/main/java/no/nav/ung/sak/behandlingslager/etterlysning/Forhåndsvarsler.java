package no.nav.ung.sak.behandlingslager.etterlysning;

import no.nav.ung.sak.behandlingslager.BaseEntitet;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPerioder;

import java.util.Objects;
import java.util.Set;

public class Forhåndsvarsler extends BaseEntitet {
    private Long id;

    private Set<Etterlysning> varsler;

    public Forhåndsvarsler() {
    }

    public Forhåndsvarsler(Set<Etterlysning> varsler) {
        this.varsler = varsler;
    }

    public Set<Etterlysning> getVarsler() {
        return varsler;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Forhåndsvarsler that)) return false;
        return Objects.equals(varsler, that.varsler);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(varsler);
    }
}
