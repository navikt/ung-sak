package no.nav.k9.sak.domene.medlem.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.SkjermlenkeType;
import no.nav.k9.kodeverk.historikk.HistorikkEndretFeltType;
import no.nav.k9.kodeverk.historikk.HistorikkEndretFeltVerdiType;
import no.nav.k9.kodeverk.historikk.HistorikkinnslagType;
import no.nav.k9.sak.behandlingslager.behandling.medlemskap.MedlemskapRepository;
import no.nav.k9.sak.behandlingslager.behandling.medlemskap.VurdertLøpendeMedlemskapBuilder;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.historikk.HistorikkInnslagTekstBuilder;
import no.nav.k9.sak.historikk.HistorikkTjenesteAdapter;
import no.nav.k9.sak.kontrakt.medlem.AvklarFortsattMedlemskapAksjonspunktDto;
import no.nav.k9.sak.kontrakt.medlem.BekreftedePerioderAdapter;

public class AvklarFortsattMedlemskapAksjonspunkt {

    interface HåndterAksjonspunkt {
        void håndter(VurdertLøpendeMedlemskapBuilder builder, BekreftedePerioderAdapter data, Long behandlingId);
    }

    private MedlemskapRepository medlemskapRepository;
    private HistorikkTjenesteAdapter historikkTjenesteAdapter;
    
    private final Map<String, HåndterAksjonspunkt> håndterMedlemskapAksjonspunkt = Map.of(
        AksjonspunktDefinisjon.AVKLAR_LOVLIG_OPPHOLD.getKode(), this::håndterAksjonspunkt5019_AvklarLovligOpphold,
        AksjonspunktDefinisjon.AVKLAR_OPPHOLDSRETT.getKode(), this::håndterAksjonspunkt5023_AvklarOppholdsrett,
        AksjonspunktDefinisjon.AVKLAR_OM_ER_BOSATT.getKode(), this::håndterAksjonspunkt5020_AvklarErBosatt,
        AksjonspunktDefinisjon.AVKLAR_GYLDIG_MEDLEMSKAPSPERIODE.getKode(), this::håndterAksjonspunkt5021_AvklarGyldigMedlemskapsperiode);

    public AvklarFortsattMedlemskapAksjonspunkt(BehandlingRepositoryProvider repositoryProvider, HistorikkTjenesteAdapter historikkTjenesteAdapter) {
        this.medlemskapRepository = repositoryProvider.getMedlemskapRepository();
        this.historikkTjenesteAdapter = historikkTjenesteAdapter;
    }

    public void oppdater(Long behandlingId, AvklarFortsattMedlemskapAksjonspunktDto adapter) {
        var perioder = adapter.getPerioder();
        validerMinstEnPeriode(adapter);

        var medlemskapBuilder = medlemskapRepository.hentBuilderFor(behandlingId);
        
        perioder.forEach(vurderingsperiode -> {
            validerAksjonspunktForAvklarFortsattMedlemskap(vurderingsperiode);

            List<String> aksjonspunkter = vurderingsperiode.getAksjonspunkter();
            validerIngenEkstraMedlemskapAksjonspunkter(aksjonspunkter, håndterMedlemskapAksjonspunkt.keySet());

            var løpendeBuilder = medlemskapBuilder.getBuilderFor(vurderingsperiode.getVurderingsdato());
            for (var aks : aksjonspunkter) {
                håndterMedlemskapAksjonspunkt.get(aks).håndter(løpendeBuilder, vurderingsperiode, behandlingId);
            }

            løpendeBuilder.medBegrunnelse(vurderingsperiode.getBegrunnelse());
            medlemskapBuilder.leggTil(løpendeBuilder);
        });
        medlemskapRepository.lagreLøpendeMedlemskapVurdering(behandlingId, medlemskapBuilder.build());
    }

    private void håndterAksjonspunkt5019_AvklarLovligOpphold(VurdertLøpendeMedlemskapBuilder builder, BekreftedePerioderAdapter data, Long behandlingId) {
        Boolean nyVerdi = data.getLovligOppholdVurdering();
        Boolean tidligereVerdi = builder.build().getLovligOppholdVurdering();

        builder.medOppholdsrettVurdering(data.getOppholdsrettVurdering())
            .medLovligOppholdVurdering(nyVerdi)
            .medErEosBorger(data.getErEosBorger());

        lagHistorikkFor(data.getBegrunnelse())
            .medEndretFelt(HistorikkEndretFeltType.OPPHOLDSRETT_IKKE_EOS,
                mapTilLovligOppholdVerdiKode(tidligereVerdi),
                mapTilLovligOppholdVerdiKode(nyVerdi));
        historikkTjenesteAdapter.opprettHistorikkInnslag(behandlingId, HistorikkinnslagType.FAKTA_ENDRET);
    }

