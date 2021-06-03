package no.nav.k9.sak.domene.registerinnhenting;

import java.util.Optional;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.EndringsresultatDiff;
import no.nav.k9.sak.behandlingslager.behandling.EndringsresultatSnapshot;

public interface DiffUtvidetBehandlingsgrunnlagTjeneste {

    public static Optional<DiffUtvidetBehandlingsgrunnlagTjeneste> finnTjeneste(FagsakYtelseType ytelseType) {
        return FagsakYtelseTypeRef.Lookup.find(DiffUtvidetBehandlingsgrunnlagTjeneste.class, ytelseType);
    }

    public void leggTilSnapshot(BehandlingReferanse ref, EndringsresultatSnapshot snapshot);

    public void leggTilDiffResultat(BehandlingReferanse ref, EndringsresultatDiff idDiff, EndringsresultatDiff sporedeEndringerDiff);
}
