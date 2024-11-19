package no.nav.ung.sak.ytelse.ung.registerdata;

import static no.nav.ung.kodeverk.behandling.FagsakYtelseType.UNGDOMSYTELSE;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.aktør.Personinfo;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.domene.registerinnhenting.YtelsesspesifikkRelasjonsFilter;
import no.nav.ung.sak.typer.Periode;

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
