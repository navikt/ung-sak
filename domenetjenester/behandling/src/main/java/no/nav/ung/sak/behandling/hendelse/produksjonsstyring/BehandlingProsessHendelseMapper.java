package no.nav.ung.sak.behandling.hendelse.produksjonsstyring;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.ung.kodeverk.Fagsystem;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.dokument.Brevkode;
import no.nav.ung.kodeverk.hendelse.EventHendelse;
import no.nav.ung.kodeverk.produksjonsstyring.UtvidetSøknadÅrsak;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandling.hendelse.produksjonsstyring.søknadsårsak.SøknadsårsakUtleder;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.ung.sak.behandlingslager.behandling.motattdokument.MottattDokument;
import no.nav.ung.sak.behandlingslager.behandling.motattdokument.MottatteDokumentRepository;
import no.nav.ung.sak.kontrakt.aksjonspunkt.AksjonspunktTilstandDto;
import no.nav.ung.sak.kontrakt.behandling.BehandlingProsessHendelse;
import no.nav.ung.sak.perioder.KravDokument;
import no.nav.ung.sak.perioder.SøktPeriode;
import no.nav.ung.sak.perioder.VurderSøknadsfristTjeneste;
import no.nav.ung.sak.perioder.VurdertSøktPeriode;
import no.nav.ung.sak.perioder.VurdertSøktPeriode.SøktPeriodeData;
import no.nav.k9.søknad.JsonUtils;
import no.nav.k9.søknad.Søknad;
import no.nav.k9.søknad.felles.Kildesystem;

@Dependent
public class BehandlingProsessHendelseMapper {

    private static final Logger logger = LoggerFactory.getLogger(BehandlingProsessHendelseMapper.class);
    private Instance<VurderSøknadsfristTjeneste<?>> søknadsfristTjenester;
    private Instance<SøknadsårsakUtleder> søknadsårsakUtledere;
    private MottatteDokumentRepository mottatteDokumentRepository;

    public BehandlingProsessHendelseMapper() {
    }

    @Inject
    public BehandlingProsessHendelseMapper(@Any Instance<VurderSøknadsfristTjeneste<?>> søknadsfristTjenester,
                                           @Any Instance<SøknadsårsakUtleder> søknadsårsakUtledere,
                                           MottatteDokumentRepository mottatteDokumentRepository) {
        this.søknadsfristTjenester = søknadsfristTjenester;
        this.søknadsårsakUtledere = søknadsårsakUtledere;
        this.mottatteDokumentRepository = mottatteDokumentRepository;
    }

