package no.nav.k9.sak.ytelse.ung.registerdata;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.UNGDOMSYTELSE;

import java.util.List;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.aktør.Personinfo;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.domene.registerinnhenting.YtelsesspesifikkRelasjonsFilter;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Periode;

@FagsakYtelseTypeRef(UNGDOMSYTELSE)
@ApplicationScoped
public class AlleBarnUtenHistorikkForRelatertePersoner implements YtelsesspesifikkRelasjonsFilter {

    @Override
    public List<Personinfo> relasjonsFiltreringBarn(Behandling behandling, List<Personinfo> barn, Periode opplysningsperioden) {
        return barn;
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
