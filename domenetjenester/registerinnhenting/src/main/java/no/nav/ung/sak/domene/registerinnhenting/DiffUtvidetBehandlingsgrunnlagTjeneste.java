package no.nav.ung.sak.domene.registerinnhenting;

import java.util.Optional;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.EndringsresultatDiff;
import no.nav.ung.sak.behandlingslager.behandling.EndringsresultatSnapshot;

public interface DiffUtvidetBehandlingsgrunnlagTjeneste {

    public static Optional<DiffUtvidetBehandlingsgrunnlagTjeneste> finnTjeneste(FagsakYtelseType ytelseType) {
        return FagsakYtelseTypeRef.Lookup.find(DiffUtvidetBehandlingsgrunnlagTjeneste.class, ytelseType);
    }

    public void leggTilSnapshot(BehandlingReferanse ref, EndringsresultatSnapshot snapshot);

    public void leggTilDiffResultat(BehandlingReferanse ref, EndringsresultatDiff idDiff, EndringsresultatDiff sporedeEndringerDiff);
}
