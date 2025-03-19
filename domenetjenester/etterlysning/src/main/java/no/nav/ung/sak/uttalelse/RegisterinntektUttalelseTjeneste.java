package no.nav.ung.sak.uttalelse;

import java.util.List;

public interface RegisterinntektUttalelseTjeneste {


    List<BrukersUttalelsePeriode> hentUttalelser(Long behandlingId);

}
