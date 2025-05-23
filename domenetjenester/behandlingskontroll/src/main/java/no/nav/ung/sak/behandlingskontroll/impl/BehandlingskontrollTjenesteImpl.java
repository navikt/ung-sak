package no.nav.ung.sak.behandlingskontroll.impl;

import static java.util.Collections.singletonList;
import static no.nav.ung.sak.behandlingskontroll.transisjoner.FellesTransisjoner.FREMHOPP_TIL_IVERKSETT_VEDTAK;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import no.nav.k9.felles.log.mdc.MdcExtendedLogContext;
import no.nav.ung.kodeverk.behandling.BehandlingResultatType;
import no.nav.ung.kodeverk.behandling.BehandlingStatus;
import no.nav.ung.kodeverk.behandling.BehandlingStegStatus;
import no.nav.ung.kodeverk.behandling.BehandlingStegType;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktType;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.Venteårsak;
import no.nav.ung.sak.behandlingskontroll.AksjonspunktResultat;
import no.nav.ung.sak.behandlingskontroll.BehandlingModell;
import no.nav.ung.sak.behandlingskontroll.BehandlingModellVisitor;
import no.nav.ung.sak.behandlingskontroll.BehandlingStegKonfigurasjon;
import no.nav.ung.sak.behandlingskontroll.BehandlingStegModell;
import no.nav.ung.sak.behandlingskontroll.BehandlingStegTilstandSnapshot;
import no.nav.ung.sak.behandlingskontroll.BehandlingStegUtfall;
import no.nav.ung.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.ung.sak.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.ung.sak.behandlingskontroll.events.AksjonspunktStatusEvent;
import no.nav.ung.sak.behandlingskontroll.events.BehandlingStegOvergangEvent;
import no.nav.ung.sak.behandlingskontroll.events.BehandlingTransisjonEvent;
import no.nav.ung.sak.behandlingskontroll.events.BehandlingskontrollEvent;
import no.nav.ung.sak.behandlingskontroll.events.BehandlingskontrollEvent.AvsluttetEvent;
import no.nav.ung.sak.behandlingskontroll.events.BehandlingskontrollEvent.ExceptionEvent;
import no.nav.ung.sak.behandlingskontroll.events.BehandlingskontrollEvent.StartetEvent;
import no.nav.ung.sak.behandlingskontroll.events.BehandlingskontrollEvent.StoppetEvent;
import no.nav.ung.sak.behandlingskontroll.spi.BehandlingskontrollServiceProvider;
import no.nav.ung.sak.behandlingskontroll.transisjoner.StegTransisjon;
import no.nav.ung.sak.behandlingskontroll.transisjoner.TransisjonIdentifikator;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.BehandlingStegTilstand;
import no.nav.ung.sak.behandlingslager.behandling.InternalManipulerBehandling;
import no.nav.ung.sak.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.ung.sak.behandlingslager.behandling.aksjonspunkt.AksjonspunktKontrollRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakLås;
import no.nav.ung.sak.behandlingslager.hendelser.StartpunktType;

@RequestScoped // må være RequestScoped sålenge ikke nøstet prosessering støttes.
public class BehandlingskontrollTjenesteImpl implements BehandlingskontrollTjeneste {

    private static final MdcExtendedLogContext LOG_CONTEXT = MdcExtendedLogContext.getContext("prosess"); //$NON-NLS-1$

    private static final Pattern DIGITS_PATTERN = Pattern.compile("\\d+");
    private AksjonspunktKontrollRepository aksjonspunktKontrollRepository;
    private BehandlingRepository behandlingRepository;
    private BehandlingModellRepository behandlingModellRepository;
    private InternalManipulerBehandling manipulerInternBehandling;
    private BehandlingskontrollEventPubliserer eventPubliserer;
    private BehandlingStegKonfigurasjon behandlingStegKonfigurasjon;

    /**
     * Sjekker om vi allerede kjører Behandlingskontroll, og aborter forsøk på nøsting av kall (støttes ikke p.t.).
     * <p>
     * Funker sålenge denne tjenesten er en {@link RequestScoped} bean.
     */
    private AtomicBoolean nøstetProsseringGuard = new AtomicBoolean();
    private BehandlingskontrollServiceProvider serviceProvider;

    BehandlingskontrollTjenesteImpl() {
        // for CDI proxy
    }

    /**
     * SE KOMMENTAR ØVERST
     */
    @Inject
    public BehandlingskontrollTjenesteImpl(BehandlingskontrollServiceProvider serviceProvider) {

        this.serviceProvider = serviceProvider;
        this.behandlingRepository = serviceProvider.getBehandlingRepository();
        this.behandlingModellRepository = serviceProvider.getBehandlingModellRepository();
        this.manipulerInternBehandling = new InternalManipulerBehandling();
        this.behandlingStegKonfigurasjon = new BehandlingStegKonfigurasjon(EnumSet.allOf(BehandlingStegStatus.class));
        this.aksjonspunktKontrollRepository = serviceProvider.getAksjonspunktKontrollRepository();
        this.eventPubliserer = serviceProvider.getEventPubliserer();
    }

    @Override
    public void prosesserBehandling(BehandlingskontrollKontekst kontekst) {
        Behandling behandling = hentBehandling(kontekst);
        if (Objects.equals(BehandlingStatus.AVSLUTTET.getKode(), behandling.getStatus().getKode())) {
            return;
        }
        BehandlingModell modell = getModell(behandling.getType(), behandling.getFagsakYtelseType());
        BehandlingModellVisitor stegVisitor = new TekniskBehandlingStegVisitor(serviceProvider, kontekst);

        prosesserBehandling(kontekst, modell, stegVisitor);
    }

