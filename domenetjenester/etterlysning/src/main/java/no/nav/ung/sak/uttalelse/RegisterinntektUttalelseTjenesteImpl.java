package no.nav.ung.sak.uttalelse;

import jakarta.enterprise.context.Dependent;

import java.util.List;

@Dependent
public class RegisterinntektUttalelseTjenesteImpl implements RegisterinntektUttalelseTjeneste {
    @Override
    public List<BrukersUttalelsePeriode> hentUttalelser(Long behandlingId) {
        return List.of();
    }
}
