package no.nav.k9.sak.domene.medlem.impl;

import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.kodeverk.geografisk.Landkoder;
import no.nav.k9.kodeverk.medlem.MedlemskapDekningType;
import no.nav.k9.kodeverk.medlem.MedlemskapKildeType;
import no.nav.k9.kodeverk.medlem.MedlemskapType;
import no.nav.k9.sak.domene.medlem.api.FinnMedlemRequest;
import no.nav.k9.sak.domene.medlem.api.Medlemskapsperiode;
import no.nav.k9.sak.domene.medlem.api.MedlemskapsperiodeKoder;
import no.nav.tjeneste.virksomhet.medlemskap.v2.PersonIkkeFunnet;
import no.nav.tjeneste.virksomhet.medlemskap.v2.Sikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.medlemskap.v2.informasjon.Foedselsnummer;
import no.nav.tjeneste.virksomhet.medlemskap.v2.informasjon.Medlemsperiode;
import no.nav.tjeneste.virksomhet.medlemskap.v2.informasjon.kodeverk.KildeMedTerm;
import no.nav.tjeneste.virksomhet.medlemskap.v2.informasjon.kodeverk.LovvalgMedTerm;
import no.nav.tjeneste.virksomhet.medlemskap.v2.informasjon.kodeverk.PeriodetypeMedTerm;
import no.nav.tjeneste.virksomhet.medlemskap.v2.informasjon.kodeverk.Statuskode;
import no.nav.tjeneste.virksomhet.medlemskap.v2.informasjon.kodeverk.TrygdedekningMedTerm;
import no.nav.tjeneste.virksomhet.medlemskap.v2.meldinger.HentPeriodeListeRequest;
import no.nav.tjeneste.virksomhet.medlemskap.v2.meldinger.HentPeriodeListeResponse;
import no.nav.vedtak.felles.integrasjon.felles.ws.DateUtil;
import no.nav.vedtak.felles.integrasjon.medl.MedlemConsumer;

@ApplicationScoped
public class HentMedlemskapFraRegister {

    private MedlemConsumer medlemConsumer;

    HentMedlemskapFraRegister() {
        // CDI
    }

    @Inject
    public HentMedlemskapFraRegister(MedlemConsumer medlemConsumer) {
        this.medlemConsumer = medlemConsumer;
    }

    public List<Medlemskapsperiode> finnMedlemskapPerioder(FinnMedlemRequest finnMedlemRequest) {
        HentPeriodeListeRequest request = new HentPeriodeListeRequest();
        List<Medlemskapsperiode> medlemskapsperiodeList;
        request.setIdent(new Foedselsnummer().withValue(finnMedlemRequest.getFnr()));
        request.setInkluderPerioderFraOgMed(DateUtil.convertToXMLGregorianCalendarRemoveTimezone(finnMedlemRequest.getFom()));
        request.setInkluderPerioderTilOgMed(DateUtil.convertToXMLGregorianCalendarRemoveTimezone(finnMedlemRequest.getTom()));
        request.withInkluderStatuskodeListe(
            new Statuskode().withValue(MedlemskapsperiodeKoder.PeriodeStatus.GYLD.toString()),
            new Statuskode().withValue(MedlemskapsperiodeKoder.PeriodeStatus.INNV.toString()),
            new Statuskode().withValue(MedlemskapsperiodeKoder.PeriodeStatus.UAVK.toString()));

        try {
            HentPeriodeListeResponse response = medlemConsumer.hentPeriodeListe(request);
            medlemskapsperiodeList = oversettFraPeriodeListeResponse(response);
        } catch (PersonIkkeFunnet ex) {
            throw MedlemFeil.FACTORY.feilVedKallTilMedlem(ex).toException();
        } catch (Sikkerhetsbegrensning ex) {
            throw MedlemFeil.FACTORY.fikkSikkerhetsavvikFraMedlem(ex).toException();
        }

        return medlemskapsperiodeList;
    }

    private List<Medlemskapsperiode> oversettFraPeriodeListeResponse(HentPeriodeListeResponse response) {
        return response.getPeriodeListe().stream().map(this::oversettFraPeriode).collect(Collectors.toList());
    }

