package no.nav.k9.sak.domene.behandling.steg.inngangsvilkår.opptjening;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetKlassifisering;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.opptjening.OpptjeningAktivitet;
import no.nav.k9.sak.behandlingslager.behandling.opptjening.OpptjeningRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.domene.behandling.steg.inngangsvilkår.InngangsvilkårFellesTjeneste;
import no.nav.k9.sak.inngangsvilkår.opptjening.regelmodell.OpptjeningsvilkårResultat;

@BehandlingStegRef(kode = "VURDER_OPPTJ")
@BehandlingTypeRef
@FagsakYtelseTypeRef
@ApplicationScoped
public class VurderOpptjeningsvilkårSteg extends VurderOpptjeningsvilkårStegFelles {

    @Inject
    public VurderOpptjeningsvilkårSteg(BehandlingRepositoryProvider repositoryProvider, OpptjeningRepository opptjeningRepository, InngangsvilkårFellesTjeneste inngangsvilkårFellesTjeneste) {
        super(repositoryProvider, opptjeningRepository, inngangsvilkårFellesTjeneste, BehandlingStegType.VURDER_OPPTJENINGSVILKÅR);
    }


    @Override
    protected List<OpptjeningAktivitet> mapTilOpptjeningsaktiviteter(MapTilOpptjeningAktiviteter mapper, OpptjeningsvilkårResultat oppResultat) {
        List<OpptjeningAktivitet> aktiviteter = new ArrayList<>();
        aktiviteter.addAll(mapper.map(oppResultat.getUnderkjentePerioder(), OpptjeningAktivitetKlassifisering.BEKREFTET_AVVIST));
        aktiviteter.addAll(mapper.map(oppResultat.getBekreftetGodkjentePerioder(), OpptjeningAktivitetKlassifisering.BEKREFTET_GODKJENT));
        return aktiviteter;
    }
}