    private void håndterAksjonspunkt5023_AvklarOppholdsrett(VurdertLøpendeMedlemskapBuilder builder, BekreftedePerioderAdapter data, Long behandlingId) {
        Boolean nyVerdi = data.getOppholdsrettVurdering();
        Boolean tidligereVerdi = builder.build().getOppholdsrettVurdering();

        builder.medOppholdsrettVurdering(nyVerdi)
            .medLovligOppholdVurdering(data.getLovligOppholdVurdering())
            .medErEosBorger(data.getErEosBorger());

        lagHistorikkFor(data.getBegrunnelse())
            .medEndretFelt(HistorikkEndretFeltType.OPPHOLDSRETT_EOS, mapTilOppholdsrettVerdiKode(tidligereVerdi), mapTilOppholdsrettVerdiKode(nyVerdi));
        historikkTjenesteAdapter.opprettHistorikkInnslag(behandlingId, HistorikkinnslagType.FAKTA_ENDRET);
    }

    private void håndterAksjonspunkt5020_AvklarErBosatt(VurdertLøpendeMedlemskapBuilder builder, BekreftedePerioderAdapter data, Long behandlingId) {
        Boolean tidligereVerdi = builder.build().getBosattVurdering();
        Boolean nyVerdi = data.getBosattVurdering();

        builder.medBosattVurdering(nyVerdi);
        lagHistorikkFor(data.getBegrunnelse()).medEndretFelt(HistorikkEndretFeltType.ER_SOKER_BOSATT_I_NORGE, tidligereVerdi, nyVerdi);
        historikkTjenesteAdapter.opprettHistorikkInnslag(behandlingId, HistorikkinnslagType.FAKTA_ENDRET);
    }

    private void håndterAksjonspunkt5021_AvklarGyldigMedlemskapsperiode(VurdertLøpendeMedlemskapBuilder builder, BekreftedePerioderAdapter data, Long behandlingId) {
        var manuellVurdering = builder.build().getMedlemsperiodeManuellVurdering();
        String tidligereVerdi = manuellVurdering != null ? manuellVurdering.getNavn() : null;
        String nyVerdi = data.getMedlemskapManuellVurderingType().getNavn();

        builder.medMedlemsperiodeManuellVurdering(data.getMedlemskapManuellVurderingType());
        lagHistorikkFor(data.getBegrunnelse()).medEndretFelt(HistorikkEndretFeltType.GYLDIG_MEDLEM_FOLKETRYGDEN, tidligereVerdi, nyVerdi);
        historikkTjenesteAdapter.opprettHistorikkInnslag(behandlingId, HistorikkinnslagType.FAKTA_ENDRET);
    }

    private HistorikkInnslagTekstBuilder lagHistorikkFor(String begrunnelse) {
        return historikkTjenesteAdapter.tekstBuilder()
            .medSkjermlenke(SkjermlenkeType.FAKTA_OM_MEDLEMSKAP)
            .medBegrunnelse(begrunnelse);
    }

    private HistorikkEndretFeltVerdiType mapTilOppholdsrettVerdiKode(Boolean harOppholdsrett) {
        if (harOppholdsrett == null) {
            return null;
        }
        return harOppholdsrett ? HistorikkEndretFeltVerdiType.OPPHOLDSRETT : HistorikkEndretFeltVerdiType.IKKE_OPPHOLDSRETT;
    }

    private HistorikkEndretFeltVerdiType mapTilLovligOppholdVerdiKode(Boolean harLovligOpphold) {
        if (harLovligOpphold == null) {
            return null;
        }
        return harLovligOpphold ? HistorikkEndretFeltVerdiType.LOVLIG_OPPHOLD : HistorikkEndretFeltVerdiType.IKKE_LOVLIG_OPPHOLD;
    }

    private void validerIngenEkstraMedlemskapAksjonspunkter(Collection<String> aksjonspunkter, Set<String> forventet) {
        if (!forventet.containsAll(aksjonspunkter)) {
            List<String> ekstras = new ArrayList<>(aksjonspunkter);
            ekstras.removeAll(forventet);
            throw new IllegalArgumentException("Støtter ikke ekstra medlemskap aksjonspunkter : " + ekstras);
        }
    }

    private void validerMinstEnPeriode(AvklarFortsattMedlemskapAksjonspunktDto adapter) {
        if (adapter.getPerioder().isEmpty()) {
            throw new IllegalArgumentException("Må angi minst 1 periode for å håndtere "
                + AksjonspunktKodeDefinisjon.AVKLAR_FORTSATT_MEDLEMSKAP_KODE);
        }
    }

    private void validerAksjonspunktForAvklarFortsattMedlemskap(BekreftedePerioderAdapter vurderingsperiode) {
        if (vurderingsperiode.getAksjonspunkter().isEmpty()) {
            throw new IllegalArgumentException("Må angi minst 1 aksjonspunkt i tillegg for å håndtere "
                + AksjonspunktKodeDefinisjon.AVKLAR_FORTSATT_MEDLEMSKAP_KODE);
        }
        if (vurderingsperiode.getAksjonspunkter().contains(AksjonspunktKodeDefinisjon.AVKLAR_FORTSATT_MEDLEMSKAP_KODE)) {
            throw new IllegalArgumentException(
                AvklarFortsattMedlemskapAksjonspunktDto.class.getSimpleName()
                    + " kan ikke være selv-referende, må inneholde annet medlemskap aksjonspunkt enn "
                    + AksjonspunktKodeDefinisjon.AVKLAR_FORTSATT_MEDLEMSKAP_KODE);
        }
    }
}
