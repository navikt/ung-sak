package no.nav.ung.sak.domene.behandling.steg.varselrevurdering;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.Venteårsak;
import no.nav.ung.kodeverk.ungdomsytelse.periodeendring.UngdomsytelsePeriodeEndringType;
import no.nav.ung.sak.behandlingskontroll.*;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.BehandlingÅrsak;
import no.nav.ung.sak.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.ung.sak.behandlingslager.behandling.motattdokument.MottattDokument;
import no.nav.ung.sak.behandlingslager.behandling.motattdokument.MottatteDokumentRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.startdato.UngdomsytelseBekreftetPeriodeEndring;
import no.nav.ung.sak.behandlingslager.behandling.startdato.UngdomsytelseStartdatoRepository;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriode;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeRepository;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static no.nav.ung.kodeverk.behandling.BehandlingStegType.VARSEL_REVURDERING;
import static no.nav.ung.kodeverk.behandling.BehandlingÅrsakType.RE_HENDELSE_ENDRET_STARTDATO_UNGDOMSPROGRAM;
import static no.nav.ung.kodeverk.behandling.BehandlingÅrsakType.RE_HENDELSE_OPPHØR_UNGDOMSPROGRAM;
import static no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon.AUTO_SATT_PÅ_VENT_REVURDERING;

@BehandlingStegRef(value = VARSEL_REVURDERING)
@BehandlingTypeRef
@FagsakYtelseTypeRef
@ApplicationScoped
public class VarselRevurderingStegImpl implements VarselRevurderingSteg {

    private UngdomsytelseStartdatoRepository ungdomsytelseStartdatoRepository;
    private BehandlingRepository behandlingRepository;
    private UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository;
    private MottatteDokumentRepository mottatteDokumentRepository;

    @Inject
    public VarselRevurderingStegImpl(BehandlingRepository behandlingRepository, UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository, UngdomsytelseStartdatoRepository ungdomsytelseStartdatoRepository, UngdomsytelseStartdatoRepository startdatoRepository, MottatteDokumentRepository mottatteDokumentRepository) {
        this.behandlingRepository = behandlingRepository;
        this.ungdomsprogramPeriodeRepository = ungdomsprogramPeriodeRepository;
        this.ungdomsytelseStartdatoRepository = ungdomsytelseStartdatoRepository;
        this.mottatteDokumentRepository = mottatteDokumentRepository;
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

        final var perioder = finnUngdomsprogramperioder(behandling);
        final var gruppertePeriodeEndringerPåEndringstype = finnBekreftelserGruppertPåEndringstype(behandling);
        final var gyldigeDokumenter = mottatteDokumentRepository.hentGyldigeDokumenterMedFagsakId(behandling.getFagsakId());
        final var eksisterendeFrist = finnEksisterendeFrist(behandling);

        if (behandling.getBehandlingÅrsaker().stream().map(BehandlingÅrsak::getBehandlingÅrsakType).anyMatch(RE_HENDELSE_OPPHØR_UNGDOMSPROGRAM::equals)) {
            final var sisteBekreftetEndring = finnSisteMottatteBekreftelseForEndringstype(gruppertePeriodeEndringerPåEndringstype, gyldigeDokumenter, UngdomsytelsePeriodeEndringType.ENDRET_OPPHØRSDATO);
            if (sisteBekreftetEndring.isEmpty() || !harMatchendeSluttdato(perioder, sisteBekreftetEndring.get())) {
                return BehandleStegResultat.utførtMedAksjonspunktResultater(AksjonspunktResultat.opprettForAksjonspunktMedFrist(
                    AUTO_SATT_PÅ_VENT_REVURDERING, Venteårsak.VENTER_BEKREFTELSE_ENDRET_OPPHØR_UNGDOMSPROGRAM, eksisterendeFrist.orElse(LocalDateTime.now().plusWeeks(4))));
            }
        }

        if (behandling.getBehandlingÅrsaker().stream().map(BehandlingÅrsak::getBehandlingÅrsakType).anyMatch(RE_HENDELSE_ENDRET_STARTDATO_UNGDOMSPROGRAM::equals)) {
            final var sisteBekreftetEndring = finnSisteMottatteBekreftelseForEndringstype(gruppertePeriodeEndringerPåEndringstype, gyldigeDokumenter, UngdomsytelsePeriodeEndringType.ENDRET_OPPHØRSDATO);
            if (sisteBekreftetEndring.isEmpty() || !harMatchendeStartdato(perioder, sisteBekreftetEndring.get())) {
                return BehandleStegResultat.utførtMedAksjonspunktResultater(AksjonspunktResultat.opprettForAksjonspunktMedFrist(
                    AUTO_SATT_PÅ_VENT_REVURDERING, Venteårsak.VENTER_BEKREFTELSE_ENDRET_STARTDATO_UNGDOMSPROGRAM, eksisterendeFrist.orElse(LocalDateTime.now().plusWeeks(4))));
            }

        }

        return BehandleStegResultat.utførtUtenAksjonspunkter();

    }

