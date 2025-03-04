package no.nav.ung.sak.domene.behandling.steg.varselrevurdering;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.ung.sak.behandlingskontroll.*;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.ung.sak.behandlingslager.behandling.motattdokument.MottatteDokumentRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.startdato.UngdomsprogramBekreftetPeriodeEndring;
import no.nav.ung.sak.behandlingslager.behandling.startdato.UngdomsytelseStartdatoRepository;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriode;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeRepository;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.ungdomsprogram.UngdomsprogramPeriodeTjeneste;

import java.time.Period;
import java.util.List;

import static no.nav.ung.kodeverk.behandling.BehandlingStegType.VARSEL_REVURDERING;
import static no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon.AUTO_SATT_PÅ_VENT_REVURDERING;

@BehandlingStegRef(value = VARSEL_REVURDERING)
@BehandlingTypeRef
@FagsakYtelseTypeRef
@ApplicationScoped
public class VarselRevurderingStegImpl implements VarselRevurderingSteg {

    private UngdomsytelseStartdatoRepository ungdomsytelseStartdatoRepository;
    private BehandlingRepository behandlingRepository;
    private UngdomsprogramPeriodeTjeneste ungdomsprogramPeriodeTjeneste;
    private MottatteDokumentRepository mottatteDokumentRepository;
    private final Period ventePeriode;

    @Inject
    public VarselRevurderingStegImpl(BehandlingRepository behandlingRepository,
                                     UngdomsprogramPeriodeTjeneste ungdomsprogramPeriodeTjeneste,
                                     UngdomsytelseStartdatoRepository ungdomsytelseStartdatoRepository,
                                     MottatteDokumentRepository mottatteDokumentRepository,
                                     @KonfigVerdi(value = "REVURDERING_ENDRET_PERIODE_VENTEFRIST", defaultVerdi = "P14D") String ventePeriode) {
        this.behandlingRepository = behandlingRepository;
        this.ungdomsprogramPeriodeTjeneste = ungdomsprogramPeriodeTjeneste;
        this.ungdomsytelseStartdatoRepository = ungdomsytelseStartdatoRepository;
        this.mottatteDokumentRepository = mottatteDokumentRepository;
        this.ventePeriode = Period.parse(ventePeriode);
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {

        Behandling behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());

        if (harUtførtVentRevurdering(behandling)) {
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        }

        if (behandling.getBehandlingÅrsaker().isEmpty()) {
            throw VarselRevurderingStegFeil.FACTORY.manglerBehandlingsårsakPåRevurdering().toException();
        }

        final var ungdomsprogramTidslinje = ungdomsprogramPeriodeTjeneste.finnPeriodeTidslinje(behandling.getId()).compress();
        final var bekreftelser = finnBekreftelser(behandling);
        final var gyldigeDokumenter = mottatteDokumentRepository.hentGyldigeDokumenterMedFagsakId(behandling.getFagsakId());

        return VarselRevurderingAksjonspunktUtleder.utledAksjonspunkt(
                behandling.getBehandlingÅrsakerTyper(),
                ungdomsprogramTidslinje,
                gyldigeDokumenter,
                bekreftelser,
                ventePeriode,
                behandling.getAksjonspunktMedDefinisjonOptional(AUTO_SATT_PÅ_VENT_REVURDERING)

            ).map(BehandleStegResultat::utførtMedAksjonspunktResultater)
            .orElse(BehandleStegResultat.utførtUtenAksjonspunkter());

    }

    private List<UngdomsprogramBekreftetPeriodeEndring> finnBekreftelser(Behandling behandling) {
        final var ungdomsytelseStartdatoGrunnlag = ungdomsytelseStartdatoRepository.hentGrunnlag(behandling.getId());
        return ungdomsytelseStartdatoGrunnlag.stream()
            .flatMap(it -> it.getBekreftetPeriodeEndringer().stream()).toList();
    }

    private boolean harUtførtVentRevurdering(Behandling behandling) {
        return behandling.getAksjonspunktMedDefinisjonOptional(AUTO_SATT_PÅ_VENT_REVURDERING).map(Aksjonspunkt::erUtført).orElse(Boolean.FALSE);
    }
}
