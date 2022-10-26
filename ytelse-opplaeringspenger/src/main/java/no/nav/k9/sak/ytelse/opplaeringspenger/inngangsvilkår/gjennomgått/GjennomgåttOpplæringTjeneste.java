package no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.gjennomgått;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OPPLÆRINGSPENGER;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;

@Dependent
public class GjennomgåttOpplæringTjeneste {

    private final VilkårResultatRepository vilkårResultatRepository;
    private final OpplæringPeriodeSomTrengerVurderingUtleder periodeUtleder;
    private final VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste;

    @Inject
    public GjennomgåttOpplæringTjeneste(VilkårResultatRepository vilkårResultatRepository,
                                        @FagsakYtelseTypeRef(OPPLÆRINGSPENGER) VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste) {
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.perioderTilVurderingTjeneste = perioderTilVurderingTjeneste;
        this.periodeUtleder = new OpplæringPeriodeSomTrengerVurderingUtleder();
    }

    public Aksjon vurder(BehandlingReferanse referanse) {

        var vilkårene = vilkårResultatRepository.hent(referanse.getBehandlingId());

        var perioderTilVurdering = perioderTilVurderingTjeneste.utled(referanse.getBehandlingId(), VilkårType.GJENNOMGÅ_OPPLÆRING);

        if (periodeUtleder.trengerVurderingFraSaksbehandler(perioderTilVurdering, vilkårene)) {
            return Aksjon.TRENGER_AVKLARING;
        }

        var perioderSomSkalVurderes = periodeUtleder.utled(perioderTilVurdering, vilkårene);

        return Aksjon.FORTSETT;
    }
}
