package no.nav.ung.ytelse.aktivitetspenger.medlemskap;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingskontroll.*;
import no.nav.ung.sak.behandlingslager.behandling.medlemskap.OppgittForutgåendeMedlemskapGrunnlag;
import no.nav.ung.sak.behandlingslager.behandling.medlemskap.OppgittForutgåendeMedlemskapRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårJsonObjectMapper;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.ung.sak.perioder.VilkårsPerioderTilVurderingTjeneste;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static no.nav.ung.kodeverk.behandling.BehandlingStegType.VURDER_FORUTGÅENDE_MEDLEMSKAPSVILKÅR;

@ApplicationScoped
@BehandlingStegRef(value = VURDER_FORUTGÅENDE_MEDLEMSKAPSVILKÅR)
@BehandlingTypeRef
@FagsakYtelseTypeRef(FagsakYtelseType.AKTIVITETSPENGER)
public class ForutgåendeMedlemskapsvilkårSteg implements BehandlingSteg {

    private VilkårResultatRepository vilkårResultatRepository;
    private OppgittForutgåendeMedlemskapRepository forutgåendeMedlemskapRepository;
    private BehandlingRepository behandlingRepository;
    private Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjenester;

    public ForutgåendeMedlemskapsvilkårSteg() {
    }

    @Inject
    public ForutgåendeMedlemskapsvilkårSteg(VilkårResultatRepository vilkårResultatRepository,
                                            OppgittForutgåendeMedlemskapRepository forutgåendeMedlemskapRepository,
                                            @Any Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjenester,
                                            BehandlingRepository behandlingRepository) {
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.forutgåendeMedlemskapRepository = forutgåendeMedlemskapRepository;
        this.perioderTilVurderingTjenester = perioderTilVurderingTjenester;
        this.behandlingRepository = behandlingRepository;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        var behandlingId = kontekst.getBehandlingId();
        var vilkårene = vilkårResultatRepository.hent(behandlingId);

        var vilkår = vilkårene.getVilkår(VilkårType.FORUTGÅENDE_MEDLEMSKAPSVILKÅRET).orElseThrow();
        var ikkeVurdertePerioder = vilkår.getPerioder().stream()
            .filter(it -> Objects.equals(it.getUtfall(), Utfall.IKKE_VURDERT))
            .map(VilkårPeriode::getPeriode)
            .collect(Collectors.toSet());

        if (ikkeVurdertePerioder.isEmpty()) {
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        }

        var grunnlagOpt = forutgåendeMedlemskapRepository.hentGrunnlagHvisEksisterer(behandlingId);
        if (grunnlagOpt.isEmpty()) {
            return BehandleStegResultat.utførtMedAksjonspunkter(List.of(AksjonspunktDefinisjon.AVKLAR_GYLDIG_MEDLEMSKAP));
        }

        var grunnlag = grunnlagOpt.get();
        boolean alleBostedITrygdeavtaleLand = grunnlag.getUtenlandskeBosteder().stream()
            .allMatch(b -> TrygdeavtaleLandOppslag.erGyldigTrygdeavtaleLand(b.getLandkode(), b.getPeriode().getFomDato()));

        if (alleBostedITrygdeavtaleLand) {
            oppfyllVilkår(vilkårene, behandlingId, grunnlag);
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        }

        return BehandleStegResultat.utførtMedAksjonspunkter(List.of(AksjonspunktDefinisjon.AVKLAR_GYLDIG_MEDLEMSKAP));
    }

    private void oppfyllVilkår(Vilkårene vilkårene, Long behandlingId, OppgittForutgåendeMedlemskapGrunnlag grunnlag) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var perioderTilVurdering = getPerioderTilVurderingTjeneste(behandling.getFagsakYtelseType(), behandling.getType())
            .utled(behandlingId, VilkårType.FORUTGÅENDE_MEDLEMSKAPSVILKÅRET);

        var jsonMapper = new VilkårJsonObjectMapper();
        var input = new RegelInput(grunnlag.getUtenlandskeBosteder().stream()
            .map(b -> new RegelInput.UtenlandskBosted(b.getLandkode(), b.getPeriode().getFomDato(), b.getPeriode().getTomDato()))
            .toList());
        String regelInput = jsonMapper.writeValueAsString(input);
        String regelEvaluering = jsonMapper.writeValueAsString(new RegelEvaluering("OPPFYLT", "Alle bosteder i land med gyldig trygdeavtale"));

        var vilkårResultatBuilder = Vilkårene.builderFraEksisterende(vilkårene);
        var vilkårBuilder = vilkårResultatBuilder.hentBuilderFor(VilkårType.FORUTGÅENDE_MEDLEMSKAPSVILKÅRET);

        perioderTilVurdering.stream()
            .map(it -> vilkårBuilder.hentBuilderFor(it)
                .medUtfall(Utfall.OPPFYLT)
                .medAvslagsårsak(null)
                .medRegelInput(regelInput)
                .medRegelEvaluering(regelEvaluering))
            .forEach(vilkårBuilder::leggTil);

        vilkårResultatBuilder.leggTil(vilkårBuilder);
        vilkårResultatRepository.lagre(behandlingId, vilkårResultatBuilder.build());
    }

    record RegelInput(List<UtenlandskBosted> utenlandskeBosteder) {
        record UtenlandskBosted(String landkode, LocalDate fom, LocalDate tom) {}
    }

    record RegelEvaluering(String utfall, String begrunnelse) {}

    private VilkårsPerioderTilVurderingTjeneste getPerioderTilVurderingTjeneste(FagsakYtelseType fagsakYtelseType, BehandlingType behandlingType) {
        return VilkårsPerioderTilVurderingTjeneste.finnTjeneste(perioderTilVurderingTjenester, fagsakYtelseType, behandlingType);
    }
}
