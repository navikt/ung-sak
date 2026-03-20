package no.nav.ung.sak.web.app.tjenester.klage.aksjonspunkt;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.SkjermlenkeType;
import no.nav.ung.kodeverk.historikk.HistorikkAktør;
import no.nav.ung.kodeverk.klage.KlageVurderingOmgjør;
import no.nav.ung.kodeverk.klage.KlageVurderingType;
import no.nav.ung.kodeverk.klage.KlageVurdertAv;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.ung.sak.behandlingslager.behandling.historikk.HistorikkinnslagLinjeBuilder;
import no.nav.ung.sak.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.ung.sak.behandlingslager.behandling.klage.KlageFormkravAdapter;
import no.nav.ung.sak.behandlingslager.behandling.klage.KlageUtredningEntitet;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.ung.sak.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.ung.sak.kontrakt.klage.KlageFormkravAksjonspunktDto;
import no.nav.ung.sak.kontrakt.klage.KlageVurderingResultatAksjonspunktDto;

import java.time.LocalDate;
import java.util.*;

import static no.nav.ung.sak.behandlingslager.behandling.historikk.HistorikkinnslagLinjeBuilder.format;

@ApplicationScoped
public class KlageHistorikkinnslagTjeneste {

    private static final String IKKE_PÅKLAGD_ET_VEDTAK_HISTORIKKINNSLAG_TEKST = "Ikke påklagd et vedtak";
    public static final String PÅKLAGD_BEHANDLING = "Påklagd behandling";

    private HistorikkinnslagRepository historikkinnslagRepository;
    private BehandlingRepository behandlingRepository;
    private BehandlingVedtakRepository behandlingVedtakRepository;

    KlageHistorikkinnslagTjeneste() {
        // for CDI proxy
    }

    @Inject
    public KlageHistorikkinnslagTjeneste(HistorikkinnslagRepository historikkinnslagRepository,
                                         BehandlingRepository behandlingRepository,
                                         BehandlingVedtakRepository behandlingVedtakRepository) {
        this.historikkinnslagRepository = historikkinnslagRepository;
        this.behandlingRepository = behandlingRepository;
        this.behandlingVedtakRepository = behandlingVedtakRepository;
    }

    public void opprettHistorikkinnslagFormkrav(Behandling klageBehandling,
                                                KlageFormkravAksjonspunktDto formkravDto,
                                                Optional<KlageFormkravAdapter> klageFormkrav,
                                                KlageUtredningEntitet klageutredning,
                                                LocalDate mottattDato,
                                                String begrunnelse) {
        var linjer = klageFormkrav.map(klageFormkravEntitet -> lagHistorikkLinjer(klageFormkravEntitet, mottattDato, formkravDto, klageutredning))
            .orElseGet(() -> lagHistorikkLinjer(mottattDato, formkravDto));
        var historikkinnslag = new Historikkinnslag.Builder().medTittel(SkjermlenkeType.FORMKRAV_KLAGE_VEDTAKSINSTANS)
            .medAktør(HistorikkAktør.SAKSBEHANDLER)
            .medFagsakId(klageBehandling.getFagsakId())
            .medBehandlingId(klageBehandling.getId())
            .medLinjer(linjer)
            .addLinje(begrunnelse)
            .build();
        historikkinnslagRepository.lagre(historikkinnslag);
    }

    public void opprettHistorikkinnslagVurdering(Behandling behandling,
                                                 AksjonspunktDefinisjon aksjonspunktDefinisjon,
                                                 KlageVurderingResultatAksjonspunktDto dto,
                                                 String begrunnelse) {
        var klageVurdering = dto.getKlageVurdering();
        var klageVurderingOmgjør = dto.getKlageVurderingOmgjoer() != null ? dto.getKlageVurderingOmgjoer() : null;
        var erNfpAksjonspunkt = AksjonspunktDefinisjon.MANUELL_VURDERING_AV_KLAGE_VEDTAKSINSTANS.equals(aksjonspunktDefinisjon);

        var resultat = historikkResultatForKlageVurdering(klageVurdering, KlageVurdertAv.VEDTAKSINSTANS, klageVurderingOmgjør);
        var linjer = new ArrayList<HistorikkinnslagLinjeBuilder>();
        if (erNfpAksjonspunkt && resultat != null) {
            linjer.add(linjeBuilder().til("Resultat", resultat));
        }
        var årsak = dto.getKlageMedholdArsak();
        if (årsak != null) {
            linjer.add(linjeBuilder().til("Årsak til omgjøring", årsak.getNavn()));
        }

        var skjermlenkeType = SkjermlenkeType.KLAGE_BEH_VEDTAKSINSTANS;
        var historikkinnslag = new Historikkinnslag.Builder().medFagsakId(behandling.getFagsakId())
            .medBehandlingId(behandling.getId())
            .medAktør(HistorikkAktør.SAKSBEHANDLER)
            .medTittel(skjermlenkeType)
            .medLinjer(linjer)
            .addLinje(begrunnelse)
            .build();
        historikkinnslagRepository.lagre(historikkinnslag);
    }

