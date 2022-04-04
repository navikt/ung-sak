package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.endring;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_NÆRSTÅENDE;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_SYKT_BARN;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
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
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.etablerttilsyn.ErEndringPåEtablertTilsynTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomGrunnlag;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomGrunnlagBehandling;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomGrunnlagRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomGrunnlagService;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.unntaketablerttilsyn.EndringUnntakEtablertTilsynTjeneste;

@ApplicationScoped
@GrunnlagRef(SykdomGrunnlag.class)
@FagsakYtelseTypeRef(PLEIEPENGER_SYKT_BARN)
@FagsakYtelseTypeRef(PLEIEPENGER_NÆRSTÅENDE)
class BehandlingÅrsakUtlederSykdomGrunnlag implements BehandlingÅrsakUtleder {

    private BehandlingRepository behandlingRepository;
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;
    private SykdomGrunnlagRepository sykdomGrunnlagRepository;
    private SykdomGrunnlagService sykdomGrunnlagService;
    private VilkårResultatRepository vilkårResultatRepository;
    private ErEndringPåEtablertTilsynTjeneste erEndringPåEtablertTilsynTjeneste;
    private EndringUnntakEtablertTilsynTjeneste endringUnntakEtablertTilsynTjeneste;

    BehandlingÅrsakUtlederSykdomGrunnlag() {
        // CDI
    }

    @Inject
    BehandlingÅrsakUtlederSykdomGrunnlag(BehandlingRepository behandlingRepository,
                                         BehandlingskontrollTjeneste behandlingskontrollTjeneste,
                                         SykdomGrunnlagRepository sykdomGrunnlagRepository,
                                         SykdomGrunnlagService sykdomGrunnlagService,
                                         VilkårResultatRepository vilkårResultatRepository,
                                         ErEndringPåEtablertTilsynTjeneste erEndringPåEtablertTilsynTjeneste,
                                         EndringUnntakEtablertTilsynTjeneste endringUnntakEtablertTilsynTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
        this.sykdomGrunnlagRepository = sykdomGrunnlagRepository;
        this.sykdomGrunnlagService = sykdomGrunnlagService;
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.erEndringPåEtablertTilsynTjeneste = erEndringPåEtablertTilsynTjeneste;
        this.endringUnntakEtablertTilsynTjeneste = endringUnntakEtablertTilsynTjeneste;
    }

    @Override
    public Set<BehandlingÅrsakType> utledBehandlingÅrsaker(BehandlingReferanse ref, Object grunnlagId1, Object grunnlagId2) {

        var årsaker = new HashSet<>(Set.of(BehandlingÅrsakType.RE_ENDRING_FRA_ANNEN_OMSORGSPERSON));

        var harEndringerForSykdom = harEndringerForSykdom(ref);
        var harEndringerForEtablertTilsyn = harEndringerForEtablertTilsyn(ref);
        var harEndringerForNattevåkOgBeredskap = harEndringerForNattevåkOgBeredskap(ref);

        if (harEndringerForSykdom && harEndringerForEtablertTilsyn && harEndringerForNattevåkOgBeredskap) {
            årsaker.add(BehandlingÅrsakType.RE_SYKDOM_ETABLERT_TILSYN_NATTVÅK_ENDRING_FRA_ANNEN_OMSORGSPERSON);
        } else if (harEndringerForEtablertTilsyn && harEndringerForSykdom) {
            årsaker.add(BehandlingÅrsakType.RE_SYKDOM_ETABLERT_TILSYN_ENDRING_FRA_ANNEN_OMSORGSPERSON);
        } else if (harEndringerForNattevåkOgBeredskap && harEndringerForSykdom) {
            årsaker.add(BehandlingÅrsakType.RE_SYKDOM_NATTEVÅK_ENDRING_FRA_ANNEN_OMSORGSPERSON);
        } else if (harEndringerForNattevåkOgBeredskap && harEndringerForEtablertTilsyn) {
            årsaker.add(BehandlingÅrsakType.RE_NATTEVÅKBEREDSKAP_ETABLERT_TILSYN_ENDRING_FRA_ANNEN_OMSORGSPERSON);
        } else if (harEndringerForSykdom) {
            årsaker.add(BehandlingÅrsakType.RE_SYKDOM_ENDRING_FRA_ANNEN_OMSORGSPERSON);
        } else if (harEndringerForEtablertTilsyn) {
            årsaker.add(BehandlingÅrsakType.RE_ETABLERT_TILSYN_ENDRING_FRA_ANNEN_OMSORGSPERSON);
        } else if (harEndringerForNattevåkOgBeredskap) {
            årsaker.add(BehandlingÅrsakType.RE_NATTEVÅKBEREDSKAP_ENDRING_FRA_ANNEN_OMSORGSPERSON);
        }

        return årsaker;
    }

    private boolean harEndringerForNattevåkOgBeredskap(BehandlingReferanse ref) {
        return endringUnntakEtablertTilsynTjeneste.harEndringerSidenBehandling(ref.getBehandlingId(), ref.getPleietrengendeAktørId()) && skalGiÅrsak(ref, BehandlingStegType.KONTROLLER_FAKTA_UTTAK);
    }

    private boolean harEndringerForEtablertTilsyn(BehandlingReferanse referanse) {
        return erEndringPåEtablertTilsynTjeneste.erEndringerSidenBehandling(referanse) && skalGiÅrsak(referanse, BehandlingStegType.VURDER_UTTAK);
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
        var vurderingsperioder = vilkårene.get().getVilkår(VilkårType.MEDISINSKEVILKÅR_UNDER_18_ÅR)
            .map(Vilkår::getPerioder)
            .orElse(List.of())
            .stream()
            .map(VilkårPeriode::getPeriode)
            .map(it -> new Periode(it.getFomDato(), it.getTomDato()))
            .collect(Collectors.toCollection(ArrayList::new));

        vurderingsperioder.addAll(vilkårene.get().getVilkår(VilkårType.MEDISINSKEVILKÅR_18_ÅR)
            .map(Vilkår::getPerioder)
            .orElse(List.of())
            .stream()
            .map(VilkårPeriode::getPeriode)
            .map(it -> new Periode(it.getFomDato(), it.getTomDato()))
            .collect(Collectors.toList()));

        return vurderingsperioder;
    }
}
