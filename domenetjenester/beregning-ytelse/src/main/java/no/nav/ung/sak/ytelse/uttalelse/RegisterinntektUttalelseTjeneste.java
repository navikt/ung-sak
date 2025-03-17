package no.nav.ung.sak.ytelse.uttalelse;

import java.util.List;

public interface RegisterinntektUttalelseTjeneste {


    List<BrukersUttalelsePeriode> hentUttalelser(Long behandlingId);

}
