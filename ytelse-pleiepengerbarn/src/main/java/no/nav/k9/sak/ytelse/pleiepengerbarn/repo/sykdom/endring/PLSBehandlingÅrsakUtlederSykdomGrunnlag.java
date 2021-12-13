package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.endring;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.BehandlingStatus;
import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.domene.registerinnhenting.GrunnlagRef;
import no.nav.k9.sak.domene.registerinnhenting.impl.behandlingårsak.BehandlingÅrsakUtleder;
import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomGrunnlagBehandling;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomGrunnlagRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomGrunnlagService;

@ApplicationScoped
@GrunnlagRef("SykdomGrunnlag")
@FagsakYtelseTypeRef("PPN")
class PLSBehandlingÅrsakUtlederSykdomGrunnlag implements BehandlingÅrsakUtleder {

    private BehandlingRepository behandlingRepository;
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;
    private SykdomGrunnlagRepository sykdomGrunnlagRepository;
    private SykdomGrunnlagService sykdomGrunnlagService;
    private VilkårResultatRepository vilkårResultatRepository;

    PLSBehandlingÅrsakUtlederSykdomGrunnlag() {
        // CDI
    }

    @Inject
    PLSBehandlingÅrsakUtlederSykdomGrunnlag(BehandlingRepository behandlingRepository,
                                            BehandlingskontrollTjeneste behandlingskontrollTjeneste,
                                            SykdomGrunnlagRepository sykdomGrunnlagRepository,
                                            SykdomGrunnlagService sykdomGrunnlagService,
                                            VilkårResultatRepository vilkårResultatRepository) {
        this.behandlingRepository = behandlingRepository;
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
        this.sykdomGrunnlagRepository = sykdomGrunnlagRepository;
        this.sykdomGrunnlagService = sykdomGrunnlagService;
        this.vilkårResultatRepository = vilkårResultatRepository;;
    }

    @Override
    public Set<BehandlingÅrsakType> utledBehandlingÅrsaker(BehandlingReferanse ref, Object grunnlagId1, Object grunnlagId2) {

        var årsaker = new HashSet<>(Set.of(BehandlingÅrsakType.RE_ENDRING_FRA_ANNEN_OMSORGSPERSON));

        var harEndringerForSykdom = harEndringerForSykdom(ref);

        if (harEndringerForSykdom) {
            årsaker.add(BehandlingÅrsakType.RE_SYKDOM_ENDRING_FRA_ANNEN_OMSORGSPERSON);
        }

        return årsaker;
    }

    private boolean harEndringerForSykdom(BehandlingReferanse ref) {
        var sykdomGrunnlag = sykdomGrunnlagRepository.hentGrunnlagForBehandling(ref.getBehandlingUuid())
            .map(SykdomGrunnlagBehandling::getGrunnlag);

        List<Periode> nyeVurderingsperioder = utledVurderingsperiode(ref.getBehandlingId());
        var utledGrunnlag = sykdomGrunnlagService.utledGrunnlagMedManglendeOmsorgFjernet(ref.getSaksnummer(), ref.getBehandlingUuid(), ref.getBehandlingId(), ref.getPleietrengendeAktørId(), nyeVurderingsperioder);
        var sykdomGrunnlagSammenlikningsresultat = sykdomGrunnlagService.sammenlignGrunnlag(sykdomGrunnlag, utledGrunnlag);

        return !sykdomGrunnlagSammenlikningsresultat.getDiffPerioder().isEmpty() && skalGiÅrsak(ref, BehandlingStegType.VURDER_MEDISINSKVILKÅR);
    }

    private boolean skalGiÅrsak(BehandlingReferanse ref, BehandlingStegType stegType) {
        if (BehandlingStatus.UTREDES.equals(ref.getBehandlingStatus()) && behandlingskontrollTjeneste.erIStegEllerSenereSteg(ref.getBehandlingId(), stegType)) {
            return true;
        }
        if (BehandlingStatus.UTREDES.equals(ref.getBehandlingStatus()) && Objects.equals(behandlingRepository.hentBehandling(ref.getBehandlingId()).getAktivtBehandlingSteg(), stegType)) {
            return true;
        }
        return BehandlingStatus.OPPRETTET.equals(ref.getBehandlingStatus());
    }

    private List<Periode> utledVurderingsperiode(Long behandlingId) {
        var vilkårene = vilkårResultatRepository.hentHvisEksisterer(behandlingId);
        if (vilkårene.isEmpty()) {
            return List.of();
        }
        var vurderingsperioder = vilkårene.get().getVilkår(VilkårType.I_LIVETS_SLUTTFASE)
            .map(Vilkår::getPerioder)
            .orElse(List.of())
            .stream()
            .map(VilkårPeriode::getPeriode)
            .map(it -> new Periode(it.getFomDato(), it.getTomDato()))
            .collect(Collectors.toCollection(ArrayList::new));
        return vurderingsperioder;
    }
}
