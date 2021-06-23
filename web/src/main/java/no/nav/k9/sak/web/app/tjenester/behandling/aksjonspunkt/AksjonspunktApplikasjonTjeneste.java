package no.nav.k9.sak.web.app.tjenester.behandling.aksjonspunkt;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static no.nav.k9.sak.web.app.tjenester.behandling.aksjonspunkt.AksjonspunktApplikasjonFeil.FACTORY;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.SkjermlenkeType;
import no.nav.k9.kodeverk.historikk.HistorikkinnslagType;
import no.nav.k9.sak.behandling.Skjæringstidspunkt;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.k9.sak.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.k9.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.k9.sak.behandling.aksjonspunkt.OverhoppResultat;
import no.nav.k9.sak.behandling.aksjonspunkt.Overstyringshåndterer;
import no.nav.k9.sak.behandling.prosessering.BehandlingsprosessApplikasjonTjeneste;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.k9.sak.behandlingskontroll.transisjoner.FellesTransisjoner;
import no.nav.k9.sak.behandlingskontroll.transisjoner.TransisjonIdentifikator;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.EndringsresultatDiff;
import no.nav.k9.sak.behandlingslager.behandling.EndringsresultatSnapshot;
import no.nav.k9.sak.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.k9.sak.behandlingslager.behandling.aksjonspunkt.AksjonspunktRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatBuilder;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.domene.behandling.steg.iverksettevedtak.HenleggBehandlingTjeneste;
import no.nav.k9.sak.domene.registerinnhenting.EndringsresultatSjekker;
import no.nav.k9.sak.historikk.HistorikkTjenesteAdapter;
import no.nav.k9.sak.kontrakt.aksjonspunkt.AksjonspunktKode;
import no.nav.k9.sak.kontrakt.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.k9.sak.kontrakt.aksjonspunkt.OverstyringAksjonspunkt;
import no.nav.k9.sak.kontrakt.aksjonspunkt.OverstyringAksjonspunktDto;
import no.nav.k9.sak.kontrakt.vedtak.FatterVedtakAksjonspunktDto;
import no.nav.k9.sak.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.k9.sikkerhet.context.SubjectHandler;

@Dependent
public class AksjonspunktApplikasjonTjeneste {
    private static final Logger LOGGER = LoggerFactory.getLogger(AksjonspunktApplikasjonTjeneste.class);

    private static final Set<AksjonspunktDefinisjon> VEDTAK_AP = Set.of(AksjonspunktDefinisjon.FORESLÅ_VEDTAK, AksjonspunktDefinisjon.VEDTAK_UTEN_TOTRINNSKONTROLL,
        AksjonspunktDefinisjon.FORESLÅ_VEDTAK_MANUELT);

    private BehandlingRepository behandlingRepository;

    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;

    private AksjonspunktRepository aksjonspunktRepository;

    private HenleggBehandlingTjeneste henleggBehandlingTjeneste;

    private HistorikkTjenesteAdapter historikkTjenesteAdapter;

    private BehandlingsprosessApplikasjonTjeneste behandlingsprosessApplikasjonTjeneste;

    private EndringsresultatSjekker endringsresultatSjekker;

    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private VilkårResultatRepository vilkårResultatRepository;

    public AksjonspunktApplikasjonTjeneste() {
        // CDI proxy
    }

    @Inject
    public AksjonspunktApplikasjonTjeneste(BehandlingRepositoryProvider repositoryProvider,
                                           BehandlingskontrollTjeneste behandlingskontrollTjeneste,
                                           AksjonspunktRepository aksjonspunktRepository,
                                           BehandlingsprosessApplikasjonTjeneste behandlingsprosessApplikasjonTjeneste,
                                           SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                                           HistorikkTjenesteAdapter historikkTjenesteAdapter,
                                           HenleggBehandlingTjeneste henleggBehandlingTjeneste,
                                           EndringsresultatSjekker endringsresultatSjekker) {

        this.aksjonspunktRepository = aksjonspunktRepository;
        this.behandlingsprosessApplikasjonTjeneste = behandlingsprosessApplikasjonTjeneste;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.historikkTjenesteAdapter = historikkTjenesteAdapter;
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.vilkårResultatRepository = repositoryProvider.getVilkårResultatRepository();
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
        this.henleggBehandlingTjeneste = henleggBehandlingTjeneste;
        this.endringsresultatSjekker = endringsresultatSjekker;

    }