    private String hentPåklagdBehandlingTekst(Long behandlingId) {
        if (behandlingId == null) {
            return IKKE_PÅKLAGD_ET_VEDTAK_HISTORIKKINNSLAG_TEKST;
        }
        var påKlagdBehandling = behandlingRepository.hentBehandling(behandlingId);
        var vedtaksDatoPåklagdBehandling = behandlingVedtakRepository.hentBehandlingVedtakForBehandlingId(påKlagdBehandling.getId())
            .map(BehandlingVedtak::getVedtaksdato);
        return påKlagdBehandling.getType().getNavn() + " " + vedtaksDatoPåklagdBehandling.map(dato -> format(dato)).orElse("");
    }

    private String hentPåklagdBehandlingTekst(UUID behandlingUuid) {
        return hentPåklagdBehandlingTekst(behandlingUuid == null ? null : behandlingRepository.hentBehandling(behandlingUuid).getId());
    }

    private String hentPåKlagdEksternBehandlingTekst(UUID påKlagdEksternBehandlingUuid, String behandlingType, LocalDate vedtakDato) {
        if (påKlagdEksternBehandlingUuid == null) {
            return IKKE_PÅKLAGD_ET_VEDTAK_HISTORIKKINNSLAG_TEKST;
        }
        return BehandlingType.fraKode(behandlingType).getNavn() + " " + (vedtakDato != null ? format(vedtakDato) : "");
    }

    private List<HistorikkinnslagLinjeBuilder> lagHistorikkLinjer(LocalDate mottattDato, KlageFormkravAksjonspunktDto formkravDto) {
        var behandlingUuid = formkravDto.hentPåklagdBehandlingUuid();
        var påKlagdBehandlingTekst = !formkravDto.erTilbakekreving() ? hentPåklagdBehandlingTekst(behandlingUuid) :
            hentPåKlagdEksternBehandlingTekst(
                formkravDto.hentPåklagdBehandlingUuid(),
                formkravDto.getPåklagdBehandlingInfo().getPåklagBehandlingType(),
                formkravDto.getPåklagdBehandlingInfo().getPåklagBehandlingVedtakDato()
            );

        var linjer = new ArrayList<>(List.of(linjeBuilder().til(PÅKLAGD_BEHANDLING, påKlagdBehandlingTekst),
            linjeBuilder().til("Er klager part", formkravDto.erKlagerPart()),
            linjeBuilder().til("Er klagefrist overholdt", formkravDto.erFristOverholdt()),
            linjeBuilder().til("Er klagen signert", formkravDto.erSignert()),
            linjeBuilder().til("Er klagen konkret", formkravDto.erKonkret())));

        return linjer;
    }

    private static HistorikkinnslagLinjeBuilder linjeBuilder() {
        return new HistorikkinnslagLinjeBuilder();
    }

    private List<HistorikkinnslagLinjeBuilder> lagHistorikkLinjer(KlageFormkravAdapter klageFormkrav,
                                                                  LocalDate mottattDato,
                                                                  KlageFormkravAksjonspunktDto formkravDto,
                                                                  KlageUtredningEntitet klageUtredning) {
        var linjer = new ArrayList<HistorikkinnslagLinjeBuilder>();
        if (erPåklagdVedtakOppdatert(klageUtredning, formkravDto)) {
            var lagretPåklagdBehandlingId = klageUtredning.getpåklagdBehandlingRef().orElse(null);
            var lagretPåklagdBehandlingType = klageUtredning.getpåklagdBehandlingType().orElse(null);
             if (lagretPåklagdBehandlingId != null) {
                linjer.add(
                    lagLinjeHvisForrigePåklagdBehandlingFinnes(formkravDto, klageUtredning, lagretPåklagdBehandlingId, lagretPåklagdBehandlingType)
                );
            } else {
                var tilVerdi = formkravDto.erTilbakekreving() ?
                    hentPåKlagdEksternBehandlingTekst(
                        formkravDto.getPåklagdBehandlingInfo().getPåklagBehandlingUuid(),
                        formkravDto.getPåklagdBehandlingInfo().getPåklagBehandlingType(),
                        formkravDto.getPåklagdBehandlingInfo().getPåklagBehandlingVedtakDato()
                    ) : hentPåklagdBehandlingTekst(formkravDto.hentPåklagdBehandlingUuid());

                linjer.add(new HistorikkinnslagLinjeBuilder().til(PÅKLAGD_BEHANDLING, tilVerdi));
            }
        }
        if (klageFormkrav.isErKlagerPart() != formkravDto.erKlagerPart()) {
            linjer.add(
                new HistorikkinnslagLinjeBuilder().fraTil("Er klager part", klageFormkrav.isErKlagerPart(), formkravDto.erKlagerPart()));
        }
        if (klageFormkrav.isFristOverholdt() != formkravDto.erFristOverholdt()) {
            linjer.add(new HistorikkinnslagLinjeBuilder().fraTil("Er klagefrist overholdt", klageFormkrav.isFristOverholdt(),
                formkravDto.erFristOverholdt()));
        }
        if (klageFormkrav.isErSignert() != formkravDto.erSignert()) {
            linjer.add(
                new HistorikkinnslagLinjeBuilder().fraTil("Er klagen signert", klageFormkrav.isErSignert(), formkravDto.erSignert()));

        }
        if (klageFormkrav.isErKonkret() != formkravDto.erKonkret()) {
            linjer.add(
                new HistorikkinnslagLinjeBuilder().fraTil("Er klagen konkret", klageFormkrav.isErKonkret(), formkravDto.erKonkret()));
        }
        return linjer;
    }

