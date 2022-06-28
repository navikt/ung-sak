package no.nav.k9.sak.web.app.tjenester.behandling.aksjonspunkt;

import static java.util.stream.Collectors.toList;
import static no.nav.k9.sak.web.app.tjenester.behandling.aksjonspunkt.AksjonspunktApplikasjonFeil.FACTORY;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.inject.Inject;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.SkjermlenkeType;
import no.nav.k9.kodeverk.historikk.HistorikkinnslagType;
import no.nav.k9.sak.behandling.Skjæringstidspunkt;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.k9.sak.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.k9.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktProsessResultat;
import no.nav.k9.sak.behandling.aksjonspunkt.Overstyringshåndterer;
import no.nav.k9.sak.behandling.prosessering.BehandlingsprosessApplikasjonTjeneste;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.k9.sak.behandlingslager.behandling.aksjonspunkt.AksjonspunktRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatBuilder;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.historikk.HistorikkTjenesteAdapter;
import no.nav.k9.sak.kontrakt.aksjonspunkt.AksjonspunktKode;
import no.nav.k9.sak.kontrakt.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.k9.sak.kontrakt.aksjonspunkt.BekreftetOgOverstyrteAksjonspunkterDto;
import no.nav.k9.sak.kontrakt.aksjonspunkt.OverstyringAksjonspunkt;
import no.nav.k9.sak.kontrakt.aksjonspunkt.OverstyringAksjonspunktDto;
import no.nav.k9.sak.kontrakt.vedtak.FatterVedtakAksjonspunktDto;
import no.nav.k9.sak.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.k9.sikkerhet.context.SubjectHandler;

@Dependent
public class AksjonspunktApplikasjonTjeneste {

    private static final Set<AksjonspunktDefinisjon> EKSTRARESULTAT_AP_SPERR_TOTRINN = Set.of(
        AksjonspunktDefinisjon.FORESLÅ_VEDTAK,
        AksjonspunktDefinisjon.FATTER_VEDTAK,
        AksjonspunktDefinisjon.VEDTAK_UTEN_TOTRINNSKONTROLL,
        AksjonspunktDefinisjon.FORESLÅ_VEDTAK_MANUELT);

    private BehandlingRepository behandlingRepository;

    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;

    private AksjonspunktRepository aksjonspunktRepository;

    private HistorikkTjenesteAdapter historikkTjenesteAdapter;

