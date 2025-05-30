package no.nav.ung.sak.behandlingskontroll;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

import no.nav.ung.kodeverk.behandling.BehandlingResultatType;
import no.nav.ung.kodeverk.behandling.BehandlingStegType;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.Venteårsak;
import no.nav.ung.sak.behandlingskontroll.transisjoner.TransisjonIdentifikator;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.behandlingslager.hendelser.StartpunktType;

public interface BehandlingskontrollTjeneste {

    /**
     * Signaliserer at aksjonspunkter er funnet eller har endret status. Bruk helst lagre-metodene
     */
    void aksjonspunkterEndretStatus(BehandlingskontrollKontekst kontekst, BehandlingStegType behandlingStegType, List<Aksjonspunkt> aksjonspunkter);

    /**
     * Flytt prosessen til senere steg. Hopper over eventuelt mellomliggende steg.
     *
     * Alle mellomliggende steg og aksjonspunkt vil bli satt til AVBRUTT når dette skjer. Prosessen vil ikke kjøres.
     * Det gjelder også dersom neste steg er det definerte neste steget i prosessen (som normalt skulle blitt kalt
     * gjennom {@link #prosesserBehandling(BehandlingskontrollKontekst)}.
     *
     * @throws IllegalstateException
     *             dersom senereSteg er før eller lik aktivt steg i behandlingen (i følge BehandlingsModell for gitt
     *             BehandlingType).
     */
    void behandlingFramføringTilSenereBehandlingSteg(BehandlingskontrollKontekst kontekst, BehandlingStegType senereSteg);

    boolean behandlingTilbakeføringHvisTidligereBehandlingSteg(BehandlingskontrollKontekst kontekst,
                                                               BehandlingStegType tidligereStegType);

    /**
     * FLytt prosesen til et tidlligere steg.
     *
     * @throws IllegalstateException
     *             dersom tidligereSteg er etter aktivt steg i behandlingen (i følge BehandlingsModell for gitt
     *             BehandlingType).
     */
    void behandlingTilbakeføringTilTidligereBehandlingSteg(BehandlingskontrollKontekst kontekst, BehandlingStegType tidligereStegType);

    /**
     * Prosesser behandling enten fra akitvt steg eller steg angitt av aksjonspunktDefinsjonerKoder dersom noen er eldre
     *
     * @see #prosesserBehandling(BehandlingskontrollKontekst)
     */
    void behandlingTilbakeføringTilTidligsteAksjonspunkt(BehandlingskontrollKontekst kontekst, Collection<String> endredeAksjonspunkt);

    boolean erIStegEllerSenereSteg(Long behandlingId, BehandlingStegType stegType);

    boolean erStegPassert(Behandling behandling, BehandlingStegType stegType);

    boolean erStegPassert(Long behandlingId, BehandlingStegType stegType);

    Set<String> finnAksjonspunktDefinisjonerFraOgMed(Behandling behandling, BehandlingStegType steg);

    BehandlingStegType finnBehandlingSteg(StartpunktType startpunktType, FagsakYtelseType fagsakYtelseType, BehandlingType behandlingType);

    void fremoverTransisjon(TransisjonIdentifikator transisjonId, BehandlingskontrollKontekst kontekst);

    BehandlingStegKonfigurasjon getBehandlingStegKonfigurasjon();

    /** Henlegg en behandling. */
    void henleggBehandling(BehandlingskontrollKontekst kontekst, BehandlingResultatType årsakKode);

    void henleggBehandlingFraSteg(BehandlingskontrollKontekst kontekst, BehandlingResultatType årsak);

    /**
     * Initierer ny behandlingskontroll for en ny behandling, som ikke er lagret i behandlingsRepository
     * og derfor ikke har fått tildelt behandlingId
     *
     * @param behandling
     *            - må være med
     */
    BehandlingskontrollKontekst initBehandlingskontroll(Behandling behandling);

