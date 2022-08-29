package no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.registerinnhenting;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OMSORGSPENGER_AO;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OMSORGSPENGER_KS;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.aktør.Personinfo;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.domene.registerinnhenting.YtelsesspesifikkRelasjonsFilter;
import no.nav.k9.sak.typer.Periode;

@FagsakYtelseTypeRef(OMSORGSPENGER_KS)
@FagsakYtelseTypeRef(OMSORGSPENGER_AO)
@ApplicationScoped
public class PleietrengendeOgTaMedHistorikk implements YtelsesspesifikkRelasjonsFilter {

    @Override
    public List<Personinfo> relasjonsFiltreringBarn(Behandling behandling, List<Personinfo> barn, Periode opplysningsperioden) {
        return barn.stream()
            .filter(barnet -> barnet.getAktørId().equals(behandling.getFagsak().getPleietrengendeAktørId()))
            .toList();
    }

    @Override
    public boolean hentHistorikkForRelatertePersoner() {
        return true;
    }

    @Override
    public boolean hentDeltBosted() {
        return true;
    }
}
