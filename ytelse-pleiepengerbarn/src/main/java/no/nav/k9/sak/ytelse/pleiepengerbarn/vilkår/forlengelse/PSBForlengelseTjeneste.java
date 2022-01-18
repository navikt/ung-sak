package no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår.forlengelse;

import java.util.NavigableSet;
import java.util.Objects;
import java.util.TreeSet;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.perioder.EndringPåForlengelsePeriodeVurderer;
import no.nav.k9.sak.perioder.ForlengelseTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.SøknadsperiodeTjeneste;

@FagsakYtelseTypeRef("PSB")
@BehandlingTypeRef("BT-004")
@ApplicationScoped
public class PSBForlengelseTjeneste implements ForlengelseTjeneste {

    private SøknadsperiodeTjeneste søknadsperiodeTjeneste;
    private VilkårResultatRepository vilkårResultatRepository;
    private InntektArbeidYtelseTjeneste iayTjeneste;
    private BehandlingRepository behandlingRepository;
    private Instance<EndringPåForlengelsePeriodeVurderer> endringsVurderere;

    PSBForlengelseTjeneste() {
    }

    @Inject
    public PSBForlengelseTjeneste(SøknadsperiodeTjeneste søknadsperiodeTjeneste,
                                  VilkårResultatRepository vilkårResultatRepository,
                                  InntektArbeidYtelseTjeneste iayTjeneste,
                                  BehandlingRepository behandlingRepository,
                                  @Any Instance<EndringPåForlengelsePeriodeVurderer> endringsVurderere) {
        this.søknadsperiodeTjeneste = søknadsperiodeTjeneste;
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.iayTjeneste = iayTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.endringsVurderere = endringsVurderere;
    }

    @Override
    public NavigableSet<DatoIntervallEntitet> utledPerioderSomSkalBehandlesSomForlengelse(BehandlingReferanse referanse, NavigableSet<DatoIntervallEntitet> perioderTilVurdering, VilkårType vilkårType) {
        var behandling = behandlingRepository.hentBehandling(referanse.getBehandlingId());
        var resultat = new TreeSet<DatoIntervallEntitet>();

        if (behandling.erManueltOpprettet()) {
            return resultat;
        }

        var relevantePerioder = søknadsperiodeTjeneste.utledPeriode(referanse.getBehandlingId());

        if (relevantePerioder.isEmpty() || perioderTilVurdering.isEmpty()) {
            return resultat;
        }

        var vilkåreneOpt = vilkårResultatRepository.hentHvisEksisterer(referanse.getBehandlingId());
        if (vilkåreneOpt.isEmpty()) {
            return resultat;
        }

        var vilkåretOpt = vilkåreneOpt.get().getVilkår(vilkårType);

        if (vilkåretOpt.isEmpty()) {
            return resultat;
        }
        var vilkårForrigeVedtak = vilkårResultatRepository.hentHvisEksisterer(referanse.getOriginalBehandlingId().orElseThrow()).orElseThrow()
            .getVilkår(vilkårType)
            .orElseThrow();

        resultat = vilkåretOpt.get()
            .getPerioder()
            .stream()
            .filter(it -> perioderTilVurdering.stream().anyMatch(at -> Objects.equals(it.getPeriode(), at)))
            .filter(it -> varInnvilgetForrigeVedtak(it, vilkårForrigeVedtak) && relevantePerioder.stream()
                .noneMatch(at -> Objects.equals(it.getPeriode().getFomDato(), at.getFomDato())))
            .map(VilkårPeriode::getPeriode)
            .collect(Collectors.toCollection(TreeSet::new));

        filtrerUtPerioderSomLikevelMåVurderesPgaEndring(referanse, vilkårType, resultat);

        return resultat;
    }

    private boolean varInnvilgetForrigeVedtak(VilkårPeriode nåværendePeriode, Vilkår vilkårForrigeVedtak) {
        return vilkårForrigeVedtak.getPerioder()
            .stream()
            .anyMatch(it -> Objects.equals(it.getSkjæringstidspunkt(), nåværendePeriode.getSkjæringstidspunkt())
                && Objects.equals(it.getGjeldendeUtfall(), Utfall.OPPFYLT));
    }

    private void filtrerUtPerioderSomLikevelMåVurderesPgaEndring(BehandlingReferanse referanse, VilkårType vilkårType, TreeSet<DatoIntervallEntitet> resultat) {
        var vurderer = EndringPåForlengelsePeriodeVurderer.finnVurderer(endringsVurderere, vilkårType, referanse.getFagsakYtelseType());
        var sakInntektsmeldinger = iayTjeneste.hentUnikeInntektsmeldingerForSak(referanse.getSaksnummer());

        var input = new PSBEndringPåForlengelseInput(referanse)
            .medInntektsmeldinger(sakInntektsmeldinger);

        resultat.removeIf(it -> vurderer.harPeriodeEndring(input, it));
    }
}