    /**
     * Initierer ny behandlingskontroll for en ny behandling, som ikke er lagret i behandlingsRepository
     * og derfor ikke har fått tildelt behandlingId
     *
     * @param behandlingId
     *            - må være med.
     */
    BehandlingskontrollKontekst initBehandlingskontroll(String behandlingd);

    /**
     * Initier ny Behandlingskontroll, oppretter kontekst som brukes til sikre at parallle behandlinger og kjøringer går
     * i tur og orden. Dette skjer gjennom å opprette en {@link BehandlingLås} som legges ved ved lagring.
     *
     * @param behandlingId
     *            - må være med
     */
    BehandlingskontrollKontekst initBehandlingskontroll(Long behandlingId);

    /**
     * Initier ny Behandlingskontroll, oppretter kontekst som brukes til sikre at parallle behandlinger og kjøringer går
     * i tur og orden. Dette skjer gjennom å opprette en {@link BehandlingLås} som legges ved ved lagring.
     *
     * @param behandlingUuid
     *            - må være med
     */
    BehandlingskontrollKontekst initBehandlingskontroll(UUID behandlingUuid);

    Optional<BehandlingStegType> nesteSteg(Behandling behandling, BehandlingStegType behandlingStegType);

    boolean inneholderSteg(FagsakYtelseType ytelseType, BehandlingType behandlingType, BehandlingStegType behandlingStegType);

    /**
     * Lagrer og håndterer avbrutte aksjonspunkt
     */
    void lagreAksjonspunkterAvbrutt(BehandlingskontrollKontekst kontekst, BehandlingStegType behandlingStegType,
                                    List<Aksjonspunkt> aksjonspunkter);

    /**
     * Oppretter og håndterer nye aksjonspunkt
     */
    List<Aksjonspunkt> lagreAksjonspunkterFunnet(BehandlingskontrollKontekst kontekst, BehandlingStegType behandlingStegType,
                                                 List<AksjonspunktDefinisjon> aksjonspunkter);

    /**
     * Oppretter og håndterer nye overstyringsaksjonspunkt
     */
    List<Aksjonspunkt> lagreAksjonspunkterFunnet(BehandlingskontrollKontekst kontekst, List<AksjonspunktDefinisjon> aksjonspunkter);

    /**
     * Lagrer og håndterer reåpning av aksjonspunkt
     */
    void lagreAksjonspunkterReåpnet(BehandlingskontrollKontekst kontekst, List<Aksjonspunkt> aksjonspunkter, boolean setTotrinn);

    /**
     * Lagrer og håndterer utførte aksjonspunkt uten begrunnelse. Dersom man skal lagre begrunnelse - bruk apRepository + aksjonspunkterUtført
     */
    void lagreAksjonspunkterUtført(BehandlingskontrollKontekst kontekst, BehandlingStegType behandlingStegType,
                                   Aksjonspunkt aksjonspunkt, String begrunnelse);

    /**
     * Lagrer og håndterer utførte aksjonspunkt uten begrunnelse. Dersom man skal lagre begrunnelse - bruk apRepository + aksjonspunkterUtført
     */
    void lagreAksjonspunkterUtført(BehandlingskontrollKontekst kontekst, BehandlingStegType behandlingStegType,
                                   List<Aksjonspunkt> aksjonspunkter);

    /**
     * Lagrer og håndterer aksjonspunktresultater fra utledning utenom steg
     */
    void lagreAksjonspunktResultat(BehandlingskontrollKontekst kontekst, BehandlingStegType behandlingStegType,
                                   List<AksjonspunktResultat> aksjonspunktResultater);

    /**
     * Lagrer en ny behandling i behandlingRepository og fyrer av event om at en Behandling er opprettet
     */
    void opprettBehandling(BehandlingskontrollKontekst kontekst, Behandling behandling);

    /**
     * Opprett ny behandling for gitt fagsak og BehandlingType.
     * <p>
     * Vil alltid opprette ny behandling, selv om det finnes eksisterende åpen behandling på fagsaken.
     *
     * @param fagsak
     *            - fagsak med eller uten eksisterende behandling
     * @param behandlingType
     *            - type behandling
     * @param behandlingOppdaterer
     *            - funksjon for oppdatering av grunnlag
     * @return Behandling - nylig opprettet og lagret.
     */
    Behandling opprettNyBehandling(Fagsak fagsak, BehandlingType behandlingType, Consumer<Behandling> behandlingOppdaterer);

