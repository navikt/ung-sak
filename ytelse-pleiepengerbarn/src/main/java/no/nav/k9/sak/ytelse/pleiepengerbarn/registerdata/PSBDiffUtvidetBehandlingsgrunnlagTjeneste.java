package no.nav.k9.sak.ytelse.pleiepengerbarn.registerdata;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_NÆRSTÅENDE;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_SYKT_BARN;

import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.EndringsresultatDiff;
import no.nav.k9.sak.behandlingslager.behandling.EndringsresultatSnapshot;
import no.nav.k9.sak.behandlingslager.diff.DiffResult;
import no.nav.k9.sak.domene.registerinnhenting.DiffUtvidetBehandlingsgrunnlagTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomGrunnlag;

@FagsakYtelseTypeRef(PLEIEPENGER_SYKT_BARN)
@FagsakYtelseTypeRef(PLEIEPENGER_NÆRSTÅENDE)
@ApplicationScoped
public class PSBDiffUtvidetBehandlingsgrunnlagTjeneste implements DiffUtvidetBehandlingsgrunnlagTjeneste {

    @Inject
    public PSBDiffUtvidetBehandlingsgrunnlagTjeneste() {
    }

    @Override
    public void leggTilSnapshot(BehandlingReferanse ref, EndringsresultatSnapshot snapshot) {
        var uuid = UUID.randomUUID();
        snapshot.leggTil(EndringsresultatSnapshot.medSnapshot(SykdomGrunnlag.class, uuid)); // For å tvinge frem at det alltid er endring
    }

    @Override
    public void leggTilDiffResultat(BehandlingReferanse ref, EndringsresultatDiff idDiff, EndringsresultatDiff sporedeEndringerDiff) {
        idDiff.hentDelresultat(SykdomGrunnlag.class)
            .ifPresent(idEndring -> sporedeEndringerDiff.leggTilSporetEndring(idEndring, () -> diffSykdom(ref)));
    }

    private DiffResult diffSykdom(BehandlingReferanse ref) {
        return new PSBGrunnlagDiff();
    }
}
