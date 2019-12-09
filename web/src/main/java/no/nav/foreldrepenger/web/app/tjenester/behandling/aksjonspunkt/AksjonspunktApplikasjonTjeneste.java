package no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt.AksjonspunktApplikasjonFeil.FACTORY;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktKode;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OverhoppResultat;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OverstyringAksjonspunkt;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OverstyringAksjonspunktDto;
import no.nav.foreldrepenger.behandling.aksjonspunkt.Overstyringshåndterer;
import no.nav.foreldrepenger.behandling.steg.iverksettevedtak.HenleggBehandlingTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.transisjoner.FellesTransisjoner;
import no.nav.foreldrepenger.behandlingskontroll.transisjoner.TransisjonIdentifikator;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.EndringsresultatDiff;
import no.nav.foreldrepenger.behandlingslager.behandling.EndringsresultatSnapshot;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat.Builder;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.registerinnhenting.EndringsresultatSjekker;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.vedtak.sikkerhet.context.SubjectHandler;

@ApplicationScoped
public class AksjonspunktApplikasjonTjeneste {
    private static final Logger LOGGER = LoggerFactory.getLogger(AksjonspunktApplikasjonTjeneste.class);

    private static final Set<AksjonspunktDefinisjon> VEDTAK_AP = Set.of(AksjonspunktDefinisjon.FORESLÅ_VEDTAK, AksjonspunktDefinisjon.VEDTAK_UTEN_TOTRINNSKONTROLL, AksjonspunktDefinisjon.FORESLÅ_VEDTAK_MANUELT);

    private BehandlingRepository behandlingRepository;
    private BehandlingsresultatRepository behandlingsresultatRepository;

    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;

    private AksjonspunktRepository aksjonspunktRepository;

    private HenleggBehandlingTjeneste henleggBehandlingTjeneste;

    private HistorikkTjenesteAdapter historikkTjenesteAdapter;

    private BehandlingsprosessApplikasjonTjeneste behandlingsprosessApplikasjonTjeneste;

    private EndringsresultatSjekker endringsresultatSjekker;

    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;

    public AksjonspunktApplikasjonTjeneste() {
        // CDI proxy
    }

    @Inject
    public AksjonspunktApplikasjonTjeneste(BehandlingRepositoryProvider repositoryProvider,
                                           BehandlingskontrollTjeneste behandlingskontrollTjeneste,
                                           BehandlingsprosessApplikasjonTjeneste behandlingsprosessApplikasjonTjeneste,
                                           SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                                           HistorikkTjenesteAdapter historikkTjenesteAdapter,
                                           HenleggBehandlingTjeneste henleggBehandlingTjeneste,
                                           EndringsresultatSjekker endringsresultatSjekker) {

        this.behandlingsprosessApplikasjonTjeneste = behandlingsprosessApplikasjonTjeneste;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.historikkTjenesteAdapter = historikkTjenesteAdapter;
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.behandlingsresultatRepository = repositoryProvider.getBehandlingsresultatRepository();
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
        this.henleggBehandlingTjeneste = henleggBehandlingTjeneste;
        this.endringsresultatSjekker = endringsresultatSjekker;

        this.aksjonspunktRepository = repositoryProvider.getAksjonspunktRepository();

    }