    /**
     * Prosesser behandling fra dit den sist har kommet.
     * Avhengig av vurderingspunkt (inngang- og utgang-kriterier) vil steget kjøres på nytt.
     *
     * @param kontekst
     *            - kontekst for prosessering. Opprettes gjennom {@link #initBehandlingskontroll(Long)}
     */
    void prosesserBehandling(BehandlingskontrollKontekst kontekst);

    /**
     * Prosesser forutsatt behandling er i angitt steg og status venter og steget.
     * Vil kalle gjenopptaSteg for angitt steg, senere vanlig framdrift
     *
     * @param kontekst
     *            - kontekst for prosessering. Opprettes gjennom {@link #initBehandlingskontroll(Long)}
     * @param behandlingStegType
     *            - precondition steg
     */
    void prosesserBehandlingGjenopptaHvisStegVenter(BehandlingskontrollKontekst kontekst, BehandlingStegType behandlingStegType);

    int sammenlignRekkefølge(FagsakYtelseType ytelseType, BehandlingType behandlingType, BehandlingStegType stegA, BehandlingStegType stegB);

    /**
     * Setter autopunkter av en spesifikk aksjonspunktdefinisjon til utført. Dette klargjør kun behandligen for
     * prosessering, men vil ikke drive prosessen videre.
     * @param aksjonspunktDefinisjon Aksjonspunktdefinisjon til de aksjonspunktene som skal lukkes
     *            Bruk {@link #prosesserBehandling(BehandlingskontrollKontekst)} el. tilsvarende for det.
     */
    void settAutopunktTilUtført(Behandling behandling, BehandlingskontrollKontekst kontekst, Collection<AksjonspunktDefinisjon> aksjonspunktDefinisjon);

    /**
     * Setter behandlingen på vent med angitt hvilket steg det står i.
     *
     * @param behandling
     * @param aksjonspunktDefinisjon hvilket Aksjonspunkt skal holde i 'ventingen'
     * @param BehandlingStegType stegType aksjonspunktet står i.
     * @param fristTid Frist før Behandlingen å adresseres
     * @param venteårsak Årsak til ventingen.
     * @param venteårsakVariant begrunnelse/variant av venteårsak kode
     *
     */
    Aksjonspunkt settBehandlingPåVent(Behandling behandling,
                                      AksjonspunktDefinisjon aksjonspunktDefinisjon,
                                      BehandlingStegType stegType,
                                      LocalDateTime fristTid,
                                      Venteårsak venteårsak,
                                      String venteårsakVariant);

    /**
     * Setter behandlingen på vent.
     *
     * @param behandling
     * @param aksjonspunktDefinisjon hvilket Aksjonspunkt skal holde i 'ventingen'
     * @param fristTid Frist før Behandlingen å adresseres
     * @param venteårsak Årsak til ventingen.
     * @param venteårsakVariant begrunnelse/variant for venteårsak
     */
    Aksjonspunkt settBehandlingPåVentUtenSteg(Behandling behandling, AksjonspunktDefinisjon aksjonspunktDefinisjon,
                                              LocalDateTime fristTid,
                                              Venteårsak venteårsak, String venteårsakVariant);

    /**
     * Ny metode som forbereder en behandling for prosessering - setter autopunkt til utført og evt tilbakeføring ved gjenopptak.
     * Behandlingen skal være klar til prosessering uten åpne autopunkt når kallet er ferdig.
     */
    void taBehandlingAvVentSetAlleAutopunktUtført(Behandling behandling, BehandlingskontrollKontekst kontekst);

    void taBehandlingAvVentSetAlleAutopunktUtførtForHenleggelse(Behandling behandling, BehandlingskontrollKontekst kontekst);

}
