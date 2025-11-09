package no.nav.ung.sak.web.app.tjenester.klage.aksjonspunkt;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.SkjermlenkeType;
import no.nav.ung.kodeverk.klage.KlageAvvistÅrsak;
import no.nav.ung.kodeverk.klage.KlageVurdertAv;
import no.nav.ung.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.ung.sak.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.ung.sak.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.ung.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.ung.sak.behandlingslager.behandling.aksjonspunkt.AksjonspunktRepository;
import no.nav.ung.sak.behandlingslager.behandling.klage.KlageFormkravAdapter;
import no.nav.ung.sak.behandlingslager.behandling.klage.KlageFormkravEntitet;
import no.nav.ung.sak.behandlingslager.behandling.klage.KlageRepository;
import no.nav.ung.sak.behandlingslager.behandling.klage.KlageUtredningEntitet;
import no.nav.ung.sak.behandlingslager.behandling.part.PartEntitet;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.kontrakt.behandling.part.PartDto;
import no.nav.ung.sak.kontrakt.klage.KlageFormkravAksjonspunktDto;
import no.nav.ung.sak.kontrakt.klage.KlageresultatEndretEvent;
import no.nav.ung.sak.kontrakt.klage.PåklagdBehandlingDto;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Optional;

import static no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon.VURDERING_AV_FORMKRAV_KLAGE_VEDTAKSINSTANS;


@ApplicationScoped
@DtoTilServiceAdapter(dto = KlageFormkravAksjonspunktDto.class, adapter = AksjonspunktOppdaterer.class)
public class KlageFormkravOppdaterer implements AksjonspunktOppdaterer<KlageFormkravAksjonspunktDto> {

    private KlageHistorikkinnslagTjeneste klageHistorikkinnslag;
    private KlageRepository klageRepository;
    private AksjonspunktRepository aksjonspunktRepository;
    private BehandlingRepository behandlingRepository;
    private Event<KlageresultatEndretEvent> klageresultatEndretEvent;

    KlageFormkravOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public KlageFormkravOppdaterer(KlageHistorikkinnslagTjeneste klageHistorikkinnslag,
                                   BehandlingRepository behandlingRepository,
                                   KlageRepository klageRepository,
                                   AksjonspunktRepository aksjonspunktRepository,
                                   Event<KlageresultatEndretEvent> klageresultatEndretEvent) {
        this.klageHistorikkinnslag = klageHistorikkinnslag;
        this.klageRepository = klageRepository;
        this.behandlingRepository = behandlingRepository;
        this.aksjonspunktRepository = aksjonspunktRepository;
        this.klageresultatEndretEvent = klageresultatEndretEvent;
    }

    @Override
    public OppdateringResultat oppdater(KlageFormkravAksjonspunktDto dto, AksjonspunktOppdaterParameter param) {
        var aksjonspunktKode = dto.getKode();
        var klageBehandling = behandlingRepository.hentBehandling(param.getBehandlingId());
        var klageUtredning = klageRepository.hentKlageUtredning(klageBehandling.getId());

        var klageFormkrav = klageUtredning.getFormkrav().map(KlageFormkravEntitet::tilFormkrav);
        var apDefFormkrav = AksjonspunktDefinisjon.fraKode(aksjonspunktKode);

        klageHistorikkinnslag.opprettHistorikkinnslagFormkrav(klageBehandling, dto, klageFormkrav, klageUtredning, null, dto.getBegrunnelse());

        Optional<KlageAvvistÅrsak> optionalAvvistÅrsak = oppdaterFormkrav(dto, klageUtredning);
        klageRepository.lagre(klageUtredning);
        klageresultatEndretEvent.fire(new KlageresultatEndretEvent(klageBehandling.getId()));

        if (optionalAvvistÅrsak.isPresent()) {
            return OppdateringResultat.builder().medTotrinn().build();
        }

        Aksjonspunkt aksjonspunkt = klageBehandling.getAksjonspunktFor(apDefFormkrav);
        if (aksjonspunkt.isToTrinnsBehandling()) {
            aksjonspunktRepository.fjernToTrinnsBehandlingKreves(aksjonspunkt);
        }

        return OppdateringResultat.builder().build();
    }

    private Optional<KlageAvvistÅrsak> oppdaterFormkrav(KlageFormkravAksjonspunktDto dto, KlageUtredningEntitet utredning) {
        var påklagdBehandlingInfo = Optional.ofNullable(dto.getPåklagdBehandlingInfo());
        boolean gjelderVedtakPåYtelse = påklagdBehandlingInfo.isPresent();

        utredning.setKlagendePart(mapPartDto(dto.getValgtKlagePart()));
        utredning.setpåklagdBehandlingRef(påklagdBehandlingInfo.map(PåklagdBehandlingDto::getPåklagBehandlingUuid).orElse(null));
        utredning.setpåklagdBehandlingType(påklagdBehandlingInfo.map(p -> BehandlingType.fraKode(p.getPåklagBehandlingType())).orElse(null));

        KlageFormkravAdapter adapter = mapKlageFormkravAdapter(dto, gjelderVedtakPåYtelse);
        return utredning.setFormkrav(adapter);
    }

    private static KlageFormkravAdapter mapKlageFormkravAdapter(KlageFormkravAksjonspunktDto dto, boolean gjelderVedtakPåYtelse) {
        return new KlageFormkravAdapter(
            dto.erKlagerPart(),
            dto.erFristOverholdt(),
            dto.erKonkret(),
            dto.erSignert(),
            gjelderVedtakPåYtelse,
            dto.getBegrunnelse());
    }

    public static PartEntitet mapPartDto(PartDto dto) {
        if (dto == null) {
            return null;
        }

        var part = new PartEntitet();
        part.identifikasjon = dto.identifikasjon.id;
        part.identifikasjonType = dto.identifikasjon.type;
        part.rolleType = dto.rolleType;
        return part;
    }
}