    public void bekreftAksjonspunkter(Collection<BekreftetAksjonspunktDto> bekreftedeAksjonspunktDtoer, Long behandlingId) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);

        BehandlingskontrollKontekst kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(behandlingId);
        setAnsvarligSaksbehandler(bekreftedeAksjonspunktDtoer, behandling);

        spoolTilbakeTilTidligsteAksjonspunkt(bekreftedeAksjonspunktDtoer, kontekst);

        Skjæringstidspunkt skjæringstidspunkter = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandlingId);

        OverhoppResultat overhoppResultat = bekreftAksjonspunkter(kontekst, bekreftedeAksjonspunktDtoer, behandling, skjæringstidspunkter);

        historikkTjenesteAdapter.opprettHistorikkInnslag(behandling.getId(), HistorikkinnslagType.FAKTA_ENDRET);

        behandlingRepository.lagre(getBehandlingsresultat(behandling).getVilkårResultat(), kontekst.getSkriveLås());
        behandlingRepository.lagre(behandling, kontekst.getSkriveLås());

        håndterOverhopp(overhoppResultat, kontekst);

        if (behandling.isBehandlingPåVent()) {
            // Skal ikke fortsette behandling dersom behandling ble satt på vent
            return;
        }
        fortsettBehandlingen(behandling, overhoppResultat);// skal ikke reinnhente her, avgjøres i steg?
    }

    protected void setAnsvarligSaksbehandler(Collection<BekreftetAksjonspunktDto> bekreftedeAksjonspunktDtoer, Behandling behandling) {
        if (bekreftedeAksjonspunktDtoer.stream().anyMatch(dto -> dto instanceof FatterVedtakAksjonspunktDto)) {
            return;
        }
        behandling.setAnsvarligSaksbehandler(getCurrentUserId());
    }

    protected String getCurrentUserId() {
        return SubjectHandler.getSubjectHandler().getUid();
    }

    private void spoolTilbakeTilTidligsteAksjonspunkt(Collection<? extends AksjonspunktKode> aksjonspunktDtoer,
                                                      BehandlingskontrollKontekst kontekst) {
        // NB: Første løsning på tilbakeføring ved endring i GUI (når aksjonspunkter tilhørende eldre enn aktivt steg
        // sendes inn spoles prosessen
        // tilbake). Vil utvides etter behov når regler for spoling bakover blir klarere.
        // Her sikres at behandlingskontroll hopper tilbake til aksjonspunktenes tidligste "løsesteg" dersom aktivt
        // behandlingssteg er lenger fremme i sekvensen
        List<String> bekreftedeApKoder = aksjonspunktDtoer.stream()
            .map(dto -> dto.getKode())
            .collect(toList());

        behandlingskontrollTjeneste.behandlingTilbakeføringTilTidligsteAksjonspunkt(kontekst, bekreftedeApKoder);
    }

    private void håndterOverhopp(OverhoppResultat overhoppResultat, BehandlingskontrollKontekst kontekst) {
        // TODO (essv): PKMANTIS-1992 Skrive om alle overhopp til å bruke transisjon (se fremoverTransisjon nedenfor)
        Optional<OppdateringResultat> funnetHenleggelse = overhoppResultat.finnHenleggelse();
        if (funnetHenleggelse.isPresent()) {
            OppdateringResultat henleggelse = funnetHenleggelse.get();
            henleggBehandlingTjeneste.henleggBehandling(kontekst.getBehandlingId(),
                henleggelse.getHenleggelseResultat(), henleggelse.getHenleggingsbegrunnelse());
            return;
        }

        Optional<TransisjonIdentifikator> fremoverTransisjon = overhoppResultat.finnFremoverTransisjon();
        if (fremoverTransisjon.isPresent()) {
            TransisjonIdentifikator riktigTransisjon = utledFremhoppTransisjon(kontekst, fremoverTransisjon.get());
            behandlingskontrollTjeneste.fremoverTransisjon(riktigTransisjon, kontekst);
            return;
        }
    }

    public void overstyrAksjonspunkter(Collection<OverstyringAksjonspunktDto> overstyrteAksjonspunkter, Long behandlingId) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        BehandlingskontrollKontekst kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(behandlingId);
        OverhoppResultat overhoppForOverstyring = overstyrVilkårEllerBeregning(overstyrteAksjonspunkter, behandling, kontekst);

        List<Aksjonspunkt> utførteAksjonspunkter = lagreHistorikkInnslag(behandling);

        behandlingskontrollTjeneste.aksjonspunkterUtført(kontekst, utførteAksjonspunkter, behandling.getAktivtBehandlingSteg());

        // Fremoverhopp hvis vilkår settes til AVSLÅTT
        håndterOverhopp(overhoppForOverstyring, kontekst);

        if (behandling.isBehandlingPåVent()) {
            // Skal ikke fortsette behandling dersom behandling ble satt på vent
            return;
        }
        fortsettBehandlingen(behandling, overhoppForOverstyring);// skal ikke reinnhente her, avgjøres i steg?
    }

    private void fortsettBehandlingen(Behandling behandling, OverhoppResultat overhoppResultat) {
        if (overhoppResultat.skalOppdatereGrunnlag()) {
            behandlingsprosessApplikasjonTjeneste.asynkRegisteroppdateringKjørProsess(behandling);
        } else {
            behandlingsprosessApplikasjonTjeneste.asynkKjørProsess(behandling);
        }
    }

    private boolean harVilkårResultat(Behandling behandling) {
        return behandlingsresultatRepository.hentHvisEksisterer(behandling.getId()).map(Behandlingsresultat::getVilkårResultat).isPresent();
    }

    private Behandlingsresultat getBehandlingsresultat(Behandling behandling) {
        return behandlingsresultatRepository.hent(behandling.getId());
    }

    private TransisjonIdentifikator utledFremhoppTransisjon(BehandlingskontrollKontekst kontekst, TransisjonIdentifikator transisjon) {
        if (FellesTransisjoner.FREMHOPP_VED_AVSLAG_VILKÅR.equals(transisjon)) {
            Behandling behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
            if (behandling.erRevurdering() && FagsakYtelseType.FORELDREPENGER.equals(behandling.getFagsak().getYtelseType())
                && !harAvslåttForrigeBehandling(behandling)) {
                return FellesTransisjoner.FREMHOPP_TIL_UTTAKSPLAN;
            }
            return FellesTransisjoner.FREMHOPP_TIL_FORESLÅ_BEHANDLINGSRESULTAT;
        }
        return transisjon;
    }

    private boolean harAvslåttForrigeBehandling(Behandling revurdering) {
        Optional<Behandling> originalBehandling = revurdering.getOriginalBehandling();
        if (originalBehandling.isPresent()) {
            Behandlingsresultat behandlingsresultat = getBehandlingsresultat(originalBehandling.get());
            // Dersom originalBehandling er et beslutningsvedtak må vi lete videre etter det faktiske resultatet for å kunne vurdere om forrige
            // behandling var avslått
            if (BehandlingResultatType.INGEN_ENDRING.equals(behandlingsresultat.getBehandlingResultatType())) {
                return harAvslåttForrigeBehandling(originalBehandling.get());
            } else {
                return behandlingsresultat.isBehandlingsresultatAvslått();
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private OverhoppResultat overstyrVilkårEllerBeregning(Collection<OverstyringAksjonspunktDto> overstyrteAksjonspunkter,
                                                          Behandling behandling, BehandlingskontrollKontekst kontekst) {
        OverhoppResultat overhoppResultat = OverhoppResultat.tomtResultat();

        // oppdater for overstyring
        overstyrteAksjonspunkter.forEach(dto -> {
            EndringsresultatSnapshot snapshotFør = endringsresultatSjekker.opprettEndringsresultatIdPåBehandlingSnapshot(behandling);

            @SuppressWarnings("rawtypes")
            Overstyringshåndterer overstyringshåndterer = finnOverstyringshåndterer(dto);
            OppdateringResultat oppdateringResultat = overstyringshåndterer.håndterOverstyring(dto, behandling, kontekst);
            overhoppResultat.leggTil(oppdateringResultat);

            settToTrinnPåOverstyrtAksjonspunktHvisEndring(behandling, dto, snapshotFør, oppdateringResultat.kreverTotrinnsKontroll());
        });

        // Tilbakestill gjeldende steg før fremføring
        spoolTilbakeTilTidligsteAksjonspunkt(overstyrteAksjonspunkter, kontekst);

        // legg til overstyring aksjonspunkt (normalt vil være utført) og historikk
        overstyrteAksjonspunkter.forEach(dto -> {
            @SuppressWarnings("rawtypes")
            Overstyringshåndterer overstyringshåndterer = finnOverstyringshåndterer(dto);
            overstyringshåndterer.håndterAksjonspunktForOverstyringPrecondition(dto, behandling);
            var aksjonspunktDefinisjon = overstyringshåndterer.aksjonspunktForInstans();
            var aksjonspunktBegrunnelse = opprettAksjonspunktForOverstyring(kontekst, behandling, dto, aksjonspunktDefinisjon);
            boolean endretBegrunnelse = begrunnelseErEndret(aksjonspunktBegrunnelse, dto.getBegrunnelse());
            overstyringshåndterer.håndterAksjonspunktForOverstyringHistorikk(dto, behandling, endretBegrunnelse);
        });

        boolean totrinn = overhoppResultat.finnTotrinn();
        overhoppResultat.finnEkstraAksjonspunktResultat().forEach(res -> håndterEkstraAksjonspunktResultat(kontekst, behandling, totrinn, res.getElement1(), res.getElement2()));

        return overhoppResultat;
    }

    private String opprettAksjonspunktForOverstyring(BehandlingskontrollKontekst kontekst, Behandling behandling, OverstyringAksjonspunkt dto, AksjonspunktDefinisjon apDef) {
        Optional<Aksjonspunkt> eksisterendeAksjonspunkt = behandling.getAksjonspunktMedDefinisjonOptional(apDef);
        Aksjonspunkt aksjonspunkt = eksisterendeAksjonspunkt.orElseGet(() -> behandlingskontrollTjeneste.lagreAksjonspunkterFunnet(kontekst, List.of(apDef)).get(0));
        String begrunnelse = aksjonspunkt.getBegrunnelse();

        if (aksjonspunkt.erAvbrutt()) {
            // Må reåpne avbrutte før de kan settes til utført (kunne ha vært én operasjon i aksjonspunktRepository)
            aksjonspunktRepository.setReåpnet(aksjonspunkt);
            behandlingskontrollTjeneste.lagreAksjonspunkterUtført(kontekst, null, aksjonspunkt, dto.getBegrunnelse());
        } else {
            behandlingskontrollTjeneste.lagreAksjonspunkterUtført(kontekst, null, aksjonspunkt, dto.getBegrunnelse());
        }
        return begrunnelse;
    }

    private void håndterEkstraAksjonspunktResultat(BehandlingskontrollKontekst kontekst, Behandling behandling, boolean totrinn, AksjonspunktDefinisjon apDef, AksjonspunktStatus nyStatus) {
        Optional<Aksjonspunkt> eksisterendeAksjonspunkt = behandling.getAksjonspunktMedDefinisjonOptional(apDef);
        Aksjonspunkt aksjonspunkt = eksisterendeAksjonspunkt.orElseGet(() -> behandlingskontrollTjeneste.lagreAksjonspunkterFunnet(kontekst, List.of(apDef)).get(0));

        if (totrinn && !AksjonspunktStatus.AVBRUTT.equals(nyStatus)  && aksjonspunktStøtterTotrinn(aksjonspunkt)) {
            aksjonspunktRepository.setToTrinnsBehandlingKreves(aksjonspunkt);
        }
        if (nyStatus.equals(aksjonspunkt.getStatus())) {
            return;
        }
        if (AksjonspunktStatus.OPPRETTET.equals(nyStatus)) {
            behandlingskontrollTjeneste.lagreAksjonspunkterReåpnet(kontekst, List.of(aksjonspunkt), Optional.empty());
        } else if (AksjonspunktStatus.AVBRUTT.equals(nyStatus)) {
            behandlingskontrollTjeneste.lagreAksjonspunkterAvbrutt(kontekst, behandling.getAktivtBehandlingSteg(), List.of(aksjonspunkt));
        } else {
            if (aksjonspunkt.erAvbrutt()) {
                // Må reåpne avbrutte før de kan settes til utført (kunne ha vært én operasjon i aksjonspunktRepository)
                aksjonspunktRepository.setReåpnet(aksjonspunkt);
            }
            behandlingskontrollTjeneste.lagreAksjonspunkterUtført(kontekst, null, List.of(aksjonspunkt));
        }
    }


    private List<Aksjonspunkt> lagreHistorikkInnslag(Behandling behandling) {

        // TODO(FC): Kan vi flytte spesielhåndtering av SØKERS_OPPLYSNINGSPLIKT_OVST ned til
        // SøkersOpplysningspliktOverstyringshåndterer?
        // vis vi aldri sender inn mer enn en overstyring kan historikk opprettes også der.

        List<Aksjonspunkt> utførteAksjonspunkter = behandling
            .getAksjonspunktMedDefinisjonOptional(AksjonspunktDefinisjon.SØKERS_OPPLYSNINGSPLIKT_OVST)
            .map(Collections::singletonList)
            .orElse(emptyList());
        if (utførteAksjonspunkter.isEmpty()) {
            historikkTjenesteAdapter.opprettHistorikkInnslag(behandling.getId(), HistorikkinnslagType.OVERSTYRT);
        } else {
            // SØKERS_OPPLYSNINGSPLIKT_OVST skal gi et "vanlig" historikkinnslag
            historikkTjenesteAdapter.opprettHistorikkInnslag(behandling.getId(), HistorikkinnslagType.FAKTA_ENDRET);
        }

        return utførteAksjonspunkter;
    }

    private OverhoppResultat bekreftAksjonspunkter(BehandlingskontrollKontekst kontekst,
                                                   Collection<BekreftetAksjonspunktDto> bekreftedeAksjonspunktDtoer,
                                                   Behandling behandling,
                                                   Skjæringstidspunkt skjæringstidspunkter) {

        List<Aksjonspunkt> utførteAksjonspunkter = new ArrayList<>();
        List<Aksjonspunkt> avbrutteAksjonspunkter = new ArrayList<>();
        OverhoppResultat overhoppResultat = OverhoppResultat.tomtResultat();

        VilkårResultat.Builder vilkårBuilder = harVilkårResultat(behandling)
            ? VilkårResultat.builderFraEksisterende(getBehandlingsresultat(behandling).getVilkårResultat())
            : VilkårResultat.builder();

        bekreftedeAksjonspunktDtoer
            .forEach(dto -> bekreftAksjonspunkt(behandling, skjæringstidspunkter, vilkårBuilder, utførteAksjonspunkter, avbrutteAksjonspunkter, overhoppResultat, dto));

        VilkårResultat vilkårResultat = vilkårBuilder.buildFor(behandling);
        behandlingRepository.lagre(vilkårResultat, kontekst.getSkriveLås());

        behandlingRepository.lagre(behandling, kontekst.getSkriveLås());

        if (!avbrutteAksjonspunkter.isEmpty()) {
            behandlingskontrollTjeneste.aksjonspunkterAvbrutt(kontekst, avbrutteAksjonspunkter, behandling.getAktivtBehandlingSteg());
        }
        if (!utførteAksjonspunkter.isEmpty()) {
            behandlingskontrollTjeneste.aksjonspunkterUtført(kontekst, utførteAksjonspunkter, behandling.getAktivtBehandlingSteg());
        }

        boolean totrinn = overhoppResultat.finnTotrinn();
        overhoppResultat.finnEkstraAksjonspunktResultat().forEach(res -> håndterEkstraAksjonspunktResultat(kontekst, behandling, totrinn, res.getElement1(), res.getElement2()));

        return overhoppResultat;
    }

    private void bekreftAksjonspunkt(Behandling behandling,
                                     Skjæringstidspunkt skjæringstidspunkter,
                                     Builder vilkårBuilder,
                                     List<Aksjonspunkt> utførteAksjonspunkter,
                                     List<Aksjonspunkt> avbrutteAksjonspunkter,
                                     OverhoppResultat overhoppResultat,
                                     BekreftetAksjonspunktDto dto) {
        // Endringskontroll for aksjonspunkt
        Aksjonspunkt aksjonspunkt = behandling.getAksjonspunktFor(dto.getKode())
            .orElseThrow(() -> new IllegalStateException("Utvikler-feil: Har ikke aksjonspunkt av type: " + dto.getKode()));

        EndringsresultatSnapshot snapshotFør = endringsresultatSjekker.opprettEndringsresultatIdPåBehandlingSnapshot(behandling);

        AksjonspunktOppdaterer<BekreftetAksjonspunktDto> oppdaterer = finnAksjonspunktOppdaterer(dto.getClass(), dto.getKode());
        AksjonspunktOppdaterParameter param = new AksjonspunktOppdaterParameter(behandling, Optional.of(aksjonspunkt), skjæringstidspunkter, vilkårBuilder, dto);
        OppdateringResultat delresultat = oppdaterer.oppdater(dto, param);
        overhoppResultat.leggTil(delresultat);

        settToTrinnHvisRevurderingOgEndring(behandling, aksjonspunkt, dto.getBegrunnelse(), snapshotFør, delresultat.kreverTotrinnsKontroll());

        if (!aksjonspunkt.erAvbrutt() && delresultat.skalUtføreAksjonspunkt()) {
            if (aksjonspunktRepository.setTilUtført(aksjonspunkt, dto.getBegrunnelse())) {
                utførteAksjonspunkter.add(aksjonspunkt);
            }
        }
        if (aksjonspunkt.erÅpentAksjonspunkt() && delresultat.skalAvbryteAksjonspunkt()) {
            aksjonspunktRepository.setTilAvbrutt(aksjonspunkt);
            avbrutteAksjonspunkter.add(aksjonspunkt);
        }
    }

    @SuppressWarnings("unchecked")
    private AksjonspunktOppdaterer<BekreftetAksjonspunktDto> finnAksjonspunktOppdaterer(Class<? extends BekreftetAksjonspunktDto> dtoClass,
                                                                                        String aksjonspunktDefinisjonKode) {
        Instance<Object> instance = finnAdapter(dtoClass, AksjonspunktOppdaterer.class);
        if (instance.isUnsatisfied()) {
            throw FACTORY.kanIkkeFinneAksjonspunktUtleder(aksjonspunktDefinisjonKode).toException();
        } else {
            Object minInstans = instance.get();
            if (minInstans.getClass().isAnnotationPresent(Dependent.class)) {
                throw new IllegalStateException(
                    "Kan ikke ha @Dependent scope bean ved Instance lookup dersom en ikke også håndtere lifecycle selv: " + minInstans.getClass());
            }
            return (AksjonspunktOppdaterer<BekreftetAksjonspunktDto>) minInstans;

        }
    }

    private Instance<Object> finnAdapter(Class<?> cls, final Class<?> targetAdapter) {
        CDI<Object> cdi = CDI.current();
        Instance<Object> instance = cdi.select(new DtoTilServiceAdapter.Literal(cls, targetAdapter));

        // hvis unsatisfied, søk parent
        while (instance.isUnsatisfied() && !Objects.equals(Object.class, cls)) {
            cls = cls.getSuperclass();
            instance = cdi.select(new DtoTilServiceAdapter.Literal(cls, targetAdapter));
            if (!instance.isUnsatisfied()) {
                return instance;
            }
        }

        return instance;
    }

    @SuppressWarnings("unchecked")
    private <V extends OverstyringAksjonspunktDto> Overstyringshåndterer<V> finnOverstyringshåndterer(V dto) {
        Instance<Object> instance = finnAdapter(dto.getClass(), Overstyringshåndterer.class);

        if (instance.isUnsatisfied()) {
            throw FACTORY.kanIkkeFinneOverstyringshåndterer(dto.getClass().getSimpleName()).toException();
        } else {
            Object minInstans = instance.get();
            if (minInstans.getClass().isAnnotationPresent(Dependent.class)) {
                throw new IllegalStateException(
                    "Kan ikke ha @Dependent scope bean ved Instance lookup dersom en ikke også håndtere lifecycle selv: " + minInstans.getClass());
            }
            return (Overstyringshåndterer<V>) minInstans;
        }
    }

    private void settToTrinnPåOverstyrtAksjonspunktHvisEndring(Behandling behandling, OverstyringAksjonspunktDto dto,
                                                               EndringsresultatSnapshot snapshotFør, boolean resultatKreverTotrinn) {
        AksjonspunktDefinisjon aksjonspunktDefinisjon = AksjonspunktDefinisjon.fraKode(dto.getKode());
        if (behandling.harAksjonspunktMedType(aksjonspunktDefinisjon)) {
            Aksjonspunkt aksjonspunkt = behandling.getAksjonspunktFor(aksjonspunktDefinisjon);
            settToTrinnHvisRevurderingOgEndring(behandling, aksjonspunkt, dto.getBegrunnelse(), snapshotFør, resultatKreverTotrinn);
        }
    }

    private void settToTrinnHvisRevurderingOgEndring(Behandling behandling, Aksjonspunkt aksjonspunkt,
                                                     String nyBegrunnelse, EndringsresultatSnapshot snapshotFør, boolean resultatKreverTotrinn) {
        if (resultatKreverTotrinn) {
            aksjonspunktRepository.setToTrinnsBehandlingKreves(aksjonspunkt);
            return;
        }
        if (behandling.erRevurdering() && aksjonspunktStøtterTotrinn(aksjonspunkt) && !aksjonspunkt.isToTrinnsBehandling()) {
            EndringsresultatDiff endringsresultatDiff = endringsresultatSjekker.finnIdEndringerPåBehandling(behandling, snapshotFør);
            boolean idEndret = endringsresultatDiff.erIdEndret();
            boolean begrunnelseEndret = begrunnelseErEndret(aksjonspunkt.getBegrunnelse(), nyBegrunnelse);
            if (idEndret || begrunnelseEndret) {
                LOGGER.info("Revurdert aksjonspunkt {} på Behandling {} har endring (id={}, begrunnelse={}) - setter totrinnskontroll", //$NON-NLS-1$
                    aksjonspunkt.getAksjonspunktDefinisjon().getKode(), behandling.getId(), idEndret, begrunnelseEndret); // NOSONAR
                aksjonspunktRepository.setToTrinnsBehandlingKreves(aksjonspunkt);
            }
        }
    }

    private boolean aksjonspunktStøtterTotrinn(Aksjonspunkt aksjonspunkt) {
        SkjermlenkeType aksjonspunktSkjermlenkeType = aksjonspunkt.getAksjonspunktDefinisjon().getSkjermlenkeType();
        return !VEDTAK_AP.contains(aksjonspunkt.getAksjonspunktDefinisjon())
            // Aksjonspunkter må ha SkjermlenkeType for å støtte totrinnskontroll
            && aksjonspunktSkjermlenkeType != null
            && !SkjermlenkeType.UDEFINERT.equals(aksjonspunktSkjermlenkeType);
    }

    private boolean begrunnelseErEndret(String gammelBegrunnelse, String nyBegrunnelse) {
        return !Objects.equals(gammelBegrunnelse, nyBegrunnelse);
    }
}