    public BehandlingProsessHendelse getProduksjonstyringEventDto(LocalDateTime eventTid, EventHendelse eventHendelse, Behandling behandling, LocalDate vedtaksdato) {
        Map<String, String> aksjonspunktKoderMedStatusListe = new HashMap<>();
        var fagsak = behandling.getFagsak();
        behandling.getAksjonspunkter().forEach(aksjonspunkt -> aksjonspunktKoderMedStatusListe.put(aksjonspunkt.getAksjonspunktDefinisjon().getKode(), aksjonspunkt.getStatus().getKode()));

        final boolean nyeKrav = sjekkOmDetHarKommetNyeKrav(behandling);
        final boolean fraEndringsdialog = sjekkOmDetFinnesEndringFraEndringsdialog(behandling);

        List<UtvidetSøknadÅrsak> søknadsårsaker = utledSøknadÅrsaker(behandling);

        return BehandlingProsessHendelse.builder()
            .medEksternId(behandling.getUuid())
            .medEventTid(eventTid)
            .medVedtaksdato(vedtaksdato)
            .medFagsystem(Fagsystem.K9SAK)
            .medSaksnummer(behandling.getFagsak().getSaksnummer().getVerdi())
            .medAktørId(behandling.getAktørId().getId())
            .getBehandlingstidFrist(behandling.getBehandlingstidFrist())
            .medEventHendelse(eventHendelse)
            .medBehandlingStatus(behandling.getStatus().getKode())
            .medBehandlingSteg(behandling.getAktivtBehandlingSteg() == null ? null : behandling.getAktivtBehandlingSteg().getKode())
            .medYtelseTypeKode(behandling.getFagsakYtelseType().getKode())
            .medBehandlingTypeKode(behandling.getType().getKode())
            .medOpprettetBehandling(behandling.getOpprettetDato())
            .medEldsteDatoMedEndringFraSøker(finnEldsteMottattdato(behandling))
            .medBehandlingResultat(behandling.getBehandlingResultatType())
            .medAksjonspunktKoderMedStatusListe(aksjonspunktKoderMedStatusListe)
            .medAnsvarligSaksbehandlerForTotrinn(behandling.getAnsvarligSaksbehandler())
            .medBehandlendeEnhet(behandling.getBehandlendeEnhet())
            .medFagsakPeriode(fagsak.getPeriode().tilPeriode())
            .medPleietrengendeAktørId(fagsak.getPleietrengendeAktørId())
            .medRelatertPartAktørId(fagsak.getRelatertPersonAktørId())
            .medAnsvarligBeslutterForTotrinn(behandling.getAnsvarligBeslutter())
            .medAksjonspunktTilstander(lagAksjonspunkttilstander(behandling.getAksjonspunkter()))
            .medNyeKrav(nyeKrav)
            .medBehandlingsårsaker(behandling.getBehandlingÅrsaker().stream().map(årsak -> årsak.getBehandlingÅrsakType().getKode()).distinct().toList())
            .medSøknadårsaker(søknadsårsaker.stream().map(UtvidetSøknadÅrsak::getKode).distinct().toList())
            .medFraEndringsdialog(fraEndringsdialog)
            .build();
    }

    private List<UtvidetSøknadÅrsak> utledSøknadÅrsaker(Behandling behandling) {
        SøknadsårsakUtleder tjeneste = SøknadsårsakUtleder.finnTjeneste(søknadsårsakUtledere, behandling.getFagsakYtelseType());
        return tjeneste != null
            ? tjeneste.utledSøknadÅrsaker(behandling)
            : List.of();
    }

    public LocalDateTime finnEldsteMottattdato(Behandling behandling) {
        final var behandlingRef = BehandlingReferanse.fra(behandling);
        final var søknadsfristTjeneste = finnVurderSøknadsfristTjeneste(behandlingRef);
        if (søknadsfristTjeneste == null) {
            return null;
        }

        final Set<KravDokument> kravdokumenter;
        try {
            kravdokumenter = søknadsfristTjeneste.relevanteKravdokumentForBehandling(behandlingRef);
        } catch (Exception e) {
            logger.info("Innhenting av kravdokumenter feilet, " + e.getMessage());
            return null;
        }
        return kravdokumenter.stream()
            .filter(kd -> {
                final boolean manglerInnsendingstidspunkt = (kd.getInnsendingsTidspunkt() == null);
                if (manglerInnsendingstidspunkt) {
                    logger.info("Mangler innsendingstidspunkt for: " + behandlingRef.getFagsakYtelseType().getKode() + " " + behandlingRef.getSaksnummer().getVerdi());
                }
                return !manglerInnsendingstidspunkt;
            })
            .min(Comparator.comparing(KravDokument::getInnsendingsTidspunkt))
            .map(KravDokument::getInnsendingsTidspunkt)
            .orElse(null);
    }

    public BehandlingProsessHendelse getProduksjonstyringEventDto(EventHendelse eventHendelse, Behandling behandling) {
        return getProduksjonstyringEventDto(LocalDateTime.now(), eventHendelse, behandling, null);
    }


    private boolean sjekkOmDetFinnesEndringFraEndringsdialog(Behandling behandling) {
        if (behandling.getFagsakYtelseType() != FagsakYtelseType.PLEIEPENGER_SYKT_BARN) {
            return false;
        }

        final List<MottattDokument> dokumenter = mottatteDokumentRepository.hentMottatteDokumentForBehandling(behandling.getFagsakId(),
            behandling.getId(),
            List.of(Brevkode.PLEIEPENGER_BARN_SOKNAD),
            false);

        return dokumenter.stream().anyMatch(md -> {
            try {
                final Søknad søknad = JsonUtils.fromString(md.getPayload(), Søknad.class);
                return søknad.getKildesystem().map(ks -> ks == Kildesystem.ENDRINGSDIALOG).orElse(false);
            } catch (RuntimeException e) {
                logger.warn("Uventet feil ved parsing av søknad for oversendelse til k9-los. Setter endringsflagget til false på oppgaven.", e);
                return false;
            }
        });
    }

