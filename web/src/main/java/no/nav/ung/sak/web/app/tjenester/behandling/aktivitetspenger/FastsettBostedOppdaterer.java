package no.nav.ung.sak.web.app.tjenester.behandling.aktivitetspenger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.historikk.HistorikkAktør;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.SkjermlenkeType;
import no.nav.ung.kodeverk.varsel.EtterlysningStatus;
import no.nav.ung.kodeverk.varsel.EtterlysningType;
import no.nav.ung.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.ung.sak.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.ung.sak.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.ung.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.ung.sak.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.ung.sak.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.bosatt.BostedsAvklaring;
import no.nav.ung.sak.behandlingslager.bosatt.BostedsGrunnlagRepository;
import no.nav.ung.sak.etterlysning.EtterlysningData;
import no.nav.ung.sak.etterlysning.EtterlysningTjeneste;
import no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår.FastsettBostedDto;
import no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår.FastsettBostedPeriodeDto;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@ApplicationScoped
@DtoTilServiceAdapter(dto = FastsettBostedDto.class, adapter = AksjonspunktOppdaterer.class)
public class FastsettBostedOppdaterer implements AksjonspunktOppdaterer<FastsettBostedDto> {

    private BehandlingRepository behandlingRepository;
    private HistorikkinnslagRepository historikkinnslagRepository;
    private BostedsGrunnlagRepository bostedsGrunnlagRepository;
    private EtterlysningTjeneste etterlysningTjeneste;

    FastsettBostedOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public FastsettBostedOppdaterer(BehandlingRepository behandlingRepository,
                                     HistorikkinnslagRepository historikkinnslagRepository,
                                     BostedsGrunnlagRepository bostedsGrunnlagRepository,
                                     EtterlysningTjeneste etterlysningTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.historikkinnslagRepository = historikkinnslagRepository;
        this.bostedsGrunnlagRepository = bostedsGrunnlagRepository;
        this.etterlysningTjeneste = etterlysningTjeneste;
    }

    @Override
    public OppdateringResultat oppdater(FastsettBostedDto dto, AksjonspunktOppdaterParameter param) {
        var behandling = behandlingRepository.hentBehandling(param.getBehandlingId());
        long behandlingId = behandling.getId();

        // Hent gjeldende etterlysninger for validering og oppslag av foreslått verdi
        Map<LocalDate, EtterlysningData> etterlysningPerFom = etterlysningTjeneste
            .hentGjeldendeEtterlysninger(behandlingId, behandling.getFagsakId(), EtterlysningType.UTTALELSE_BOSTED)
            .stream()
            .collect(Collectors.toMap(e -> e.periode().getFomDato(), e -> e));

        // Hent eksisterende foreslåtte avklaringer for oppslag av verdi ved foreslåttVurderingErGyldig=true
        Map<LocalDate, Boolean> foreslåtteAvklaringer = bostedsGrunnlagRepository
            .hentGrunnlagHvisEksisterer(behandlingId)
            .map(g -> g.getForeslåttHolder().getAvklaringer().stream()
                .collect(Collectors.toMap(BostedsAvklaring::getFomDato, BostedsAvklaring::erBosattITrondheim)))
            .orElse(Map.of());

        Map<LocalDate, Boolean> fastsatteAvklaringer = new LinkedHashMap<>();
        for (FastsettBostedPeriodeDto avklaring : dto.getAvklaringer()) {
            LocalDate fom = avklaring.getPeriode().getFom();

            // Valider at perioden har mottatt uttalelse
            var etterlysning = etterlysningPerFom.get(fom);
            if (etterlysning == null
                || etterlysning.status() != EtterlysningStatus.MOTTATT_SVAR
                || etterlysning.uttalelseData() == null
                || !etterlysning.uttalelseData().harUttalelse()) {
                throw new IllegalArgumentException(
                    "Periode " + fom + " har ikke mottatt uttalelse og kan ikke fastsettes via FASTSETT_BOSTED");
            }

            if (Boolean.TRUE.equals(avklaring.getForeslåttVurderingErGyldig())) {
                fastsatteAvklaringer.put(fom, foreslåtteAvklaringer.getOrDefault(fom, false));
            } else {
                if (avklaring.getNyVurdering() == null) {
                    throw new IllegalArgumentException(
                        "nyVurdering må oppgis når foreslåttVurderingErGyldig=false for periode " + fom);
                }
                fastsatteAvklaringer.putAll(BostedAvklaringUtil.splittAvklaring(fom, avklaring.getNyVurdering()));
            }
        }

        bostedsGrunnlagRepository.fastsettAvklaringerDirekte(behandlingId, fastsatteAvklaringer);

        var historikkinnslag = new Historikkinnslag.Builder()
            .medAktør(HistorikkAktør.LOKALKONTOR_SAKSBEHANDLER)
            .medFagsakId(behandling.getFagsakId())
            .medBehandlingId(behandlingId)
            .medTittel(SkjermlenkeType.BOSTEDSVILKÅR)
            .addLinje("Bostedsavklaring fastsatt etter brukerens uttalelse")
            .build();
        historikkinnslagRepository.lagre(historikkinnslag);

        var resultat = OppdateringResultat.nyttResultat();
        resultat.setSteg(no.nav.ung.kodeverk.behandling.BehandlingStegType.VURDER_BOSTED);
        resultat.rekjørSteg();
        return resultat;
    }
}