    private Medlemskapsperiode oversettFraPeriode(Medlemsperiode medlemsperiode) {
        return new Medlemskapsperiode.Builder()
            .medFom(DateUtil.convertToLocalDate(medlemsperiode.getFraOgMed()))
            .medTom(DateUtil.convertToLocalDate(medlemsperiode.getTilOgMed()))
            .medDatoBesluttet(DateUtil.convertToLocalDate(medlemsperiode.getDatoBesluttet()))
            .medErMedlem(bestemErMedlem(medlemsperiode.getType()))
            .medKilde(mapTilKilde(medlemsperiode.getKilde()))
            .medDekning(mapTilDekning(medlemsperiode.getTrygdedekning()))
            .medLovvalg(mapTilLovvalg(medlemsperiode.getLovvalg()))
            .medLovvalgsland(finnLovvalgsland(medlemsperiode))
            .medStudieland(finnStudieland(medlemsperiode))
            .medMedlId(medlemsperiode.getId())
            .build();
    }

    private Landkoder finnStudieland(Medlemsperiode medlemsperiode) {
        if (medlemsperiode.getStudieinformasjon() != null
            && medlemsperiode.getStudieinformasjon().getStudieland() != null) {
            return Landkoder.fraKode(medlemsperiode.getStudieinformasjon().getStudieland().getValue());
        }
        return null;
    }

    private Landkoder finnLovvalgsland(Medlemsperiode medlemsperiode) {
        if (medlemsperiode.getLand() != null) {
            return Landkoder.fraKode(medlemsperiode.getLand().getValue());
        }
        return null;
    }

    private MedlemskapDekningType mapTilDekning(TrygdedekningMedTerm term) {
        MedlemskapDekningType dekningType = MedlemskapDekningType.UDEFINERT;
        if (term != null) {
            String strTerm = term.getValue();
            dekningType = MedlemskapsperiodeKoder.getDekningMap().get(strTerm);
            if (dekningType == null) {
                dekningType = MedlemskapDekningType.UDEFINERT;
            }
        }
        return dekningType;
    }

    private MedlemskapType mapTilLovvalg(LovvalgMedTerm term) {
        MedlemskapType medlemskapType = MedlemskapType.UDEFINERT;
        if (term != null) {
            String strTerm = term.getValue();
            if (MedlemskapsperiodeKoder.Lovvalg.ENDL.name().equals(strTerm)) {
                medlemskapType = MedlemskapType.ENDELIG;
            }
            if (MedlemskapsperiodeKoder.Lovvalg.UAVK.name().equals(strTerm)) {
                medlemskapType = MedlemskapType.UNDER_AVKLARING;
            }
            if (MedlemskapsperiodeKoder.Lovvalg.FORL.name().equals(strTerm)) {
                medlemskapType = MedlemskapType.FORELOPIG;
            }
        }
        return medlemskapType;
    }

    private MedlemskapKildeType mapTilKilde(KildeMedTerm term) {
        MedlemskapKildeType kildeType = MedlemskapKildeType.UDEFINERT;
        if (term != null) {
            String strTerm = term.getValue();
            if (strTerm != null) {
                kildeType = MedlemskapKildeType.fraKode(strTerm);
                if (kildeType == null) {
                    kildeType = MedlemskapKildeType.ANNEN;
                }
                if (MedlemskapKildeType.SRVGOSYS.equals(kildeType)) {
                    kildeType = MedlemskapKildeType.FS22;
                }
                if (MedlemskapKildeType.SRVMELOSYS.equals(kildeType)) {
                    kildeType = MedlemskapKildeType.MEDL;
                }
            }
        }
        return kildeType;
    }

    private boolean bestemErMedlem(PeriodetypeMedTerm term) {
        boolean erMedlem = false;
        if (term != null) {
            String strTerm = term.getValue();
            if (MedlemskapsperiodeKoder.PeriodeType.PMMEDSKP.name().equals(strTerm)) {
                erMedlem = true;
            }
            if (MedlemskapsperiodeKoder.PeriodeType.PUMEDSKP.name().equals(strTerm)) {
                erMedlem = false;
            }
            if (MedlemskapsperiodeKoder.PeriodeType.E500INFO.name().equals(strTerm)) {
                erMedlem = false;
            }
        }
        return erMedlem;
    }
}
