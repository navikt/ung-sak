package no.nav.k9.sak.ytelse.pleiepengerbarn.iverksett;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.abakus.vedtak.ytelse.Ytelse;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.hendelse.vedtak.VurderOmVedtakPåvirkerSakerTjeneste;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.etablerttilsyn.ErEndringPåEtablertTilsynTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomGrunnlagBehandling;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomGrunnlagRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomGrunnlagService;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomVurderingRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.unntaketablerttilsyn.EndringUnntakEtablertTilsynTjeneste;

@ApplicationScoped
@FagsakYtelseTypeRef("PSB")
public class VurderOmPSBVedtakPåvirkerAndreSakerTjeneste implements VurderOmVedtakPåvirkerSakerTjeneste {

    private static final Logger log = LoggerFactory.getLogger(VurderOmPSBVedtakPåvirkerAndreSakerTjeneste.class);

    private BehandlingRepository behandlingRepository;
    private FagsakRepository fagsakRepository;
    private VilkårResultatRepository vilkårResultatRepository;
    private SykdomGrunnlagRepository sykdomGrunnlagRepository;
    private SykdomVurderingRepository sykdomVurderingRepository;
    private SykdomGrunnlagService sykdomGrunnlagService;
    private ErEndringPåEtablertTilsynTjeneste erEndringPåEtablertTilsynTjeneste;
    private EndringUnntakEtablertTilsynTjeneste endringUnntakEtablertTilsynTjeneste;

    VurderOmPSBVedtakPåvirkerAndreSakerTjeneste() {
    }

    @Inject
    public VurderOmPSBVedtakPåvirkerAndreSakerTjeneste(BehandlingRepository behandlingRepository,
                                                       FagsakRepository fagsakRepository,
                                                       VilkårResultatRepository vilkårResultatRepository,
                                                       SykdomGrunnlagRepository sykdomGrunnlagRepository,
                                                       SykdomVurderingRepository sykdomVurderingRepository,
                                                       SykdomGrunnlagService sykdomGrunnlagService,
                                                       ErEndringPåEtablertTilsynTjeneste erEndringPåEtablertTilsynTjeneste,
                                                       EndringUnntakEtablertTilsynTjeneste endringUnntakEtablertTilsynTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.fagsakRepository = fagsakRepository;
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.sykdomGrunnlagRepository = sykdomGrunnlagRepository;
        this.sykdomVurderingRepository = sykdomVurderingRepository;
        this.sykdomGrunnlagService = sykdomGrunnlagService;
        this.erEndringPåEtablertTilsynTjeneste = erEndringPåEtablertTilsynTjeneste;
        this.endringUnntakEtablertTilsynTjeneste = endringUnntakEtablertTilsynTjeneste;
    }

    @Override
    public List<Saksnummer> utledSakerSomErKanVærePåvirket(Ytelse vedtakHendelse) {
        var fagsak = fagsakRepository.hentSakGittSaksnummer(new Saksnummer(vedtakHendelse.getSaksnummer())).orElseThrow();
        Behandling vedtattBehandling = behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(fagsak.getId()).orElseThrow();

        AktørId pleietrengende = vedtattBehandling.getFagsak().getPleietrengendeAktørId();
        List<Saksnummer> alleSaksnummer = sykdomVurderingRepository.hentAlleSaksnummer(pleietrengende);

        var result = new ArrayList<Saksnummer>();
        for (Saksnummer kandidatsaksnummer : alleSaksnummer) {
            if (!kandidatsaksnummer.equals(fagsak.getSaksnummer())) {
                var kandidatFagsak = fagsakRepository.hentSakGittSaksnummer(kandidatsaksnummer, false).orElseThrow();
                var sisteBehandlingPåKandidat = behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(kandidatFagsak.getId()).orElseThrow();
                boolean skalRevurderesPgaSykdom = vurderBehovForRevurderingPgaSykdom(pleietrengende, kandidatsaksnummer, sisteBehandlingPåKandidat);
                var referanse = BehandlingReferanse.fra(sisteBehandlingPåKandidat);
                var skalRevurderesPgaEtablertTilsyn = skalRevurderesPgaEtablertTilsyn(referanse);
                var skalRevurderesPgaNattevåkOgBeredskap = skalRevurderesPgaNattevåkOgBeredskap(referanse);
                if (skalRevurderesPgaSykdom || skalRevurderesPgaEtablertTilsyn || skalRevurderesPgaNattevåkOgBeredskap) {
                    result.add(kandidatsaksnummer);
                    log.info("Sak='{}' revurderes pga => sykdom={}, etablertTilsyn={}, nattevåk&beredskap={}", kandidatsaksnummer, skalRevurderesPgaSykdom, skalRevurderesPgaEtablertTilsyn, skalRevurderesPgaNattevåkOgBeredskap);
                }
            }
        }

        return result;
    }

    private boolean skalRevurderesPgaNattevåkOgBeredskap(BehandlingReferanse referanse) {
        return endringUnntakEtablertTilsynTjeneste.harEndringerSidenForrigeBehandling(referanse.getBehandlingId(), referanse.getPleietrengendeAktørId());
    }

    private boolean skalRevurderesPgaEtablertTilsyn(BehandlingReferanse referanse) {
        return erEndringPåEtablertTilsynTjeneste.erUhåndterteEndringerFraForrigeBehandling(referanse);
    }

    private boolean vurderBehovForRevurderingPgaSykdom(AktørId pleietrengende, Saksnummer kandidatsaksnummer, Behandling sisteBehandlingPåKandidat) {
        SykdomGrunnlagBehandling kandidatSykdomBehandling = sykdomGrunnlagRepository.hentGrunnlagForBehandling(sisteBehandlingPåKandidat.getUuid())
            .orElseThrow();
        var behandling = behandlingRepository.hentBehandlingHvisFinnes(kandidatSykdomBehandling.getBehandlingUuid()).orElseThrow();
        var vurderingsperioder = utledVurderingsperiode(behandling);
        var manglendeOmsorgenForPerioder = sykdomGrunnlagService.hentManglendeOmsorgenForPerioder(behandling.getId());
        var utledetGrunnlag = sykdomGrunnlagRepository.utledGrunnlag(kandidatsaksnummer, kandidatSykdomBehandling.getBehandlingUuid(), pleietrengende, vurderingsperioder, manglendeOmsorgenForPerioder);
        final LocalDateTimeline<Boolean> endringerISøktePerioder = sykdomGrunnlagService.sammenlignGrunnlag(Optional.of(kandidatSykdomBehandling.getGrunnlag()), utledetGrunnlag).getDiffPerioder();

        return !endringerISøktePerioder.isEmpty();
    }

    private List<Periode> utledVurderingsperiode(Behandling behandling) {
        var vilkårene = vilkårResultatRepository.hent(behandling.getId());
        var vurderingsperioder = vilkårene.getVilkår(VilkårType.MEDISINSKEVILKÅR_UNDER_18_ÅR)
            .map(Vilkår::getPerioder)
            .orElse(List.of())
            .stream()
            .map(VilkårPeriode::getPeriode)
            .map(it -> new Periode(it.getFomDato(), it.getTomDato()))
            .collect(Collectors.toCollection(ArrayList::new));

        vurderingsperioder.addAll(vilkårene.getVilkår(VilkårType.MEDISINSKEVILKÅR_18_ÅR)
            .map(Vilkår::getPerioder)
            .orElse(List.of())
            .stream()
            .map(VilkårPeriode::getPeriode)
            .map(it -> new Periode(it.getFomDato(), it.getTomDato()))
            .collect(Collectors.toList()));

        return vurderingsperioder;
    }

}
