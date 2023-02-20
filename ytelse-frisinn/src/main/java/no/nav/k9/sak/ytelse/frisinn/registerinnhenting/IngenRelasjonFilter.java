package no.nav.k9.sak.ytelse.frisinn.registerinnhenting;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.FRISINN;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.aktør.Personinfo;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.domene.registerinnhenting.YtelsesspesifikkRelasjonsFilter;
import no.nav.k9.sak.typer.Periode;

@FagsakYtelseTypeRef(FRISINN)
@ApplicationScoped
public class IngenRelasjonFilter implements YtelsesspesifikkRelasjonsFilter {

    @Override
    public List<Personinfo> relasjonsFiltreringBarn(Behandling behandling, List<Personinfo> barn, Periode opplysningsperioden) {
        return List.of();
    }

    @Override
    public boolean hentHistorikkForRelatertePersoner() {
        return false;
    }

    @Override
    public boolean hentDeltBosted() {
        return false;
    }
}
