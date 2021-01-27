package no.nav.k9.sak.domene.arbeidsforhold.aksjonspunkt;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.kodeverk.arbeidsforhold.ArbeidsforholdHandlingType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.SkjermlenkeType;
import no.nav.k9.kodeverk.historikk.HistorikkEndretFeltType;
import no.nav.k9.kodeverk.historikk.VurderArbeidsforholdHistorikkinnslag;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.k9.sak.behandlingslager.behandling.historikk.HistorikkinnslagDel;
import no.nav.k9.sak.domene.iay.modell.ArbeidsforholdOverstyring;
import no.nav.k9.sak.historikk.HistorikkTjenesteAdapter;
import no.nav.k9.sak.kontrakt.arbeidsforhold.AvklarArbeidsforholdDto;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;

@ApplicationScoped
class ArbeidsforholdHistorikkinnslagTjeneste {

    private HistorikkTjenesteAdapter historikkAdapter;
    private ArbeidsgiverHistorikkinnslag arbeidsgiverHistorikkinnslagTjeneste;

    ArbeidsforholdHistorikkinnslagTjeneste() {
        // CDI
    }

    @Inject
    ArbeidsforholdHistorikkinnslagTjeneste(HistorikkTjenesteAdapter historikkAdapter,
                                           ArbeidsgiverHistorikkinnslag arbeidsgiverHistorikkinnslagTjeneste) {
        this.historikkAdapter = historikkAdapter;
        this.arbeidsgiverHistorikkinnslagTjeneste = arbeidsgiverHistorikkinnslagTjeneste;
    }

    public void opprettHistorikkinnslag(AksjonspunktOppdaterParameter param, AvklarArbeidsforholdDto arbeidsforholdDto, Arbeidsgiver arbeidsgiver, InternArbeidsforholdRef ref, List<ArbeidsforholdOverstyring> overstyringer) {
        String arbeidsforholdNavn = arbeidsgiverHistorikkinnslagTjeneste.lagArbeidsgiverHistorikkinnslagTekst(arbeidsgiver, ref, overstyringer);
        opprettHistorikkinnslag(arbeidsforholdDto, arbeidsforholdNavn);
    }

    public void opprettHistorikkinnslag(AvklarArbeidsforholdDto arbeidsforholdDto, String arbeidsforholdNavn) {
        List<VurderArbeidsforholdHistorikkinnslag> historikkinnslagKoder = utledKoderForHistorikkinnslagdelerForArbeidsforholdSomSkalBrukes(arbeidsforholdDto);
        historikkinnslagKoder.forEach(kode -> opprettHistorikkinnslagDel(kode, arbeidsforholdDto.getBegrunnelse(), arbeidsforholdNavn));
    }

    private List<VurderArbeidsforholdHistorikkinnslag> utledKoderForHistorikkinnslagdelerForArbeidsforholdSomSkalBrukes(AvklarArbeidsforholdDto arbeidsforholdDto) {
        List<VurderArbeidsforholdHistorikkinnslag> list = new ArrayList<>();
        var handlingType = arbeidsforholdDto.getHandlingType();
        if (EnumSet.of(ArbeidsforholdHandlingType.LAGT_TIL_AV_SAKSBEHANDLER, ArbeidsforholdHandlingType.BASERT_PÅ_INNTEKTSMELDING).contains(handlingType)) {
            list.add(VurderArbeidsforholdHistorikkinnslag.LAGT_TIL_AV_SAKSBEHANDLER);
        }
        return list;
    }

    private void opprettHistorikkinnslagDel(VurderArbeidsforholdHistorikkinnslag tilVerdi, String begrunnelse, String arbeidsforholdNavn) {
        historikkAdapter.tekstBuilder().ferdigstillHistorikkinnslagDel();
        List<HistorikkinnslagDel> historikkDeler = historikkAdapter.tekstBuilder().getHistorikkinnslagDeler();
        historikkAdapter.tekstBuilder().medEndretFelt(HistorikkEndretFeltType.ARBEIDSFORHOLD, arbeidsforholdNavn, null, tilVerdi);
        historikkAdapter.tekstBuilder().medBegrunnelse(begrunnelse);
        if (!harSkjermlenke(historikkDeler)) {
            historikkAdapter.tekstBuilder().medSkjermlenke(SkjermlenkeType.FAKTA_OM_ARBEIDSFORHOLD);
        }
        historikkAdapter.tekstBuilder().ferdigstillHistorikkinnslagDel();
    }

    private boolean harSkjermlenke(List<HistorikkinnslagDel> historikkDeler) {
        return historikkDeler.stream().anyMatch(historikkDel -> historikkDel.getSkjermlenke().isPresent());
    }

}
