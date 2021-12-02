package no.nav.k9.sak.ytelse.pleiepengerbarn.uttak;

import java.util.HashMap;
import java.util.Map;
import java.util.NavigableSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingSteg;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.tjeneste.UttakTjeneste;
import no.nav.pleiepengerbarn.uttak.kontrakter.EndrePerioderGrunnlag;
import no.nav.pleiepengerbarn.uttak.kontrakter.LukketPeriode;
import no.nav.pleiepengerbarn.uttak.kontrakter.Årsak;

@ApplicationScoped
@BehandlingStegRef(kode = "BEKREFT_UTTAK")
@BehandlingTypeRef
@FagsakYtelseTypeRef("PSB")
public class BekreftUttakSteg implements BehandlingSteg {

    private BehandlingRepository behandlingRepository;
    private VilkårResultatRepository vilkårResultatRepository;
    private UttakTjeneste uttakTjeneste;
    private VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste;

    BekreftUttakSteg() {
        // CDI
    }

    @Inject
    private BekreftUttakSteg(BehandlingRepository behandlingRepository,
                             VilkårResultatRepository vilkårResultatRepository,
                             UttakTjeneste uttakTjeneste,
                             @FagsakYtelseTypeRef("PSB") @BehandlingTypeRef VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.uttakTjeneste = uttakTjeneste;
        this.perioderTilVurderingTjeneste = perioderTilVurderingTjeneste;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        var vilkårene = vilkårResultatRepository.hent(kontekst.getBehandlingId());

        var perioderTilVurdering = perioderTilVurderingTjeneste.utled(kontekst.getBehandlingId(), VilkårType.BEREGNINGSGRUNNLAGVILKÅR);

        var beregningsgrunnlagsvilkåret = vilkårene.getVilkår(VilkårType.BEREGNINGSGRUNNLAGVILKÅR).orElseThrow();

        var perioderSomHarBlittAvslått = harNoenPerioderTilVurderingBlittAvslåttIBeregning(perioderTilVurdering, beregningsgrunnlagsvilkåret);

        if (!perioderSomHarBlittAvslått.isEmpty()) {
            var behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());

            var request = new EndrePerioderGrunnlag(behandling.getFagsak().getSaksnummer().getVerdi(), behandling.getUuid().toString(), opprettMap(perioderSomHarBlittAvslått));
            uttakTjeneste.bekreftUttaksplan(request);
        }

        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

    private Map<LukketPeriode, Årsak> opprettMap(NavigableSet<DatoIntervallEntitet> perioderSomHarBlittAvslått) {
        var map = new HashMap<LukketPeriode, Årsak>();
        for (DatoIntervallEntitet periode : perioderSomHarBlittAvslått) {
            map.put(new LukketPeriode(periode.getFomDato(), periode.getTomDato()), Årsak.FOR_LAV_INNTEKT);
        }
        return map;
    }

    private NavigableSet<DatoIntervallEntitet> harNoenPerioderTilVurderingBlittAvslåttIBeregning(NavigableSet<DatoIntervallEntitet> perioderTilVurdering, Vilkår beregningsgrunnlagsvilkåret) {
        return beregningsgrunnlagsvilkåret.getPerioder()
            .stream()
            .filter(it -> Utfall.IKKE_OPPFYLT.equals(it.getUtfall()) && perioderTilVurdering.stream().anyMatch(at -> at.overlapper(it.getPeriode())))
            .map(VilkårPeriode::getPeriode)
            .collect(Collectors.toCollection(TreeSet::new));
    }
}
