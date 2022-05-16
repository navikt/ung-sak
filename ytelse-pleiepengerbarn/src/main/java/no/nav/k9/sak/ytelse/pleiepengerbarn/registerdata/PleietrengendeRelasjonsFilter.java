package no.nav.k9.sak.ytelse.pleiepengerbarn.registerdata;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_NÆRSTÅENDE;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_SYKT_BARN;

import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.aktør.Personinfo;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.domene.registerinnhenting.YtelsesspesifikkRelasjonsFilter;
import no.nav.k9.sak.typer.Periode;

@FagsakYtelseTypeRef(PLEIEPENGER_SYKT_BARN)
@FagsakYtelseTypeRef(PLEIEPENGER_NÆRSTÅENDE)
@ApplicationScoped
public class PleietrengendeRelasjonsFilter implements YtelsesspesifikkRelasjonsFilter {

    @Override
    public List<Personinfo> relasjonsFiltreringBarn(Behandling behandling, List<Personinfo> barn, Periode opplysningsperioden) {
        final var pleietrengende = Optional.ofNullable(behandling.getFagsak().getPleietrengendeAktørId());
        if (pleietrengende.isEmpty()) {
            return List.of();
        }
        return barn.stream()
            .filter(barnet -> barnet.getAktørId().equals(pleietrengende.orElse(null)))
            .toList();
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
