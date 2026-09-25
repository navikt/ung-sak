package no.nav.ung.ytelse.aktivitetspenger.del1.steg.beslutte;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.behandling.BehandlingDel;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.ung.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.ung.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingAnsvarligRepository;
import no.nav.ung.sak.produksjonsstyring.totrinn.TotrinnTjeneste;
import no.nav.ung.sak.produksjonsstyring.totrinn.Totrinnsvurdering;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static java.lang.Boolean.TRUE;

@ApplicationScoped
public class LokalkontorBeslutteVilkårTjeneste {

    private TotrinnTjeneste totrinnTjeneste;
    private BehandlingAnsvarligRepository behandlingAnsvarligRepository;

    LokalkontorBeslutteVilkårTjeneste() {
        // for CDI proxy
    }

    @Inject
    public LokalkontorBeslutteVilkårTjeneste(TotrinnTjeneste totrinnTjeneste,
                                             BehandlingAnsvarligRepository behandlingAnsvarligRepository) {
        this.totrinnTjeneste = totrinnTjeneste;
        this.behandlingAnsvarligRepository = behandlingAnsvarligRepository;
    }

    public BehandleStegResultat besluttVilkår(BehandlingskontrollKontekst kontekst, Behandling behandling) {
        if (!behandling.erYtelseBehandling()) {
            throw new IllegalStateException("Kun ytelsesbehandling er støttet her p.t.");
        }

        if (behandlingAnsvarligRepository.erTotrinnsBehandling(behandling.getId(), BehandlingDel.LOKAL)) {
            final var fatterVedtakAksjonspunkt = behandling.getAksjonspunktMedDefinisjonOptional(AksjonspunktDefinisjon.LOKALKONTOR_BESLUTTER_VILKÅR);

            // Dersom vi ikke har fatter vedtak aksjonspunkt eller allerede har opprettet aksjonspunkt og behandlingen er flagget som totrinnsbehandling returnerer vi med aksjonspunkt og går videre til steg-ut
            if (fatterVedtakAksjonspunkt.filter(Aksjonspunkt::erUtført).isEmpty()) {
                return BehandleStegResultat.utførtMedAksjonspunkter(List.of(AksjonspunktDefinisjon.LOKALKONTOR_BESLUTTER_VILKÅR));
            }

            Collection<Totrinnsvurdering> totrinnaksjonspunktvurderinger = totrinnTjeneste.hentTotrinnaksjonspunktvurderinger(behandling, BehandlingDel.LOKAL);
            // Sjekker om vi har minst en ikke godkjent vurdering og om behandlingen skal flyttes tilbake
            if (sendesTilbakeTilSaksbehandler(totrinnaksjonspunktvurderinger)) {
                List<AksjonspunktDefinisjon> aksjonspunktDefinisjoner = finnIkkeGodkjenteVurderinger(totrinnaksjonspunktvurderinger);
                // Flytter behandling tilbake til første ikke-godkjente aksjonspunkt
                return BehandleStegResultat.tilbakeførtMedAksjonspunkter(aksjonspunktDefinisjoner);
            } else if (harUtførtAksjonspunktOgGodkjentAlleVurderinger(fatterVedtakAksjonspunkt.get(), totrinnaksjonspunktvurderinger)) {
                // Dersom alle vurderinger er godkjent og aksjonspunktet er utført går vi videre
            } else {
                throw new IllegalStateException("Kunne ikke fatte vedtak. Hadde aksjonspunkt med status " + fatterVedtakAksjonspunkt.get().getStatus() + " og totrinnsvurderinger: " + totrinnaksjonspunktvurderinger);
            }
        } else {
            totrinnTjeneste.deaktiverTotrinnaksjonspunktvurderinger(behandling, BehandlingDel.LOKAL);
        }


        // Ingen nye aksjonspunkt herfra
        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

    private static boolean harUtførtAksjonspunktOgGodkjentAlleVurderinger(Aksjonspunkt fatterVedtakAksjonspunkt, Collection<Totrinnsvurdering> totrinnaksjonspunktvurderinger) {
        return fatterVedtakAksjonspunkt.getStatus() == AksjonspunktStatus.UTFØRT && !totrinnaksjonspunktvurderinger.isEmpty() && totrinnaksjonspunktvurderinger.stream().allMatch(Totrinnsvurdering::isGodkjent);
    }

    private static List<AksjonspunktDefinisjon> finnIkkeGodkjenteVurderinger(Collection<Totrinnsvurdering> totrinnaksjonspunktvurderinger) {
        return totrinnaksjonspunktvurderinger.stream()
            .filter(a -> !a.isGodkjent())
            .map(Totrinnsvurdering::getAksjonspunktDefinisjon)
            .collect(Collectors.toList());
    }

    private boolean sendesTilbakeTilSaksbehandler(Collection<Totrinnsvurdering> medTotrinnskontroll) {
        return medTotrinnskontroll.stream()
            .anyMatch(a -> !TRUE.equals(a.isGodkjent()));
    }


}
