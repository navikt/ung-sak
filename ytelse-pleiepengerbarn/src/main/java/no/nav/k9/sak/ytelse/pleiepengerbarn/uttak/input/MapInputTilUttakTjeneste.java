package no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.input;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.domene.uttak.repo.UttakRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.medisinsk.MedisinskGrunnlagRepository;

@Dependent
public class MapInputTilUttakTjeneste {

    private VilkårResultatRepository vilkårResultatRepository;
    private MedisinskGrunnlagRepository medisinskGrunnlagRepository;
    private UttakRepository uttakRepository;

    @Inject
    public MapInputTilUttakTjeneste(VilkårResultatRepository vilkårResultatRepository,
                                    MedisinskGrunnlagRepository medisinskGrunnlagRepository,
                                    UttakRepository uttakRepository) {
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.medisinskGrunnlagRepository = medisinskGrunnlagRepository;
        this.uttakRepository = uttakRepository;
    }

    public void hentUtOgMapRequest(BehandlingReferanse referanse) {
        var vilkårene = vilkårResultatRepository.hent(referanse.getBehandlingId());
        var medisinskGrunnlag = medisinskGrunnlagRepository.hent(referanse.getBehandlingId());
        var uttakGrunnlag = uttakRepository.hentGrunnlag(referanse.getBehandlingId()).orElseThrow();

        var oppgittUttak = uttakGrunnlag.getOppgittUttak();
    }
}