    private static Optional<LocalDateTime> finnEksisterendeFrist(Behandling behandling) {
        final var eksisterendeAksjonspunkt = behandling.getAksjonspunktMedDefinisjonOptional(AUTO_SATT_PÅ_VENT_REVURDERING);
        return eksisterendeAksjonspunkt.map(Aksjonspunkt::getFristTid);
    }

    private List<DatoIntervallEntitet> finnUngdomsprogramperioder(Behandling behandling) {
        final var ungdomsprogramPeriodeGrunnlag = ungdomsprogramPeriodeRepository.hentGrunnlag(behandling.getId());
        return ungdomsprogramPeriodeGrunnlag.stream().flatMap(it -> it.getUngdomsprogramPerioder().getPerioder().stream())
            .map(UngdomsprogramPeriode::getPeriode)
            .toList();
    }

    private Map<UngdomsytelsePeriodeEndringType, List<UngdomsytelseBekreftetPeriodeEndring>> finnBekreftelserGruppertPåEndringstype(Behandling behandling) {
        final var ungdomsytelseStartdatoGrunnlag = ungdomsytelseStartdatoRepository.hentGrunnlag(behandling.getId());
        return ungdomsytelseStartdatoGrunnlag.stream()
            .flatMap(it -> it.getBekreftetPeriodeEndringer().stream())
            .collect(Collectors.groupingBy(UngdomsytelseBekreftetPeriodeEndring::getEndringType));
    }

    private static boolean harMatchendeSluttdato(List<DatoIntervallEntitet> perioder, UngdomsytelseBekreftetPeriodeEndring sisteBekreftetEndring) {
        return perioder.stream().anyMatch(p -> p.getTomDato().equals(sisteBekreftetEndring.getDato()));
    }

    private static boolean harMatchendeStartdato(List<DatoIntervallEntitet> perioder, UngdomsytelseBekreftetPeriodeEndring sisteBekreftetEndring) {
        return perioder.stream().anyMatch(p -> p.getFomDato().equals(sisteBekreftetEndring.getDato()));
    }

    private static Optional<UngdomsytelseBekreftetPeriodeEndring> finnSisteMottatteBekreftelseForEndringstype(Map<UngdomsytelsePeriodeEndringType, List<UngdomsytelseBekreftetPeriodeEndring>> gruppertePeriodeEndringerPåEndringstype, List<MottattDokument> gyldigeDokumenter, UngdomsytelsePeriodeEndringType endringType) {
        final var bekreftetEndringer = gruppertePeriodeEndringerPåEndringstype.getOrDefault(endringType, List.of());
        return bekreftetEndringer.stream()
            .max(Comparator.comparing(e -> finnMottattTidspunkt(e, gyldigeDokumenter)));
    }

    private static LocalDateTime finnMottattTidspunkt(UngdomsytelseBekreftetPeriodeEndring e, List<MottattDokument> gyldigeDokumenter) {
        return gyldigeDokumenter.stream().filter(d -> d.getJournalpostId().equals(e.getJournalpostId())).map(MottattDokument::getMottattTidspunkt).findFirst().orElseThrow();
    }

    private boolean harUtførtVentRevurdering(Behandling behandling) {
        return behandling.getAksjonspunktMedDefinisjonOptional(AUTO_SATT_PÅ_VENT_REVURDERING).map(Aksjonspunkt::erUtført).orElse(Boolean.FALSE);
    }
}
