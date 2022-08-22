package no.nav.k9.sak.web.app.tjenester.behandling.omsorgspenger;

import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.historikk.HistorikkEndretFeltType;
import no.nav.k9.kodeverk.historikk.HistorikkinnslagType;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.k9.sak.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.k9.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.k9.sak.domene.person.tps.TpsTjeneste;
import no.nav.k9.sak.historikk.HistorikkInnslagTekstBuilder;
import no.nav.k9.sak.historikk.HistorikkTjenesteAdapter;
import no.nav.k9.sak.kontrakt.uttak.ÅrskvantumFosterbarnDto;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.PersonIdent;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.Fosterbarn;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.FosterbarnRepository;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.Fosterbarna;
import no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.tjenester.ÅrskvantumTjeneste;

@ApplicationScoped
@DtoTilServiceAdapter(dto = ÅrskvantumFosterbarnDto.class, adapter = AksjonspunktOppdaterer.class)
public class ÅrskvantumFosterbarn implements AksjonspunktOppdaterer<ÅrskvantumFosterbarnDto> {


    private HistorikkTjenesteAdapter historikkTjenesteAdapter;
    private ÅrskvantumTjeneste årskvantumTjeneste;
    private FosterbarnRepository fosterbarnRepository;
    private TpsTjeneste tpsTjeneste;

    ÅrskvantumFosterbarn() {
        // for CDI proxy
    }

    @Inject
    ÅrskvantumFosterbarn(HistorikkTjenesteAdapter historikkTjenesteAdapter,
                         ÅrskvantumTjeneste årskvantumTjeneste,
                         FosterbarnRepository fosterbarnRepository,
                         TpsTjeneste tpsTjeneste) {
        this.historikkTjenesteAdapter = historikkTjenesteAdapter;
        this.årskvantumTjeneste = årskvantumTjeneste;
        this.fosterbarnRepository = fosterbarnRepository;
        this.tpsTjeneste = tpsTjeneste;
    }

    @Override
    public OppdateringResultat oppdater(ÅrskvantumFosterbarnDto dto, AksjonspunktOppdaterParameter param) {
        var fortsettBehandling = dto.getfortsettBehandling();
        Long behandlingId = param.getBehandlingId();
        Set<AktørId> fosterbarnRegistrertAllerede = fosterbarnRepository.hentHvisEksisterer(behandlingId).stream()
            .flatMap(g -> g.getFosterbarna().getFosterbarn().stream())
            .map(Fosterbarn::getAktørId)
            .collect(Collectors.toSet());
        Set<AktørId> fosterbarnOppdatert = dto.getFosterbarn().stream()
            .map(f -> tpsTjeneste.hentAktørForFnr(new PersonIdent(f.getFnr())).orElseThrow())
            .collect(Collectors.toSet());

        valider(fortsettBehandling, fosterbarnRegistrertAllerede, fosterbarnOppdatert);

        fosterbarnRepository.lagreOgFlush(behandlingId, new Fosterbarna(fosterbarnOppdatert.stream().map(Fosterbarn::new).collect(Collectors.toSet())));
        opprettHistorikkInnslag(dto, behandlingId, lagHistorikkinnslagTekstEndringFosterbarn(fosterbarnRegistrertAllerede, fosterbarnOppdatert));

        if (fortsettBehandling) {
            årskvantumTjeneste.bekreftUttaksplan(behandlingId);
            return OppdateringResultat.nyttResultat();
        } else {
            // må kjøre registerinnhenting på nytt, siden det er endring i personer på saken
            OppdateringResultat resultat = OppdateringResultat.nyttResultat();
            resultat.rekjørSteg();
            resultat.setSteg(BehandlingStegType.INNHENT_REGISTEROPP);
            return resultat;
        }
    }

    private String lagHistorikkinnslagTekstEndringFosterbarn(Set<AktørId> fosterbarnRegistrertAllerede, Set<AktørId> fosterbarnOppdatert) {
        int antallNye = fosterbarnOppdatert.stream().filter(fb -> !fosterbarnRegistrertAllerede.contains(fb)).toList().size();
        int antallFjernet = fosterbarnRegistrertAllerede.stream().filter(fb -> !fosterbarnOppdatert.contains(fb)).toList().size();
        if (antallNye > 0 && antallFjernet > 0){
            return "La til " + antallNye + " fosterbarn og fjernet " + antallFjernet + " fosterbarn";
        }
        if (antallNye > 0){
            return "La til " + antallNye + " fosterbarn";
        }
        if (antallFjernet > 0) {
            return "Fjernet " + antallFjernet + " fosterbarn";
        }
        return "Fortsetter uten endring. Det er korrekt at ingen fosterbarn er registrert.";
    }

    private static void valider(boolean fortsettBehandling, Set<AktørId> fosterbarnRegistrertAllerede, Set<AktørId> fosterbarnOppdatert) {
        boolean harEndring = !fosterbarnRegistrertAllerede.equals(fosterbarnOppdatert);
        if (fortsettBehandling && harEndring) {
            throw new IllegalArgumentException("Ugyldig input, kan ikke velge fortsett behandling og sende endring i listen av barn");
        }
        if (!fortsettBehandling && !harEndring) {
            throw new IllegalArgumentException("Ugyldig input, kan ikke ha fortsettBehandling=false og ikke ikke ha endring i listen av barn");
        }
    }

    private void opprettHistorikkInnslag(ÅrskvantumFosterbarnDto dto, Long behandlingId, String valg) {
        HistorikkInnslagTekstBuilder builder = historikkTjenesteAdapter.tekstBuilder();
        builder.medEndretFelt(HistorikkEndretFeltType.VALG, null, valg);
        builder.medBegrunnelse(dto.getBegrunnelse());
        historikkTjenesteAdapter.opprettHistorikkInnslag(behandlingId, HistorikkinnslagType.FAKTA_ENDRET);
    }
}
