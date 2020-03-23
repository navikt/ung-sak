package no.nav.k9.sak.web.app.tjenester.behandling.medlem;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.k9.sak.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.k9.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.k9.sak.domene.medlem.MedlemskapAksjonspunktTjeneste;
import no.nav.k9.sak.kontrakt.medlem.AvklarFortsattMedlemskapAksjonspunktDto;
import no.nav.k9.sak.kontrakt.medlem.AvklarFortsattMedlemskapDto;
import no.nav.k9.sak.kontrakt.medlem.BekreftedePerioderAdapter;
import no.nav.k9.sak.kontrakt.medlem.BekreftedePerioderDto;

@ApplicationScoped
@DtoTilServiceAdapter(dto = AvklarFortsattMedlemskapDto.class, adapter = AksjonspunktOppdaterer.class)
public class AvklarFortsattMedlemskapOppdaterer implements AksjonspunktOppdaterer<AvklarFortsattMedlemskapDto> {

    private MedlemskapAksjonspunktTjeneste medlemTjeneste;

    AvklarFortsattMedlemskapOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public AvklarFortsattMedlemskapOppdaterer(MedlemskapAksjonspunktTjeneste medlemTjeneste) {
        this.medlemTjeneste = medlemTjeneste;
    }

    @Override
    public OppdateringResultat oppdater(AvklarFortsattMedlemskapDto dto, AksjonspunktOppdaterParameter param) {
        validerMinstEnPeriode(dto);
        var behandlingId = param.getBehandlingId();
        var adapter = new AvklarFortsattMedlemskapAksjonspunktDto(mapTilAdapterFra(dto));
        medlemTjeneste.aksjonspunktAvklarFortsattMedlemskap(behandlingId, adapter);
        return OppdateringResultat.utenOveropp();
    }

    private List<BekreftedePerioderAdapter> mapTilAdapterFra(AvklarFortsattMedlemskapDto dto) {
        List<BekreftedePerioderAdapter> resultat = new ArrayList<>();
        dto.getBekreftedePerioder().forEach(periode -> {
            validerAksjonspunktForAvklarFortsattMedlemskap(dto, periode);

            BekreftedePerioderAdapter adapter = new BekreftedePerioderAdapter();
            if (periode.getMedlemskapManuellVurderingType() != null) {
                adapter.setMedlemskapManuellVurderingType(periode.getMedlemskapManuellVurderingType());
            }
            adapter.setAksjonspunkter(periode.getAksjonspunkter());
            adapter.setBosattVurdering(periode.getBosattVurdering());
            adapter.setErEosBorger(periode.getErEosBorger());
            adapter.setFodselsdato(periode.getFodselsdato());
            adapter.setLovligOppholdVurdering(periode.getLovligOppholdVurdering());
            adapter.setVurderingsdato(periode.getVurderingsdato());
            adapter.setOmsorgsovertakelseDato(periode.getOmsorgsovertakelseDato());
            adapter.setOppholdsrettVurdering(periode.getOppholdsrettVurdering());
            adapter.setBegrunnelse(periode.getBegrunnelse());
            resultat.add(adapter);
        });
        return resultat;
    }

    private void validerMinstEnPeriode(AvklarFortsattMedlemskapDto dto) {
        if (dto.getBekreftedePerioder().isEmpty()) {
            throw new IllegalArgumentException("Må angi minst 1 periode for å håndtere " + AksjonspunktKodeDefinisjon.AVKLAR_FORTSATT_MEDLEMSKAP_KODE);
        }
    }

    private void validerAksjonspunktForAvklarFortsattMedlemskap(AvklarFortsattMedlemskapDto dto, BekreftedePerioderDto periode) {
        if (periode.getAksjonspunkter().isEmpty()) {
            throw new IllegalArgumentException("Må angi minst 1 aksjonspunkt i tillegg for å håndtere " + AksjonspunktKodeDefinisjon.AVKLAR_FORTSATT_MEDLEMSKAP_KODE);
        }
        if (periode.getAksjonspunkter().contains(AksjonspunktKodeDefinisjon.AVKLAR_FORTSATT_MEDLEMSKAP_KODE)) {
            throw new IllegalArgumentException(
                dto.getClass().getSimpleName() + " kan ikke være selv-referende, må inneholde annet medlemskap aksjonspunkt enn " + AksjonspunktKodeDefinisjon.AVKLAR_FORTSATT_MEDLEMSKAP_KODE);
        }
    }
}
