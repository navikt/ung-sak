package no.nav.ung.sak.kontrakt.klage;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeName;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.ung.kodeverk.klage.KlageAvvistÅrsak;
import no.nav.ung.kodeverk.klage.KlageMedholdÅrsak;
import no.nav.ung.kodeverk.klage.KlageVurderingOmgjør;
import no.nav.ung.kodeverk.klage.KlageVurderingType;
import no.nav.ung.sak.kontrakt.aksjonspunkt.BekreftetAksjonspunktDto;

import java.time.LocalDate;
import java.util.Objects;

public abstract class KlageVurderingResultatAksjonspunktDto extends BekreftetAksjonspunktDto {

    @NotNull
    private KlageVurderingType klageVurderingType;

    @Size(max = 100000)
    @Pattern(regexp = TekstValideringRegex.FRITEKST)
    private String begrunnelse;

    // Økt størrelsen for å håndtere all fritekst som blir skrevet til klagebrev
    @Size(max = 100000)
    @Pattern(regexp = TekstValideringRegex.FRITEKST)
    private String fritekstTilBrev;

    @Size(max = 100)
    @Pattern(regexp = TekstValideringRegex.FRITEKST)
    private String klageHjemmel;

    private KlageAvvistÅrsak klageAvvistArsak;

    private KlageMedholdÅrsak klageMedholdArsak;

    private KlageVurderingOmgjør klageVurderingOmgjoer;

    @Valid
    private LocalDate vedtaksdatoPaklagdBehandling;

    private boolean erGodkjentAvMedunderskriver;

    KlageVurderingResultatAksjonspunktDto() { // NOSONAR
        // For Jackson
    }

    public KlageVurderingResultatAksjonspunktDto( // NOSONAR
            String begrunnelse,
            KlageVurderingType klageVurderingType,
            KlageMedholdÅrsak klageMedholdArsak,
            KlageAvvistÅrsak klageAvvistArsak,
            LocalDate vedtaksdatoPaklagdBehandling,
            String fritekstTilBrev,
            String klageHjemmel,
            KlageVurderingOmgjør klageVurderingOmgjoer,
            boolean erGodkjentAvMedunderskriver) {
        super(begrunnelse);
        this.klageVurderingType = klageVurderingType;
        this.begrunnelse = Objects.requireNonNull(begrunnelse, "begrunnelse");
        this.fritekstTilBrev = fritekstTilBrev;
        this.klageHjemmel = klageHjemmel;
        this.klageAvvistArsak = klageAvvistArsak;
        this.klageMedholdArsak = klageMedholdArsak;
        this.klageVurderingOmgjoer = klageVurderingOmgjoer;
        this.vedtaksdatoPaklagdBehandling = vedtaksdatoPaklagdBehandling;
        this.erGodkjentAvMedunderskriver = erGodkjentAvMedunderskriver;
    }

    public KlageVurderingType getKlageVurdering() {
        return klageVurderingType;
    }

    @Override
    public String getBegrunnelse() {
        return begrunnelse;
    }

    public String getFritekstTilBrev() {
        return fritekstTilBrev;
    }

    public String getKlageHjemmel() {
        return klageHjemmel;
    }

    public KlageAvvistÅrsak getKlageAvvistArsak() {
        return klageAvvistArsak;
    }

    public KlageMedholdÅrsak getKlageMedholdArsak() {
        return klageMedholdArsak;
    }

    public KlageVurderingOmgjør getKlageVurderingOmgjoer() {
        return klageVurderingOmgjoer;
    }

    public LocalDate getVedtaksdatoPaklagdBehandling() {
        return vedtaksdatoPaklagdBehandling;
    }

    public boolean isErGodkjentAvMedunderskriver() {
        return erGodkjentAvMedunderskriver;
    }

    @JsonIgnore
    public boolean valider() {
        if (klageVurderingType == KlageVurderingType.STADFESTE_YTELSESVEDTAK) {
            if (klageHjemmel == null) { throw new IllegalArgumentException("Hjemmel må være satt ved oversendelse av klage"); }
            return false;
        }
        return true;
    }

    @JsonTypeName(AksjonspunktKodeDefinisjon.MANUELL_VURDERING_AV_KLAGE_KODE)
    public static class KlageVurderingResultatNfpAksjonspunktDto extends KlageVurderingResultatAksjonspunktDto {


        KlageVurderingResultatNfpAksjonspunktDto() {
            super();
        }

        public KlageVurderingResultatNfpAksjonspunktDto(String begrunnelse, KlageVurderingType klageVurderingType,
                                                        KlageMedholdÅrsak klageMedholdÅrsak, KlageAvvistÅrsak klageAvvistÅrsak,
                                                        LocalDate vedtaksdatoPaklagdBehandling, String fritekstTilBrev, String klageHjemmel, KlageVurderingOmgjør klageVurderingOmgjoer) {
            super(begrunnelse, klageVurderingType, klageMedholdÅrsak, klageAvvistÅrsak, vedtaksdatoPaklagdBehandling, fritekstTilBrev, klageHjemmel, klageVurderingOmgjoer, false);
        }

    }
}
