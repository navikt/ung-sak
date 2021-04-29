package no.nav.k9.sak.domene.iay.modell;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.sak.behandlingslager.diff.ChangeTracked;

public class OppgittOpptjeningAggregat {

    private static final Logger logger = LoggerFactory.getLogger(OppgittOpptjeningAggregat.class);

    @ChangeTracked
    private List<OppgittOpptjening> oppgitteOpptjeninger = new ArrayList<>();


    OppgittOpptjeningAggregat() {
    }

    OppgittOpptjeningAggregat(OppgittOpptjeningAggregat oppgittOpptjeningAggregat) {
        this(oppgittOpptjeningAggregat.oppgitteOpptjeninger);
    }

    public OppgittOpptjeningAggregat(Collection<OppgittOpptjening> oppgitteOpptjeninger) {
        this.oppgitteOpptjeninger.addAll(oppgitteOpptjeninger.stream().map(it -> {
            final OppgittOpptjening oppgittOpptjening = new OppgittOpptjening(it);
            return oppgittOpptjening;
        }).collect(Collectors.toList()));
    }

    public List<OppgittOpptjening> getOppgitteOpptjeninger() {
        return oppgitteOpptjeninger;
    }

    public void leggTil(OppgittOpptjening oppgittOpptjening) {
        oppgitteOpptjeninger.add(oppgittOpptjening);
    }



    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || !(o instanceof OppgittOpptjeningAggregat))
            return false;
        OppgittOpptjeningAggregat that = (OppgittOpptjeningAggregat) o;
        return Objects.equals(oppgitteOpptjeninger, that.oppgitteOpptjeninger);
    }

    @Override
    public int hashCode() {
        return Objects.hash(oppgitteOpptjeninger);
    }
}
