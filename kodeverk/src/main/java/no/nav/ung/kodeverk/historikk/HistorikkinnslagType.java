package no.nav.ung.kodeverk.historikk;

import com.fasterxml.jackson.annotation.JsonValue;
import no.nav.ung.kodeverk.api.Kodeverdi;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public enum HistorikkinnslagType implements Kodeverdi {

    // Mal Type 1
    BREV_SENT("BREV_SENT", "Brev sendt", HistorikkinnslagMal.MAL_TYPE_1),
    MERKNAD_NY("MERKNAD_NY", "Ny markering", HistorikkinnslagMal.MAL_TYPE_1),
    MERKNAD_FJERNET("MERKNAD_FJERNET", "Markering fjernet", HistorikkinnslagMal.MAL_TYPE_1),
    BREV_BESTILT("BREV_BESTILT", "Brev bestilt", HistorikkinnslagMal.MAL_TYPE_1),
    BEH_STARTET_PÅ_NYTT("BEH_STARTET_PÅ_NYTT", "Behandling startet på nytt", HistorikkinnslagMal.MAL_TYPE_1),
    BEH_STARTET("BEH_STARTET", "Behandling startet", HistorikkinnslagMal.MAL_TYPE_1),
    BEH_OPPDATERT_NYE_OPPL("BEH_OPPDATERT_NYE_OPPL", "Behandlingen oppdatert med nye opplysninger", HistorikkinnslagMal.MAL_TYPE_1),
    BEH_MAN_GJEN("BEH_MAN_GJEN", "Gjenoppta behandling", HistorikkinnslagMal.MAL_TYPE_1),
    BEH_GJEN("BEH_GJEN", "Behandling gjenopptatt", HistorikkinnslagMal.MAL_TYPE_1),
    BEH_AVBRUTT_VUR("BEH_AVBRUTT_VUR", "Vurdering før vedtak", HistorikkinnslagMal.MAL_TYPE_1),
    BEH_AVBRUTT_OVERLAPP("BEH_AVBRUTT_OVERLAPP", "Vurdering av overlapp andre ytelser før vedtak", HistorikkinnslagMal.MAL_TYPE_1),
    VRS_REV_IKKE_SNDT("VRS_REV_IKKE_SNDT", "Varsel om revurdering ikke sendt", HistorikkinnslagMal.MAL_TYPE_1),
    VEDLEGG_MOTTATT("VEDLEGG_MOTTATT", "Vedlegg mottatt", HistorikkinnslagMal.MAL_TYPE_1),
    SPOLT_TILBAKE("SPOLT_TILBAKE", "Behandlingen er flyttet", HistorikkinnslagMal.MAL_TYPE_1),
    REVURD_OPPR("REVURD_OPPR", "Revurdering opprettet", HistorikkinnslagMal.MAL_TYPE_1),
    NYE_REGOPPLYSNINGER("NYE_REGOPPLYSNINGER", "Nye registeropplysninger", HistorikkinnslagMal.MAL_TYPE_1),
    MIGRERT_FRA_INFOTRYGD_FJERNET("MIGRERT_FRA_INFOTRYGD_FJERNET", "Behandling gjelder ikke lenger flytting av sak fra Infotrygd", HistorikkinnslagMal.MAL_TYPE_1),
    MIGRERT_FRA_INFOTRYGD("MIGRERT_FRA_INFOTRYGD", "Behandling gjelder flytting av sak fra Infotrygd", HistorikkinnslagMal.MAL_TYPE_1),
    MANGELFULL_SØKNAD("MANGELFULL_SØKNAD", "Mangelfull søknad", HistorikkinnslagMal.MAL_TYPE_1),
    KØET_BEH_GJEN("KØET_BEH_GJEN", "Køet behandling er gjenopptatt", HistorikkinnslagMal.MAL_TYPE_1),
    SØKNADSFRIST_VURDERT("SØKNADSFRIST_VURDERT", "Søknadsfrist manuelt vurdert:", HistorikkinnslagMal.MAL_TYPE_1),
    FORTSETT_UTEN_Å_VENTE_PÅ_TILBAKEKREVING("FORTSETT_UTEN_Å_VENTE_PÅ_TILBAKEKREVING", "Fortsetter behandlingen uten å behandle tilbakekrevingsaken først", HistorikkinnslagMal.MAL_TYPE_1),

    // Mal Type 2
    VEDTAK_FATTET("VEDTAK_FATTET", "Vedtak fattet", HistorikkinnslagMal.MAL_TYPE_2),
    UENDRET_UTFALL("UENDRET_UTFALL", "Uendret utfall", HistorikkinnslagMal.MAL_TYPE_2),
    TILBAKEKREVING_VIDEREBEHANDLING("TILBAKEKR_VIDEREBEHANDLING", "Metode for å håndtere tilbakekreving av feilutbetaling er valgt.", HistorikkinnslagMal.MAL_TYPE_2),
    REGISTRER_OM_VERGE("REGISTRER_OM_VERGE", "Registrering av opplysninger om verge/fullmektig/søker under 18 år", HistorikkinnslagMal.MAL_TYPE_2),
    FORSLAG_VEDTAK_UTEN_TOTRINN("FORSLAG_VEDTAK_UTEN_TOTRINN", "Vedtak foreslått", HistorikkinnslagMal.MAL_TYPE_2),
    FORSLAG_VEDTAK("FORSLAG_VEDTAK", "Vedtak foreslått og sendt til beslutter", HistorikkinnslagMal.MAL_TYPE_2),
    VIRKNINGSDATO_UTTAK_NYE_REGLER("VIRKNINGSDATO_UTTAK_NYE_REGLER", "Nye regler for uttak.", HistorikkinnslagMal.MAL_TYPE_2),

    // Mal Type 3
    SAK_RETUR("SAK_RETUR", "Sak retur", HistorikkinnslagMal.MAL_TYPE_3),
    SAK_GODKJENT("SAK_GODKJENT", "Sak godkjent", HistorikkinnslagMal.MAL_TYPE_3),

    // Mal Type 4
    FJERNET_VERGE("FJERNET_VERGE", "Opplysninger om verge/fullmektig fjernet", HistorikkinnslagMal.MAL_TYPE_4),
    IVERKSETTELSE_VENT("IVERKSETTELSE_VENT", "Behandlingen venter på iverksettelse", HistorikkinnslagMal.MAL_TYPE_4),
    BEH_VENT("BEH_VENT", "Behandling på vent", HistorikkinnslagMal.MAL_TYPE_4),
    BEH_KØET("BEH_KØET", "Behandlingen er satt på vent", HistorikkinnslagMal.MAL_TYPE_4),
    AVBRUTT_BEH("AVBRUTT_BEH", "Behandling er henlagt", HistorikkinnslagMal.MAL_TYPE_4),

    // Mal Type 5
    UTTAK("UTTAK", "Behandlet soknadsperiode", HistorikkinnslagMal.MAL_TYPE_5),
    FAKTA_ENDRET("FAKTA_ENDRET", "Fakta endret", HistorikkinnslagMal.MAL_TYPE_5),
    BYTT_ENHET("BYTT_ENHET", "Bytt enhet", HistorikkinnslagMal.MAL_TYPE_5),


    // Mal Type 6
    NY_INFO_FRA_TPS("NY_INFO_FRA_TPS", "Ny info fra TPS", HistorikkinnslagMal.MAL_TYPE_6),

    // Mal Type 7
    OVERSTYRT("OVERSTYRT", "Overstyrt", HistorikkinnslagMal.MAL_TYPE_7),
    FJERNET_OVERSTYRING("FJERNET_OVERSTYRING", "Overstyring er avbrutt gjeldende fra ", HistorikkinnslagMal.MAL_TYPE_7),

    // Mal Type 8
    OPPTJENING("OPPTJENING", "Behandlet opptjeningsperiode", HistorikkinnslagMal.MAL_TYPE_8),

    // Mal Type 9
    OVST_UTTAK_SPLITT("OVST_UTTAK_SPLITT", "Manuelt overstyring av uttak - splitting av periode", HistorikkinnslagMal.MAL_TYPE_9),
    FASTSATT_UTTAK_SPLITT("FASTSATT_UTTAK_SPLITT", "Manuelt fastsetting av uttak - splitting av periode", HistorikkinnslagMal.MAL_TYPE_9),

    // Mal Type 10
    FASTSATT_UTTAK("FASTSATT_UTTAK", "Manuelt fastsetting av uttak", HistorikkinnslagMal.MAL_TYPE_10),
    OVST_UTTAK("OVST_UTTAK", "Manuelt overstyring av uttak", HistorikkinnslagMal.MAL_TYPE_10),
    OVST_UTTAK_NY("OVST_UTTAK_NY", "Manuell overstyring av uttak - la til overstyring for periode", HistorikkinnslagMal.MAL_TYPE_5),
    OVST_UTTAK_FJERNET("OVST_UTTAK_FJERNE", "Manuell overstyring av uttak - fjernet overstyring for periode", HistorikkinnslagMal.MAL_TYPE_5),
    OVST_UTTAK_OPPDATERT("OVST_UTTAK_OPPDATERE", "Manuell overstyring av uttak - oppdaterte overstyring for periode", HistorikkinnslagMal.MAL_TYPE_5),

    UDEFINERT("-", "Ikke definert", null),
    ;

    private String mal;

    private static final Map<String, HistorikkinnslagType> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "HISTORIKKINNSLAG_TYPE";

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

    private String navn;

    private String kode;

    private HistorikkinnslagType(String kode, String navn, String mal) {
        this.kode = kode;
        this.navn = navn;
        this.mal = mal;
    }

    public static HistorikkinnslagType  fraKode(final String kode)  {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent HistorikkinnslagType: " + kode);
        }
        return ad;
    }

    public static Map<String, HistorikkinnslagType> kodeMap() {
        return Collections.unmodifiableMap(KODER);
    }

    @Override
    public String getNavn() {
        return navn;
    }

    @Override
    public String getKodeverk() {
        return KODEVERK;
    }

    @JsonValue
    @Override
    public String getKode() {
        return kode;
    }

    @Override
    public String getOffisiellKode() {
        return getKode();
    }

    public String getMal() {
        return mal;
    }
}