    @Override
    public void prosesserBehandlingGjenopptaHvisStegVenter(BehandlingskontrollKontekst kontekst, BehandlingStegType behandlingStegType) {
        Behandling behandling = hentBehandling(kontekst);
        if (Objects.equals(BehandlingStatus.AVSLUTTET.getKode(), behandling.getStatus().getKode())) {
            return;
        }

        Optional<BehandlingStegTilstand> tilstand = behandling.getBehandlingStegTilstand(behandlingStegType);
        if (tilstand.isPresent() && BehandlingStegStatus.VENTER.equals(tilstand.get().getBehandlingStegStatus())) {
            BehandlingModell modell = getModell(behandling.getType(), behandling.getFagsakYtelseType());
            BehandlingModellVisitor stegVisitor = new TekniskBehandlingStegVenterVisitor(serviceProvider, kontekst);

            prosesserBehandling(kontekst, modell, stegVisitor);
        }
    }

    private BehandlingStegUtfall prosesserBehandling(BehandlingskontrollKontekst kontekst, BehandlingModell modell, BehandlingModellVisitor visitor) {

        validerOgFlaggStartetProsessering();
        BehandlingStegUtfall behandlingStegUtfall;
        try {
            fyrEventBehandlingskontrollStartet(kontekst, modell);
            behandlingStegUtfall = doProsesserBehandling(kontekst, modell, visitor);
            fyrEventBehandlingskontrollStoppet(kontekst, modell, behandlingStegUtfall);
        } catch (RuntimeException e) {
            fyrEventBehandlingskontrollException(kontekst, modell, e);
            throw e;
        }
        return behandlingStegUtfall;
    }

    @Override
    public void behandlingTilbakeføringTilTidligsteAksjonspunkt(BehandlingskontrollKontekst kontekst,
                                                                Collection<String> oppdaterteAksjonspunkter) {

        if (oppdaterteAksjonspunkter == null || oppdaterteAksjonspunkter.isEmpty()) {
            return;
        }

        Long behandlingId = kontekst.getBehandlingId();
        Behandling behandling = serviceProvider.hentBehandling(behandlingId);

        BehandlingStegType stegType = behandling.getAktivtBehandlingSteg();

        BehandlingModell modell = getModell(behandling.getType(), behandling.getFagsakYtelseType());

        validerOgFlaggStartetProsessering();
        try {
            doTilbakeføringTilTidligsteAksjonspunkt(behandling, stegType, modell, oppdaterteAksjonspunkter);
        } finally {
            ferdigProsessering();
        }

    }

    @Override
    public boolean behandlingTilbakeføringHvisTidligereBehandlingSteg(BehandlingskontrollKontekst kontekst,
                                                                      BehandlingStegType tidligereStegType) {
        if (!erSenereSteg(kontekst, tidligereStegType)) {
            behandlingTilbakeføringTilTidligereBehandlingSteg(kontekst, tidligereStegType);
            return true;
        }
        return false;
    }

    private boolean erSenereSteg(BehandlingskontrollKontekst kontekst, BehandlingStegType tidligereStegType) {
        Behandling behandling = serviceProvider.hentBehandling(kontekst.getBehandlingId());
        return sammenlignRekkefølge(behandling.getFagsakYtelseType(), behandling.getType(),
            behandling.getAktivtBehandlingSteg(), tidligereStegType) < 0;
    }

    @Override
    public void behandlingTilbakeføringTilTidligereBehandlingSteg(BehandlingskontrollKontekst kontekst,
                                                                  BehandlingStegType tidligereStegType) {

        final BehandlingStegStatus startStatusForNyttSteg = getStatusKonfigurasjon().getInngang();
        Long behandlingId = kontekst.getBehandlingId();
        Behandling behandling = serviceProvider.hentBehandling(behandlingId);

        BehandlingStegType stegType = behandling.getAktivtBehandlingSteg();

        BehandlingModell modell = getModell(behandling.getType(), behandling.getFagsakYtelseType());

        validerOgFlaggStartetProsessering();
        try {
            doTilbakeføringTilTidligereBehandlingSteg(behandling, modell, tidligereStegType, stegType, startStatusForNyttSteg);
        } finally {
            ferdigProsessering();
        }
    }

    @Override
    public int sammenlignRekkefølge(FagsakYtelseType ytelseType, BehandlingType behandlingType, BehandlingStegType stegA, BehandlingStegType stegB) {
        BehandlingModell modell = getModell(behandlingType, ytelseType);
        return modell.erStegAFørStegB(stegA, stegB) ? -1
            : modell.erStegAFørStegB(stegB, stegA) ? 1
            : 0;
    }

    @Override
    public boolean erStegPassert(Long behandlingId, BehandlingStegType behandlingSteg) {
        Behandling behandling = serviceProvider.hentBehandling(behandlingId);
        return erStegPassert(behandling, behandlingSteg);
    }

    @Override
    public boolean erStegPassert(Behandling behandling, BehandlingStegType behandlingSteg) {
        return sammenlignRekkefølge(behandling.getFagsakYtelseType(), behandling.getType(),
            behandling.getAktivtBehandlingSteg(), behandlingSteg) > 0;
    }

