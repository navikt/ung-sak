package no.nav.k9.sak.domene.registerinnhenting;

import java.util.List;
import java.util.Set;

import jakarta.enterprise.inject.Instance;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.aktør.Personinfo;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Periode;

public interface YtelsesspesifikkRelasjonsFilter {
    static YtelsesspesifikkRelasjonsFilter finnTjeneste(Instance<YtelsesspesifikkRelasjonsFilter> instances, FagsakYtelseType ytelseType) {
        return FagsakYtelseTypeRef.Lookup.find(instances, ytelseType)
            .orElseThrow(() -> new IllegalStateException("Har ikke relasjonsfilter for ytelseType=" + ytelseType));
    }

    boolean hentHistorikkForRelatertePersoner();

    boolean hentDeltBosted();

    List<Personinfo> relasjonsFiltreringBarn(Behandling behandling, List<Personinfo> barn, Periode opplysningsperioden);

    default Set<AktørId> hentFosterbarn(Behandling behandling, Periode opplysningsperioden) {
        return Set.of();
    }
}
