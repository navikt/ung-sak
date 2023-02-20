package no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.registerinnhenting;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OMSORGSPENGER_MA;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.aktør.Personinfo;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.domene.registerinnhenting.YtelsesspesifikkRelasjonsFilter;
import no.nav.k9.sak.typer.Periode;

@FagsakYtelseTypeRef(OMSORGSPENGER_MA)
@ApplicationScoped
public class AlleBarnOgTaMedHistorikk implements YtelsesspesifikkRelasjonsFilter {

    @Override
    public List<Personinfo> relasjonsFiltreringBarn(Behandling behandling, List<Personinfo> barn, Periode opplysningsperioden) {
        return barn;
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