    private BehandlingsprosessApplikasjonTjeneste behandlingsprosessApplikasjonTjeneste;


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
                                           HistorikkTjenesteAdapter historikkTjenesteAdapter) {

        this.aksjonspunktRepository = aksjonspunktRepository;
        this.behandlingsprosessApplikasjonTjeneste = behandlingsprosessApplikasjonTjeneste;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.historikkTjenesteAdapter = historikkTjenesteAdapter;
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.vilkårResultatRepository = repositoryProvider.getVilkårResultatRepository();
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;

    }

    public void bekreftAksjonspunkter(Collection<BekreftetAksjonspunktDto> bekreftedeAksjonspunktDtoer, Long behandlingId, BehandlingskontrollKontekst kontekst) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);

        AksjonspunktProsessResultat aksjonspunktProsessResultat = spolTilbakeOgBekreft(bekreftedeAksjonspunktDtoer, behandling, kontekst);

        behandlingRepository.lagre(behandling, kontekst.getSkriveLås());

        if (behandling.isBehandlingPåVent()) {
            // Skal ikke fortsette behandling dersom behandling ble satt på vent
            return;
        }
        fortsettBehandlingen(behandling, kontekst, aksjonspunktProsessResultat);// skal ikke reinnhente her, avgjøres i steg?
    }

    private AksjonspunktProsessResultat spolTilbakeOgBekreft(Collection<BekreftetAksjonspunktDto> bekreftedeAksjonspunktDtoer,
                                                             Behandling behandling,
                                                             BehandlingskontrollKontekst kontekst) {
        setAnsvarligSaksbehandler(bekreftedeAksjonspunktDtoer, behandling);
        spoolTilbakeTilTidligsteAksjonspunkt(behandling, bekreftedeAksjonspunktDtoer, kontekst);

        // TODO: Fjern bruk av skjæringstidspunktTjeneste her
        Skjæringstidspunkt skjæringstidspunkter = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandling.getId());

        AksjonspunktProsessResultat aksjonspunktProsessResultat = bekreftAksjonspunkter(kontekst, bekreftedeAksjonspunktDtoer, behandling, skjæringstidspunkter);
        historikkTjenesteAdapter.opprettHistorikkInnslag(behandling.getId(), HistorikkinnslagType.FAKTA_ENDRET);
        return aksjonspunktProsessResultat;
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


    /**
     * Overstyrer aksjonspunkter.
     * <p>
     * Metoden kalles ved kall til overstyr-endepunktet som krever egne rettigheter.
     * <p>
     * Dto som sendes inn inneholder to lister. Den ene listen inneholder overstyrte aksjonspunkter, og den andre inneholder bekreftede aksjonspunkter som ikke krever overstyr-rettighet.
     * I fakta om beregning kan saksbehandler velge å overstyre enkelte perioder og samtidig ha aksjonspunkt for bekreftelse av fakta i andre.
     * I en slik sitausjon er det nødvendig at saksbehandler får lov til å både overstyre grunnlag og samtidig bekrefte fakta i samme kall.
     *
     * @param aksjonspunkterDto AksjonspunktDto for overstyring, inneholder overstyrte aksjonspunkte og eventuelt bekreftede
     * @param behandlingId      BehandlingId
     */
    public void overstyrAksjonspunkter(BekreftetOgOverstyrteAksjonspunkterDto aksjonspunkterDto, Long behandlingId) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        BehandlingskontrollKontekst kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(behandlingId);
        AksjonspunktProsessResultat prosessResultat = overstyrVilkårEllerBeregning(aksjonspunkterDto.getOverstyrteAksjonspunktDtoer(), behandling, kontekst);

        aksjonspunkterDto.getOverstyrteAksjonspunktDtoer().forEach(dto ->
            behandling.getAksjonspunktFor(dto.getKode()).ifPresent(aksjonspunkt ->
                aksjonspunkt.setAnsvarligSaksbehandler(getCurrentUserId())
            )
        );

        if (aksjonspunkterDto.getBekreftedeAksjonspunktDtoer().size() > 0) {
            AksjonspunktProsessResultat overhoppForBekreft = spolTilbakeOgBekreft(aksjonspunkterDto.getBekreftedeAksjonspunktDtoer(), behandling, kontekst);
            overhoppForBekreft.getOppdatereResultater().forEach(prosessResultat::leggTil);
        }

        if (behandling.isBehandlingPåVent()) {
            // Skal ikke fortsette behandling dersom behandling ble satt på vent
            return;
        }
        fortsettBehandlingen(behandling, kontekst, prosessResultat);// skal ikke reinnhente her, avgjøres i steg?
    }

    private void fortsettBehandlingen(Behandling behandling, BehandlingskontrollKontekst kontekst, AksjonspunktProsessResultat aksjonspunktProsessResultat) {
        if (aksjonspunktProsessResultat.skalRekjøreSteg()) {
            var behandlingSteg = aksjonspunktProsessResultat.stegSomSkalRekjøres();
            // Denne bør tilbakeføre til inngangen av steget
            behandlingskontrollTjeneste.behandlingTilbakeføringTilTidligereBehandlingSteg(kontekst, behandlingSteg);
        }
        behandlingsprosessApplikasjonTjeneste.asynkKjørProsess(behandling);
    }

    @SuppressWarnings("unchecked")
    private AksjonspunktProsessResultat overstyrVilkårEllerBeregning(Collection<OverstyringAksjonspunktDto> overstyrteAksjonspunkter,
                                                                     Behandling behandling, BehandlingskontrollKontekst kontekst) {
        AksjonspunktProsessResultat aksjonspunktProsessResultat = AksjonspunktProsessResultat.tomtResultat();

        // oppdater for overstyring
        overstyrteAksjonspunkter.forEach(dto -> {
            @SuppressWarnings("rawtypes")
            Overstyringshåndterer overstyringshåndterer = finnOverstyringshåndterer(dto);
            var aksjonspunktDefinisjon = overstyringshåndterer.aksjonspunktForInstans();
            opprettAksjonspunktForOverstyring(kontekst, behandling, aksjonspunktDefinisjon);
            OppdateringResultat oppdateringResultat = overstyringshåndterer.håndterOverstyring(dto, behandling, kontekst);
            aksjonspunktProsessResultat.leggTil(oppdateringResultat);

            settToTrinnPåOverstyrtAksjonspunktHvisEndring(behandling, dto, oppdateringResultat.kreverTotrinnsKontroll());
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

        historikkTjenesteAdapter.opprettHistorikkInnslag(behandling.getId(), HistorikkinnslagType.OVERSTYRT);

        boolean totrinn = aksjonspunktProsessResultat.finnTotrinn();
        aksjonspunktProsessResultat.finnEkstraAksjonspunktResultat().forEach(res -> håndterEkstraAksjonspunktResultat(kontekst, behandling, totrinn, res.getElement1(), res.getElement2()));

        return aksjonspunktProsessResultat;
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

    private AksjonspunktProsessResultat bekreftAksjonspunkter(BehandlingskontrollKontekst kontekst,
                                                              Collection<BekreftetAksjonspunktDto> bekreftedeAksjonspunktDtoer,
                                                              Behandling behandling,
                                                              Skjæringstidspunkt skjæringstidspunkter) {

        AksjonspunktProsessResultat aksjonspunktProsessResultat = AksjonspunktProsessResultat.tomtResultat();

        final var vilkåreneOptional = vilkårResultatRepository.hentHvisEksisterer(behandling.getId());
        VilkårResultatBuilder vilkårBuilder = vilkåreneOptional.map(Vilkårene::builderFraEksisterende).orElse(Vilkårene.builder());

        bekreftedeAksjonspunktDtoer
            .forEach(dto -> bekreftAksjonspunkt(kontekst, behandling, skjæringstidspunkter, vilkårBuilder, aksjonspunktProsessResultat, dto));

        Vilkårene vilkårene = vilkårBuilder.build();
        behandlingRepository.lagre(behandling, kontekst.getSkriveLås());
        vilkårResultatRepository.lagre(kontekst.getBehandlingId(), vilkårene);

        boolean totrinn = aksjonspunktProsessResultat.finnTotrinn();
        aksjonspunktProsessResultat.finnEkstraAksjonspunktResultat().forEach(res -> håndterEkstraAksjonspunktResultat(kontekst, behandling, totrinn, res.getElement1(), res.getElement2()));

        return aksjonspunktProsessResultat;
    }

    private void bekreftAksjonspunkt(BehandlingskontrollKontekst kontekst, Behandling behandling,
                                     Skjæringstidspunkt skjæringstidspunkter,
                                     VilkårResultatBuilder vilkårBuilder,
                                     AksjonspunktProsessResultat aksjonspunktProsessResultat,
                                     BekreftetAksjonspunktDto dto) {
        // Endringskontroll for aksjonspunkt
        Aksjonspunkt aksjonspunkt = behandling.getAksjonspunktFor(dto.getKode())
            .orElseThrow(() -> new IllegalStateException("Utvikler-feil: Har ikke aksjonspunkt av type: " + dto.getKode()));

        aksjonspunkt.setAnsvarligSaksbehandler(getCurrentUserId());

        AksjonspunktOppdaterer<BekreftetAksjonspunktDto> oppdaterer = finnAksjonspunktOppdaterer(dto.getClass(), dto.getKode());
        AksjonspunktOppdaterParameter param = new AksjonspunktOppdaterParameter(behandling, Optional.of(aksjonspunkt), skjæringstidspunkter, vilkårBuilder, dto);
        OppdateringResultat delresultat = oppdaterer.oppdater(dto, param);

        if (aksjonspunkt.tilbakehoppVedGjenopptakelse()) {
            delresultat.rekjørSteg();
            delresultat.setSteg(aksjonspunkt.getAksjonspunktDefinisjon().getBehandlingSteg());
        }

        aksjonspunktProsessResultat.leggTil(delresultat);

        settToTrinnHvisRevurderingOgEndring(aksjonspunkt, delresultat.kreverTotrinnsKontroll());

        if (!aksjonspunkt.erAvbrutt()) {
            behandlingskontrollTjeneste.lagreAksjonspunkterUtført(kontekst, behandling.getAktivtBehandlingSteg(), aksjonspunkt, dto.getBegrunnelse());
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
                                                               boolean resultatKreverTotrinn) {
        AksjonspunktDefinisjon aksjonspunktDefinisjon = AksjonspunktDefinisjon.fraKode(dto.getKode());
        if (behandling.harAksjonspunktMedType(aksjonspunktDefinisjon)) {
            Aksjonspunkt aksjonspunkt = behandling.getAksjonspunktFor(aksjonspunktDefinisjon);
            settToTrinnHvisRevurderingOgEndring(aksjonspunkt, resultatKreverTotrinn);
        }
    }

    private void settToTrinnHvisRevurderingOgEndring(Aksjonspunkt aksjonspunkt,
                                                     boolean resultatKreverTotrinn) {
        if (resultatKreverTotrinn) {
            aksjonspunktRepository.setToTrinnsBehandlingKreves(aksjonspunkt);
        }
    }

    private boolean aksjonspunktStøtterTotrinn(Aksjonspunkt aksjonspunkt) {
        SkjermlenkeType aksjonspunktSkjermlenkeType = aksjonspunkt.getAksjonspunktDefinisjon().getSkjermlenkeType();
        return !EKSTRARESULTAT_AP_SPERR_TOTRINN.contains(aksjonspunkt.getAksjonspunktDefinisjon())
            // Aksjonspunkter må ha SkjermlenkeType for å støtte totrinnskontroll
            && aksjonspunktSkjermlenkeType != null
            && !SkjermlenkeType.UDEFINERT.equals(aksjonspunktSkjermlenkeType);
    }

    private boolean begrunnelseErEndret(String gammelBegrunnelse, String nyBegrunnelse) {
        return gammelBegrunnelse != null && !Objects.equals(gammelBegrunnelse, nyBegrunnelse);
    }
}