    @Override
    public boolean erIStegEllerSenereSteg(Long behandlingId, BehandlingStegType behandlingSteg) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        return sammenlignRekkefølge(behandling.getFagsakYtelseType(), behandling.getType(),
            behandling.getAktivtBehandlingSteg(), behandlingSteg) >= 0;
    }

    @Override
    public BehandlingStegType finnBehandlingSteg(StartpunktType startpunktType, FagsakYtelseType fagsakYtelseType, BehandlingType behandlingType) {
        BehandlingModell modell = getModell(behandlingType, fagsakYtelseType);
        return modell.finnBehandlingSteg(startpunktType);
    }

    @Override
    public void behandlingFramføringTilSenereBehandlingSteg(BehandlingskontrollKontekst kontekst,
                                                            BehandlingStegType senereSteg) {

        final BehandlingStegStatus statusInngang = getStatusKonfigurasjon().getInngang();
        Long behandlingId = kontekst.getBehandlingId();
        Behandling behandling = serviceProvider.hentBehandling(behandlingId);

        BehandlingStegType inneværendeSteg = behandling.getAktivtBehandlingSteg();

        BehandlingModell modell = getModell(behandling.getType(), behandling.getFagsakYtelseType());

        validerOgFlaggStartetProsessering();
        try {
            doFramføringTilSenereBehandlingSteg(senereSteg, statusInngang, behandling, inneværendeSteg, modell);
        } finally {
            ferdigProsessering();
        }
    }

    @Override
    public BehandlingskontrollKontekst initBehandlingskontroll(Long behandlingId) {
        Objects.requireNonNull(behandlingId, "behandlingId"); //$NON-NLS-1$
        // først lås
        BehandlingLås lås = serviceProvider.taLås(behandlingId);
        // så les
        Behandling behandling = serviceProvider.hentBehandling(behandlingId);
        initLogContext(behandling);
        return new BehandlingskontrollKontekst(behandling.getFagsakId(), behandling.getAktørId(), lås);
    }

    @Override
    public BehandlingskontrollKontekst initBehandlingskontroll(String behandlingId) {
        // sjekk om Long eller UUID (støtter ikke vilkårlig string p.t.)
        Objects.requireNonNull(behandlingId, "behandlingId"); //$NON-NLS-1$
        if (DIGITS_PATTERN.matcher(behandlingId).matches()) {
            return initBehandlingskontroll(Long.parseLong(behandlingId));
        } else {
            return initBehandlingskontroll(UUID.fromString(behandlingId));
        }
    }

    @Override
    public BehandlingskontrollKontekst initBehandlingskontroll(UUID behandlingUuid) {
        Objects.requireNonNull(behandlingUuid, "behandlingUuid"); //$NON-NLS-1$
        // først lås
        BehandlingLås lås = serviceProvider.taLås(behandlingUuid);
        // så les
        Behandling behandling = serviceProvider.hentBehandling(behandlingUuid);
        initLogContext(behandling);
        return new BehandlingskontrollKontekst(behandling.getFagsakId(), behandling.getAktørId(), lås);
    }

    @Override
    public BehandlingskontrollKontekst initBehandlingskontroll(Behandling behandling) {
        Objects.requireNonNull(behandling, "behandling"); //$NON-NLS-1$
        initLogContext(behandling);
        // først lås
        BehandlingLås lås = behandlingRepository.taSkriveLås(behandling);

        // så les
        return new BehandlingskontrollKontekst(behandling.getFagsakId(), behandling.getAktørId(), lås);
    }

    @Override
    public void aksjonspunkterEndretStatus(BehandlingskontrollKontekst kontekst, BehandlingStegType behandlingStegType, List<Aksjonspunkt> aksjonspunkter) { // handlinger som skal skje når funnet
        if (!aksjonspunkter.isEmpty()) {
            eventPubliserer.fireEvent(new AksjonspunktStatusEvent(kontekst, aksjonspunkter, behandlingStegType));
        }
    }

    @Override
    public List<Aksjonspunkt> lagreAksjonspunkterFunnet(BehandlingskontrollKontekst kontekst, List<AksjonspunktDefinisjon> aksjonspunkter) {
        Behandling behandling = serviceProvider.hentBehandling(kontekst.getBehandlingId());
        List<Aksjonspunkt> nyeAksjonspunkt = new ArrayList<>();
        aksjonspunkter.forEach(apdef -> nyeAksjonspunkt.add(aksjonspunktKontrollRepository.leggTilAksjonspunkt(behandling, apdef)));
        behandlingRepository.lagre(behandling, kontekst.getSkriveLås());
        aksjonspunkterEndretStatus(kontekst, null, nyeAksjonspunkt);
        return nyeAksjonspunkt;
    }

    @Override
    public List<Aksjonspunkt> lagreAksjonspunkterFunnet(BehandlingskontrollKontekst kontekst, BehandlingStegType behandlingStegType,
                                                        List<AksjonspunktDefinisjon> aksjonspunkter) {
        Behandling behandling = serviceProvider.hentBehandling(kontekst.getBehandlingId());
        List<Aksjonspunkt> nyeAksjonspunkt = new ArrayList<>();
        aksjonspunkter.forEach(apdef -> nyeAksjonspunkt.add(aksjonspunktKontrollRepository.leggTilAksjonspunkt(behandling, apdef, behandlingStegType)));
        behandlingRepository.lagre(behandling, kontekst.getSkriveLås());
        aksjonspunkterEndretStatus(kontekst, behandlingStegType, nyeAksjonspunkt);
        return nyeAksjonspunkt;
    }

    @Override
    public void lagreAksjonspunkterUtført(BehandlingskontrollKontekst kontekst, BehandlingStegType behandlingStegType,
                                          List<Aksjonspunkt> aksjonspunkter) {
        Behandling behandling = serviceProvider.hentBehandling(kontekst.getBehandlingId());
        List<Aksjonspunkt> utførte = new ArrayList<>();
        aksjonspunkter.stream().filter(ap -> !ap.erUtført()).forEach(ap -> {
            aksjonspunktKontrollRepository.setTilUtført(ap, null);
            utførte.add(ap);
        });
        behandlingRepository.lagre(behandling, kontekst.getSkriveLås());
        aksjonspunkterEndretStatus(kontekst, behandlingStegType, utførte);
    }

    @Override
    public void lagreAksjonspunkterUtført(BehandlingskontrollKontekst kontekst, BehandlingStegType behandlingStegType,
                                          Aksjonspunkt aksjonspunkt, String begrunnelse) {
        Objects.requireNonNull(aksjonspunkt);
        Behandling behandling = serviceProvider.hentBehandling(kontekst.getBehandlingId());
        List<Aksjonspunkt> utførte = new ArrayList<>();

        if (!aksjonspunkt.erUtført() || !Objects.equals(aksjonspunkt.getBegrunnelse(), begrunnelse)) {
            aksjonspunktKontrollRepository.setTilUtført(aksjonspunkt, begrunnelse);
            utførte.add(aksjonspunkt);
        }

        behandlingRepository.lagre(behandling, kontekst.getSkriveLås());
        aksjonspunkterEndretStatus(kontekst, behandlingStegType, utførte);
    }

    @Override
    public void lagreAksjonspunkterAvbrutt(BehandlingskontrollKontekst kontekst, BehandlingStegType behandlingStegType,
                                           List<Aksjonspunkt> aksjonspunkter) {
        Behandling behandling = serviceProvider.hentBehandling(kontekst.getBehandlingId());
        List<Aksjonspunkt> avbrutte = new ArrayList<>();
        aksjonspunkter.stream().filter(ap -> !ap.erAvbrutt()).forEach(ap -> {
            aksjonspunktKontrollRepository.setTilAvbrutt(ap);
            avbrutte.add(ap);
        });
        behandlingRepository.lagre(behandling, kontekst.getSkriveLås());
        aksjonspunkterEndretStatus(kontekst, behandlingStegType, avbrutte);
    }

    @Override
    public void lagreAksjonspunkterReåpnet(BehandlingskontrollKontekst kontekst, List<Aksjonspunkt> aksjonspunkter, boolean setTotrinn) {
        Behandling behandling = serviceProvider.hentBehandling(kontekst.getBehandlingId());
        List<Aksjonspunkt> reåpnet = new ArrayList<>();
        aksjonspunkter.stream().filter(ap -> !ap.erOpprettet()).forEach(ap -> {
            // TODO (FC): Mangler sjekk på at angitt aksjonspunkt fins som allerede utført i Behandling. bør dobbeltsjekkes/strammes til etterhvert.
            aksjonspunktKontrollRepository.setReåpnetMedTotrinn(ap, setTotrinn);
            reåpnet.add(ap);
        });
        behandlingRepository.lagre(behandling, kontekst.getSkriveLås());
        aksjonspunkterEndretStatus(kontekst, null, reåpnet);
    }

    @Override
    public void lagreAksjonspunktResultat(BehandlingskontrollKontekst kontekst, BehandlingStegType behandlingStegType, List<AksjonspunktResultat> aksjonspunktResultater) {
        Behandling behandling = serviceProvider.hentBehandling(kontekst.getBehandlingId());
        AksjonspunktResultatOppretter apHåndterer = new AksjonspunktResultatOppretter(aksjonspunktKontrollRepository, behandling);
        List<Aksjonspunkt> endret = apHåndterer.opprettAksjonspunkter(aksjonspunktResultater, behandlingStegType);
        behandlingRepository.lagre(behandling, kontekst.getSkriveLås());
        aksjonspunkterEndretStatus(kontekst, behandlingStegType, endret);
    }

    @Override
    public BehandlingStegKonfigurasjon getBehandlingStegKonfigurasjon() {
        return behandlingStegKonfigurasjon;
    }

    @Override
    public void opprettBehandling(BehandlingskontrollKontekst kontekst, Behandling behandling) {
        final FagsakLås fagsakLås = serviceProvider.taFagsakLås(behandling.getFagsakId());

        behandlingRepository.lagre(behandling, kontekst.getSkriveLås());

        serviceProvider.oppdaterLåsVersjon(fagsakLås);
        eventPubliserer.fireEvent(kontekst, null, behandling.getStatus());
    }

    @Override
    public Behandling opprettNyBehandling(Fagsak fagsak, BehandlingType behandlingType, Consumer<Behandling> behandlingOppdaterer) {
        Behandling.Builder behandlingBuilder = Behandling.nyBehandlingFor(fagsak, behandlingType);
        Behandling nyBehandling = behandlingBuilder.build();
        behandlingOppdaterer.accept(nyBehandling);

        BehandlingskontrollKontekst kontekst = this.initBehandlingskontroll(nyBehandling);
        this.opprettBehandling(kontekst, nyBehandling);
        return nyBehandling;
    }

    void avsluttBehandling(BehandlingskontrollKontekst kontekst) {
        Behandling behandling = hentBehandling(kontekst);
        BehandlingStatus gammelStatus = behandling.getStatus();
        behandling.avsluttBehandling();
        behandlingRepository.lagre(behandling, kontekst.getSkriveLås());
        eventPubliserer.fireEvent(kontekst, gammelStatus, behandling.getStatus());

    }

    @Override
    public Aksjonspunkt settBehandlingPåVentUtenSteg(Behandling behandling,
                                                     AksjonspunktDefinisjon aksjonspunktDefinisjonIn,
                                                     LocalDateTime fristTid,
                                                     Venteårsak venteårsak,
                                                     String venteårsakVariant) {
        return settBehandlingPåVent(behandling, aksjonspunktDefinisjonIn, null, fristTid, venteårsak, venteårsakVariant);
    }

    @Override
    public Aksjonspunkt settBehandlingPåVent(Behandling behandling,
                                             AksjonspunktDefinisjon aksjonspunktDefinisjonIn,
                                             BehandlingStegType stegType,
                                             LocalDateTime fristTid,
                                             Venteårsak venteårsak,
                                             String venteårsakVariant) {
        BehandlingskontrollKontekst kontekst = initBehandlingskontroll(behandling);
        Aksjonspunkt aksjonspunkt = aksjonspunktKontrollRepository.settBehandlingPåVent(behandling, aksjonspunktDefinisjonIn,
            stegType,
            fristTid,
            venteårsak, venteårsakVariant);
        behandlingRepository.lagre(behandling, kontekst.getSkriveLås());
        if (aksjonspunkt != null) {
            aksjonspunkterEndretStatus(kontekst, aksjonspunkt.getBehandlingStegFunnet(), singletonList(aksjonspunkt));
        }
        return aksjonspunkt;
    }

    @Override
    public void settAutopunktTilUtført(Behandling behandling, BehandlingskontrollKontekst kontekst, Collection<AksjonspunktDefinisjon> aksjonspunktDefinisjon) {
        List<Aksjonspunkt> åpneAksjonspunktAvDef = behandling.getÅpneAksjonspunkter(aksjonspunktDefinisjon);
        lagreAksjonspunkterUtført(kontekst, behandling.getAktivtBehandlingSteg(), åpneAksjonspunktAvDef);
    }

    private void settAutopunkterTilUtført(BehandlingskontrollKontekst kontekst, Behandling behandling) {
        List<Aksjonspunkt> åpneAutopunkter = behandling.getÅpneAksjonspunkter(AksjonspunktType.AUTOPUNKT);
        lagreAksjonspunkterUtført(kontekst, behandling.getAktivtBehandlingSteg(), åpneAutopunkter);
    }

    private void settAutopunkterTilAvbrutt(BehandlingskontrollKontekst kontekst, Behandling behandling) {
        List<Aksjonspunkt> åpneAutopunkter = behandling.getÅpneAksjonspunkter(AksjonspunktType.AUTOPUNKT);
        lagreAksjonspunkterAvbrutt(kontekst, behandling.getAktivtBehandlingSteg(), åpneAutopunkter);
    }

    @Override
    public void taBehandlingAvVentSetAlleAutopunktUtført(Behandling behandling, BehandlingskontrollKontekst kontekst) {
        doForberedGjenopptak(behandling, kontekst, false);
    }

    @Override
    public void taBehandlingAvVentSetAlleAutopunktUtførtForHenleggelse(Behandling behandling, BehandlingskontrollKontekst kontekst) {
        doForberedGjenopptak(behandling, kontekst, true);
    }

    private void doForberedGjenopptak(Behandling behandling, BehandlingskontrollKontekst kontekst, boolean erHenleggelse) {
        List<Aksjonspunkt> aksjonspunkterSomMedførerTilbakehopp = behandling.getÅpneAksjonspunkter().stream()
            .filter(Aksjonspunkt::erAutopunkt)
            .filter(Aksjonspunkt::tilbakehoppVedGjenopptakelse)
            .collect(Collectors.toList());

        if (erHenleggelse) {
            settAutopunkterTilAvbrutt(kontekst, behandling);
        } else {
            settAutopunkterTilUtført(kontekst, behandling);
        }
        if (!aksjonspunkterSomMedførerTilbakehopp.isEmpty()) {
            final var unikeSteg = aksjonspunkterSomMedførerTilbakehopp.stream().map(Aksjonspunkt::getBehandlingStegFunnet)
                .collect(Collectors.toSet());
            if (unikeSteg.size() > 1) {
                throw BehandlingskontrollFeil.FACTORY.kanIkkeTilbakeføreBehandlingTilFlereSteg(behandling.getId()).toException();
            }
            BehandlingStegType behandlingStegFunnet = unikeSteg.iterator().next();
            behandlingTilbakeføringTilTidligereBehandlingSteg(kontekst, behandlingStegFunnet);
            // I tilfelle tilbakehopp reåpner autopunkt - de skal reutledes av steget.
            settAutopunkterTilUtført(kontekst, behandling);
        }
    }

    @Override
    public void henleggBehandling(BehandlingskontrollKontekst kontekst, BehandlingResultatType årsak) {
        // valider invarianter
        Objects.requireNonNull(årsak, "årsak"); //$NON-NLS-1$

        Optional<BehandlingStegTilstand> stegTilstandFør = doHenleggBehandling(kontekst, årsak);

        // FIXME (MAUR): Bør løses via FellesTransisjoner og unngå hardkoding av BehandlingStegType her.
        // må fremoverføres for å trigge riktig events for opprydding
        behandlingFramføringTilSenereBehandlingSteg(kontekst, BehandlingStegType.IVERKSETT_VEDTAK);

        publiserFremhoppTransisjonHenleggelse(kontekst, stegTilstandFør, BehandlingStegType.IVERKSETT_VEDTAK);


        // sett Avsluttet og fyr status
        avsluttBehandling(kontekst);
    }

    @Override
    public void henleggBehandlingFraSteg(BehandlingskontrollKontekst kontekst, BehandlingResultatType årsak) {
        // valider invarianter
        Objects.requireNonNull(årsak, "årsak"); //$NON-NLS-1$

        Optional<BehandlingStegTilstand> stegTilstandFør = doHenleggBehandling(kontekst, årsak);

        // TODO håndter henleggelse fra tidlig steg. Nå avbrytes steget og behandlingen framoverføres ikke (ok?).
        // OBS mulig rekursiv prosessering kan oppstå (evt må BehKtrl framføre til ived og fortsette)
        publiserFremhoppTransisjonHenleggelse(kontekst, stegTilstandFør, BehandlingStegType.IVERKSETT_VEDTAK);

        // sett Avsluttet og fyr status
        avsluttBehandling(kontekst);
    }

    private Optional<BehandlingStegTilstand> doHenleggBehandling(BehandlingskontrollKontekst kontekst, BehandlingResultatType årsak) {
        Behandling behandling = hentBehandling(kontekst);

        if (behandling.erSaksbehandlingAvsluttet()) {
            throw BehandlingskontrollFeil.FACTORY.kanIkkeHenleggeAvsluttetBehandling(behandling.getId()).toException();
        }
        behandling.setBehandlingResultatType(årsak);
        BehandlingStegType behandlingStegType = null;
        Optional<BehandlingStegTilstand> stegTilstandFør = behandling.getBehandlingStegTilstand();
        if (stegTilstandFør.isPresent()) {
            behandlingStegType = stegTilstandFør.get().getBehandlingSteg();
        }

        // avbryt aksjonspunkt
        List<Aksjonspunkt> åpneAksjonspunkter = behandling.getÅpneAksjonspunkter();
        åpneAksjonspunkter.forEach(aksjonspunktKontrollRepository::setTilAvbrutt);
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
        eventPubliserer.fireEvent(new AksjonspunktStatusEvent(kontekst, åpneAksjonspunkter, behandlingStegType));
        return stegTilstandFør;
    }

    private void publiserFremhoppTransisjonHenleggelse(BehandlingskontrollKontekst kontekst, Optional<BehandlingStegTilstand> stegTilstandFør, BehandlingStegType stegEtter) {
        // Publiser tranisjonsevent (eventobserver(e) håndterer tilhørende tranisjonsregler)
        boolean erOverhopp = true;
        BehandlingTransisjonEvent event = new BehandlingTransisjonEvent(kontekst, FREMHOPP_TIL_IVERKSETT_VEDTAK, stegTilstandFør.orElse(null), stegEtter, erOverhopp);
        eventPubliserer.fireEvent(event);
    }



    // TODO: (PK-49128) Midlertidig løsning for å filtrere aksjonspunkter til høyre for steg i hendelsemodul
    @Override
    public Set<String> finnAksjonspunktDefinisjonerFraOgMed(Behandling behandling, BehandlingStegType steg) {
        BehandlingModell modell = getModell(behandling.getType(), behandling.getFagsakYtelseType());
        return modell.finnAksjonspunktDefinisjonerFraOgMed(steg);
    }

    protected BehandlingStegUtfall doProsesserBehandling(BehandlingskontrollKontekst kontekst, BehandlingModell modell, BehandlingModellVisitor visitor) {

        Behandling behandling = hentBehandling(kontekst);

        if (Objects.equals(BehandlingStatus.AVSLUTTET.getKode(), behandling.getStatus().getKode())) {
            throw new IllegalStateException("Utviklerfeil: Kan ikke prosessere avsluttet behandling"); //$NON-NLS-1$
        }

        BehandlingStegType startSteg = behandling.getAktivtBehandlingSteg();
        BehandlingStegUtfall behandlingStegUtfall = modell.prosesserFra(startSteg, visitor);

        if (behandlingStegUtfall == null) {
            avsluttBehandling(kontekst);
        }
        return behandlingStegUtfall;
    }

    protected void doFramføringTilSenereBehandlingSteg(BehandlingStegType senereSteg, final BehandlingStegStatus startStatusForNyttSteg,
                                                       Behandling behandling, BehandlingStegType inneværendeSteg, BehandlingModell modell) {
        if (!erSenereSteg(modell, inneværendeSteg, senereSteg)) {
            throw new IllegalStateException(
                "Kan ikke angi steg [" + senereSteg + "] som er før eller lik inneværende steg [" + inneværendeSteg + "]" + "for behandlingId " //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
                    + behandling.getId());
        }
        oppdaterEksisterendeBehandlingStegStatusVedFramføringEllerTilbakeføring(behandling, senereSteg, startStatusForNyttSteg,
            BehandlingStegStatus.AVBRUTT);
    }

    protected void doTilbakeføringTilTidligereBehandlingSteg(Behandling behandling, BehandlingModell modell,
                                                             BehandlingStegType tidligereStegType, BehandlingStegType stegType,
                                                             final BehandlingStegStatus startStatusForNyttSteg) {
        if (behandling.erSaksbehandlingAvsluttet()) {
            throw new IllegalStateException(
                "Kan ikke tilbakeføre fra [" + stegType + "]"); //$NON-NLS-1$ //$NON-NLS-2$
        }
        if (!erLikEllerTidligereSteg(modell, stegType, tidligereStegType)) {
            throw new IllegalStateException(
                "Kan ikke angi steg [" + tidligereStegType + "] som er etter [" + stegType + "]" + "for behandlingId " + behandling.getId()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        }
        if (tidligereStegType.equals(stegType) && behandling.getBehandlingStegStatus() != null && behandling.getBehandlingStegStatus().erVedInngang()) {
            // Her står man allerede på steget man skal tilbakeføres, på inngang -> ingen tilbakeføring gjennomføres.
            return;
        }
        oppdaterEksisterendeBehandlingStegStatusVedFramføringEllerTilbakeføring(behandling, tidligereStegType, startStatusForNyttSteg,
            BehandlingStegStatus.TILBAKEFØRT);
    }

    protected void doTilbakeføringTilTidligsteAksjonspunkt(Behandling behandling, BehandlingStegType stegType, BehandlingModell modell,
                                                           Collection<String> oppdaterteAksjonspunkter) {
        Consumer<BehandlingStegType> oppdaterBehandlingStegStatus = (bst) -> {
            Optional<BehandlingStegStatus> stegStatus = modell.finnStegStatusFor(bst, oppdaterteAksjonspunkter);
            if (stegStatus.isPresent()
                && !(Objects.equals(stegStatus.get(), behandling.getBehandlingStegStatus())
                && Objects.equals(bst, behandling.getAktivtBehandlingSteg()))) {
                // er på starten av steg med endret aksjonspunkt. Ikke kjør steget her, kun oppdater
                oppdaterEksisterendeBehandlingStegStatusVedFramføringEllerTilbakeføring(behandling, bst, stegStatus.get(),
                    BehandlingStegStatus.TILBAKEFØRT);
            }
        };

        BehandlingStegModell førsteAksjonspunktSteg = modell
            .finnTidligsteStegForAksjonspunktDefinisjon(oppdaterteAksjonspunkter);

        BehandlingStegType aksjonspunktStegType = førsteAksjonspunktSteg == null ? null
            : førsteAksjonspunktSteg.getBehandlingStegType();

        if (Objects.equals(stegType, aksjonspunktStegType)) {
            // samme steg, kan ha ny BehandlingStegStatus
            oppdaterBehandlingStegStatus.accept(stegType);
        } else {
            // tilbakeføring til tidligere steg
            BehandlingStegModell revidertStegType = modell.finnFørsteSteg(stegType, aksjonspunktStegType);
            oppdaterBehandlingStegStatus.accept(revidertStegType.getBehandlingStegType());
        }
    }

    protected void fireEventBehandlingStegOvergang(BehandlingskontrollKontekst kontekst, Behandling behandling,
                                                   BehandlingStegTilstandSnapshot forrigeTilstand, BehandlingStegTilstandSnapshot nyTilstand) {
        BehandlingModell modell = getModell(behandling.getType(), behandling.getFagsakYtelseType());
        BehandlingStegOvergangEvent event = BehandlingModellImpl.nyBehandlingStegOvergangEvent(modell, forrigeTilstand, nyTilstand, kontekst);
        getEventPubliserer().fireEvent(event);
    }

    protected void oppdaterEksisterendeBehandlingStegStatusVedFramføringEllerTilbakeføring(Behandling behandling, BehandlingStegType revidertStegType,
                                                                                           BehandlingStegStatus behandlingStegStatus,
                                                                                           BehandlingStegStatus sluttStatusForAndreÅpneSteg) {
        oppdaterEksisterendeBehandling(behandling,
            (beh) -> manipulerInternBehandling.forceOppdaterBehandlingSteg(behandling, revidertStegType, behandlingStegStatus, sluttStatusForAndreÅpneSteg));
    }

    protected Behandling hentBehandling(BehandlingskontrollKontekst kontekst) {
        Objects.requireNonNull(kontekst, "kontekst"); //$NON-NLS-1$
        Long behandlingId = kontekst.getBehandlingId();
        return serviceProvider.hentBehandling(behandlingId);
    }

    protected BehandlingskontrollEventPubliserer getEventPubliserer() {
        return eventPubliserer;
    }

    protected BehandlingModell getModell(BehandlingType behandlingType, FagsakYtelseType ytelseType) {
        return behandlingModellRepository.getModell(behandlingType, ytelseType);
    }

    private void fyrEventBehandlingskontrollException(BehandlingskontrollKontekst kontekst, BehandlingModell modell, RuntimeException e) {
        Behandling behandling = hentBehandling(kontekst);
        BehandlingskontrollEvent.ExceptionEvent stoppetEvent = new ExceptionEvent(kontekst, modell, behandling.getAktivtBehandlingSteg(), behandling.getBehandlingStegStatus(), e);
        eventPubliserer.fireEvent(stoppetEvent);
    }

    private void fyrEventBehandlingskontrollStoppet(BehandlingskontrollKontekst kontekst, BehandlingModell modell, BehandlingStegUtfall stegUtfall) {
        Behandling behandling = hentBehandling(kontekst);
        BehandlingskontrollEvent event;
        if (behandling.erAvsluttet()) {
            event = new AvsluttetEvent(kontekst, modell, behandling.getAktivtBehandlingSteg(), behandling.getBehandlingStegStatus());
        } else if (stegUtfall == null) {
            event = new StoppetEvent(kontekst, modell, behandling.getSisteBehandlingStegTilstand().map(BehandlingStegTilstand::getBehandlingSteg).orElse(null), null);
        } else {
            event = new StoppetEvent(kontekst, modell, stegUtfall.getBehandlingStegType(), stegUtfall.getResultat());
        }
        eventPubliserer.fireEvent(event);
    }

    private void fyrEventBehandlingskontrollStartet(BehandlingskontrollKontekst kontekst, BehandlingModell modell) {
        Behandling behandling = hentBehandling(kontekst);
        BehandlingskontrollEvent.StartetEvent startetEvent = new StartetEvent(kontekst, modell, behandling.getAktivtBehandlingSteg(), behandling.getBehandlingStegStatus());
        eventPubliserer.fireEvent(startetEvent);
    }

    private void oppdaterEksisterendeBehandling(Behandling behandling,
                                                Consumer<Behandling> behandlingOppdaterer) {

        BehandlingStatus statusFør = behandling.getStatus();
        BehandlingStegTilstandSnapshot fraTilstand = BehandlingModellImpl.tilBehandlingsStegSnapshot(behandling.getBehandlingStegTilstand());

        // Oppdater behandling og lagre
        behandlingOppdaterer.accept(behandling);
        BehandlingLås skriveLås = behandlingRepository.taSkriveLås(behandling);
        BehandlingskontrollKontekst kontekst = new BehandlingskontrollKontekst(behandling.getFagsakId(), behandling.getAktørId(), skriveLås);
        behandlingRepository.lagre(behandling, skriveLås);

        // Publiser oppdatering
        BehandlingStatus statusEtter = behandling.getStatus();
        BehandlingStegTilstandSnapshot tilTilstand = BehandlingModellImpl.tilBehandlingsStegSnapshot(behandling.getBehandlingStegTilstand());
        fireEventBehandlingStegOvergang(kontekst, behandling, fraTilstand, tilTilstand);
        eventPubliserer.fireEvent(kontekst, statusFør, statusEtter);
    }

    @Override
    public void fremoverTransisjon(TransisjonIdentifikator transisjonId, BehandlingskontrollKontekst kontekst) {
        Behandling behandling = serviceProvider.hentBehandling(kontekst.getBehandlingId());
        Optional<BehandlingStegTilstand> stegTilstandFør = behandling.getBehandlingStegTilstand();
        BehandlingStegType fraSteg = behandling.getAktivtBehandlingSteg();

        // Flytt behandlingssteg-peker fremover
        BehandlingModell modell = getModell(behandling.getType(), behandling.getFagsakYtelseType());
        StegTransisjon transisjon = modell.finnTransisjon(transisjonId);
        BehandlingStegModell fraStegModell = modell.finnSteg(fraSteg);
        BehandlingStegModell tilStegModell = transisjon.nesteSteg(fraStegModell);
        BehandlingStegType tilSteg = tilStegModell.getBehandlingStegType();

        behandlingFramføringTilSenereBehandlingSteg(kontekst, tilSteg);

        // Publiser tranisjonsevent (eventobserver(e) håndterer tilhørende tranisjonsregler)
        BehandlingTransisjonEvent event = new BehandlingTransisjonEvent(kontekst, transisjonId, stegTilstandFør.orElse(null), tilSteg, transisjon.getMålstegHvisFremoverhopp().isPresent());
        eventPubliserer.fireEvent(event);
    }

    @Override
    public Optional<BehandlingStegType> nesteSteg(Behandling behandling, BehandlingStegType behandlingStegType) {
        if (inneholderSteg(behandling.getFagsakYtelseType(), behandling.getType(), behandlingStegType)) {
            BehandlingModell modell = getModell(behandling.getType(), behandling.getFagsakYtelseType());
            return modell.hvertStegEtter(behandlingStegType)
                .map(BehandlingStegModell::getBehandlingStegType)
                .findFirst();
        }
        return Optional.empty();
    }

    @Override
    public boolean inneholderSteg(FagsakYtelseType ytelseType, BehandlingType behandlingType, BehandlingStegType behandlingStegType) {
        BehandlingModell modell = getModell(behandlingType, ytelseType);
        return modell.hvertSteg()
            .anyMatch(steg -> steg.getBehandlingStegType().equals(behandlingStegType));
    }

    private BehandlingStegKonfigurasjon getStatusKonfigurasjon() {
        if (behandlingStegKonfigurasjon == null) {
            behandlingStegKonfigurasjon = new BehandlingStegKonfigurasjon(EnumSet.allOf(BehandlingStegStatus.class));
        }
        return behandlingStegKonfigurasjon;
    }

    private boolean erSenereSteg(BehandlingModell modell, BehandlingStegType inneværendeSteg,
                                 BehandlingStegType forventetSenereSteg) {
        return modell.erStegAFørStegB(inneværendeSteg, forventetSenereSteg);
    }

    private boolean erLikEllerTidligereSteg(BehandlingModell modell, BehandlingStegType inneværendeSteg,
                                            BehandlingStegType forventetTidligereSteg) {
        // TODO (BIXBITE) skal fjernes når innlegging av papirsøknad er inn i et steg
        if (inneværendeSteg == null) {
            return false;
        }
        if (Objects.equals(inneværendeSteg, forventetTidligereSteg)) {
            return true;
        } else {
            BehandlingStegType førsteSteg = modell.finnFørsteSteg(inneværendeSteg, forventetTidligereSteg).getBehandlingStegType();
            return Objects.equals(forventetTidligereSteg, førsteSteg);
        }
    }

    private void validerOgFlaggStartetProsessering() {
        if (nøstetProsseringGuard.get()) {
            throw new IllegalStateException("Støtter ikke nøstet prosessering i " + getClass().getSimpleName());
        } else {
            nøstetProsseringGuard.set(true);
        }
    }

    private void ferdigProsessering() {
        nøstetProsseringGuard.set(false);
    }

    private static void initLogContext(Behandling behandling) {
        LOG_CONTEXT.add("fagsak", behandling.getFagsakId()); // NOSONAR //$NON-NLS-1$
        LOG_CONTEXT.add("behandling", behandling.getId()); // NOSONAR //$NON-NLS-1$
        LOG_CONTEXT.add("saksnummer", behandling.getFagsak().getSaksnummer());
        LOG_CONTEXT.add("ytelseType", behandling.getFagsakYtelseType());
    }
}