    private HistorikkinnslagLinjeBuilder lagLinjeHvisForrigePåklagdBehandlingFinnes(
        KlageFormkravAksjonspunktDto formkravDto,
        KlageUtredningEntitet klageUtredning,
        UUID lagretPåklagdBehandlingUuid,
        BehandlingType lagretPåklagdBehandlingType) {
        if (formkravDto.erTilbakekreving() || lagretPåklagdBehandlingType == BehandlingType.TILBAKEKREVING) {
            String fraVerdi;
            if (lagretPåklagdBehandlingType != BehandlingType.TILBAKEKREVING) {
                fraVerdi = hentPåklagdBehandlingTekst(lagretPåklagdBehandlingUuid);
            } else {
                fraVerdi = hentPåKlagdEksternBehandlingTekst(lagretPåklagdBehandlingUuid, lagretPåklagdBehandlingType.getKode(), null);
            }
            var tilVerdi = hentPåKlagdEksternBehandlingTekst(
                formkravDto.hentPåklagdBehandlingUuid(),
                formkravDto.getPåklagdBehandlingInfo().getPåklagBehandlingType(),
                formkravDto.getPåklagdBehandlingInfo().getPåklagBehandlingVedtakDato()
            );
            return HistorikkinnslagLinjeBuilder.fraTilEquals(PÅKLAGD_BEHANDLING, fraVerdi, tilVerdi);
        } else {
            var fraVerdi = hentPåklagdBehandlingTekst(klageUtredning.getpåklagdBehandlingRef().orElse(null));
            var tilVerdi = hentPåklagdBehandlingTekst(formkravDto.hentPåklagdBehandlingUuid());
            return HistorikkinnslagLinjeBuilder.fraTilEquals(PÅKLAGD_BEHANDLING, fraVerdi, tilVerdi);
        }
    }

    private boolean erPåklagdVedtakOppdatert(KlageUtredningEntitet klageUtredning, KlageFormkravAksjonspunktDto formkravDto) {
        var lagretBehandlingUuid = klageUtredning.getpåklagdBehandlingRef().orElse(null);
        var påKlagdBehandlingUuid = formkravDto.hentPåklagdBehandlingUuid();

        return !Objects.equals(lagretBehandlingUuid, påKlagdBehandlingUuid);
    }

    public static String historikkResultatForKlageVurdering(KlageVurderingType vurdering, KlageVurdertAv vurdertAv, KlageVurderingOmgjør klageVurderingOmgjør) {
        if (KlageVurderingType.AVVIS_KLAGE.equals(vurdering)) {
            return "Klagen er avvist";
        }
        if (KlageVurderingType.MEDHOLD_I_KLAGE.equals(vurdering)) {
            if (KlageVurderingOmgjør.DELVIS_MEDHOLD_I_KLAGE.equals(klageVurderingOmgjør)) {
                return "Vedtaket er delvis omgjort";
            }
            if (KlageVurderingOmgjør.UGUNST_MEDHOLD_I_KLAGE.equals(klageVurderingOmgjør)) {
                return "Vedtaket er omgjort til ugunst";
            }
            return "Vedtaket er omgjort";
        }
        if (KlageVurderingType.OPPHEVE_YTELSESVEDTAK.equals(vurdering)) {
            return "Vedtaket er opphevet";
        }
        if (KlageVurderingType.HJEMSENDE_UTEN_Å_OPPHEVE.equals(vurdering)) {
            return "Behandling er hjemsendt";
        }
        if (KlageVurderingType.STADFESTE_YTELSESVEDTAK.equals(vurdering)) {
            if (KlageVurdertAv.VEDTAKSINSTANS.equals(vurdertAv)) {
                return "Vedtaket er opprettholdt";
            }
            return "Vedtaket er stadfestet";
        }
        return null;
    }

}
