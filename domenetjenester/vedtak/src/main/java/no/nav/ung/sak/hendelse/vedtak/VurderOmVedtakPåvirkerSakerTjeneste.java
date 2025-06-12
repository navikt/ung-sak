package no.nav.ung.sak.hendelse.vedtak;

import java.util.List;
import java.util.Optional;

import no.nav.abakus.vedtak.ytelse.Ytelse;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;

public interface VurderOmVedtakPåvirkerSakerTjeneste {

    List<SakMedPeriode> utledSakerMedPerioderSomErKanVærePåvirket(Ytelse vedtakHendelse);

}
