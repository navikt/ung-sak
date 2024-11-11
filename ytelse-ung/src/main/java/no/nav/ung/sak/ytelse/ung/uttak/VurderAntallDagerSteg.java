package no.nav.ung.sak.ytelse.ung.uttak;

import static no.nav.k9.kodeverk.behandling.BehandlingStegType.VURDER_ANTALL_DAGER;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.UNGDOMSYTELSE;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.ung.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.ung.sak.behandlingskontroll.BehandlingSteg;
import no.nav.ung.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.ung.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.ung.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.vilkår.VilkårTjeneste;
import no.nav.ung.sak.ytelse.ung.beregning.UngdomsytelseGrunnlagRepository;

@FagsakYtelseTypeRef(UNGDOMSYTELSE)
@BehandlingStegRef(value = VURDER_ANTALL_DAGER)
@BehandlingTypeRef
@ApplicationScoped
public class VurderAntallDagerSteg implements BehandlingSteg {

    private VilkårTjeneste vilkårTjeneste;
    private UngdomsytelseGrunnlagRepository ungdomsytelseGrunnlagRepository;

    @Inject
    public VurderAntallDagerSteg(VilkårTjeneste vilkårTjeneste, UngdomsytelseGrunnlagRepository ungdomsytelseGrunnlagRepository) {
        this.vilkårTjeneste = vilkårTjeneste;
        this.ungdomsytelseGrunnlagRepository = ungdomsytelseGrunnlagRepository;
    }

    public VurderAntallDagerSteg() {
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        var samletVilkårResultatTidslinje = vilkårTjeneste.samletVilkårsresultat(kontekst.getBehandlingId());
        var godkjentePerioder = samletVilkårResultatTidslinje
            .mapValue(it -> it.getSamletUtfall().equals(Utfall.OPPFYLT))
            .filterValue(Boolean.TRUE::equals);
        var ungdomsytelseUttakPerioder = VurderAntallDagerTjeneste.vurderAntallDagerOgLagUttaksperioder(godkjentePerioder);
        ungdomsytelseUttakPerioder.ifPresent(it -> ungdomsytelseGrunnlagRepository.lagre(kontekst.getBehandlingId(), it)); ;
        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }


}