    public void bekreftAksjonspunkter(Collection<BekreftetAksjonspunktDto> bekreftedeAksjonspunktDtoer, Long behandlingId) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);

        BehandlingskontrollKontekst kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(behandlingId);
        setAnsvarligSaksbehandler(bekreftedeAksjonspunktDtoer, behandling);

        spoolTilbakeTilTidligsteAksjonspunkt(behandling, bekreftedeAksjonspunktDtoer, kontekst);

        Skjæringstidspunkt skjæringstidspunkter = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandlingId);

        OverhoppResultat overhoppResultat = bekreftAksjonspunkter(kontekst, bekreftedeAksjonspunktDtoer, behandling, skjæringstidspunkter);

        historikkTjenesteAdapter.opprettHistorikkInnslag(behandling.getId(), HistorikkinnslagType.FAKTA_ENDRET);

        behandlingRepository.lagre(behandling, kontekst.getSkriveLås());

        håndterOverhopp(overhoppResultat, kontekst);

        if (behandling.isBehandlingPåVent()) {
            // Skal ikke fortsette behandling dersom behandling ble satt på vent
            return;
        }
        fortsettBehandlingen(behandling, kontekst, overhoppResultat);// skal ikke reinnhente her, avgjøres i steg?
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

    private void spoolTilbakeTilTidligsteAksjonspunkt(Behandling behandling, Collection<? extends AksjonspunktKode> aksjonspunktDtoer,
                                                      BehandlingskontrollKontekst kontekst) {
        // NB: Første løsning på tilbakeføring ved endring i GUI (når aksjonspunkter tilhørende eldre enn aktivt steg
        // sendes inn spoles prosessen
        // tilbake). Vil utvides etter behov når regler for spoling bakover blir klarere.
        // Her sikres at behandlingskontroll hopper tilbake til aksjonspunktenes tidligste "løsesteg" dersom aktivt
        // behandlingssteg er lenger fremme i sekvensen
        List<String> bekreftedeApKoder = aksjonspunktDtoer.stream()
            .map(AksjonspunktKode::getKode)
            .collect(toList());

        // Valider at aksjonspunktene eksisterer på behandlingen
        var invalidAksjonspunkt = bekreftedeApKoder.stream()
            .filter(it -> behandling.getAksjonspunktFor(it).isEmpty())
            .collect(Collectors.toSet());

        if (!invalidAksjonspunkt.isEmpty()) {
            throw new IllegalStateException("Prøver å løse aksjonspunkter som ikke finnes på behandlingen. Har ikke '" + invalidAksjonspunkt + "'");
        }

        behandlingskontrollTjeneste.behandlingTilbakeføringTilTidligsteAksjonspunkt(kontekst, bekreftedeApKoder);
    }

    private void håndterOverhopp(OverhoppResultat overhoppResultat, BehandlingskontrollKontekst kontekst) {
        Optional<TransisjonIdentifikator> fremoverTransisjon = overhoppResultat.finnFremoverTransisjon();
        if (fremoverTransisjon.isPresent()) {
            TransisjonIdentifikator riktigTransisjon = utledFremhoppTransisjon(fremoverTransisjon.get());
            if (riktigTransisjon != null) {
                behandlingskontrollTjeneste.fremoverTransisjon(riktigTransisjon, kontekst);
            }
        }
    }

    public void overstyrAksjonspunkter(Collection<OverstyringAksjonspunktDto> overstyrteAksjonspunkter, Long behandlingId) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        BehandlingskontrollKontekst kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(behandlingId);
        OverhoppResultat overhoppForOverstyring = overstyrVilkårEllerBeregning(overstyrteAksjonspunkter, behandling, kontekst);

        List<Aksjonspunkt> utførteAksjonspunkter = lagreHistorikkInnslag(behandling);

        behandlingskontrollTjeneste.aksjonspunkterEndretStatus(kontekst, behandling.getAktivtBehandlingSteg(), utførteAksjonspunkter);

        // Fremoverhopp hvis vilkår settes til AVSLÅTT
        håndterOverhopp(overhoppForOverstyring, kontekst);

        if (behandling.isBehandlingPåVent()) {
            // Skal ikke fortsette behandling dersom behandling ble satt på vent
            return;
        }
        fortsettBehandlingen(behandling, kontekst, overhoppForOverstyring);// skal ikke reinnhente her, avgjøres i steg?
    }

    private void fortsettBehandlingen(Behandling behandling, BehandlingskontrollKontekst kontekst, OverhoppResultat overhoppResultat) {
        if (overhoppResultat.skalOppdatereGrunnlag()) {
            behandlingsprosessApplikasjonTjeneste.asynkRegisteroppdateringKjørProsess(behandling);
        } else {
            if (overhoppResultat.skalRekjøreSteg()) {
                var behandlingSteg = overhoppResultat.stegSomSkalRekjøres();
                // Denne bør tilbakeføre til inngangen av steget
                behandlingskontrollTjeneste.behandlingTilbakeføringTilTidligereBehandlingSteg(kontekst, behandlingSteg);
            }
            behandlingsprosessApplikasjonTjeneste.asynkKjørProsess(behandling);
        }
    }

    private TransisjonIdentifikator utledFremhoppTransisjon(TransisjonIdentifikator transisjon) {
        if (FellesTransisjoner.FREMHOPP_VED_AVSLAG_VILKÅR.equals(transisjon)) {
            return FellesTransisjoner.FREMHOPP_TIL_FORESLÅ_BEHANDLINGSRESULTAT;
        }
        return transisjon;
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
            var aksjonspunktDefinisjon = overstyringshåndterer.aksjonspunktForInstans();
            opprettAksjonspunktForOverstyring(kontekst, behandling, aksjonspunktDefinisjon);
            OppdateringResultat oppdateringResultat = overstyringshåndterer.håndterOverstyring(dto, behandling, kontekst);
            overhoppResultat.leggTil(oppdateringResultat);

            settToTrinnPåOverstyrtAksjonspunktHvisEndring(behandling, dto, snapshotFør, oppdateringResultat.kreverTotrinnsKontroll());
        });

        // Tilbakestill gjeldende steg før fremføring
        spoolTilbakeTilTidligsteAksjonspunkt(behandling, overstyrteAksjonspunkter, kontekst);

        // legg til overstyring aksjonspunkt (normalt vil være utført) og historikk
        overstyrteAksjonspunkter.forEach(dto -> {
            @SuppressWarnings("rawtypes")
            Overstyringshåndterer overstyringshåndterer = finnOverstyringshåndterer(dto);
            overstyringshåndterer.håndterAksjonspunktForOverstyringPrecondition(dto, behandling);
            var aksjonspunktDefinisjon = overstyringshåndterer.aksjonspunktForInstans();
            var aksjonspunktBegrunnelse = utførAksjonspunktForOverstyring(kontekst, behandling, dto, aksjonspunktDefinisjon);
            boolean endretBegrunnelse = begrunnelseErEndret(aksjonspunktBegrunnelse, dto.getBegrunnelse());
            overstyringshåndterer.håndterAksjonspunktForOverstyringHistorikk(dto, behandling, endretBegrunnelse);
        });

        boolean totrinn = overhoppResultat.finnTotrinn();
        overhoppResultat.finnEkstraAksjonspunktResultat().forEach(res -> håndterEkstraAksjonspunktResultat(kontekst, behandling, totrinn, res.getElement1(), res.getElement2()));

        return overhoppResultat;
    }

    private void opprettAksjonspunktForOverstyring(BehandlingskontrollKontekst kontekst, Behandling behandling, AksjonspunktDefinisjon apDef) {
        Optional<Aksjonspunkt> eksisterendeAksjonspunkt = behandling.getAksjonspunktMedDefinisjonOptional(apDef);
        Aksjonspunkt aksjonspunkt = eksisterendeAksjonspunkt.orElseGet(() -> behandlingskontrollTjeneste.lagreAksjonspunkterFunnet(kontekst, List.of(apDef)).get(0));

        if (aksjonspunkt.erAvbrutt()) {
            // Må reåpne avbrutte før de kan settes til utført (kunne ha vært én operasjon i aksjonspunktRepository)
            behandlingskontrollTjeneste.lagreAksjonspunkterReåpnet(kontekst, List.of(aksjonspunkt), false);
        }
    }

    private String utførAksjonspunktForOverstyring(BehandlingskontrollKontekst kontekst, Behandling behandling, OverstyringAksjonspunkt dto, AksjonspunktDefinisjon apDef) {
        Optional<Aksjonspunkt> eksisterendeAksjonspunkt = behandling.getAksjonspunktMedDefinisjonOptional(apDef);
        Aksjonspunkt aksjonspunkt = eksisterendeAksjonspunkt.orElseThrow();
        String begrunnelse = aksjonspunkt.getBegrunnelse();

        if (aksjonspunkt.erAvbrutt()) {
            // Må reåpne avbrutte før de kan settes til utført (kunne ha vært én operasjon i aksjonspunktRepository)
            behandlingskontrollTjeneste.lagreAksjonspunkterReåpnet(kontekst, List.of(aksjonspunkt), false);
            behandlingskontrollTjeneste.lagreAksjonspunkterUtført(kontekst, null, aksjonspunkt, dto.getBegrunnelse());
        } else {
            behandlingskontrollTjeneste.lagreAksjonspunkterUtført(kontekst, null, aksjonspunkt, dto.getBegrunnelse());
        }
        return begrunnelse;
    }

    private void håndterEkstraAksjonspunktResultat(BehandlingskontrollKontekst kontekst, Behandling behandling, boolean totrinn, AksjonspunktDefinisjon apDef, AksjonspunktStatus nyStatus) {
        Optional<Aksjonspunkt> eksisterendeAksjonspunkt = behandling.getAksjonspunktMedDefinisjonOptional(apDef);
        Aksjonspunkt aksjonspunkt = eksisterendeAksjonspunkt.orElseGet(() -> behandlingskontrollTjeneste.lagreAksjonspunkterFunnet(kontekst, List.of(apDef)).get(0));

        if (totrinn && !AksjonspunktStatus.AVBRUTT.equals(nyStatus) && aksjonspunktStøtterTotrinn(aksjonspunkt)) {
            aksjonspunktRepository.setToTrinnsBehandlingKreves(aksjonspunkt);
        }
        if (nyStatus.equals(aksjonspunkt.getStatus())) {
            return;
        }
        if (AksjonspunktStatus.OPPRETTET.equals(nyStatus)) {
            behandlingskontrollTjeneste.lagreAksjonspunkterReåpnet(kontekst, List.of(aksjonspunkt), false);
        } else if (AksjonspunktStatus.AVBRUTT.equals(nyStatus)) {
            behandlingskontrollTjeneste.lagreAksjonspunkterAvbrutt(kontekst, behandling.getAktivtBehandlingSteg(), List.of(aksjonspunkt));
        } else {
            if (aksjonspunkt.erAvbrutt()) {
                // Må reåpne avbrutte før de kan settes til utført (kunne ha vært én operasjon i aksjonspunktRepository)
                behandlingskontrollTjeneste.lagreAksjonspunkterReåpnet(kontekst, List.of(aksjonspunkt), false);
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

        OverhoppResultat overhoppResultat = OverhoppResultat.tomtResultat();

        final var vilkåreneOptional = vilkårResultatRepository.hentHvisEksisterer(behandling.getId());
        VilkårResultatBuilder vilkårBuilder = vilkåreneOptional.map(Vilkårene::builderFraEksisterende).orElse(Vilkårene.builder());

        bekreftedeAksjonspunktDtoer
            .forEach(dto -> bekreftAksjonspunkt(kontekst, behandling, skjæringstidspunkter, vilkårBuilder, overhoppResultat, dto));

        Vilkårene vilkårene = vilkårBuilder.build();
        behandlingRepository.lagre(behandling, kontekst.getSkriveLås());
        vilkårResultatRepository.lagre(kontekst.getBehandlingId(), vilkårene);

        boolean totrinn = overhoppResultat.finnTotrinn();
        overhoppResultat.finnEkstraAksjonspunktResultat().forEach(res -> håndterEkstraAksjonspunktResultat(kontekst, behandling, totrinn, res.getElement1(), res.getElement2()));

        return overhoppResultat;
    }

    private void bekreftAksjonspunkt(BehandlingskontrollKontekst kontekst, Behandling behandling,
                                     Skjæringstidspunkt skjæringstidspunkter,
                                     VilkårResultatBuilder vilkårBuilder,
                                     OverhoppResultat overhoppResultat,
                                     BekreftetAksjonspunktDto dto) {
        // Endringskontroll for aksjonspunkt
        Aksjonspunkt aksjonspunkt = behandling.getAksjonspunktFor(dto.getKode())
            .orElseThrow(() -> new IllegalStateException("Utvikler-feil: Har ikke aksjonspunkt av type: " + dto.getKode()));

        EndringsresultatSnapshot snapshotFør = endringsresultatSjekker.opprettEndringsresultatIdPåBehandlingSnapshot(behandling);

        AksjonspunktOppdaterer<BekreftetAksjonspunktDto> oppdaterer = finnAksjonspunktOppdaterer(dto.getClass(), dto.getKode());
        AksjonspunktOppdaterParameter param = new AksjonspunktOppdaterParameter(behandling, Optional.of(aksjonspunkt), skjæringstidspunkter, vilkårBuilder, dto);
        OppdateringResultat delresultat = oppdaterer.oppdater(dto, param);

        if (aksjonspunkt.tilbakehoppVedGjenopptakelse()) {
            delresultat.skalRekjøreSteg();
            delresultat.setSteg(aksjonspunkt.getAksjonspunktDefinisjon().getBehandlingSteg());
        }

        overhoppResultat.leggTil(delresultat);

        settToTrinnHvisRevurderingOgEndring(behandling, aksjonspunkt, dto.getBegrunnelse(), snapshotFør, delresultat.kreverTotrinnsKontroll());

        if (!aksjonspunkt.erAvbrutt() && delresultat.skalUtføreAksjonspunkt()) {
            behandlingskontrollTjeneste.lagreAksjonspunkterUtført(kontekst, behandling.getAktivtBehandlingSteg(), aksjonspunkt, dto.getBegrunnelse());
        }
        if (aksjonspunkt.erÅpentAksjonspunkt() && delresultat.skalAvbryteAksjonspunkt()) {
            behandlingskontrollTjeneste.lagreAksjonspunkterAvbrutt(kontekst, behandling.getAktivtBehandlingSteg(), List.of(aksjonspunkt));
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
        return gammelBegrunnelse != null && !Objects.equals(gammelBegrunnelse, nyBegrunnelse);
    }
}