    public List<AksjonspunktTilstandDto> lagAksjonspunkttilstander(Collection<Aksjonspunkt> aksjonspunkter) {
        return aksjonspunkter.stream().map(it ->
            new AksjonspunktTilstandDto(
                it.getAksjonspunktDefinisjon().getKode(),
                it.getStatus(),
                it.getVenteårsak(),
                it.getAnsvarligSaksbehandler(),
                it.getFristTid(),
                it.getOpprettetTidspunkt(),
                it.getEndretTidspunkt()
            )
        ).toList();
    }

    private boolean sjekkOmDetHarKommetNyeKrav(Behandling behandling) {
        final var behandlingRef = BehandlingReferanse.fra(behandling);
        final var søknadsfristTjeneste = finnVurderSøknadsfristTjeneste(behandlingRef);
        if (søknadsfristTjeneste == null) {
            return false;
        }

        final Set<KravDokument> kravdokumenter = søknadsfristTjeneste.relevanteKravdokumentForBehandling(behandlingRef);
        if (kravdokumenter.isEmpty()) {
            return false;
        }

        final LocalDateTimeline<KravDokument> eldsteKravTidslinje = hentKravdokumenterMedEldsteKravFørst(behandlingRef, søknadsfristTjeneste);

        return eldsteKravTidslinje
            .stream()
            .anyMatch(it -> kravdokumenter.stream()
                .anyMatch(at -> at.getJournalpostId().equals(it.getValue().getJournalpostId())));
    }

    private LocalDateTimeline<KravDokument> hentKravdokumenterMedEldsteKravFørst(BehandlingReferanse behandlingRef,
                                                                                 VurderSøknadsfristTjeneste<SøktPeriodeData> søknadsfristTjeneste) {
        final Map<KravDokument, List<SøktPeriode<SøktPeriodeData>>> kravdokumenterMedPeriode = søknadsfristTjeneste.hentPerioderTilVurdering(behandlingRef);
        final var kravdokumenterMedEldsteFørst = kravdokumenterMedPeriode.keySet()
            .stream()
            .sorted(Comparator.comparing(KravDokument::getInnsendingsTidspunkt))
            .collect(Collectors.toCollection(LinkedHashSet::new));

        LocalDateTimeline<KravDokument> eldsteKravTidslinje = LocalDateTimeline.empty();
        for (KravDokument kravdokument : kravdokumenterMedEldsteFørst) {
            final List<SøktPeriode<SøktPeriodeData>> perioder = kravdokumenterMedPeriode.get(kravdokument);
            final var segments = perioder.stream()
                .map(it -> new LocalDateSegment<>(it.getPeriode().getFomDato(), it.getPeriode().getTomDato(), kravdokument))
                .collect(Collectors.toList());
            final var tidslinje = new LocalDateTimeline<>(segments, StandardCombinators::coalesceLeftHandSide);
            eldsteKravTidslinje = eldsteKravTidslinje.union(tidslinje, StandardCombinators::coalesceLeftHandSide);
        }
        return eldsteKravTidslinje;
    }

    private VurderSøknadsfristTjeneste<VurdertSøktPeriode.SøktPeriodeData> finnVurderSøknadsfristTjeneste(BehandlingReferanse ref) {
        final FagsakYtelseType ytelseType = ref.getFagsakYtelseType();

        @SuppressWarnings("unchecked") final var tjeneste = (VurderSøknadsfristTjeneste<VurdertSøktPeriode.SøktPeriodeData>) FagsakYtelseTypeRef.Lookup.find(søknadsfristTjenester, ytelseType).orElse(null);
        return tjeneste;
    }
}
