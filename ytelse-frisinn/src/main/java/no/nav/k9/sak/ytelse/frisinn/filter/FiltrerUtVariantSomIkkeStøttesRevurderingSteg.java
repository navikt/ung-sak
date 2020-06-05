package no.nav.k9.sak.ytelse.frisinn.filter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.Venteårsak;
import no.nav.k9.kodeverk.uttak.UttakArbeidType;
import no.nav.k9.sak.behandlingskontroll.AksjonspunktResultat;
import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.domene.behandling.steg.beregnytelse.BeregneYtelseSteg;
import no.nav.k9.sak.domene.uttak.repo.UttakAktivitet;
import no.nav.k9.sak.domene.uttak.repo.UttakAktivitetPeriode;
import no.nav.k9.sak.domene.uttak.repo.UttakGrunnlag;
import no.nav.k9.sak.domene.uttak.repo.UttakRepository;

@FagsakYtelseTypeRef("FRISINN")
@BehandlingStegRef(kode = "VARIANT_FILTER")
@BehandlingTypeRef("BT-004")
@ApplicationScoped
public class FiltrerUtVariantSomIkkeStøttesRevurderingSteg implements BeregneYtelseSteg {


    private BehandlingRepository behandlingRepository;
    private UttakRepository uttakRepository;

    protected FiltrerUtVariantSomIkkeStøttesRevurderingSteg() {
        // for proxy
    }

    @Inject
    public FiltrerUtVariantSomIkkeStøttesRevurderingSteg(UttakRepository uttakRepository, BehandlingRepositoryProvider provider) {
        this.uttakRepository = uttakRepository;
        this.behandlingRepository = provider.getBehandlingRepository();
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        var nyBehandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        var origBehandling = nyBehandling.getOriginalBehandling().orElseThrow();

        return filtrerBehandlinger(nyBehandling, origBehandling);
    }


    private BehandleStegResultat filtrerBehandlinger(Behandling nyBehandling, Behandling origBehandling) {
        var erNySøknadperiode = erNySøknadperiode(nyBehandling, origBehandling);
        var harSøktForNyInntektstype = harSøktForNyInntektstype(nyBehandling, origBehandling);

        if (erNySøknadperiode && harSøktForNyInntektstype) {
            var apResultater = List.of(AksjonspunktResultat.opprettForAksjonspunktMedFrist(
                AksjonspunktDefinisjon.AUTO_VENT_FRISINN_MANGLENDE_FUNKSJONALITET,
                Venteårsak.FRISINN_VARIANT_ENDRET_INNTEKTSTYPE,
                LocalDateTime.now().plusDays(3)));
            return BehandleStegResultat.utførtMedAksjonspunktResultater(apResultater);
        }

        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

    private boolean erNySøknadperiode(Behandling revurdering, Behandling origBehandling) {
        var nyttUttak = uttakRepository.hentFastsattUttak(revurdering.getId());
        var origUttak = uttakRepository.hentFastsattUttak(origBehandling.getId());

        return nyttUttak.getPerioder().size() > origUttak.getPerioder().size();
    }

    private boolean harSøktForNyInntektstype(Behandling nyBehandling, Behandling origBehandling) {
        var nyttUttakGrunnlag = uttakRepository.hentGrunnlag(nyBehandling.getId());
        var origUttakGrunnlag = uttakRepository.hentGrunnlag(origBehandling.getId());

        var nyeUttakAktiviteter = hentUttaksaktviteter(nyttUttakGrunnlag);
        var origUttakAktiviteter = hentUttaksaktviteter(origUttakGrunnlag);

        return !nyeUttakAktiviteter.equals(origUttakAktiviteter);
    }

    private Set<UttakArbeidType> hentUttaksaktviteter(Optional<UttakGrunnlag> uttakGrunnlag) {
        return uttakGrunnlag.map(UttakGrunnlag::getFastsattUttak)
            .map(UttakAktivitet::getPerioder).orElse(Set.of()).stream()
            .map(UttakAktivitetPeriode::getAktivitetType)
            .collect(Collectors.toSet());
    }

}
