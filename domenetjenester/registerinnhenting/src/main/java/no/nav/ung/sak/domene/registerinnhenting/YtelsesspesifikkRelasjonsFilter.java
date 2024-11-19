package no.nav.ung.sak.domene.registerinnhenting;

import java.util.List;
import java.util.Set;

import jakarta.enterprise.inject.Instance;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.aktør.Personinfo;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.Periode;

public interface YtelsesspesifikkRelasjonsFilter {
    static YtelsesspesifikkRelasjonsFilter finnTjeneste(Instance<YtelsesspesifikkRelasjonsFilter> instances, FagsakYtelseType ytelseType) {
        return FagsakYtelseTypeRef.Lookup.find(instances, ytelseType)
            .orElseThrow(() -> new IllegalStateException("Har ikke relasjonsfilter for ytelseType=" + ytelseType));
    }

    boolean hentHistorikkForRelatertePersoner();

    boolean hentDeltBosted();

    List<Personinfo> relasjonsFiltreringBarn(Behandling behandling, List<Personinfo> barn, Periode opplysningsperioden);

    default Set<AktørId> hentFosterbarn(Behandling behandling) {
        return Set.of();
    }
}
