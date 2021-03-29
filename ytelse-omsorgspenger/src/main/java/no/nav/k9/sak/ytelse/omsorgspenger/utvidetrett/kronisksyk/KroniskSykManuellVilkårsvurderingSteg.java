package no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.kronisksyk;

import java.time.LocalDate;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.kodeverk.behandling.BehandlingResultatType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingSteg;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

@FagsakYtelseTypeRef("OMP_KS")
@BehandlingStegRef(kode = "MANUELL_VILKÅRSVURDERING")
@BehandlingTypeRef
@ApplicationScoped
public class KroniskSykManuellVilkårsvurderingSteg implements BehandlingSteg {

    private static final Logger log = LoggerFactory.getLogger(KroniskSykManuellVilkårsvurderingSteg.class);

    private BehandlingRepository behandlingRepository;
    private VilkårResultatRepository vilkårResultatRepository;

    private AksjonspunktDefinisjon aksjonspunktDef = AksjonspunktDefinisjon.VURDER_OMS_UTVIDET_RETT;
    private SøknadRepository søknadRepository;

    public KroniskSykManuellVilkårsvurderingSteg() {
        // CDO
    }

    @Inject
    public KroniskSykManuellVilkårsvurderingSteg(BehandlingRepository behandlingRepository,
                                                            SøknadRepository søknadRepository,
                                                            VilkårResultatRepository vilkårResultatRepository) {
        this.behandlingRepository = behandlingRepository;
        this.søknadRepository = søknadRepository;
        this.vilkårResultatRepository = vilkårResultatRepository;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        Long behandlingId = kontekst.getBehandlingId();
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var fagsak = behandling.getFagsak();

        var søknad = søknadRepository.hentSøknad(behandling);
        var vilkårene = vilkårResultatRepository.hent(behandlingId);
        var vilkår = vilkårene.getVilkår(VilkårType.UTVIDETRETT);

        if (erNoenVilkårHeltAvslått(vilkårene, søknad.getMottattDato(), fagsak.getPeriode().getTomDato())) {
            behandling.getAksjonspunktMedDefinisjonOptional(aksjonspunktDef).ifPresent(a -> a.avbryt());
            behandling.setBehandlingResultatType(BehandlingResultatType.AVSLÅTT);
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        }

        if (vilkår.get().getPerioder().stream().anyMatch(v -> v.getUtfall() == Utfall.IKKE_VURDERT)) {
            return BehandleStegResultat.utførtMedAksjonspunkter(List.of(aksjonspunktDef));
        } else {
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        }

    }

    private boolean erNoenVilkårHeltAvslått(Vilkårene vilkårene, LocalDate fom, LocalDate tom) {
        if (vilkårene.getHarAvslåtteVilkårsPerioder()) {
            boolean heltAvslått = true;
            var periode = DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom);
            for (var v : vilkårene.getVilkårTidslinjer(periode).entrySet()) {
                var timeline = v.getValue();
                if (timeline.isEmpty()) {
                    continue;
                }
                if (v.getKey() == VilkårType.UTVIDETRETT) {
                    // skip oss selv
                    continue;
                }
                var altAvslått = timeline.toSegments().stream().allMatch(vp -> vp.getValue().getUtfall() == Utfall.IKKE_OPPFYLT);
                if (altAvslått) {
                    log.info("Alle perioder avslått for vilkår {}, maksPeriode={}", v.getKey(), periode);
                    return true;
                }

                heltAvslått = false;
            }
            return heltAvslått;
        }
        return false;
    }

}
