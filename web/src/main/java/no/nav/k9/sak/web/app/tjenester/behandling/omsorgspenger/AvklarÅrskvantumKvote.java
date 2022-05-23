package no.nav.k9.sak.web.app.tjenester.behandling.omsorgspenger;

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
import no.nav.k9.sak.kontrakt.uttak.AvklarÅrskvantumDto;
import no.nav.k9.sak.typer.PersonIdent;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.Fosterbarn;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.FosterbarnRepository;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.Fosterbarna;
import no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.tjenester.ÅrskvantumTjeneste;

@ApplicationScoped
@DtoTilServiceAdapter(dto = AvklarÅrskvantumDto.class, adapter = AksjonspunktOppdaterer.class)
public class AvklarÅrskvantumKvote implements AksjonspunktOppdaterer<AvklarÅrskvantumDto> {

    private HistorikkTjenesteAdapter historikkTjenesteAdapter;
    private ÅrskvantumTjeneste årskvantumTjeneste;
    private FosterbarnRepository fosterbarnRepository;
    private TpsTjeneste tpsTjeneste;

    AvklarÅrskvantumKvote() {
        // for CDI proxy
    }

    @Inject
    AvklarÅrskvantumKvote(HistorikkTjenesteAdapter historikkTjenesteAdapter,
                          ÅrskvantumTjeneste årskvantumTjeneste,
                          FosterbarnRepository fosterbarnRepository,
                          TpsTjeneste tpsTjeneste) {
        this.historikkTjenesteAdapter = historikkTjenesteAdapter;
        this.årskvantumTjeneste = årskvantumTjeneste;
        this.fosterbarnRepository = fosterbarnRepository;
        this.tpsTjeneste = tpsTjeneste;
    }

    @Override
    public OppdateringResultat oppdater(AvklarÅrskvantumDto dto, AksjonspunktOppdaterParameter param) {
        var fortsettBehandling = dto.getfortsettBehandling();
        Long behandlingId = param.getBehandlingId();

        if (fortsettBehandling) {
            //Bekreft uttaksplan og fortsett behandling
            opprettHistorikkInnslag(dto, behandlingId, HistorikkinnslagType.FASTSATT_UTTAK, "Fortsett uten endring, avslåtte perioder er korrekt");
            årskvantumTjeneste.bekreftUttaksplan(behandlingId);
            return OppdateringResultat.nyttResultat(); //skulle her ønske å overstyre Aksjonspunktets tilbakehopp
        } else {
            // Oppretter fosterbarn kun dersom eksplisitt angitt av GUI
            if (dto.getFosterbarn() != null) {
                var fosterbarn = dto.getFosterbarn().stream()
                    .map(barn -> tpsTjeneste.hentAktørForFnr(new PersonIdent(barn.getFnr())).orElseThrow(() -> new IllegalArgumentException("Finner ikke fnr")))
                    .map(aktørId -> new Fosterbarn(aktørId))
                    .collect(Collectors.toSet());
                fosterbarnRepository.lagreOgFlush(param.getBehandlingId(), new Fosterbarna(fosterbarn));
            }

            // kjør steget på nytt, aka hent nye rammevedtak fra infotrygd
            opprettHistorikkInnslag(dto, behandlingId, HistorikkinnslagType.FAKTA_ENDRET, "Rammemelding er endret eller lagt til");

            OppdateringResultat resultat = OppdateringResultat.nyttResultat();
            resultat.rekjørSteg();
            //må til innhent registeropplysninger for å få med barn over 12 år når hvis det er lag til kronisk syk-rammevedtak
            resultat.setSteg(BehandlingStegType.INNHENT_REGISTEROPP);
            return resultat;
        }

    }

    private void opprettHistorikkInnslag(AvklarÅrskvantumDto dto, Long behandlingId, HistorikkinnslagType historikkinnslagType, String valg) {
        HistorikkInnslagTekstBuilder builder = historikkTjenesteAdapter.tekstBuilder();
        builder.medEndretFelt(HistorikkEndretFeltType.VALG, null, valg);
        builder.medBegrunnelse(dto.getBegrunnelse());
        historikkTjenesteAdapter.opprettHistorikkInnslag(behandlingId, historikkinnslagType);
    }
}
